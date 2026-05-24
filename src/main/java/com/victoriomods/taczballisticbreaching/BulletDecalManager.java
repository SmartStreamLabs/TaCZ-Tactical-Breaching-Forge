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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class BulletDecalManager {
    private static final double MAX_RENDER_DISTANCE_SQR = 72.0D * 72.0D;
    private static final double SURFACE_OFFSET = 0.0035D;
    private static final double STACKING_JITTER = 0.018D;
    private static final Map<ChunkPos, ArrayDeque<BulletDecal>> DECALS = new HashMap<>();

    private BulletDecalManager() {
    }

    public static void addFromImpact(ImpactFxPacket packet) {
        if (!BallisticBlockConfig.ENABLE_BULLET_DECALS.get() || packet.finalBreak() || (packet.penetrationExit() && !BallisticBlockConfig.ENABLE_EXIT_DECALS.get())) {
            return;
        }
        int maxPerChunk = BallisticBlockConfig.MAX_DECALS_PER_CHUNK.get();
        if (maxPerChunk <= 0) {
            return;
        }

        RandomSource random = Minecraft.getInstance().level == null ? RandomSource.create() : Minecraft.getInstance().level.random;
        Vec3 normal = normalizedNormal(packet);
        Vec3 base = new Vec3(packet.x(), packet.y(), packet.z()).add(normal.scale(SURFACE_OFFSET));
        Basis impactBasis = Basis.fromNormal(normal);
        int count = packet.shotgunImpact() ? 4 + random.nextInt(3) : 1;

        for (int i = 0; i < count; i++) {
            Vec3 position = base.add(randomSurfaceOffset(random, impactBasis, STACKING_JITTER));
            if (count > 1 || packet.automaticImpact()) {
                double spread = packet.shotgunImpact() ? 0.18D : 0.035D;
                position = position.add(randomSurfaceOffset(random, impactBasis, spread));
            }

            double size = BallisticBlockConfig.BULLET_DECAL_SIZE.get() * (0.82D + random.nextDouble() * 0.36D);
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
                    ImpactMaterial.byId(packet.materialId()),
                    size,
                    random.nextFloat() * ((float) Math.PI * 2.0F),
                    System.currentTimeMillis(),
                    packet.highCaliberImpact(),
                    packet.armorPiercingImpact()
            );
            add(decal);
        }
    }

    public static void render(PoseStack poseStack, Camera camera) {
        if (!BallisticBlockConfig.ENABLE_BULLET_DECALS.get() || DECALS.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        cleanup(Minecraft.getInstance().level, now);
        Vec3 cameraPos = camera.getPosition();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (ArrayDeque<BulletDecal> chunkDecals : DECALS.values()) {
            for (BulletDecal decal : chunkDecals) {
                if (decal.position.distanceToSqr(cameraPos) > MAX_RENDER_DISTANCE_SQR) {
                    continue;
                }
                renderDecal(buffer, matrix, cameraPos, decal);
            }
        }

        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
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
        int maxPerChunk = BallisticBlockConfig.MAX_DECALS_PER_CHUNK.get();
        while (chunkDecals.size() > maxPerChunk) {
            chunkDecals.removeFirst();
        }
    }

    private static void cleanup(ClientLevel level, long now) {
        long lifetimeMillis = BallisticBlockConfig.BULLET_DECAL_LIFETIME_SECONDS.get() * 1000L;
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
            return true;
        }
        return !level.getBlockState(decal.backingPos).isAir();
    }

    private static void renderDecal(BufferBuilder buffer, Matrix4f matrix, Vec3 cameraPos, BulletDecal decal) {
        Basis basis = Basis.fromNormal(decal.normal).rotated(decal.rotation);
        DecalStyle style = DecalStyle.forMaterial(decal.material, decal.highCaliber, decal.armorPiercing);
        float alpha = getFadeAlpha(decal);
        if (alpha <= 0.01F) {
            return;
        }

        addQuad(buffer, matrix, cameraPos, decal.position, basis, decal.size, decal.size, style.r, style.g, style.b, style.alpha * alpha);
        if (BallisticBlockConfig.ENABLE_MATERIAL_SPECIFIC_DECALS.get()) {
            renderDetail(buffer, matrix, cameraPos, decal, basis, style, alpha);
        }
    }

    private static float getFadeAlpha(BulletDecal decal) {
        double lifetime = Math.max(1.0D, BallisticBlockConfig.BULLET_DECAL_LIFETIME_SECONDS.get());
        double age = (System.currentTimeMillis() - decal.createdMillis) / 1000.0D;
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
        Vec3 c = center.subtract(cameraPos);
        Vec3 tx = basis.tangent.scale(width * 0.5D);
        Vec3 by = basis.bitangent.scale(height * 0.5D);
        vertex(buffer, matrix, c.subtract(tx).subtract(by), r, g, b, a);
        vertex(buffer, matrix, c.add(tx).subtract(by), r, g, b, a);
        vertex(buffer, matrix, c.add(tx).add(by), r, g, b, a);
        vertex(buffer, matrix, c.subtract(tx).add(by), r, g, b, a);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 position, float r, float g, float b, float a) {
        buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z).color(r, g, b, a).endVertex();
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

    private record DecalStyle(float r, float g, float b, float alpha) {
        static DecalStyle forMaterial(ImpactMaterial material, boolean highCaliber, boolean armorPiercing) {
            if (!BallisticBlockConfig.ENABLE_MATERIAL_SPECIFIC_DECALS.get()) {
                return new DecalStyle(0.035F, 0.032F, 0.028F, armorPiercing ? 0.82F : 0.7F);
            }
            float darken = armorPiercing ? 0.62F : 1.0F;
            float alpha = highCaliber ? 0.78F : 0.66F;
            return switch (material) {
                case GLASS -> new DecalStyle(0.78F, 0.88F, 0.96F, 0.38F);
                case WOOD -> new DecalStyle(0.18F * darken, 0.105F * darken, 0.045F * darken, alpha);
                case METAL -> new DecalStyle(0.045F * darken, 0.043F * darken, 0.04F * darken, alpha + 0.08F);
                case STONE -> new DecalStyle(0.055F * darken, 0.052F * darken, 0.048F * darken, alpha);
                default -> new DecalStyle(0.045F * darken, 0.04F * darken, 0.035F * darken, alpha);
            };
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
