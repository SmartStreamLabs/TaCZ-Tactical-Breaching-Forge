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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class GlassShardDebrisManager {
    private static final double MAX_RENDER_DISTANCE_SQR = 64.0D * 64.0D;
    private static final double GRAVITY = 0.075D;
    private static final Map<ChunkPos, ArrayDeque<GlassShard>> SHARDS = new HashMap<>();

    private GlassShardDebrisManager() {
    }

    public static void addFromImpact(ImpactFxPacket packet) {
        if (!BallisticBlockConfig.ENABLE_GLASS_SHARDS.get() || ImpactMaterial.byId(packet.materialId()) != ImpactMaterial.GLASS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || BallisticBlockConfig.MAX_GLASS_SHARDS_PER_CHUNK.get() <= 0) {
            return;
        }

        RandomSource random = level.random;
        Vec3 normal = normalizedNormal(packet);
        Vec3 origin = new Vec3(packet.x(), packet.y(), packet.z()).add(normal.scale(0.04D));
        int count = calculateShardCount(packet);
        double spread = packet.shotgunImpact() ? 0.14D : 0.075D;
        if (packet.highCaliberImpact()) {
            spread *= 1.35D;
        }

        for (int i = 0; i < count; i++) {
            Vec3 velocity = normal.scale(0.025D + random.nextDouble() * 0.06D)
                    .add(randomSpread(random, spread))
                    .add(0.0D, 0.04D + random.nextDouble() * 0.09D, 0.0D);
            Vec3 position = origin.add(randomSpread(random, 0.08D));
            double groundY = findGroundY(level, position);
            GlassShard shard = new GlassShard(
                    position,
                    velocity,
                    groundY,
                    0.035D + random.nextDouble() * 0.055D,
                    random.nextFloat() * ((float) Math.PI * 2.0F),
                    (random.nextFloat() - 0.5F) * 9.0F,
                    (random.nextFloat() - 0.5F) * 9.0F,
                    (random.nextFloat() - 0.5F) * 9.0F,
                    System.currentTimeMillis(),
                    0.62F + random.nextFloat() * 0.2F
            );
            add(shard);
        }

        spawnSparkles(level, random, origin, Math.min(8, Math.max(2, count / 3)));
    }

    public static void render(PoseStack poseStack, Camera camera) {
        if (!BallisticBlockConfig.ENABLE_GLASS_SHARDS.get() || SHARDS.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        cleanup(now);
        Vec3 cameraPos = camera.getPosition();

        ClientEffectRenderSafety.beginDepthTestedTranslucent();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (ArrayDeque<GlassShard> chunkShards : SHARDS.values()) {
            for (GlassShard shard : chunkShards) {
                Vec3 position = shard.positionAt(now);
                if (position.distanceToSqr(cameraPos) > MAX_RENDER_DISTANCE_SQR) {
                    continue;
                }
                if (!ClientEffectRenderSafety.canRenderAt(cameraPos, position)) {
                    continue;
                }
                renderShard(buffer, matrix, cameraPos, shard, position, now);
            }
        }

        var meshData = buffer.build();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }
        ClientEffectRenderSafety.endDepthTestedTranslucent();
    }

    public static void clear() {
        SHARDS.clear();
    }

    private static int calculateShardCount(ImpactFxPacket packet) {
        int amount = BallisticBlockConfig.GLASS_SHARD_AMOUNT.get();
        if (packet.glassPane()) {
            amount = Math.max(2, (int) Math.ceil(amount * 0.45D));
        }
        if (packet.shotgunImpact()) {
            amount = (int) Math.ceil(amount * 1.45D);
        }
        if (packet.highCaliberImpact()) {
            amount = (int) Math.ceil(amount * 1.55D);
        }
        if (packet.finalBreak()) {
            amount = (int) Math.ceil(amount * 1.35D);
        }
        return Math.min(amount, 96);
    }

    private static void add(GlassShard shard) {
        ChunkPos chunkPos = new ChunkPos((int) Math.floor(shard.start.x) >> 4, (int) Math.floor(shard.start.z) >> 4);
        ArrayDeque<GlassShard> chunkShards = SHARDS.computeIfAbsent(chunkPos, ignored -> new ArrayDeque<>());
        chunkShards.addLast(shard);
        int maxPerChunk = BallisticBlockConfig.MAX_GLASS_SHARDS_PER_CHUNK.get();
        while (chunkShards.size() > maxPerChunk) {
            chunkShards.removeFirst();
        }
    }

    private static void cleanup(long now) {
        long lifetimeMillis = BallisticBlockConfig.GLASS_SHARD_LIFETIME_SECONDS.get() * 1000L;
        Iterator<Map.Entry<ChunkPos, ArrayDeque<GlassShard>>> mapIterator = SHARDS.entrySet().iterator();
        while (mapIterator.hasNext()) {
            ArrayDeque<GlassShard> chunkShards = mapIterator.next().getValue();
            while (!chunkShards.isEmpty() && now - chunkShards.peekFirst().createdMillis > lifetimeMillis) {
                chunkShards.removeFirst();
            }
            if (chunkShards.isEmpty()) {
                mapIterator.remove();
            }
        }
    }

    private static void renderShard(BufferBuilder buffer, Matrix4f matrix, Vec3 cameraPos, GlassShard shard, Vec3 position, long now) {
        double ageSeconds = (now - shard.createdMillis) / 1000.0D;
        boolean landed = position.y <= shard.groundY + 0.002D;
        float alpha = shard.alpha;
        if (BallisticBlockConfig.GLASS_SHARD_FADE_OUT.get()) {
            double lifetime = Math.max(1.0D, BallisticBlockConfig.GLASS_SHARD_LIFETIME_SECONDS.get());
            double fadeStart = lifetime * 0.62D;
            if (ageSeconds > fadeStart) {
                alpha *= (float) Math.max(0.0D, 1.0D - (ageSeconds - fadeStart) / Math.max(1.0D, lifetime - fadeStart));
            }
        }
        if (alpha <= 0.01F) {
            return;
        }

        double spin = landed ? 0.0D : ageSeconds;
        Vec3 tangent = new Vec3(Math.cos(shard.baseRotation + shard.spinY * spin), 0.0D, Math.sin(shard.baseRotation + shard.spinY * spin)).normalize();
        Vec3 bitangent = landed ? new Vec3(-tangent.z, 0.0D, tangent.x) : new Vec3(shard.spinX * 0.02D, 1.0D, shard.spinZ * 0.02D).normalize();
        double sizeX = shard.size * (1.0D + Math.abs(Math.sin(shard.baseRotation)) * 0.55D);
        double sizeY = shard.size * 0.45D;

        Vec3 center = position.subtract(cameraPos);
        Vec3 tx = tangent.scale(sizeX);
        Vec3 by = bitangent.scale(sizeY);
        vertex(buffer, matrix, center.subtract(tx).subtract(by), 0.74F, 0.92F, 1.0F, alpha);
        vertex(buffer, matrix, center.add(tx).subtract(by.scale(0.45D)), 0.92F, 0.98F, 1.0F, alpha * 0.85F);
        vertex(buffer, matrix, center.add(tx.scale(0.35D)).add(by), 0.82F, 0.96F, 1.0F, alpha);
        vertex(buffer, matrix, center.subtract(tx.scale(0.65D)).add(by.scale(0.35D)), 0.62F, 0.84F, 1.0F, alpha * 0.72F);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 position, float r, float g, float b, float a) {
        buffer.addVertex(matrix, (float) position.x, (float) position.y, (float) position.z).setColor(r, g, b, a);
    }

    private static double findGroundY(ClientLevel level, Vec3 position) {
        BlockPos start = BlockPos.containing(position);
        int maxSearch = Math.min(96, Math.max(16, start.getY() - level.getMinBuildHeight()));
        for (int i = 0; i <= maxSearch; i++) {
            BlockPos pos = start.below(i);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return pos.getY() + 1.012D;
            }
        }
        return level.getMinBuildHeight() - 4.0D;
    }

    private static void spawnSparkles(ClientLevel level, RandomSource random, Vec3 origin, int count) {
        DustParticleOptions sparkle = new DustParticleOptions(new Vector3f(0.74F, 0.92F, 1.0F), 0.36F);
        for (int i = 0; i < count; i++) {
            Vec3 velocity = randomSpread(random, 0.035D).add(0.0D, 0.012D + random.nextDouble() * 0.02D, 0.0D);
            level.addParticle(sparkle, origin.x + randomSpread(random, 0.08D).x, origin.y + randomSpread(random, 0.08D).y, origin.z + randomSpread(random, 0.08D).z, velocity.x, velocity.y, velocity.z);
        }
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

    private record GlassShard(Vec3 start, Vec3 velocity, double groundY, double size, float baseRotation, float spinX, float spinY, float spinZ, long createdMillis, float alpha) {
        Vec3 positionAt(long now) {
            double t = Math.max(0.0D, (now - createdMillis) / 1000.0D);
            double x = start.x + velocity.x * t * 10.0D;
            double y = start.y + velocity.y * t * 10.0D - 0.5D * GRAVITY * t * t * 20.0D;
            double z = start.z + velocity.z * t * 10.0D;
            if (y <= groundY) {
                y = groundY;
            }
            return new Vec3(x, y, z);
        }
    }
}
