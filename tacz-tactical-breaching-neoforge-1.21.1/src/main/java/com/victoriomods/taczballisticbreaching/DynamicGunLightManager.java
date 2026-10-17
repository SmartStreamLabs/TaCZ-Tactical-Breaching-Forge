package com.victoriomods.taczballisticbreaching;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Iterator;

@OnlyIn(Dist.CLIENT)
public final class DynamicGunLightManager {
    private static final int MAX_ACTIVE_LIGHTS = 36;
    private static final ArrayDeque<DynamicLightFlash> LIGHTS = new ArrayDeque<>();

    private DynamicGunLightManager() {
    }

    public static void addMuzzleFlash(Vec3 position, MuzzleSmokePacket packet) {
        if (!BallisticBlockConfig.ENABLE_DYNAMIC_GUN_LIGHTING.get()) {
            return;
        }
        double intensity = BallisticBlockConfig.MUZZLE_FLASH_LIGHT_INTENSITY.get();
        MuzzleSmokeNetwork.WeaponKind kind = MuzzleSmokeNetwork.WeaponKind.byId(packet.weaponKind());
        intensity *= switch (kind) {
            case PISTOL -> 0.72D;
            case RIFLE -> 1.0D;
            case SHOTGUN, SNIPER -> 1.55D;
        };
        if (packet.highCaliber()) {
            intensity *= 1.25D;
        }
        if (packet.suppressed() && BallisticBlockConfig.REDUCED_LIGHTING_FOR_SUPPRESSORS.get()) {
            intensity *= 0.28D;
        }
        add(position, 1.0F, 0.55F, 0.16F, intensity, 95L, 1.0D);
    }

    public static void addSparkFlash(Vec3 position) {
        if (!BallisticBlockConfig.ENABLE_DYNAMIC_GUN_LIGHTING.get() || !BallisticBlockConfig.ENABLE_SPARK_LIGHTING.get()) {
            return;
        }
        add(position, 1.0F, 0.78F, 0.18F, BallisticBlockConfig.SPARK_LIGHT_INTENSITY.get(), 80L, 0.45D);
    }

    public static void addBreachingFlash(Vec3 position, ImpactMaterial material, boolean highCaliber) {
        if (!BallisticBlockConfig.ENABLE_DYNAMIC_GUN_LIGHTING.get()) {
            return;
        }
        double intensity = BallisticBlockConfig.MUZZLE_FLASH_LIGHT_INTENSITY.get() * (highCaliber ? 1.6D : 1.15D);
        if (material == ImpactMaterial.METAL) {
            add(position, 1.0F, 0.68F, 0.16F, intensity, 130L, 0.85D);
        } else if (material == ImpactMaterial.GLASS) {
            add(position, 0.78F, 0.9F, 1.0F, intensity * 0.85D, 120L, 0.65D);
        } else {
            add(position, 1.0F, 0.55F, 0.2F, intensity * 0.72D, 110L, 0.7D);
        }
    }

    public static void render(PoseStack poseStack, Camera camera) {
        if (!BallisticBlockConfig.ENABLE_DYNAMIC_GUN_LIGHTING.get() || LIGHTS.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        cleanup(now);
        Vec3 cameraPos = camera.getPosition();
        double maxDistance = Math.max(8.0D, BallisticBlockConfig.DYNAMIC_LIGHT_RANGE.get() * 5.0D);

        ClientEffectRenderSafety.beginDepthTestedTranslucent();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        boolean hasGeometry = false;
        for (DynamicLightFlash light : LIGHTS) {
            if (light.position.distanceToSqr(cameraPos) > maxDistance * maxDistance) {
                continue;
            }
            if (!ClientEffectRenderSafety.canRenderAt(cameraPos, light.position)) {
                continue;
            }
            hasGeometry |= renderLight(buffer, matrix, cameraPos, light, now);
        }

        var meshData = buffer.build();
        if (hasGeometry && meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }

        ClientEffectRenderSafety.endDepthTestedTranslucent();
    }

    public static void clear() {
        LIGHTS.clear();
    }

    private static void add(Vec3 position, float red, float green, float blue, double intensity, long lifetimeMillis, double rangeScale) {
        if (intensity <= 0.0D) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            double maxDistance = Math.max(8.0D, BallisticBlockConfig.DYNAMIC_LIGHT_RANGE.get() * 5.0D);
            if (minecraft.player.distanceToSqr(position) > maxDistance * maxDistance) {
                return;
            }
        }
        if (isReducedGraphics(minecraft)) {
            intensity *= 0.55D;
            lifetimeMillis = Math.max(35L, lifetimeMillis / 2L);
        }

        double darknessBoost = getDarknessBoost(position);
        double range = BallisticBlockConfig.DYNAMIC_LIGHT_RANGE.get() * rangeScale * Mth.clamp(intensity, 0.25D, 2.4D);
        LIGHTS.addLast(new DynamicLightFlash(position, red, green, blue, (float) intensity, (float) range, System.currentTimeMillis(), lifetimeMillis, (float) darknessBoost));
        ClientIndoorSmokeManager.illuminate(position, intensity, red, green, blue);
        while (LIGHTS.size() > MAX_ACTIVE_LIGHTS) {
            LIGHTS.removeFirst();
        }
    }

