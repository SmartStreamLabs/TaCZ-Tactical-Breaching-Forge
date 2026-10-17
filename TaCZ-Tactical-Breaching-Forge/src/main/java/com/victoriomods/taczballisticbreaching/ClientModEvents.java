package com.victoriomods.taczballisticbreaching;

import com.tacz.guns.init.ModCreativeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import java.io.IOException;

@Mod.EventBusSubscriber(modid = TaczTacticalBreaching.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        ClientNightstickFlashlightHandler.registerKeys(event);
        ClientWeaponJamHandler.registerKeys(event);
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        ClientFlashlightSpotlight.registerShader(event);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenus.MAGAZINE_LOADER.get(), MagazineLoaderScreen::new));
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SHELL_CASING.get(), ShellCasingRenderer::new);
        event.registerEntityRenderer(ModEntities.DROPPED_MAGAZINE.get(), DroppedMagazineRenderer::new);
        event.registerEntityRenderer(ModEntities.FRAG_GRENADE.get(), FragGrenadeRenderer::new);
        event.registerEntityRenderer(ModEntities.FLASHBANG_GRENADE.get(), FlashbangGrenadeRenderer::new);
    }

    @SubscribeEvent
    public static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == ModCreativeTabs.OTHER_TAB.get()) {
            event.accept(ModItems.FRAG_GRENADE.get());
            event.accept(ModItems.FLASHBANG_GRENADE.get());
            event.accept(ModItems.DROPPED_MAGAZINE.get());
            event.accept(ModItems.HANDCUFFS.get());
            event.accept(ModItems.HANDCUFF_KEY.get());
            event.accept(ModItems.SOFT_ARMOR_PLATE.get());
            event.accept(ModItems.CERAMIC_PLATE.get());
            event.accept(ModItems.HEAVY_PLATE.get());
            event.accept(ModItems.MILITARY_GRADE_PLATE.get());
            event.accept(ModItems.GAS_MASK.get());
            event.accept(ModItems.BASIC_FILTER.get());
            event.accept(ModItems.MILITARY_FILTER.get());
            event.accept(ModItems.ADVANCED_NBC_FILTER.get());
        }
    }
}
