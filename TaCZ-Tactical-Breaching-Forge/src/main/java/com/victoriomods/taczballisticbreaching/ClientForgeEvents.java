package com.victoriomods.taczballisticbreaching;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TaczTacticalBreaching.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void playSound(PlaySoundEvent event) {
        if (ClientSuppressedGunshotSilencer.shouldMute(event.getOriginalSound())) {
            event.setSound(null);
        }
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            ClientEffectRenderSafety.logStartupOnce();
            BulletDecalManager.render(event.getPoseStack(), event.getCamera());
            ClientBloodHitFxHandler.render(event.getPoseStack(), event.getCamera());
            BurnMarkManager.render(event.getPoseStack(), event.getCamera());
            GlassShardDebrisManager.render(event.getPoseStack(), event.getCamera());
            PhysicalDebrisManager.render(event.getPoseStack(), event.getCamera());
            ClientIndoorSmokeManager.render(event.getPoseStack(), event.getCamera());
            ClientMuzzleSmokeRenderer.render(event.getPoseStack(), event.getCamera());
            ClientShockwaveDustManager.render(event.getPoseStack(), event.getCamera());
            ClientNightstickFlashlightHandler.render(event.getPoseStack(), event.getCamera());
            DynamicGunLightManager.render(event.getPoseStack(), event.getCamera());
        }
    }

    @SubscribeEvent
    public static void renderGui(RenderGuiOverlayEvent.Post event) {
        ClientSuppressionEffectsHandler.render(event.getGuiGraphics());
        ClientFlashbangFxHandler.render(event.getGuiGraphics());
        ClientGasMaskHandler.render(event.getGuiGraphics());
        ClientWeaponJamHandler.render(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientScreenDustHandler.tick();
            ClientCameraShakeHandler.tick();
            ClientWeaponHeatHandler.tick();
            ClientGunshotAcousticsHandler.tick();
        ClientDistantGunshotHandler.tick();
        ClientBloodHitFxHandler.tick();
        ClientIndoorSmokeManager.tick();
        ClientMuzzleSmokeRenderer.tick();
            ClientShockwaveDustManager.tick();
            ClientNightstickFlashlightHandler.tick();
            ClientWeaponJamHandler.tick();
            ClientGasMaskHandler.tick();
            ClientSuppressionEffectsHandler.tick();
            ClientFlashbangFxHandler.tick();
        }
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles event) {
        ClientCameraShakeHandler.ShakeOffset shake = ClientCameraShakeHandler.offset((float) event.getPartialTick());
        if (shake.roll() != 0.0F || shake.pitch() != 0.0F) {
            event.setRoll(event.getRoll() + shake.roll());
            event.setPitch(event.getPitch() + shake.pitch());
        }
    }

    @SubscribeEvent
    public static void explosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide()) {
            ClientCameraShakeHandler.explosion(event.getExplosion().getPosition());
            ClientIndoorSmokeManager.addExplosionSmoke(event.getExplosion().getPosition());
            ClientShockwaveDustManager.explosion(event.getExplosion().getPosition());
            ClientNightstickFlashlightHandler.explosion(event.getExplosion().getPosition());
            BurnMarkManager.addFromExplosion(event.getExplosion().getPosition());
            ClientSuppressionEffectsHandler.triggerExplosion(event.getExplosion().getPosition());
        }
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        BulletDecalManager.clear();
        BurnMarkManager.clear();
        GlassShardDebrisManager.clear();
        PhysicalDebrisManager.clear();
        ClientBloodHitFxHandler.clear();
        DynamicGunLightManager.clear();
        ClientScreenDustHandler.clear();
        ClientCameraShakeHandler.clear();
        ClientWeaponHeatHandler.clear();
        ClientGunshotAcousticsHandler.clear();
        ClientDistantGunshotHandler.clear();
        ClientIndoorSmokeManager.clear();
        ClientMuzzleSmokeRenderer.clear();
        ClientShockwaveDustManager.clear();
        ClientNightstickFlashlightHandler.clear();
        ClientWeaponJamHandler.clear();
        ClientSuppressionEffectsHandler.clear();
        ClientFlashbangFxHandler.clear();
        ClientHandcuffState.clear();
    }
}