    private static boolean renderLight(BufferBuilder buffer, Matrix4f matrix, Vec3 cameraPos, DynamicLightFlash light, long now) {
        float age = (float) (now - light.createdMillis) / Math.max(1.0F, light.lifetimeMillis);
        if (age >= 1.0F) {
            return false;
        }
        float fade = (1.0F - age) * (1.0F - age);
        float alpha = Mth.clamp(0.055F * light.intensity * fade, 0.0F, 0.12F);
        if (alpha <= 0.001F) {
            return false;
        }
        float radius = Math.min(0.85F, light.range * 0.12F) * (0.35F + 0.65F * fade);
        Vec3 center = light.position.subtract(cameraPos);

        addQuad(buffer, matrix, center, new Vec3(1.0D, 0.0D, 0.0D), new Vec3(0.0D, 1.0D, 0.0D), radius, light, alpha);
        addQuad(buffer, matrix, center, new Vec3(1.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, 1.0D), radius, light, alpha * 0.72F);
        addQuad(buffer, matrix, center, new Vec3(0.0D, 1.0D, 0.0D), new Vec3(0.0D, 0.0D, 1.0D), radius, light, alpha * 0.72F);
        return true;
    }

    private static void addQuad(BufferBuilder buffer, Matrix4f matrix, Vec3 center, Vec3 tangent, Vec3 bitangent, float radius, DynamicLightFlash light, float alpha) {
        Vec3 tx = tangent.scale(radius);
        Vec3 by = bitangent.scale(radius);
        vertex(buffer, matrix, center.subtract(tx).subtract(by), 0.92F, 0.92F, 0.92F, 0.0F);
        vertex(buffer, matrix, center.add(tx).subtract(by), 0.92F, 0.92F, 0.92F, 0.0F);
        vertex(buffer, matrix, center.add(tx.scale(0.16D)).add(by.scale(0.16D)), 0.92F, 0.92F, 0.92F, alpha);
        vertex(buffer, matrix, center.subtract(tx).add(by), 0.92F, 0.92F, 0.92F, 0.0F);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 position, float red, float green, float blue, float alpha) {
        buffer.addVertex(matrix, (float) position.x, (float) position.y, (float) position.z).setColor(red, green, blue, alpha);
    }

    private static void cleanup(long now) {
        Iterator<DynamicLightFlash> iterator = LIGHTS.iterator();
        while (iterator.hasNext()) {
            DynamicLightFlash light = iterator.next();
            if (now - light.createdMillis > light.lifetimeMillis) {
                iterator.remove();
            }
        }
    }

    private static boolean isReducedGraphics(Minecraft minecraft) {
        String particles = String.valueOf(minecraft.options.particles().get()).toLowerCase(java.util.Locale.ROOT);
        String graphics = String.valueOf(minecraft.options.graphicsMode().get()).toLowerCase(java.util.Locale.ROOT);
        return particles.contains("minimal") || particles.contains("decreased") || graphics.contains("fast");
    }

    private static double getDarknessBoost(Vec3 position) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 1.0D;
        }
        int brightness = minecraft.level.getMaxLocalRawBrightness(BlockPos.containing(position));
        boolean indoor = !minecraft.level.canSeeSky(BlockPos.containing(position));
        double boost = indoor ? 1.25D : 1.0D;
        if (brightness <= 7) {
            boost += 0.45D;
        } else if (brightness >= 13) {
            boost *= 0.55D;
        }
        return boost;
    }

    private record DynamicLightFlash(Vec3 position, float red, float green, float blue, float intensity, float range, long createdMillis, long lifetimeMillis, float darknessBoost) {
    }
}
