package com.victoriomods.taczballisticbreaching;

import com.tacz.guns.init.ModCreativeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.bus.api.SubscribeEvent;

@EventBusSubscriber(modid = TaczTacticalBreaching.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        ClientNightstickFlashlightHandler.registerKeys(event);
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.MAGAZINE_LOADER.get(), MagazineLoaderScreen::new);
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
            event.accept(ModItems.GAS_MASK.get());
            event.accept(ModItems.BASIC_FILTER.get());
            event.accept(ModItems.MILITARY_FILTER.get());
            event.accept(ModItems.ADVANCED_NBC_FILTER.get());
        }
    }
}
