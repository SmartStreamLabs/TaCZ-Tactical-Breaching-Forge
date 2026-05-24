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
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class PhysicalDebrisManager {
    private static final double MAX_RENDER_DISTANCE_SQR = 72.0D * 72.0D;
    private static final double GRAVITY = 0.105D;
    private static final Map<ChunkPos, ArrayDeque<DebrisPiece>> DEBRIS = new HashMap<>();

    private PhysicalDebrisManager() {
    }

    public static void addFromImpact(ImpactFxPacket packet, ImpactMaterial material) {
        if (!BallisticBlockConfig.ENABLE_PHYSICAL_DEBRIS.get() || !packet.finalBreak() || BallisticBlockConfig.MAX_DEBRIS_PER_CHUNK.get() <= 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }
        Vec3 origin = new Vec3(packet.x(), packet.y(), packet.z());
        if (minecraft.player.distanceToSqr(origin) > MAX_RENDER_DISTANCE_SQR) {
            return;
        }

        RandomSource random = level.random;
        Vec3 normal = normalizedNormal(packet);
        int count = reduceForGraphics(minecraft, amountFor(material, packet));
        count = Math.min(count, 96);
        double burst = packet.highCaliberImpact() ? 1.34D : 1.0D;
        if (packet.shotgunImpact()) {
            burst *= 1.22D;
        }
        if (packet.automaticImpact()) {
            burst *= 0.82D;
        }

        for (int i = 0; i < count; i++) {
            double spread = packet.shotgunImpact() ? 0.18D : 0.095D;
            Vec3 velocity = normal.scale((0.055D + random.nextDouble() * 0.135D) * burst)
                    .add(randomSpread(random, spread * burst))
                    .add(0.0D, 0.045D + random.nextDouble() * 0.13D, 0.0D);
            Vec3 start = origin.add(normal.scale(0.065D)).add(randomSpread(random, 0.16D));
            DebrisStyle style = styleFor(material, random);
            DebrisPiece piece = new DebrisPiece(
                    start,
                    velocity,
                    findGroundY(level, start),
                    style,
                    sizeFor(material, random),
                    random.nextFloat() * ((float) Math.PI * 2.0F),
                    (random.nextFloat() - 0.5F) * 10.0F,
                    (random.nextFloat() - 0.5F) * 10.0F,
                    (random.nextFloat() - 0.5F) * 10.0F,
                    0.48F + random.nextFloat() * 0.38F,
                    System.currentTimeMillis()
            );
            add(piece);
        }
        spawnFreshDust(level, random, origin, material, Math.min(18, Math.max(4, count / 3)));
    }

    public static void render(PoseStack poseStack, Camera camera) {
        if (!BallisticBlockConfig.ENABLE_PHYSICAL_DEBRIS.get() || DEBRIS.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        cleanup(now);
        Vec3 cameraPos = camera.getPosition();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (ArrayDeque<DebrisPiece> chunkDebris : DEBRIS.values()) {
            for (DebrisPiece piece : chunkDebris) {
                Vec3 position = piece.positionAt(now);
                if (position.distanceToSqr(cameraPos) > MAX_RENDER_DISTANCE_SQR) {
                    continue;
                }
                renderPiece(buffer, matrix, cameraPos, piece, position, now);
            }
        }

        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void clear() {
        DEBRIS.clear();
    }

    private static int amountFor(ImpactMaterial material, ImpactFxPacket packet) {
        int amount = switch (material) {
            case WOOD -> BallisticBlockConfig.WOOD_DEBRIS_AMOUNT.get();
            case GLASS -> BallisticBlockConfig.GLASS_DEBRIS_AMOUNT.get();
            case METAL -> BallisticBlockConfig.METAL_DEBRIS_AMOUNT.get();
            case STONE -> BallisticBlockConfig.CONCRETE_DEBRIS_AMOUNT.get();
            default -> Math.max(6, BallisticBlockConfig.CONCRETE_DEBRIS_AMOUNT.get() / 2);
        };
        if (packet.glassPane() && material == ImpactMaterial.GLASS) {
            amount = Math.max(3, (int) Math.ceil(amount * 0.48D));
        }
        if (packet.shotgunImpact()) {
            amount = (int) Math.ceil(amount * 1.28D);
        }
        if (packet.highCaliberImpact()) {
            amount = (int) Math.ceil(amount * 1.35D);
        }
        return amount;
    }

    private static DebrisStyle styleFor(ImpactMaterial material, RandomSource random) {
        return switch (material) {
            case WOOD -> new DebrisStyle(0.55F + random.nextFloat() * 0.14F, 0.34F + random.nextFloat() * 0.12F, 0.16F + random.nextFloat() * 0.08F, 0.9F, 0.78F);
            case GLASS -> new DebrisStyle(0.68F, 0.9F, 1.0F, 0.58F, 0.45F);
            case METAL -> new DebrisStyle(0.48F + random.nextFloat() * 0.16F, 0.47F + random.nextFloat() * 0.14F, 0.43F + random.nextFloat() * 0.12F, 0.88F, 0.62F);
            case STONE -> new DebrisStyle(0.36F + random.nextFloat() * 0.15F, 0.35F + random.nextFloat() * 0.13F, 0.32F + random.nextFloat() * 0.12F, 0.86F, 0.72F);
            default -> new DebrisStyle(0.42F, 0.39F, 0.34F, 0.78F, 0.68F);
        };
    }

    private static double sizeFor(ImpactMaterial material, RandomSource random) {
        return switch (material) {
            case WOOD -> 0.055D + random.nextDouble() * 0.105D;
            case GLASS -> 0.04D + random.nextDouble() * 0.085D;
            case METAL -> 0.045D + random.nextDouble() * 0.08D;
            case STONE -> 0.055D + random.nextDouble() * 0.095D;
            default -> 0.045D + random.nextDouble() * 0.075D;
        };
    }

    private static void add(DebrisPiece piece) {
        ChunkPos chunkPos = new ChunkPos((int) Math.floor(piece.start.x) >> 4, (int) Math.floor(piece.start.z) >> 4);
        ArrayDeque<DebrisPiece> chunkDebris = DEBRIS.computeIfAbsent(chunkPos, ignored -> new ArrayDeque<>());
        chunkDebris.addLast(piece);
        int maxPerChunk = BallisticBlockConfig.MAX_DEBRIS_PER_CHUNK.get();
        while (chunkDebris.size() > maxPerChunk) {
            chunkDebris.removeFirst();
        }
    }

    private static void cleanup(long now) {
        long lifetimeMillis = BallisticBlockConfig.DEBRIS_LIFETIME_SECONDS.get() * 1000L;
        Iterator<Map.Entry<ChunkPos, ArrayDeque<DebrisPiece>>> iterator = DEBRIS.entrySet().iterator();
        while (iterator.hasNext()) {
            ArrayDeque<DebrisPiece> chunkDebris = iterator.next().getValue();
            while (!chunkDebris.isEmpty() && now - chunkDebris.peekFirst().createdMillis > lifetimeMillis) {
                chunkDebris.removeFirst();
            }
            if (chunkDebris.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private static void renderPiece(BufferBuilder buffer, Matrix4f matrix, Vec3 cameraPos, DebrisPiece piece, Vec3 position, long now) {
        double lifetime = Math.max(1.0D, BallisticBlockConfig.DEBRIS_LIFETIME_SECONDS.get());
        double age = (now - piece.createdMillis) / 1000.0D;
        double fadeStart = lifetime * 0.68D;
        float alpha = piece.alpha;
        if (age > fadeStart) {
            alpha *= (float) Math.max(0.0D, 1.0D - (age - fadeStart) / Math.max(0.5D, lifetime - fadeStart));
        }
        if (alpha <= 0.01F) {
            return;
        }

        boolean landed = position.y <= piece.groundY + 0.004D;
        double spinAge = landed ? Math.min(age, 0.9D) : age;
        Vec3 tangent = new Vec3(Math.cos(piece.baseRotation + piece.spinY * spinAge), landed ? 0.0D : piece.spinX * 0.025D, Math.sin(piece.baseRotation + piece.spinY * spinAge)).normalize();
        Vec3 bitangent = landed ? new Vec3(-tangent.z, 0.0D, tangent.x).normalize() : new Vec3(piece.spinZ * 0.02D, 1.0D, piece.spinX * 0.02D).normalize();
        double stretch = piece.style.slide * (1.0D + Math.abs(Math.sin(piece.baseRotation)) * 0.75D);
        Vec3 center = position.subtract(cameraPos);
        Vec3 tx = tangent.scale(piece.size * stretch);
        Vec3 by = bitangent.scale(piece.size * (0.34D + piece.style.bounce * 0.3D));

        vertex(buffer, matrix, center.subtract(tx).subtract(by), piece.style.r * 0.75F, piece.style.g * 0.75F, piece.style.b * 0.75F, alpha);
        vertex(buffer, matrix, center.add(tx).subtract(by.scale(0.45D)), piece.style.r, piece.style.g, piece.style.b, alpha);
        vertex(buffer, matrix, center.add(tx.scale(0.35D)).add(by), Math.min(1.0F, piece.style.r * 1.18F), Math.min(1.0F, piece.style.g * 1.18F), Math.min(1.0F, piece.style.b * 1.18F), alpha * 0.9F);
        vertex(buffer, matrix, center.subtract(tx.scale(0.65D)).add(by.scale(0.35D)), piece.style.r * 0.86F, piece.style.g * 0.86F, piece.style.b * 0.86F, alpha * 0.82F);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 position, float r, float g, float b, float a) {
        buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z).color(r, g, b, a).endVertex();
    }

    private static double findGroundY(ClientLevel level, Vec3 position) {
        BlockPos start = BlockPos.containing(position);
        int maxSearch = Math.min(96, Math.max(16, start.getY() - level.getMinBuildHeight()));
        for (int i = 0; i <= maxSearch; i++) {
            BlockPos pos = start.below(i);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return pos.getY() + 1.014D;
            }
        }
        return level.getMinBuildHeight() - 4.0D;
    }

    private static void spawnFreshDust(ClientLevel level, RandomSource random, Vec3 origin, ImpactMaterial material, int count) {
        Vector3f color = switch (material) {
            case WOOD -> new Vector3f(0.58F, 0.38F, 0.18F);
            case GLASS -> new Vector3f(0.72F, 0.92F, 1.0F);
            case METAL -> new Vector3f(1.0F, 0.68F, 0.14F);
            case STONE -> new Vector3f(0.42F, 0.41F, 0.38F);
            default -> new Vector3f(0.4F, 0.37F, 0.32F);
        };
        DustParticleOptions dust = new DustParticleOptions(color, material == ImpactMaterial.METAL ? 0.48F : 0.72F);
        for (int i = 0; i < count; i++) {
            Vec3 offset = randomSpread(random, 0.22D);
            Vec3 velocity = randomSpread(random, 0.045D).add(0.0D, 0.015D + random.nextDouble() * 0.035D, 0.0D);
            level.addParticle(dust, origin.x + offset.x, origin.y + offset.y, origin.z + offset.z, velocity.x, velocity.y, velocity.z);
        }
    }

    private static int reduceForGraphics(Minecraft minecraft, int amount) {
        String particles = String.valueOf(minecraft.options.particles().get()).toLowerCase(java.util.Locale.ROOT);
        String graphics = String.valueOf(minecraft.options.graphicsMode().get()).toLowerCase(java.util.Locale.ROOT);
        if (particles.contains("minimal")) {
            return Math.max(1, amount / 4);
        }
        if (particles.contains("decreased") || graphics.contains("fast")) {
            return Math.max(1, amount / 2);
        }
        return amount;
    }

    private static Vec3 normalizedNormal(ImpactFxPacket packet) {
        Vec3 normal = new Vec3(packet.normalX(), packet.normalY(), packet.normalZ());
        return normal.lengthSqr() < 0.001D ? new Vec3(0.0D, 1.0D, 0.0D) : normal.normalize();
    }

    private static Vec3 randomSpread(RandomSource random, double amount) {
        return new Vec3(
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount
        );
    }

    private record DebrisStyle(float r, float g, float b, float bounce, float slide) {
    }

    private record DebrisPiece(Vec3 start, Vec3 velocity, double groundY, DebrisStyle style, double size, float baseRotation, float spinX, float spinY, float spinZ, float alpha, long createdMillis) {
        Vec3 positionAt(long now) {
            double t = Math.max(0.0D, (now - createdMillis) / 1000.0D);
            double x = start.x + velocity.x * t * 9.0D;
            double y = start.y + velocity.y * t * 9.0D - 0.5D * GRAVITY * t * t * 20.0D;
            double z = start.z + velocity.z * t * 9.0D;
            if (y <= groundY) {
                double slideTime = Math.max(0.0D, t - 0.45D);
                double slide = Math.exp(-slideTime * 0.9D) * slideTime * style.slide;
                x += velocity.x * slide * 2.2D;
                z += velocity.z * slide * 2.2D;
                double bounce = Math.max(0.0D, Math.sin(slideTime * 13.0D) * Math.exp(-slideTime * 5.5D)) * style.bounce * 0.11D;
                y = groundY + bounce;
            }
            return new Vec3(x, y, z);
        }
    }
}
