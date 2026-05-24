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
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
public final class ClientBloodHitFxHandler {
    private static final double MAX_VISIBLE_DISTANCE_SQR = 64.0D * 64.0D;
    private static final double MAX_DECAL_RENDER_DISTANCE_SQR = 72.0D * 72.0D;
    private static final double SURFACE_OFFSET = 0.0035D;
    private static final int MAX_MIST_PARTICLES_PER_SECOND = 180;
    private static final Map<ChunkPos, ArrayDeque<BloodDecal>> DECALS = new HashMap<>();
    private static long particleLimiterSecond = -1L;
    private static int particlesThisSecond;

    private ClientBloodHitFxHandler() {
    }

    public static void handle(BloodHitFxPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null) {
            return;
        }
        Vec3 impact = new Vec3(packet.x(), packet.y(), packet.z());
        if (player.distanceToSqr(impact) > MAX_VISIBLE_DISTANCE_SQR) {
            return;
        }

        Vec3 direction = new Vec3(packet.dirX(), packet.dirY(), packet.dirZ());
        direction = direction.lengthSqr() < 0.001D ? player.getLookAngle() : direction.normalize();
        RandomSource random = level.random;
        if (BallisticBlockConfig.ENABLE_BLOOD_MIST.get()) {
            spawnMist(level, random, impact, direction, packet);
            spawnFallingDroplets(level, random, impact, direction, packet);
        }
        if (BallisticBlockConfig.ENABLE_WALL_SPLATTER.get() && random.nextDouble() < splatterChance(level, impact, packet)) {
            addWallSplatter(level, random, impact, direction, packet);
        }
        if (BallisticBlockConfig.ENABLE_WALL_SPLATTER.get()) {
            addGroundSplatter(level, random, impact, direction, packet);
        }
    }

    public static void render(PoseStack poseStack, Camera camera) {
        if (!BallisticBlockConfig.ENABLE_WALL_SPLATTER.get() || DECALS.isEmpty()) {
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

        for (ArrayDeque<BloodDecal> chunkDecals : DECALS.values()) {
            for (BloodDecal decal : chunkDecals) {
                if (decal.position.distanceToSqr(cameraPos) <= MAX_DECAL_RENDER_DISTANCE_SQR) {
                    renderDecal(buffer, matrix, cameraPos, decal);
                }
            }
        }

        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void clear() {
        DECALS.clear();
        particleLimiterSecond = -1L;
        particlesThisSecond = 0;
    }

    private static void spawnMist(ClientLevel level, RandomSource random, Vec3 impact, Vec3 direction, BloodHitFxPacket packet) {
        double intensity = BallisticBlockConfig.BLOOD_MIST_INTENSITY.get() * packet.armorReduction();
        if (intensity <= 0.01D) {
            return;
        }
        if (isIndoor(level, impact)) {
            intensity *= 1.14D;
        }
        if (packet.highCaliber()) {
            intensity *= 1.35D;
        }
        if (packet.shotgun()) {
            intensity *= 1.45D;
        }
        if (packet.helmetHit()) {
            intensity *= 0.82D;
        }
        int requested = (int) Math.ceil(28.0D * intensity);
        int count = reserveParticles(Math.min(Math.max(8, requested), packet.shotgun() ? 42 : 30));
        if (count <= 0) {
            return;
        }

        DustParticleOptions bloodDust = new DustParticleOptions(new Vector3f(0.62F, 0.018F, 0.014F), 0.92F);
        for (int i = 0; i < count; i++) {
            Vec3 spread = randomSpread(random, packet.shotgun() ? 0.16D : 0.085D);
            Vec3 velocity = direction.scale(0.055D + random.nextDouble() * 0.105D).add(spread).add(0.0D, 0.005D + random.nextDouble() * 0.018D, 0.0D);
            Vec3 position = impact.add(randomSpread(random, 0.105D));
            if (i % 4 == 0) {
                Particle particle = Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.SMOKE, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
                if (particle != null) {
                    particle.setColor(0.52F + random.nextFloat() * 0.16F, 0.018F, 0.014F);
                    particle.scale(0.48F + random.nextFloat() * 0.32F);
                    particle.setLifetime(8 + random.nextInt(10));
                }
            } else {
                level.addParticle(bloodDust, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
            }
        }
    }

    private static void spawnFallingDroplets(ClientLevel level, RandomSource random, Vec3 impact, Vec3 direction, BloodHitFxPacket packet) {
        double intensity = BallisticBlockConfig.BLOOD_MIST_INTENSITY.get() * packet.armorReduction();
        if (packet.highCaliber()) {
            intensity *= 1.25D;
        }
        if (packet.shotgun()) {
            intensity *= 1.35D;
        }
        int count = reserveParticles(Math.min(Math.max(5, (int) Math.ceil(18.0D * intensity)), packet.shotgun() ? 28 : 18));
        if (count <= 0) {
            return;
        }

        DustParticleOptions droplet = new DustParticleOptions(new Vector3f(0.52F, 0.0F, 0.0F), 1.05F);
        for (int i = 0; i < count; i++) {
            Vec3 outward = direction.scale(0.018D + random.nextDouble() * 0.05D);
            Vec3 lateral = randomSpread(random, packet.shotgun() ? 0.10D : 0.055D);
            Vec3 velocity = outward.add(lateral).add(0.0D, -0.085D - random.nextDouble() * 0.12D, 0.0D);
            Vec3 position = impact.add(randomSpread(random, 0.12D)).add(0.0D, random.nextDouble() * 0.12D, 0.0D);
            Particle particle = Minecraft.getInstance().particleEngine.createParticle(droplet, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
            if (particle != null) {
                particle.setColor(0.48F + random.nextFloat() * 0.18F, 0.0F, 0.0F);
                particle.scale(0.72F + random.nextFloat() * 0.42F);
                particle.setLifetime(16 + random.nextInt(12));
            }
        }
    }

    private static void addWallSplatter(ClientLevel level, RandomSource random, Vec3 impact, Vec3 direction, BloodHitFxPacket packet) {
        int maxPerChunk = BallisticBlockConfig.MAX_BLOOD_DECALS_PER_CHUNK.get();
        if (maxPerChunk <= 0 || packet.armorReduction() < 0.18F) {
            return;
        }

        Vec3 start = impact.add(direction.scale(0.20D));
        Vec3 end = impact.add(direction.scale(packet.highCaliber() || packet.shotgun() ? 4.6D : 3.2D));
        BlockHitResult result = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Minecraft.getInstance().player));
        if (result.getType() != HitResult.Type.BLOCK) {
            return;
        }

        Vec3 normal = Vec3.atLowerCornerOf(result.getDirection().getNormal()).normalize();
        Vec3 position = result.getLocation().add(normal.scale(SURFACE_OFFSET));
        Basis basis = Basis.fromNormal(normal);
        double size = 0.18D + random.nextDouble() * 0.12D;
        if (packet.highCaliber()) {
            size *= 1.3D;
        }
        if (packet.shotgun()) {
            size *= 1.18D;
        }
        size *= Math.max(0.45D, packet.armorReduction());

        add(new BloodDecal(
                position.add(randomSurfaceOffset(random, basis, 0.045D)),
                backingBlockPos(position, normal),
                normal,
                size,
                random.nextFloat() * ((float) Math.PI * 2.0F),
                System.currentTimeMillis(),
                2 + random.nextInt(packet.shotgun() ? 4 : 3),
                packet.highCaliber(),
                packet.shotgun()
        ));
    }

    private static void addGroundSplatter(ClientLevel level, RandomSource random, Vec3 impact, Vec3 direction, BloodHitFxPacket packet) {
        int maxPerChunk = BallisticBlockConfig.MAX_BLOOD_DECALS_PER_CHUNK.get();
        if (maxPerChunk <= 0 || packet.armorReduction() < 0.16F) {
            return;
        }

        Vec3 start = impact.add(direction.scale(0.18D)).add(0.0D, 0.12D, 0.0D);
        Vec3 end = start.add(direction.scale(packet.shotgun() ? 0.55D : 0.32D)).add(0.0D, -3.2D, 0.0D);
        BlockHitResult result = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Minecraft.getInstance().player));
        if (result.getType() != HitResult.Type.BLOCK) {
            return;
        }

        Vec3 normal = Vec3.atLowerCornerOf(result.getDirection().getNormal()).normalize();
        if (normal.y < 0.45D) {
            return;
        }
        Vec3 position = result.getLocation().add(normal.scale(SURFACE_OFFSET));
        Basis basis = Basis.fromNormal(normal);
        double size = 0.22D + random.nextDouble() * 0.18D;
        if (packet.highCaliber()) {
            size *= 1.25D;
        }
        if (packet.shotgun()) {
            size *= 1.45D;
        }
        size *= Math.max(0.42D, packet.armorReduction());

        add(new BloodDecal(
                position.add(randomSurfaceOffset(random, basis, packet.shotgun() ? 0.16D : 0.09D)),
                backingBlockPos(position, normal),
                normal,
                size,
                random.nextFloat() * ((float) Math.PI * 2.0F),
                System.currentTimeMillis(),
                5 + random.nextInt(packet.shotgun() ? 8 : 5),
                packet.highCaliber(),
                packet.shotgun()
        ));
    }

    private static double splatterChance(ClientLevel level, Vec3 impact, BloodHitFxPacket packet) {
        double chance = BallisticBlockConfig.BLOOD_SPLATTER_CHANCE.get() * packet.armorReduction();
        if (packet.highCaliber()) {
            chance *= 1.35D;
        }
        if (packet.shotgun()) {
            chance *= 1.45D;
        }
        if (packet.armorPiercing() && packet.armored()) {
            chance *= 0.72D;
        }
        if (isIndoor(level, impact)) {
            chance *= 1.12D;
        }
        return Math.max(0.0D, Math.min(0.85D, chance));
    }

    private static void add(BloodDecal decal) {
        ChunkPos chunkPos = new ChunkPos((int) Math.floor(decal.position.x) >> 4, (int) Math.floor(decal.position.z) >> 4);
        ArrayDeque<BloodDecal> chunkDecals = DECALS.computeIfAbsent(chunkPos, ignored -> new ArrayDeque<>());
        chunkDecals.addLast(decal);
        int maxPerChunk = BallisticBlockConfig.MAX_BLOOD_DECALS_PER_CHUNK.get();
        while (chunkDecals.size() > maxPerChunk) {
            chunkDecals.removeFirst();
        }
    }

    private static void cleanup(ClientLevel level, long now) {
        long lifetimeMillis = BallisticBlockConfig.BLOOD_DECAL_LIFETIME_SECONDS.get() * 1000L;
        Iterator<Map.Entry<ChunkPos, ArrayDeque<BloodDecal>>> mapIterator = DECALS.entrySet().iterator();
        while (mapIterator.hasNext()) {
            ArrayDeque<BloodDecal> chunkDecals = mapIterator.next().getValue();
            while (!chunkDecals.isEmpty() && now - chunkDecals.peekFirst().createdMillis > lifetimeMillis) {
                chunkDecals.removeFirst();
            }
            chunkDecals.removeIf(decal -> !hasBackingBlock(level, decal));
            if (chunkDecals.isEmpty()) {
                mapIterator.remove();
            }
        }
    }

    private static boolean hasBackingBlock(ClientLevel level, BloodDecal decal) {
        if (level == null || !level.hasChunkAt(decal.backingPos)) {
            return true;
        }
        return !level.getBlockState(decal.backingPos).isAir();
    }

    private static void renderDecal(BufferBuilder buffer, Matrix4f matrix, Vec3 cameraPos, BloodDecal decal) {
        float alpha = fadeAlpha(decal);
        if (alpha <= 0.01F) {
            return;
        }
        Basis basis = Basis.fromNormal(decal.normal).rotated(decal.rotation);
        float r = decal.highCaliber ? 0.34F : 0.28F;
        float g = 0.006F;
        float b = 0.004F;
        addQuad(buffer, matrix, cameraPos, decal.position, basis, decal.size, decal.size * (0.72D + decal.droplets * 0.04D), r, g, b, 0.72F * alpha);
        for (int i = 0; i < decal.droplets; i++) {
            double angle = decal.rotation + i * 1.77D;
            double distance = decal.size * (0.42D + (i % 3) * 0.18D);
            Vec3 offset = basis.tangent.scale(Math.cos(angle) * distance).add(basis.bitangent.scale(Math.sin(angle) * distance));
            double dropletSize = decal.size * (decal.shotgun ? 0.28D : 0.23D) * (0.75D + (i % 2) * 0.24D);
            addQuad(buffer, matrix, cameraPos, decal.position.add(offset), basis.rotated((float) angle), dropletSize, dropletSize * 0.78D, r * 0.95F, g, b, 0.56F * alpha);
        }
    }

    private static float fadeAlpha(BloodDecal decal) {
        double lifetime = Math.max(1.0D, BallisticBlockConfig.BLOOD_DECAL_LIFETIME_SECONDS.get());
        double age = (System.currentTimeMillis() - decal.createdMillis) / 1000.0D;
        double normalizedAge = Math.max(0.0D, Math.min(1.0D, age / lifetime));
        if (normalizedAge <= 0.12D) {
            return 1.0F;
        }
        double fadeProgress = (normalizedAge - 0.12D) / 0.88D;
        return (float) Math.max(0.0D, Math.pow(1.0D - fadeProgress, 1.18D));
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

    private static int reserveParticles(int requested) {
        if (requested <= 0) {
            return 0;
        }
        long currentSecond = System.currentTimeMillis() / 1000L;
        if (currentSecond != particleLimiterSecond) {
            particleLimiterSecond = currentSecond;
            particlesThisSecond = 0;
        }
        int available = Math.max(0, MAX_MIST_PARTICLES_PER_SECOND - particlesThisSecond);
        int reserved = Math.min(requested, available);
        particlesThisSecond += reserved;
        return reserved;
    }

    private static boolean isIndoor(ClientLevel level, Vec3 position) {
        BlockPos pos = BlockPos.containing(position);
        return level.hasChunkAt(pos) && !level.canSeeSky(pos);
    }

    private static Vec3 randomSpread(RandomSource random, double amount) {
        return new Vec3(
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount
        );
    }

    private static Vec3 randomSurfaceOffset(RandomSource random, Basis basis, double amount) {
        return basis.tangent.scale((random.nextDouble() - 0.5D) * amount)
                .add(basis.bitangent.scale((random.nextDouble() - 0.5D) * amount));
    }

    private static BlockPos backingBlockPos(Vec3 position, Vec3 normal) {
        return BlockPos.containing(position.subtract(normal.scale(0.03D)));
    }

    private record BloodDecal(Vec3 position, BlockPos backingPos, Vec3 normal, double size, float rotation, long createdMillis, int droplets, boolean highCaliber, boolean shotgun) {
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
