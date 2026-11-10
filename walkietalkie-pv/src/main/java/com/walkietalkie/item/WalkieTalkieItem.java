package com.walkietalkie.item;

import com.walkietalkie.client.ClientScreens;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WalkieTalkieItem extends Item {
    public WalkieTalkieItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            int freq = Frequency.get(stack);
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientScreens.openTuner(hand, freq));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.walkietalkie.walkie_talkie.frequency",
                Frequency.format(Frequency.get(stack))).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("item.walkietalkie.walkie_talkie.hint").withStyle(ChatFormatting.GRAY));
    }
}
