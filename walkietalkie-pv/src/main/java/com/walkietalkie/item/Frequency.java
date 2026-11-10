package com.walkietalkie.item;

import com.walkietalkie.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/** Frequency is stored in hundredths: 10000 = 100.00, 50000 = 500.00. */
public final class Frequency {
    public static final int MIN = 10000;
    public static final int MAX = 50000;
    private static final String KEY = "Frequency";

    private Frequency() {}

    public static int clamp(int v) {
        return Math.max(MIN, Math.min(MAX, v));
    }

    public static boolean isWalkie(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.WALKIE_TALKIE.get());
    }

    public static int get(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(KEY, Tag.TAG_INT)) return MIN;
        return clamp(tag.getInt(KEY));
    }

    public static void set(ItemStack stack, int value) {
        stack.getOrCreateTag().putInt(KEY, clamp(value));
    }

    public static String format(int value) {
        return String.format(Locale.ROOT, "%d.%02d", value / 100, value % 100);
    }
}
