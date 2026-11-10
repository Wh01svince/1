package com.walkietalkie.radio;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Makes a 48 kHz mono voice sound like a handheld radio:
 * band-pass 450-2600 Hz, sample-rate reduction (grit), soft clipping, bit crush, hiss and random crackle.
 * Stateful: use one instance per speaking session and feed it consecutive frames.
 */
public final class RadioEffect {
    private static final double FS = 48000.0;
    private static final int HOLD = 4;              // 48k -> 12k "digital" grit
    private static final float DRIVE = 3.2f;
    private static final float HISS = 0.030f;
    private static final float CRACKLE_PROB = 0.0004f;

    private final Biquad hp1 = Biquad.highPass(FS, 450, 0.707);
    private final Biquad hp2 = Biquad.highPass(FS, 450, 0.707);
    private final Biquad lp1 = Biquad.lowPass(FS, 2600, 0.707);
    private final Biquad lp2 = Biquad.lowPass(FS, 2600, 0.707);
    private int holdCounter;
    private float held;
    private float noise;

    /** Processes one frame in place. */
    public void process(short[] pcm) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < pcm.length; i++) {
            float x = pcm[i] / 32768f;
            x = hp1.process(x);
            x = hp2.process(x);
            x = lp1.process(x);
            x = lp2.process(x);

            if ((holdCounter++ % HOLD) == 0) held = x;
            x = held;

            x = (float) Math.tanh(x * DRIVE) * 0.85f;
            x = Math.round(x * 256f) / 256f;

            noise = noise * 0.6f + (rnd.nextFloat() * 2f - 1f) * 0.4f;
            x += noise * HISS;
            if (rnd.nextFloat() < CRACKLE_PROB) x += (rnd.nextFloat() - 0.5f) * 0.4f;

            pcm[i] = clip(x * 0.9f);
        }
    }

    /** End-of-transmission "roger beep" followed by a short fading squelch burst (6 frames = 120 ms). */
    public List<short[]> tail() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<short[]> frames = new ArrayList<>();
        int n = 960;
        for (int f = 0; f < 6; f++) {
            short[] frame = new short[n];
            if (f < 2) { // 40 ms beep at 1400 Hz with tiny fades
                for (int i = 0; i < n; i++) {
                    int t = f * n + i;
                    double env = Math.min(1.0, Math.min(t, 1920 - t) / 120.0);
                    frame[i] = (short) (Math.sin(2 * Math.PI * 1400.0 * t / FS) * 0.28 * env * 32767);
                }
                process(frame);
            } else {     // decaying static
                float amp = 0.10f * (1f - (f - 2) / 4f);
                for (int i = 0; i < n; i++) {
                    noise = noise * 0.5f + (rnd.nextFloat() * 2f - 1f) * 0.5f;
                    frame[i] = clip(noise * amp);
                }
            }
            frames.add(frame);
        }
        return frames;
    }

    private static short clip(float x) {
        int v = Math.round(x * 32767f);
        return (short) Math.max(-32768, Math.min(32767, v));
    }
}
