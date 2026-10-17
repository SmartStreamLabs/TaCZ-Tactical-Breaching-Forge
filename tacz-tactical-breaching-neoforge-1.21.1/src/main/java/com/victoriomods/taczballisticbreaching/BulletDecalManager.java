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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class BulletDecalManager {
    private static final ResourceLocation DECAL_TEXTURE = ResourceLocation.fromNamespaceAndPath(TaczTacticalBreaching.MOD_ID, "textures/effect/impact_decal_atlas.png");
    private static final double MAX_RENDER_DISTANCE_SQR = 72.0D * 72.0D;
    private static final double SURFACE_OFFSET = 0.0035D;
    private static final double STACKING_JITTER = 0.018D;
    private static final Map<ChunkPos, ArrayDeque<BulletDecal>> DECALS = new HashMap<>();

    private BulletDecalManager() {
    }

    public static void addFromImpact(ImpactFxPacket packet) {
        if (!BallisticBlockConfig.IMPACT_DECALS_ENABLED.get() || !BallisticBlockConfig.ENABLE_BULLET_DECALS.get() || packet.finalBreak() || (packet.penetrationExit() && !BallisticBlockConfig.ENABLE_EXIT_DECALS.get())) {
            return;
        }
        int maxPerChunk = BallisticBlockConfig.MAX_IMPACT_DECALS_PER_CHUNK.get();
        if (maxPerChunk <= 0) {
            return;
        }

        RandomSource random = Minecraft.getInstance().level == null ? RandomSource.create() : Minecraft.getInstance().level.random;
        Vec3 normal = normalizedNormal(packet);
        Vec3 base = new Vec3(packet.x(), packet.y(), packet.z()).add(normal.scale(SURFACE_OFFSET));
        Basis impactBasis = Basis.fromNormal(normal);
        ImpactMaterial material = BallisticBlockConfig.MATERIAL_SPECIFIC_IMPACTS.get() ? ImpactMaterial.byId(packet.materialId()) : ImpactMaterial.GENERIC;
        int count = packet.shotgunImpact() ? 4 + random.nextInt(3) : 1;

        for (int i = 0; i < count; i++) {
            Vec3 position = base.add(randomSurfaceOffset(random, impactBasis, STACKING_JITTER));
            if (count > 1 || packet.automaticImpact()) {
                double spread = packet.shotgunImpact() ? 0.18D : 0.035D;
                position = position.add(randomSurfaceOffset(random, impactBasis, spread));
            }

            double randomScale = BallisticBlockConfig.BULLET_HOLE_RANDOM_SCALE.get() ? 0.82D + random.nextDouble() * 0.36D : 1.0D;
            double size = BallisticBlockConfig.BULLET_DECAL_SIZE.get() * randomScale;
            if (packet.highCaliberImpact()) {
                size *= 1.45D;
            }
            if (packet.shotgunImpact()) {
                size *= 0.72D;
            }

            BulletDecal decal = new BulletDecal(
                    position,
                    backingBlockPos(position, normal),
                    normal,
                    material,
                    size,
                    BallisticBlockConfig.BULLET_HOLE_RANDOM_ROTATION.get() ? random.nextFloat() * ((float) Math.PI * 2.0F) : 0.0F,
                    System.currentTimeMillis(),
                    packet.highCaliberImpact(),
                    packet.armorPiercingImpact()
            );
            add(decal);
        }
    }

    public static void render(PoseStack poseStack, Camera camera) {
        if (!BallisticBlockConfig.IMPACT_DECALS_ENABLED.get() || !BallisticBlockConfig.ENABLE_BULLET_DECALS.get() || DECALS.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        cleanup(Minecraft.getInstance().level, now);
        Vec3 cameraPos = camera.getPosition();

        ClientEffectRenderSafety.beginDepthTestedTranslucent();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, DECAL_TEXTURE);

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        for (ArrayDeque<BulletDecal> chunkDecals : DECALS.values()) {
            for (BulletDecal decal : chunkDecals) {
                if (decal.position.distanceToSqr(cameraPos) > MAX_RENDER_DISTANCE_SQR) {
                    continue;
                }
                if (!ClientEffectRenderSafety.canRenderAt(cameraPos, decal.position)) {
                    continue;
                }
                renderDecal(buffer, matrix, cameraPos, decal);
            }
        }

        var meshData = buffer.build();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }
        ClientEffectRenderSafety.endDepthTestedTranslucent();
    }

    public static void clear() {
        DECALS.clear();
    }

    public static void removeNear(Vec3 position, double radius) {
        double radiusSqr = radius * radius;
        Iterator<Map.Entry<ChunkPos, ArrayDeque<BulletDecal>>> mapIterator = DECALS.entrySet().iterator();
        while (mapIterator.hasNext()) {
            ArrayDeque<BulletDecal> chunkDecals = mapIterator.next().getValue();
            chunkDecals.removeIf(decal -> decal.position.distanceToSqr(position) <= radiusSqr || Vec3.atCenterOf(decal.backingPos).distanceToSqr(position) <= radiusSqr);
            if (chunkDecals.isEmpty()) {
                mapIterator.remove();
            }
        }
    }

    private static void add(BulletDecal decal) {
        ChunkPos chunkPos = new ChunkPos((int) Math.floor(decal.position.x) >> 4, (int) Math.floor(decal.position.z) >> 4);
        ArrayDeque<BulletDecal> chunkDecals = DECALS.computeIfAbsent(chunkPos, ignored -> new ArrayDeque<>());
        chunkDecals.addLast(decal);
        int maxPerChunk = BallisticBlockConfig.MAX_IMPACT_DECALS_PER_CHUNK.get();
        while (chunkDecals.size() > maxPerChunk) {
            chunkDecals.removeFirst();
        }
        trimGlobalDecals();
    }

    private static void cleanup(ClientLevel level, long now) {
        long lifetimeMillis = Math.max(1L, BallisticBlockConfig.BULLET_HOLE_LIFETIME_TICKS.get()) * 50L;
        Iterator<Map.Entry<ChunkPos, ArrayDeque<BulletDecal>>> mapIterator = DECALS.entrySet().iterator();
        while (mapIterator.hasNext()) {
            ArrayDeque<BulletDecal> chunkDecals = mapIterator.next().getValue();
            while (!chunkDecals.isEmpty() && now - chunkDecals.peekFirst().createdMillis > lifetimeMillis) {
                chunkDecals.removeFirst();
            }
            chunkDecals.removeIf(decal -> !hasBackingBlock(level, decal));
            if (chunkDecals.isEmpty()) {
                mapIterator.remove();
            }
        }
    }

    private static boolean hasBackingBlock(ClientLevel level, BulletDecal decal) {
        if (level == null || !level.hasChunkAt(decal.backingPos)) {
            return false;
        }
        return !level.getBlockState(decal.backingPos).isAir();
    }

    private static void trimGlobalDecals() {
        int max = BallisticBlockConfig.MAX_IMPACT_DECALS.get();
        if (max <= 0) {
            DECALS.clear();
            return;
        }

        int total = 0;
        for (ArrayDeque<BulletDecal> chunkDecals : DECALS.values()) {
            total += chunkDecals.size();
        }
        while (total > max) {
            ChunkPos oldestChunk = null;
            long oldestTime = Long.MAX_VALUE;
            for (Map.Entry<ChunkPos, ArrayDeque<BulletDecal>> entry : DECALS.entrySet()) {
                BulletDecal first = entry.getValue().peekFirst();
                if (first != null && first.createdMillis < oldestTime) {
                    oldestTime = first.createdMillis;
                    oldestChunk = entry.getKey();
                }
            }
            if (oldestChunk == null) {
                return;
            }
            ArrayDeque<BulletDecal> chunkDecals = DECALS.get(oldestChunk);
            if (chunkDecals == null || chunkDecals.isEmpty()) {
                DECALS.remove(oldestChunk);
                continue;
            }
            chunkDecals.removeFirst();
            total--;
            if (chunkDecals.isEmpty()) {
                DECALS.remove(oldestChunk);
            }
        }
    }

    private static void renderDecal(BufferBuilder buffer, Matrix4f matrix, Vec3 cameraPos, BulletDecal decal) {
        Basis basis = Basis.fromNormal(decal.normal).rotated(decal.rotation);
        DecalStyle style = DecalStyle.forMaterial(decal.material, decal.highCaliber, decal.armorPiercing);
        float alpha = getFadeAlpha(decal);
        if (alpha <= 0.01F) {
            return;
        }

        addTexturedQuad(buffer, matrix, cameraPos, decal.position, basis, decal.size * style.widthScale, decal.size * style.heightScale, style.r, style.g, style.b, style.alpha * alpha, style.u0, style.v0, style.u1, style.v1);
    }

    private static float getFadeAlpha(BulletDecal decal) {
        double lifetime = Math.max(1.0D, BallisticBlockConfig.BULLET_HOLE_LIFETIME_TICKS.get() * 50.0D);
        double age = System.currentTimeMillis() - decal.createdMillis;
        double normalizedAge = Math.max(0.0D, Math.min(1.0D, age / lifetime));
        double holdTime = 0.08D;
        if (normalizedAge <= holdTime) {
            return 1.0F;
        }
        double fadeProgress = (normalizedAge - holdTime) / (1.0D - holdTime);
        return (float) Math.max(0.0D, Math.pow(1.0D - fadeProgress, 1.35D));
    }

    private static void renderDetail(BufferBuilder buffer, Matrix4f matrix, Vec3 cameraPos, BulletDecal decal, Basis basis, DecalStyle style, float alpha) {
        switch (decal.material) {
            case GLASS -> {
                addLine(buffer, matrix, cameraPos, decal.position, basis.rotated(0.0F), decal.size * 1.35D, decal.size * 0.045D, 0.85F, 0.92F, 0.96F, 0.42F * alpha);
                addLine(buffer, matrix, cameraPos, decal.position, basis.rotated(1.05F), decal.size * 1.05D, decal.size * 0.035D, 0.85F, 0.92F, 0.96F, 0.34F * alpha);
                addLine(buffer, matrix, cameraPos, decal.position, basis.rotated(2.25F), decal.size * 0.95D, decal.size * 0.03D, 0.85F, 0.92F, 0.96F, 0.32F * alpha);
            }
            case WOOD -> {
                addLine(buffer, matrix, cameraPos, decal.position, basis.rotated(0.35F), decal.size * 1.25D, decal.size * 0.09D, 0.64F, 0.42F, 0.20F, 0.5F * alpha);
                addLine(buffer, matrix, cameraPos, decal.position, basis.rotated(1.85F), decal.size * 0.75D, decal.size * 0.055D, 0.80F, 0.57F, 0.30F, 0.42F * alpha);
            }
            case METAL -> {
                addQuad(buffer, matrix, cameraPos, decal.position.add(decal.normal.scale(0.001D)), basis, decal.size * 1.35D, decal.size * 1.35D, 0.06F, 0.055F, 0.05F, 0.2F * alpha);
                addLine(buffer, matrix, cameraPos, decal.position, basis.rotated(0.8F), decal.size * 1.0D, decal.size * 0.05D, 0.78F, 0.72F, 0.60F, 0.38F * alpha);
            }
            case STONE -> {
                addLine(buffer, matrix, cameraPos, decal.position, basis.rotated(0.2F), decal.size * 1.1D, decal.size * 0.045D, style.r * 0.55F, style.g * 0.55F, style.b * 0.55F, 0.55F * alpha);
                addLine(buffer, matrix, cameraPos, decal.position, basis.rotated(2.1F), decal.size * 0.8D, decal.size * 0.035D, style.r * 0.6F, style.g * 0.6F, style.b * 0.6F, 0.45F * alpha);
            }
            default -> {
            }
        }
    }

    private static void addLine(BufferBuilder buffer, Matrix4f matrix, Vec3 cameraPos, Vec3 center, Basis basis, double length, double width, float r, float g, float b, float a) {
        addQuad(buffer, matrix, cameraPos, center.add(basis.tangent.scale(length * 0.08D)), basis, length, width, r, g, b, a);
    }

    private static void addQuad(BufferBuilder buffer, Matrix4f matrix, Vec3 cameraPos, Vec3 center, Basis basis, double width, double height, float r, float g, float b, float a) {
        addTexturedQuad(buffer, matrix, cameraPos, center, basis, width, height, r, g, b, a, 0.0F, 0.0F, 1.0F / 6.0F, 1.0F);
    }

    private static void addTexturedQuad(BufferBuilder buffer, Matrix4f matrix, Vec3 cameraPos, Vec3 center, Basis basis, double width, double height, float r, float g, float b, float a, float u0, float v0, float u1, float v1) {
        Vec3 c = center.subtract(cameraPos);
        Vec3 tx = basis.tangent.scale(width * 0.5D);
        Vec3 by = basis.bitangent.scale(height * 0.5D);
        vertex(buffer, matrix, c.subtract(tx).subtract(by), r, g, b, a, u0, v1);
        vertex(buffer, matrix, c.add(tx).subtract(by), r, g, b, a, u1, v1);
        vertex(buffer, matrix, c.add(tx).add(by), r, g, b, a, u1, v0);
        vertex(buffer, matrix, c.subtract(tx).add(by), r, g, b, a, u0, v0);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 position, float r, float g, float b, float a) {
        vertex(buffer, matrix, position, r, g, b, a, 0.5F, 0.5F);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 position, float r, float g, float b, float a, float u, float v) {
        buffer.addVertex(matrix, (float) position.x, (float) position.y, (float) position.z).setColor(r, g, b, a).setUv(u, v);
    }

    private static Vec3 normalizedNormal(ImpactFxPacket packet) {
        Vec3 normal = new Vec3(packet.normalX(), packet.normalY(), packet.normalZ());
        return normal.lengthSqr() < 0.001D ? new Vec3(0.0D, 1.0D, 0.0D) : normal.normalize();
    }

    private static Vec3 randomSurfaceOffset(RandomSource random, Basis basis, double amount) {
        return basis.tangent.scale((random.nextDouble() - 0.5D) * amount)
                .add(basis.bitangent.scale((random.nextDouble() - 0.5D) * amount));
    }

    private static BlockPos backingBlockPos(Vec3 position, Vec3 normal) {
        return BlockPos.containing(position.subtract(normal.scale(0.03D)));
    }

    private record BulletDecal(Vec3 position, BlockPos backingPos, Vec3 normal, ImpactMaterial material, double size, float rotation, long createdMillis, boolean highCaliber, boolean armorPiercing) {
    }

    private record DecalStyle(float r, float g, float b, float alpha, float u0, float v0, float u1, float v1, double widthScale, double heightScale) {
        static DecalStyle forMaterial(ImpactMaterial material, boolean highCaliber, boolean armorPiercing) {
            if (!BallisticBlockConfig.ENABLE_MATERIAL_SPECIFIC_DECALS.get() || !BallisticBlockConfig.MATERIAL_SPECIFIC_IMPACTS.get()) {
                return atlas(4, 0.045F, 0.04F, 0.035F, armorPiercing ? 0.78F : 0.62F, 1.0D, 1.0D);
            }
            float darken = armorPiercing ? 0.62F : 1.0F;
            float alpha = highCaliber ? 0.78F : 0.66F;
            return switch (material) {
                case GLASS -> atlas(2, 0.72F, 0.86F, 0.96F, 0.44F, 1.35D, 1.35D);
                case WOOD -> atlas(1, 0.20F * darken, 0.115F * darken, 0.052F * darken, alpha, 0.92D, 0.82D);
                case METAL -> atlas(3, 0.055F * darken, 0.052F * darken, 0.048F * darken, alpha + 0.1F, 0.78D, 0.78D);
                case STONE -> atlas(0, 0.065F * darken, 0.062F * darken, 0.057F * darken, alpha, 1.08D, 1.08D);
                case DIRT -> atlas(5, 0.24F, 0.18F, 0.10F, 0.58F, 1.12D, 0.92D);
                default -> atlas(4, 0.052F * darken, 0.047F * darken, 0.040F * darken, alpha * 0.82F, 1.0D, 1.0D);
            };
        }

        private static DecalStyle atlas(int cell, float r, float g, float b, float alpha, double widthScale, double heightScale) {
            float u0 = cell / 6.0F;
            float u1 = (cell + 1) / 6.0F;
            return new DecalStyle(r, g, b, alpha, u0, 0.0F, u1, 1.0F, widthScale, heightScale);
        }
    }

    private record Basis(Vec3 tangent, Vec3 bitangent) {
        static Basis fromNormal(Vec3 normal) {
            Vec3 up = Math.abs(normal.y) > 0.9D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
            Vec3 tangent = up.cross(normal).normalize();
            Vec3 bitangent = normal.cross(tangent).normalize();
            return new Basis(tangent, bitangent);
        }

        Basis rotated(float angle) {
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Vec3 rotatedTangent = tangent.scale(cos).add(bitangent.scale(sin));
            Vec3 rotatedBitangent = bitangent.scale(cos).subtract(tangent.scale(sin));
            return new Basis(rotatedTangent, rotatedBitangent);
        }
    }
}
