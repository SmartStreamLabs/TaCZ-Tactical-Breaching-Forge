package com.victoriomods.taczballisticbreaching;

import com.tacz.guns.init.ModCreativeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TaczTacticalBreaching.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        ClientNightstickFlashlightHandler.registerKeys(event);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SHELL_CASING.get(), ShellCasingRenderer::new);
        event.registerEntityRenderer(ModEntities.FRAG_GRENADE.get(), FragGrenadeRenderer::new);
        event.registerEntityRenderer(ModEntities.FLASHBANG_GRENADE.get(), FlashbangGrenadeRenderer::new);
    }

    @SubscribeEvent
    public static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == ModCreativeTabs.OTHER_TAB.get()) {
            event.accept(ModItems.FRAG_GRENADE.get());
            event.accept(ModItems.FLASHBANG_GRENADE.get());
        }
    }
}
