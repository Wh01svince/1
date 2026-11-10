package com.walkietalkie;

import com.walkietalkie.net.ModNetwork;
import com.walkietalkie.pv.WalkieTalkieAddon;
import com.walkietalkie.radio.RadioRegistry;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import su.plo.voice.api.server.PlasmoVoiceServer;

@Mod(WalkieTalkieMod.MOD_ID)
public final class WalkieTalkieMod {
    public static final String MOD_ID = "walkietalkie";

    // Addon class annotated with @Addon (Plasmo Voice)
    private final WalkieTalkieAddon addon = new WalkieTalkieAddon();

    public WalkieTalkieMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        modBus.addListener(this::addToCreativeTab);

        ModNetwork.register();
        MinecraftForge.EVENT_BUS.register(RadioRegistry.class);

        PlasmoVoiceServer.getAddonsLoader().load(addon);
    }

    private void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.WALKIE_TALKIE);
        }
    }
}
