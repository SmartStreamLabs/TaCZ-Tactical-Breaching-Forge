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
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public final class ClientShockwaveDustManager {
    private static final double MAX_DISTANCE_SQR = 80.0D * 80.0D;
    private static final int MAX_WAVES = 24;
    private static final int MAX_FALLOFFS = 72;
    private static final ArrayDeque<Shockwave> WAVES = new ArrayDeque<>();
    private static final ArrayDeque<FalloffEmitter> FALLOFFS = new ArrayDeque<>();
    private static long limiterSecond = -1L;
    private static int particlesThisSecond;

    private ClientShockwaveDustManager() {
    }

    public static void explosion(Vec3 origin) {
        if (!BallisticBlockConfig.ENABLE_SHOCKWAVE_DUST.get() && !BallisticBlockConfig.ENABLE_INDOOR_PRESSURE_EFFECT.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null || player.distanceToSqr(origin) > MAX_DISTANCE_SQR) {
            return;
        }

        double enclosure = enclosureScore(level, BlockPos.containing(origin));
        boolean indoor = enclosure >= 0.34D;
        double intensity = BallisticBlockConfig.SHOCKWAVE_DUST_INTENSITY.get() * (indoor ? 0.82D + enclosure : 0.28D);
        if (intensity <= 0.01D) {
            return;
        }

        if (BallisticBlockConfig.ENABLE_SHOCKWAVE_DUST.get()) {
            addWave(level, origin, indoor, intensity);
            spawnShockwaveBurst(level, origin, indoor, intensity);
        }
        if (BallisticBlockConfig.ENABLE_DEBRIS_FALLOFF.get() && indoor) {
            addFalloffEmitters(level, origin, intensity, enclosure);
        }
        if (BallisticBlockConfig.ENABLE_INDOOR_PRESSURE_EFFECT.get()) {
            pressureFeedback(level, player, origin, indoor, intensity);
        }
        DynamicGunLightManager.addBreachingFlash(origin, ImpactMaterial.STONE, true);
        ClientIndoorSmokeManager.illuminate(origin, 1.4D * intensity, 1.0F, 0.68F, 0.28F);
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null) {
            clear();
            return;
        }
        long now = System.currentTimeMillis();
        WAVES.removeIf(wave -> now - wave.createdMillis > wave.lifetimeMillis);

        Iterator<FalloffEmitter> iterator = FALLOFFS.iterator();
        while (iterator.hasNext()) {
            FalloffEmitter emitter = iterator.next();
            if (now - emitter.createdMillis > emitter.lifetimeMillis || player.distanceToSqr(emitter.position) > MAX_DISTANCE_SQR) {
                iterator.remove();
                continue;
            }
            spawnFalloff(level, emitter);
        }
    }

    public static void render(PoseStack poseStack, Camera camera) {
        if (!BallisticBlockConfig.ENABLE_SHOCKWAVE_DUST.get() || WAVES.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Vec3 cameraPos = camera.getPosition();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (Shockwave wave : WAVES) {
            if (wave.origin.distanceToSqr(cameraPos) <= MAX_DISTANCE_SQR) {
                renderWave(buffer, matrix, cameraPos, wave, now);
            }
        }

        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void clear() {
        WAVES.clear();
        FALLOFFS.clear();
        limiterSecond = -1L;
        particlesThisSecond = 0;
    }

    private static void addWave(ClientLevel level, Vec3 origin, boolean indoor, double intensity) {
        WAVES.addLast(new Shockwave(origin, indoor, (float) Mth.clamp(intensity, 0.1D, 2.2D), System.currentTimeMillis(), indoor ? 950L : 560L, level.random.nextFloat() * ((float) Math.PI * 2.0F)));
        while (WAVES.size() > MAX_WAVES) {
            WAVES.removeFirst();
        }
    }

    private static void spawnShockwaveBurst(ClientLevel level, Vec3 origin, boolean indoor, double intensity) {
        RandomSource random = level.random;
        int count = reserveParticles(reduceForGraphics(Minecraft.getInstance(), (int) Math.ceil((indoor ? 58.0D : 24.0D) * intensity)));
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(0.50F, 0.48F, 0.42F), indoor ? 0.86F : 0.58F);
        for (int i = 0; i < count; i++) {
            Vec3 outward = randomHorizontal(random);
            double speed = (indoor ? 0.05D : 0.035D) + random.nextDouble() * (indoor ? 0.13D : 0.07D);
            Vec3 position = origin.add(outward.scale(random.nextDouble() * 0.55D)).add(0.0D, random.nextDouble() * 1.4D - 0.25D, 0.0D);
            Vec3 velocity = outward.scale(speed * intensity).add(0.0D, 0.012D + random.nextDouble() * 0.04D, 0.0D).add(randomSpread(random, 0.018D));
            if (i % 4 == 0) {
            Particle particle = Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
                if (particle != null) {
                    float grey = 0.52F + random.nextFloat() * 0.15F;
                    particle.setColor(grey, grey, grey * 0.92F);
                    particle.scale((float) ((indoor ? 0.75D : 0.45D) + random.nextDouble() * 0.65D));
                    particle.setLifetime(Mth.clamp((int) (BallisticBlockConfig.DUST_LIFETIME_TICKS.get() * (indoor ? 0.85D : 0.36D)), 8, 160));
                }
            } else {
                level.addParticle(dust, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
            }
        }
    }

    private static void addFalloffEmitters(ClientLevel level, Vec3 origin, double intensity, double enclosure) {
        RandomSource random = level.random;
        int attempts = Math.min(32, Math.max(8, (int) Math.ceil(12.0D * intensity)));
        for (int i = 0; i < attempts; i++) {
            Vec3 sample = origin.add(randomSpread(random, 7.2D));
            BlockPos ceiling = findCeiling(level, BlockPos.containing(sample));
            if (ceiling == null) {
                continue;
            }
            ImpactMaterial material = ImpactMaterial.classify(level.getBlockState(ceiling));
            Vec3 position = Vec3.atCenterOf(ceiling).add(0.0D, -0.54D, 0.0D).add(randomSpread(random, 0.55D));
            int lifetime = Mth.clamp((int) (BallisticBlockConfig.DUST_LIFETIME_TICKS.get() * (0.55D + random.nextDouble() * 0.75D) * enclosure), 18, 180);
            FALLOFFS.addLast(new FalloffEmitter(position, material, (float) intensity, System.currentTimeMillis() + random.nextInt(260), lifetime * 50L, random.nextInt(5)));
            while (FALLOFFS.size() > MAX_FALLOFFS) {
                FALLOFFS.removeFirst();
            }
        }
    }

    private static void spawnFalloff(ClientLevel level, FalloffEmitter emitter) {
        long now = System.currentTimeMillis();
        if (now < emitter.createdMillis || level.random.nextInt(3 + emitter.stagger) != 0) {
            return;
        }
        int count = reserveParticles(reduceForGraphics(Minecraft.getInstance(), emitter.intensity > 1.2F && level.random.nextBoolean() ? 2 : 1));
        if (count <= 0) {
            return;
        }
        RandomSource random = level.random;
        DustParticleOptions dust = dustFor(emitter.material, random);
        for (int i = 0; i < count; i++) {
            Vec3 position = emitter.position.add(randomSpread(random, 0.34D));
            Vec3 velocity = randomSpread(random, 0.014D).add(0.0D, -0.024D - random.nextDouble() * 0.055D, 0.0D);
            if (emitter.material == ImpactMaterial.GLASS && random.nextInt(4) == 0) {
                velocity = velocity.add(randomSpread(random, 0.026D));
            }
            level.addParticle(dust, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
        }
    }

    private static void pressureFeedback(ClientLevel level, Player player, Vec3 origin, boolean indoor, double intensity) {
        double distance = player.getEyePosition().distanceTo(origin);
        double maxDistance = indoor ? 22.0D : 13.0D;
        if (distance > maxDistance) {
            return;
        }
        float closeness = (float) (1.0D - Mth.clamp(distance / maxDistance, 0.0D, 1.0D));
        float strength = (float) (closeness * closeness * intensity * BallisticBlockConfig.PRESSURE_EFFECT_STRENGTH.get());
        if (strength <= 0.02F) {
            return;
        }
        ClientCameraShakeHandler.breaching(origin, strength > 0.18F);
        ClientSuppressionEffectsHandler.triggerExplosion(origin);
        float volume = Mth.clamp(strength * (indoor ? 0.46F : 0.22F), 0.04F, 0.5F);
        level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, volume, 0.52F + level.random.nextFloat() * 0.12F, false);
    }

    private static void renderWave(BufferBuilder buffer, Matrix4f matrix, Vec3 cameraPos, Shockwave wave, long now) {
        double progress = Mth.clamp((now - wave.createdMillis) / (double) wave.lifetimeMillis, 0.0D, 1.0D);
        float alpha = (float) ((1.0D - progress) * (wave.indoor ? 0.18D : 0.075D) * wave.intensity);
        if (alpha <= 0.01F) {
            return;
        }
        float radius = (float) ((wave.indoor ? 1.2D : 1.8D) + progress * progress * (wave.indoor ? 9.0D : 6.0D) * wave.intensity);
        float height = (float) (0.55D + progress * (wave.indoor ? 1.2D : 0.7D));
        Vec3 center = wave.origin.add(0.0D, height * 0.5D, 0.0D).subtract(cameraPos);
        float red = wave.indoor ? 0.56F : 0.52F;
        float green = wave.indoor ? 0.54F : 0.53F;
        float blue = wave.indoor ? 0.49F : 0.50F;
        Vec3 x = new Vec3(Math.cos(wave.rotation), 0.0D, Math.sin(wave.rotation));
        Vec3 z = new Vec3(-x.z, 0.0D, x.x);
        addQuad(buffer, matrix, center, x, new Vec3(0.0D, 1.0D, 0.0D), radius, height, red, green, blue, alpha);
        addQuad(buffer, matrix, center, z, new Vec3(0.0D, 1.0D, 0.0D), radius * 0.82F, height * 0.92F, red, green, blue, alpha * 0.72F);
    }

    private static void addQuad(BufferBuilder buffer, Matrix4f matrix, Vec3 center, Vec3 tangent, Vec3 bitangent, float width, float height, float red, float green, float blue, float alpha) {
        Vec3 tx = tangent.scale(width);
        Vec3 by = bitangent.scale(height);
        vertex(buffer, matrix, center.subtract(tx).subtract(by), red, green, blue, 0.0F);
        vertex(buffer, matrix, center.add(tx).subtract(by), red, green, blue, 0.0F);
        vertex(buffer, matrix, center.add(tx.scale(0.18D)).add(by.scale(0.18D)), red, green, blue, alpha);
        vertex(buffer, matrix, center.subtract(tx).add(by), red, green, blue, 0.0F);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 position, float red, float green, float blue, float alpha) {
        buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z).color(red, green, blue, alpha).endVertex();
    }

    private static DustParticleOptions dustFor(ImpactMaterial material, RandomSource random) {
        return switch (material) {
            case WOOD -> new DustParticleOptions(new Vector3f(0.58F + random.nextFloat() * 0.08F, 0.39F, 0.20F), 0.72F);
            case GLASS -> new DustParticleOptions(new Vector3f(0.78F, 0.93F, 1.0F), 0.54F);
            case METAL -> new DustParticleOptions(new Vector3f(0.72F, 0.69F, 0.62F), 0.48F);
            case STONE -> new DustParticleOptions(new Vector3f(0.45F, 0.44F, 0.40F), 0.76F);
            default -> new DustParticleOptions(new Vector3f(0.49F, 0.46F, 0.40F), 0.68F);
        };
    }

    private static BlockPos findCeiling(ClientLevel level, BlockPos start) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int y = 1; y <= 7; y++) {
            mutable.set(start).move(Direction.UP, y);
            BlockState state = level.getBlockState(mutable);
            if (!state.isAir() && state.isSolidRender(level, mutable)) {
                return mutable.immutable();
            }
        }
        return null;
    }

    private static double enclosureScore(ClientLevel level, BlockPos pos) {
        int blocked = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            for (int distance = 1; distance <= 5; distance++) {
                mutable.set(pos).move(direction, distance);
                BlockState state = level.getBlockState(mutable);
                if (!state.isAir() && (state.isSolidRender(level, mutable) || state.getSoundType() == SoundType.GLASS)) {
                    blocked++;
                    break;
                }
            }
        }
        double score = blocked / 6.0D;
        if (!level.canSeeSky(pos)) {
            score += 0.22D;
        }
        if (level.getMaxLocalRawBrightness(pos) < 12) {
            score += 0.10D;
        }
        return Mth.clamp(score, 0.0D, 1.0D);
    }

    private static int reserveParticles(int requested) {
        if (requested <= 0) {
            return 0;
        }
        long currentSecond = System.currentTimeMillis() / 1000L;
        if (currentSecond != limiterSecond) {
            limiterSecond = currentSecond;
            particlesThisSecond = 0;
        }
        int available = Math.max(0, BallisticBlockConfig.MAX_SHOCKWAVE_PARTICLES.get() - particlesThisSecond);
        int reserved = Math.min(requested, available);
        particlesThisSecond += reserved;
        return reserved;
    }

    private static int reduceForGraphics(Minecraft minecraft, int amount) {
        String particles = String.valueOf(minecraft.options.particles().get()).toLowerCase(Locale.ROOT);
        String graphics = String.valueOf(minecraft.options.graphicsMode().get()).toLowerCase(Locale.ROOT);
        if (particles.contains("minimal")) {
            return Math.max(1, amount / 4);
        }
        if (particles.contains("decreased") || graphics.contains("fast")) {
            return Math.max(1, amount / 2);
        }
        return amount;
    }

    private static Vec3 randomHorizontal(RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        return new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
    }

    private static Vec3 randomSpread(RandomSource random, double amount) {
        return new Vec3(
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount
        );
    }

    private record Shockwave(Vec3 origin, boolean indoor, float intensity, long createdMillis, long lifetimeMillis, float rotation) {
    }

    private record FalloffEmitter(Vec3 position, ImpactMaterial material, float intensity, long createdMillis, long lifetimeMillis, int stagger) {
    }
}
