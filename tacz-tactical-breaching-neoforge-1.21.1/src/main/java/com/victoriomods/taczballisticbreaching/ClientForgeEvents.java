package com.victoriomods.taczballisticbreaching;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.world.phys.Vec3;

@EventBusSubscriber(modid = TaczTacticalBreaching.MOD_ID, value = Dist.CLIENT)
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
    public static void renderGui(RenderGuiEvent.Post event) {
        ClientSuppressionEffectsHandler.render(event.getGuiGraphics());
        ClientFlashbangFxHandler.render(event.getGuiGraphics());
        ClientGasMaskHandler.render(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
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
        ClientSuppressionEffectsHandler.tick();
        ClientFlashbangFxHandler.tick();
        ClientGasMaskHandler.tick();
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
            Vec3 position = event.getExplosion().center();
            ClientCameraShakeHandler.explosion(position);
            ClientIndoorSmokeManager.addExplosionSmoke(position);
            ClientShockwaveDustManager.explosion(position);
            ClientNightstickFlashlightHandler.explosion(position);
            BurnMarkManager.addFromExplosion(position);
            ClientSuppressionEffectsHandler.triggerExplosion(position);
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
        ClientSuppressionEffectsHandler.clear();
        ClientFlashbangFxHandler.clear();
        ClientHandcuffState.clear();
    }
}
