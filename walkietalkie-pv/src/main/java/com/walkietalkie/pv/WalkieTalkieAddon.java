package com.walkietalkie.pv;

import com.walkietalkie.radio.RadioEffect;
import com.walkietalkie.radio.RadioRegistry;
import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.audio.codec.AudioDecoder;
import su.plo.voice.api.audio.codec.AudioEncoder;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.capture.ServerActivation;
import su.plo.voice.api.server.audio.line.ServerSourceLine;
import su.plo.voice.api.server.audio.source.ServerDirectSource;
import su.plo.voice.api.server.player.VoiceServerPlayer;
import su.plo.voice.proto.packets.tcp.serverbound.PlayerAudioEndPacket;
import su.plo.voice.proto.packets.udp.serverbound.PlayerAudioPacket;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;

/**
 * Plasmo Voice addon:
 *  - registers a new push-to-talk activation ("Walkie Talkie") that appears in the Plasmo Voice menu
 *    where each player binds their own key
 *  - takes the voice from that activation, applies a radio effect, and forwards it as a private
 *    (direct) source to every player carrying a walkie talkie on the same frequency, at any distance
 */
@Addon(
        id = "pv-addon-walkietalkie",
        name = "Walkie Talkie",
        version = "1.0.0",
        authors = {"WalkieTalkie"}
)
public final class WalkieTalkieAddon implements AddonInitializer {
    public static final String ACTIVATION_NAME = "walkie_talkie";
    public static final String PERMISSION = "pv.addon.walkietalkie.use";
    private static final long SESSION_TIMEOUT_MS = 700;
    private static final long REFRESH_INTERVAL_MS = 250;

    @InjectPlasmoVoice
    private PlasmoVoiceServer voiceServer;

