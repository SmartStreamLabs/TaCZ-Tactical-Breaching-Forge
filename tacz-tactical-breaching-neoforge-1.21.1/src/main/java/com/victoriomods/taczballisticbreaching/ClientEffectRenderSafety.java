package com.victoriomods.taczballisticbreaching;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;

@OnlyIn(Dist.CLIENT)
public final class ClientEffectRenderSafety {
    private static Boolean sodiumLikeLoaded;
    private static Boolean irisLikeLoaded;
    private static boolean startupLogged;

    private ClientEffectRenderSafety() {
    }

    public static void logStartupOnce() {
        if (startupLogged) {
            return;
        }
        startupLogged = true;
        TaczTacticalBreaching.LOGGER.info(
                "TaCZ Tactical Breaching effect rendering: sodiumLikeLoaded={}, irisLikeLoaded={}, sodiumSafeRendering={}, safeRenderingActive={}",
                isSodiumLikeLoaded(),
                isIrisLikeLoaded(),
                BallisticBlockConfig.SODIUM_SAFE_RENDERING.get(),
                isSafeRenderingMode()
        );
    }

    public static void beginDepthTestedTranslucent() {
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
    }

    public static void endDepthTestedTranslucent() {
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static boolean canRenderAt(Vec3 cameraPos, Vec3 target) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || cameraPos == null || target == null) {
            return false;
        }

        Vec3 end = target;
        Vec3 delta = target.subtract(cameraPos);
        if (delta.lengthSqr() > 1.0E-6D) {
            end = target.subtract(delta.normalize().scale(0.025D));
        }

        BlockHitResult blockHit = level.clip(new ClipContext(
                cameraPos,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                minecraft.player
        ));
        if (blockHit.getType() == HitResult.Type.BLOCK && blockHit.getLocation().distanceToSqr(target) > 0.075D * 0.075D) {
            return false;
        }
        return !hasEntityOccluder(level, minecraft.player, cameraPos, end);
    }

    public static boolean isSafeRenderingMode() {
        return BallisticBlockConfig.SODIUM_SAFE_RENDERING.get() || isSodiumLikeLoaded() || isIrisLikeLoaded();
    }

    private static boolean hasEntityOccluder(ClientLevel level, Entity localPlayer, Vec3 start, Vec3 end) {
        AABB rayBounds = new AABB(start, end).inflate(0.2D);
        for (Entity entity : level.getEntities((Entity) null, rayBounds, entity -> entity != localPlayer && entity.isAlive() && entity.isPickable())) {
            if (entity.getBoundingBox().inflate(0.025D).clip(start, end).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSodiumLikeLoaded() {
        if (sodiumLikeLoaded == null) {
            sodiumLikeLoaded = ModList.get().isLoaded("sodium")
                    || ModList.get().isLoaded("embeddium")
                    || ModList.get().isLoaded("rubidium");
        }
        return sodiumLikeLoaded;
    }

    private static boolean isIrisLikeLoaded() {
        if (irisLikeLoaded == null) {
            irisLikeLoaded = ModList.get().isLoaded("iris")
                    || ModList.get().isLoaded("oculus");
        }
        return irisLikeLoaded;
    }
}
