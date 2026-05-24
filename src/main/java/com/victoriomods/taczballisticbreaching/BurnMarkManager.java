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
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class BurnMarkManager {
    private static final double MAX_RENDER_DISTANCE_SQR = 80.0D * 80.0D;
    private static final double SURFACE_OFFSET = 0.004D;
    private static final Map<ChunkPos, ArrayDeque<BurnMark>> MARKS = new HashMap<>();

    private BurnMarkManager() {
    }

    public static void addFromExplosion(Vec3 origin) {
        if (!BallisticBlockConfig.ENABLE_BURN_MARKS.get() || !BallisticBlockConfig.ENABLE_EXPLOSION_SCORCH.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || minecraft.player.distanceToSqr(origin) > MAX_RENDER_DISTANCE_SQR) {
            return;
        }

        RandomSource random = level.random;
        boolean indoor = !level.canSeeSky(BlockPos.containing(origin));
        int attempts = indoor ? 12 : 7;
        for (int i = 0; i < attempts; i++) {
            Vec3 direction = i == 0 ? new Vec3(0.0D, -1.0D, 0.0D) : randomDirection(random, indoor ? 1.0D : 0.65D);
            addRayScorch(level, random, origin.add(randomSpread(random, 0.6D)), direction, indoor ? 5.8D : 4.2D, 0.72D + random.nextDouble() * 0.62D, BurnKind.EXPLOSION);
        }
        spawnSmokeWisps(level, random, origin, 8);
    }

    public static void addFromImpact(ImpactFxPacket packet, ImpactMaterial material) {
        if (!BallisticBlockConfig.ENABLE_BURN_MARKS.get() || !BallisticBlockConfig.ENABLE_BREACH_SCORCH.get() || !packet.finalBreak()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }
        Vec3 impact = new Vec3(packet.x(), packet.y(), packet.z());
        if (minecraft.player.distanceToSqr(impact) > MAX_RENDER_DISTANCE_SQR) {
            return;
        }
        Vec3 normal = new Vec3(packet.normalX(), packet.normalY(), packet.normalZ());
        normal = normal.lengthSqr() < 0.001D ? new Vec3(0.0D, 1.0D, 0.0D) : normal.normalize();
        double size = material == ImpactMaterial.METAL ? 0.34D : material == ImpactMaterial.WOOD ? 0.48D : 0.42D;
        if (packet.highCaliberImpact()) {
            size *= 1.35D;
        }
        addDirect(level, impact.add(normal.scale(SURFACE_OFFSET)), normal, backingBlockPos(impact, normal), ImpactMaterial.byId(packet.materialId()), size, BurnKind.BREACH);
    }

    public static void addHeatResidue(MuzzleSmokePacket packet, Vec3 muzzle, Vec3 forward, double heatSmokeMultiplier) {
        if (!BallisticBlockConfig.ENABLE_BURN_MARKS.get() || !BallisticBlockConfig.ENABLE_HEAT_RESIDUE.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || minecraft.player.distanceToSqr(muzzle) > MAX_RENDER_DISTANCE_SQR) {
            return;
        }
        MuzzleSmokeNetwork.WeaponKind kind = MuzzleSmokeNetwork.WeaponKind.byId(packet.weaponKind());
        boolean heavy = packet.highCaliber() || kind == MuzzleSmokeNetwork.WeaponKind.SNIPER || kind == MuzzleSmokeNetwork.WeaponKind.SHOTGUN || (packet.automatic() && heatSmokeMultiplier > 1.25D);
        if (!heavy || packet.suppressed() && level.random.nextFloat() > 0.38F) {
            return;
        }
        double chance = packet.automatic() ? 0.16D : 0.055D;
        if (packet.highCaliber()) {
            chance += 0.07D;
        }
        if (level.random.nextDouble() > chance * Math.min(1.7D, heatSmokeMultiplier)) {
            return;
        }
        addRayScorch(level, level.random, muzzle, forward, kind == MuzzleSmokeNetwork.WeaponKind.PISTOL ? 1.0D : 1.8D, 0.13D + level.random.nextDouble() * 0.12D, BurnKind.HEAT);
    }

    public static void render(PoseStack poseStack, Camera camera) {
        if (!BallisticBlockConfig.ENABLE_BURN_MARKS.get() || MARKS.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        ClientLevel level = Minecraft.getInstance().level;
        cleanup(level, now);
        Vec3 cameraPos = camera.getPosition();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (ArrayDeque<BurnMark> chunkMarks : MARKS.values()) {
            for (BurnMark mark : chunkMarks) {
                if (mark.position.distanceToSqr(cameraPos) <= MAX_RENDER_DISTANCE_SQR) {
                    renderMark(buffer, matrix, cameraPos, mark, now, level);
                }
            }
        }

        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void clear() {
        MARKS.clear();
    }

    private static void addRayScorch(ClientLevel level, RandomSource random, Vec3 origin, Vec3 direction, double distance, double size, BurnKind kind) {
        if (BallisticBlockConfig.MAX_BURN_MARKS_PER_CHUNK.get() <= 0) {
            return;
        }
        direction = direction.lengthSqr() < 0.001D ? new Vec3(0.0D, -1.0D, 0.0D) : direction.normalize();
        BlockHitResult result = level.clip(new ClipContext(origin, origin.add(direction.scale(distance)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Minecraft.getInstance().player));
        if (result.getType() != HitResult.Type.BLOCK) {
            return;
        }
        Vec3 normal = Vec3.atLowerCornerOf(result.getDirection().getNormal()).normalize();
        Vec3 position = result.getLocation().add(normal.scale(SURFACE_OFFSET));
        BlockPos backing = backingBlockPos(position, normal);
        BlockState state = level.getBlockState(backing);
        ImpactMaterial material = ImpactMaterial.classify(state);
        addDirect(level, position, normal, backing, material, size, kind);
        if (kind != BurnKind.HEAT) {
            spawnSmokeWisps(level, random, position, 2 + random.nextInt(3));
        }
    }

    private static void addDirect(ClientLevel level, Vec3 position, Vec3 normal, BlockPos backing, ImpactMaterial material, double size, BurnKind kind) {
        RandomSource random = level.random;
        Basis basis = Basis.fromNormal(normal);
        BurnMark mark = new BurnMark(
                position.add(randomSurfaceOffset(random, basis, 0.045D)),
                backing,
                normal,
                material,
                kind,
                size * (0.82D + random.nextDouble() * 0.36D),
                random.nextFloat() * ((float) Math.PI * 2.0F),
                System.currentTimeMillis(),
                (float) Mth.clamp(kind.opacity * materialOpacity(material) * BallisticBlockConfig.BURN_MARK_OPACITY.get(), 0.0D, 1.0D),
                random.nextInt(5)
        );
        add(mark);
    }

    private static void add(BurnMark mark) {
        ChunkPos chunkPos = new ChunkPos((int) Math.floor(mark.position.x) >> 4, (int) Math.floor(mark.position.z) >> 4);
        ArrayDeque<BurnMark> chunkMarks = MARKS.computeIfAbsent(chunkPos, ignored -> new ArrayDeque<>());
        chunkMarks.addLast(mark);
        int max = BallisticBlockConfig.MAX_BURN_MARKS_PER_CHUNK.get();
        while (chunkMarks.size() > max) {
            chunkMarks.removeFirst();
        }
    }

    private static void cleanup(ClientLevel level, long now) {
        long lifetimeMillis = BallisticBlockConfig.BURN_MARK_LIFETIME_SECONDS.get() * 1000L;
        Iterator<Map.Entry<ChunkPos, ArrayDeque<BurnMark>>> mapIterator = MARKS.entrySet().iterator();
        while (mapIterator.hasNext()) {
            ArrayDeque<BurnMark> chunkMarks = mapIterator.next().getValue();
            while (!chunkMarks.isEmpty() && now - chunkMarks.peekFirst().createdMillis > lifetimeMillis) {
                chunkMarks.removeFirst();
            }
            chunkMarks.removeIf(mark -> !hasBackingBlock(level, mark));
            if (chunkMarks.isEmpty()) {
                mapIterator.remove();
            }
        }
    }

    private static boolean hasBackingBlock(ClientLevel level, BurnMark mark) {
        if (level == null || !level.hasChunkAt(mark.backingPos)) {
            return true;
        }
        return !level.getBlockState(mark.backingPos).isAir();
    }

    private static void renderMark(BufferBuilder buffer, Matrix4f matrix, Vec3 cameraPos, BurnMark mark, long now, ClientLevel level) {
        float alpha = fadeAlpha(mark, now, level);
        if (alpha <= 0.01F) {
            return;
        }
        Basis basis = Basis.fromNormal(mark.normal).rotated(mark.rotation);
        BurnStyle style = BurnStyle.forMaterial(mark.material, mark.kind);
        addQuad(buffer, matrix, cameraPos, mark.position, basis, mark.size, mark.size * (0.76D + mark.noise * 0.045D), style.r, style.g, style.b, mark.opacity * alpha);
        addQuad(buffer, matrix, cameraPos, mark.position.add(mark.normal.scale(0.001D)), basis.rotated(0.72F), mark.size * 0.68D, mark.size * 0.42D, 0.02F, 0.018F, 0.015F, mark.opacity * 0.62F * alpha);
        if (mark.kind == BurnKind.EXPLOSION || mark.material == ImpactMaterial.WOOD) {
            for (int i = 0; i < 3; i++) {
                double angle = mark.rotation + i * 2.05D;
                Vec3 offset = basis.tangent.scale(Math.cos(angle) * mark.size * 0.24D).add(basis.bitangent.scale(Math.sin(angle) * mark.size * 0.18D));
                addQuad(buffer, matrix, cameraPos, mark.position.add(offset).add(mark.normal.scale(0.002D)), basis.rotated((float) angle), mark.size * 0.42D, mark.size * 0.12D, style.edgeR, style.edgeG, style.edgeB, mark.opacity * 0.38F * alpha);
            }
        }
    }

    private static float fadeAlpha(BurnMark mark, long now, ClientLevel level) {
        double lifetime = Math.max(1.0D, BallisticBlockConfig.BURN_MARK_LIFETIME_SECONDS.get());
        double age = (now - mark.createdMillis) / 1000.0D;
        if (level != null && level.isRainingAt(mark.backingPos.above()) && level.canSeeSky(mark.backingPos.above())) {
            age *= 2.6D;
        }
        double normalized = Mth.clamp(age / lifetime, 0.0D, 1.0D);
        if (normalized <= 0.16D) {
            return 1.0F;
        }
        return (float) Math.pow(1.0D - (normalized - 0.16D) / 0.84D, 1.25D);
    }

    private static void addQuad(BufferBuilder buffer, Matrix4f matrix, Vec3 cameraPos, Vec3 center, Basis basis, double width, double height, float r, float g, float b, float a) {
        Vec3 c = center.subtract(cameraPos);
        Vec3 tx = basis.tangent.scale(width * 0.5D);
        Vec3 by = basis.bitangent.scale(height * 0.5D);
        vertex(buffer, matrix, c.subtract(tx).subtract(by), r, g, b, a * 0.72F);
        vertex(buffer, matrix, c.add(tx).subtract(by), r, g, b, a * 0.88F);
        vertex(buffer, matrix, c.add(tx).add(by), r, g, b, a);
        vertex(buffer, matrix, c.subtract(tx).add(by), r, g, b, a * 0.82F);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 position, float r, float g, float b, float a) {
        buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z).color(r, g, b, a).endVertex();
    }

    private static void spawnSmokeWisps(ClientLevel level, RandomSource random, Vec3 position, int count) {
        for (int i = 0; i < count; i++) {
            Vec3 velocity = randomSpread(random, 0.012D).add(0.0D, 0.018D + random.nextDouble() * 0.028D, 0.0D);
            Particle particle = Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
            if (particle != null) {
                float grey = 0.18F + random.nextFloat() * 0.12F;
                particle.setColor(grey, grey, grey);
                particle.scale(0.32F + random.nextFloat() * 0.42F);
                particle.setLifetime(18 + random.nextInt(18));
            }
        }
    }

    private static Vec3 randomDirection(RandomSource random, double verticalBias) {
        Vec3 direction = randomSpread(random, 2.0D).add(0.0D, -verticalBias, 0.0D);
        return direction.lengthSqr() < 0.001D ? new Vec3(0.0D, -1.0D, 0.0D) : direction.normalize();
    }

    private static Vec3 randomSpread(RandomSource random, double amount) {
        return new Vec3((random.nextDouble() - 0.5D) * amount, (random.nextDouble() - 0.5D) * amount, (random.nextDouble() - 0.5D) * amount);
    }

    private static Vec3 randomSurfaceOffset(RandomSource random, Basis basis, double amount) {
        return basis.tangent.scale((random.nextDouble() - 0.5D) * amount)
                .add(basis.bitangent.scale((random.nextDouble() - 0.5D) * amount));
    }

    private static BlockPos backingBlockPos(Vec3 position, Vec3 normal) {
        return BlockPos.containing(position.subtract(normal.scale(0.03D)));
    }

    private static double materialOpacity(ImpactMaterial material) {
        return switch (material) {
            case WOOD -> 1.12D;
            case METAL -> 0.72D;
            case GLASS -> 0.48D;
            case STONE -> 0.92D;
            default -> 0.78D;
        };
    }

    private enum BurnKind {
        EXPLOSION(1.0F),
        BREACH(0.78F),
        HEAT(0.46F);

        private final float opacity;

        BurnKind(float opacity) {
            this.opacity = opacity;
        }
    }

    private record BurnMark(Vec3 position, BlockPos backingPos, Vec3 normal, ImpactMaterial material, BurnKind kind, double size, float rotation, long createdMillis, float opacity, int noise) {
    }

    private record BurnStyle(float r, float g, float b, float edgeR, float edgeG, float edgeB) {
        static BurnStyle forMaterial(ImpactMaterial material, BurnKind kind) {
            if (material == ImpactMaterial.METAL) {
                return new BurnStyle(0.10F, 0.075F, 0.055F, 0.34F, 0.18F, 0.08F);
            }
            if (material == ImpactMaterial.GLASS) {
                return new BurnStyle(0.12F, 0.12F, 0.12F, 0.22F, 0.20F, 0.17F);
            }
            if (material == ImpactMaterial.WOOD) {
                return new BurnStyle(0.035F, 0.026F, 0.018F, 0.18F, 0.08F, 0.035F);
            }
            if (kind == BurnKind.HEAT) {
                return new BurnStyle(0.07F, 0.06F, 0.052F, 0.16F, 0.11F, 0.07F);
            }
            return new BurnStyle(0.045F, 0.042F, 0.038F, 0.15F, 0.12F, 0.085F);
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
