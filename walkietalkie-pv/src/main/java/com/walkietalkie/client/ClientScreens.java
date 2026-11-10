package com.walkietalkie.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

public final class ClientScreens {
    private ClientScreens() {}

    public static void openTuner(InteractionHand hand, int frequency) {
        Minecraft.getInstance().setScreen(new WalkieTalkieScreen(hand, frequency));
    }
}
