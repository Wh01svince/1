package com.walkietalkie.net;

import com.walkietalkie.item.Frequency;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: set the frequency of the walkie talkie held in the given hand. */
public record SetFrequencyPacket(int frequency, boolean mainHand) {

    public static void encode(SetFrequencyPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.frequency);
        buf.writeBoolean(msg.mainHand);
    }

    public static SetFrequencyPacket decode(FriendlyByteBuf buf) {
        return new SetFrequencyPacket(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(SetFrequencyPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            InteractionHand hand = msg.mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!Frequency.isWalkie(stack)) return;

            Frequency.set(stack, msg.frequency); // server-side clamp to 100.00 - 500.00
            player.displayClientMessage(Component.translatable(
                    "message.walkietalkie.frequency_set", Frequency.format(Frequency.get(stack))), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK,
                    SoundSource.PLAYERS, 0.6f, 1.6f);
        });
        ctx.setPacketHandled(true);
    }
}
