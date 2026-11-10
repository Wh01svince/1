package com.walkietalkie.radio;

import com.walkietalkie.item.Frequency;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

/**
 * Snapshot of who can talk / listen on which frequency. Rebuilt on the main thread every 5 ticks so the
 * Plasmo Voice audio threads can read it without touching Minecraft objects.
 *
 *  - talk:   the walkie talkie must be in main hand or off hand
 *  - listen: a walkie talkie anywhere in the inventory
 */
public final class RadioRegistry {
    private static volatile Map<UUID, Integer> talkers = Map.of();
    private static volatile Map<Integer, Set<UUID>> listeners = Map.of();
    private static int tick;

    private RadioRegistry() {}

    public static Integer talkerFrequency(UUID player) {
        return talkers.get(player);
    }

    public static Set<UUID> listenersOn(int frequency) {
        return listeners.getOrDefault(frequency, Set.of());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || (++tick % 5) != 0) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        Map<UUID, Integer> t = new HashMap<>();
        Map<Integer, Set<UUID>> l = new HashMap<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ItemStack main = p.getMainHandItem();
            ItemStack off = p.getOffhandItem();
            if (Frequency.isWalkie(main)) t.put(p.getUUID(), Frequency.get(main));
            else if (Frequency.isWalkie(off)) t.put(p.getUUID(), Frequency.get(off));

            Inventory inv = p.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (Frequency.isWalkie(s)) {
                    l.computeIfAbsent(Frequency.get(s), k -> new HashSet<>()).add(p.getUUID());
                }
            }
        }
        talkers = t;
        listeners = l;
    }
}