    private ServerActivation activation;
    private ServerSourceLine sourceLine;
    private ScheduledExecutorService scheduler;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    @Override
    public void onAddonInitialize() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "walkietalkie-radio");
            t.setDaemon(true);
            return t;
        });

        registerLanguages();
        PermissionCompat.registerDefaultTrue(voiceServer, PERMISSION);

        try (InputStream actIcon = open("icons/walkie_talkie.png");
             InputStream lineIcon = open("icons/walkie_talkie.png")) {

            sourceLine = voiceServer.getSourceLineManager().createBuilder(
                    this,
                    ACTIVATION_NAME,
                    "pv.addon.walkietalkie.line",
                    lineIcon,
                    20
            ).build();

            activation = voiceServer.getActivationManager().createBuilder(
                    this,
                    ACTIVATION_NAME,
                    "pv.addon.walkietalkie.activation",
                    actIcon,
                    PERMISSION,
                    20
            ).build();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load walkie talkie icon", e);
        }

        activation.onPlayerActivation(this::onAudio);
        activation.onPlayerActivationEnd(this::onAudioEnd);

        scheduler.scheduleWithFixedDelay(this::sweepTimedOut, 500, 250, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onAddonShutdown() {
        if (scheduler != null) scheduler.shutdownNow();
        for (Session s : sessions.values()) {
            synchronized (s) {
                s.finish(s.lastSeq + 1);
            }
        }
        sessions.clear();
    }

    // ------------------------------------------------------------------ languages

    private void registerLanguages() {
        File addonFolder = new File(voiceServer.getMinecraftServer().getConfigsFolder(), "pv-addon-walkietalkie");
        try {
            voiceServer.getLanguages().register(this::getLanguageResource, new File(addonFolder, "languages"));
        } catch (Exception e) {
            System.err.println("[WalkieTalkie] Failed to register languages: " + e);
        }
    }

    private InputStream getLanguageResource(String resourcePath) throws IOException {
        return getClass().getClassLoader().getResourceAsStream("walkietalkie_pv/" + resourcePath);
    }

    private InputStream open(String path) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("walkietalkie_pv/" + path);
        if (is == null) throw new IOException("Missing resource " + path);
        return is;
    }

    // ------------------------------------------------------------------ activation callbacks

    private ServerActivation.Result onAudio(VoiceServerPlayer player, PlayerAudioPacket packet) {
        UUID id = uuidOf(player);
        Integer frequency = RadioRegistry.talkerFrequency(id);

        if (frequency == null) { // not holding a walkie talkie -> nothing is transmitted
            Session old = sessions.remove(id);
            if (old != null) endSession(old);
            return ServerActivation.Result.HANDLED;
        }

        Session session = sessions.computeIfAbsent(id, k -> new Session(id));
        session.handle(frequency, packet);
        return ServerActivation.Result.HANDLED;
    }

    private ServerActivation.Result onAudioEnd(VoiceServerPlayer player, PlayerAudioEndPacket packet) {
        Session session = sessions.remove(uuidOf(player));
        if (session != null) endSession(session);
        return ServerActivation.Result.HANDLED;
    }

    // VERIFY: McPlayer#getGameProfile().getId() -- if your API version names it differently, adjust here only.
    private static UUID uuidOf(VoiceServerPlayer player) {
        return player.getInstance().getGameProfile().getId();
    }

    // ------------------------------------------------------------------ session end / sweeper

    private void sweepTimedOut() {
        try {
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, Session> e : sessions.entrySet()) {
                Session s = e.getValue();
                if (now - s.lastPacketMs > SESSION_TIMEOUT_MS && sessions.remove(e.getKey(), s)) {
                    endSession(s);
                }
            }
        } catch (Throwable t) {
            System.err.println("[WalkieTalkie] sweeper error: " + t);
        }
    }

    /** Plays the roger beep + squelch tail at 20 ms intervals, then closes the sources. */
    private void endSession(Session s) {
        final List<short[]> frames;
        final long base;
        synchronized (s) {
            if (s.ending) return;
            s.ending = true;
            frames = s.effect.tail();
            base = s.lastSeq;
        }
        for (int i = 0; i < frames.size(); i++) {
            final int idx = i;
            scheduler.schedule(() -> {
                synchronized (s) {
                    byte[] frame = s.encode(frames.get(idx));
                    if (frame != null) s.broadcast(frame, base + 1 + idx);
                }
            }, 20L * i, TimeUnit.MILLISECONDS);
        }
        scheduler.schedule(() -> {
            synchronized (s) {
                s.finish(base + 1 + frames.size());
            }
        }, 20L * frames.size() + 20, TimeUnit.MILLISECONDS);
    }

    // ------------------------------------------------------------------ one speaking session

    private final class Session {
        final UUID speaker;
        final AudioDecoder decoder = voiceServer.createOpusDecoder(false); // false = mono
        final AudioEncoder encoder = voiceServer.createOpusEncoder(false);
        final RadioEffect effect = new RadioEffect();
        final Map<UUID, ServerDirectSource> sources = new HashMap<>();

        volatile long lastPacketMs = System.currentTimeMillis();
        long lastSeq;
        long lastRefresh;
        int currentFrequency = -1;
        boolean ending;
        boolean closed;

        Session(UUID speaker) {
            this.speaker = speaker;
        }

        synchronized void handle(int frequency, PlayerAudioPacket packet) {
            if (ending || closed) return;
            long now = System.currentTimeMillis();
            lastPacketMs = now;
            lastSeq = packet.getSequenceNumber();

            if (frequency != currentFrequency || now - lastRefresh > REFRESH_INTERVAL_MS) {
                refreshReceivers(frequency);
                currentFrequency = frequency;
                lastRefresh = now;
            }
            if (sources.isEmpty()) return;

            byte[] out = transform(packet.getData());
            if (out != null) broadcast(out, lastSeq);
        }

        /** Keeps one direct source per player who currently has a walkie talkie tuned to the frequency. */
        private void refreshReceivers(int frequency) {
            Set<UUID> wanted = new HashSet<>(RadioRegistry.listenersOn(frequency));
            wanted.remove(speaker);

            Iterator<Map.Entry<UUID, ServerDirectSource>> it = sources.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, ServerDirectSource> e = it.next();
                if (!wanted.contains(e.getKey())) {
                    e.getValue().remove();
                    it.remove();
                }
            }
            for (UUID id : wanted) {
                if (sources.containsKey(id)) continue;
                voiceServer.getPlayerManager().getPlayerById(id).ifPresent(vp ->
                        sources.put(id, sourceLine.createDirectSource(vp, false)));
            }
        }

        /** decrypt -> opus decode -> radio effect -> opus encode -> encrypt */
        private byte[] transform(byte[] encryptedFrame) {
            try {
                var encryption = voiceServer.getDefaultEncryption();
                short[] pcm = decoder.decode(encryption.decrypt(encryptedFrame));
                effect.process(pcm);
                return encryption.encrypt(encoder.encode(pcm));
            } catch (Exception e) {
                return null;
            }
        }

        byte[] encode(short[] pcm) {
            try {
                var encryption = voiceServer.getDefaultEncryption();
                return encryption.encrypt(encoder.encode(pcm));
            } catch (Exception e) {
                return null;
            }
        }

        void broadcast(byte[] frame, long sequence) {
            for (ServerDirectSource source : sources.values()) {
                source.sendAudioFrame(frame, sequence);
            }
        }

        void finish(long endSequence) {
            if (closed) return;
            closed = true;
            for (ServerDirectSource source : sources.values()) {
                source.sendAudioEnd(endSequence);
                source.remove();
            }
            sources.clear();
            decoder.close();
            encoder.close();
        }
    }
}
