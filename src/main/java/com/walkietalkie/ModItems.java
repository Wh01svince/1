package com.walkietalkie;

import com.walkietalkie.item.WalkieTalkieItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, WalkieTalkieMod.MOD_ID);

    public static final RegistryObject<Item> WALKIE_TALKIE =
            ITEMS.register("walkie_talkie", () -> new WalkieTalkieItem(new Item.Properties().stacksTo(1)));

    private ModItems() {}
}
