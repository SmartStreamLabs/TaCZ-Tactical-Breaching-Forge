package com.victoriomods.taczballisticbreaching;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public final class ClientFragExplosionFxHandler {
    private static final double MAX_VISUAL_DISTANCE_SQR = 128.0D * 128.0D;
    private static long limiterSecond = -1L;
    private static int particlesThisSecond;
    private static long soundLimiterSecond = -1L;
    private static int soundsThisSecond;

    private ClientFragExplosionFxHandler() {
    }

    public static void handle(FragExplosionFxPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null) {
            return;
        }

        Vec3 origin = new Vec3(packet.x(), packet.y(), packet.z());
        double distance = player.getEyePosition().distanceTo(origin);
        if (distance > BallisticBlockConfig.FRAG_EXPLOSION_SOUND_DISTANCE.get()) {
            return;
        }

        RandomSource random = level.random;
        boolean indoor = isIndoor(level, origin);
        playLayeredExplosionAudio(level, random, player, origin, distance, indoor);
        if (player.distanceToSqr(origin) > MAX_VISUAL_DISTANCE_SQR) {
            return;
        }

        if (BallisticBlockConfig.ENABLE_FRAG_SMOKE.get()) {
            spawnSmoke(level, random, origin, packet.radius(), indoor);
            ClientIndoorSmokeManager.addExplosionSmoke(origin);
        }
        if (BallisticBlockConfig.ENABLE_FRAG_DEBRIS.get()) {
            spawnShrapnelBurst(level, random, origin, packet.radius(), indoor);
            spawnDustAndSparks(level, random, origin, packet.radius(), indoor);
        }
        if (BallisticBlockConfig.ENABLE_FRAG_INDOOR_PRESSURE.get() || BallisticBlockConfig.ENABLE_FRAG_CEILING_DUST.get()) {
            ClientShockwaveDustManager.explosion(origin);
        }
        if (BallisticBlockConfig.ENABLE_FRAG_EAR_RINGING.get()) {
            ClientSuppressionEffectsHandler.triggerExplosion(origin);
        }

        ClientCameraShakeHandler.explosion(origin);
        ClientNightstickFlashlightHandler.explosion(origin);
        DynamicGunLightManager.addBreachingFlash(origin, ImpactMaterial.STONE, true);
        ClientIndoorSmokeManager.illuminate(origin, indoor ? 1.65D : 1.2D, 1.0F, 0.66F, 0.25F);
        BurnMarkManager.addFromExplosion(origin);
    }

    private static void spawnSmoke(ClientLevel level, RandomSource random, Vec3 origin, float radius, boolean indoor) {
        Minecraft minecraft = Minecraft.getInstance();
        int baseCount = reserveParticles(reduceForGraphics(minecraft, Mth.clamp((int) (radius * (indoor ? 6.5F : 5.2F)), 22, indoor ? 70 : 56)));
        for (int i = 0; i < baseCount; i++) {
            Vec3 direction = randomDirection(random);
            double speed = (indoor ? 0.018D : 0.028D) + random.nextDouble() * (indoor ? 0.045D : 0.065D);
            Vec3 position = origin.add(direction.scale(random.nextDouble() * 0.85D)).add(0.0D, random.nextDouble() * 0.75D - 0.05D, 0.0D);
            Vec3 velocity = direction.scale(speed).add(0.0D, 0.035D + random.nextDouble() * 0.055D, 0.0D).add(randomSpread(random, 0.01D));
            Particle particle = minecraft.particleEngine.createParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
            if (particle != null) {
                float grey = 0.18F + random.nextFloat() * 0.16F;
                particle.setColor(grey, grey * 0.96F, grey * 0.9F);
                particle.scale((indoor ? 0.8F : 0.64F) + random.nextFloat() * 0.48F);
                particle.setLifetime(indoor ? 70 + random.nextInt(55) : 52 + random.nextInt(42));
            }
        }

        int columnCount = reserveParticles(reduceForGraphics(minecraft, Mth.clamp((int) (radius * (indoor ? 5.0F : 4.2F)), 18, indoor ? 58 : 46)));
        for (int i = 0; i < columnCount; i++) {
            double heightSeed = random.nextDouble();
            double columnRadius = (0.12D + heightSeed * 0.58D) * (0.75D + random.nextDouble() * 0.65D);
            double angle = random.nextDouble() * Math.PI * 2.0D;
            Vec3 position = origin.add(Math.cos(angle) * columnRadius, 0.28D + heightSeed * 1.35D, Math.sin(angle) * columnRadius);
            Vec3 velocity = new Vec3(
                    (random.nextDouble() - 0.5D) * 0.035D,
                    0.09D + random.nextDouble() * (indoor ? 0.075D : 0.11D),
                    (random.nextDouble() - 0.5D) * 0.035D
            );
            Particle particle = minecraft.particleEngine.createParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
            if (particle != null) {
                float grey = 0.08F + random.nextFloat() * 0.11F;
                particle.setColor(grey, grey * 0.95F, grey * 0.88F);
                particle.scale((indoor ? 1.05F : 0.88F) + random.nextFloat() * 0.55F);
                particle.setLifetime(indoor ? 95 + random.nextInt(70) : 72 + random.nextInt(58));
            }
        }

        int capCount = reserveParticles(reduceForGraphics(minecraft, Mth.clamp((int) (radius * 2.0F), 8, indoor ? 26 : 20)));
        for (int i = 0; i < capCount; i++) {
            Vec3 direction = randomDirection(random);
            Vec3 position = origin.add(direction.x * 0.35D, 1.4D + random.nextDouble() * 1.35D, direction.z * 0.35D);
            Vec3 velocity = new Vec3(direction.x * (0.025D + random.nextDouble() * 0.045D), 0.04D + random.nextDouble() * 0.045D, direction.z * (0.025D + random.nextDouble() * 0.045D));
            Particle particle = minecraft.particleEngine.createParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
            if (particle != null) {
                float grey = 0.12F + random.nextFloat() * 0.18F;
                particle.setColor(grey, grey * 0.96F, grey * 0.9F);
                particle.scale(0.9F + random.nextFloat() * 0.7F);
                particle.setLifetime(indoor ? 85 + random.nextInt(60) : 62 + random.nextInt(44));
            }
        }
    }

    private static void spawnDustAndSparks(ClientLevel level, RandomSource random, Vec3 origin, float radius, boolean indoor) {
        int dustCount = reserveParticles(reduceForGraphics(Minecraft.getInstance(), Mth.clamp((int) (radius * (indoor ? 8.8F : 6.4F)), 26, indoor ? 96 : 72)));
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(0.46F, 0.44F, 0.38F), indoor ? 0.82F : 0.68F);
        for (int i = 0; i < dustCount; i++) {
            Vec3 direction = randomDirection(random);
            double speed = 0.055D + random.nextDouble() * (indoor ? 0.11D : 0.16D);
            Vec3 position = origin.add(direction.scale(random.nextDouble() * 0.75D)).add(0.0D, random.nextDouble() * 0.65D - 0.12D, 0.0D);
            Vec3 velocity = direction.scale(speed).add(0.0D, 0.02D + random.nextDouble() * 0.07D, 0.0D).add(randomSpread(random, 0.018D));
            level.addParticle(dust, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
        }

        int sparkCount = reserveParticles(reduceForGraphics(Minecraft.getInstance(), Mth.clamp((int) (radius * 2.0F), 8, 20)));
        for (int i = 0; i < sparkCount; i++) {
            Vector3f color = random.nextBoolean() ? new Vector3f(1.0F, 0.63F, 0.08F) : new Vector3f(1.0F, 0.88F, 0.22F);
            DustParticleOptions spark = new DustParticleOptions(color, 0.5F + random.nextFloat() * 0.25F);
            Vec3 direction = randomDirection(random);
            Vec3 position = origin.add(direction.scale(0.18D + random.nextDouble() * 0.26D)).add(0.0D, 0.15D + random.nextDouble() * 0.25D, 0.0D);
            Vec3 velocity = direction.scale(0.08D + random.nextDouble() * 0.16D).add(0.0D, 0.02D + random.nextDouble() * 0.04D, 0.0D);
            Particle particle = Minecraft.getInstance().particleEngine.createParticle(spark, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
            if (particle != null) {
                particle.setLifetime(4 + random.nextInt(4));
            }
        }

        int debrisCount = reserveParticles(reduceForGraphics(Minecraft.getInstance(), Mth.clamp((int) (radius * 2.4F), 8, 26)));
        ItemParticleOption metalFragment = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.IRON_NUGGET));
        ItemParticleOption stoneChip = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.FLINT));
        for (int i = 0; i < debrisCount; i++) {
            Vec3 direction = randomDirection(random);
            double speed = 0.08D + random.nextDouble() * 0.18D;
            Vec3 position = origin.add(direction.scale(0.18D + random.nextDouble() * 0.36D)).add(0.0D, 0.15D + random.nextDouble() * 0.35D, 0.0D);
            Vec3 velocity = direction.scale(speed).add(0.0D, 0.055D + random.nextDouble() * 0.12D, 0.0D);
            level.addParticle(random.nextBoolean() ? metalFragment : stoneChip, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
        }
    }

    private static void spawnShrapnelBurst(ClientLevel level, RandomSource random, Vec3 origin, float radius, boolean indoor) {
        int streakCount = reserveParticles(reduceForGraphics(Minecraft.getInstance(), Mth.clamp((int) (radius * 12.0F), 96, indoor ? 180 : 160)));
        DustParticleOptions hotMetal = new DustParticleOptions(new Vector3f(1.0F, 0.78F, 0.18F), 0.95F);
        DustParticleOptions coolMetal = new DustParticleOptions(new Vector3f(0.78F, 0.75F, 0.68F), 0.72F);
        for (int i = 0; i < streakCount; i++) {
            Vec3 direction = randomDirection(random);
            double speed = 0.34D + random.nextDouble() * (indoor ? 0.58D : 0.72D);
            Vec3 position = origin.add(direction.scale(0.08D + random.nextDouble() * 0.22D)).add(0.0D, 0.18D + random.nextDouble() * 0.45D, 0.0D);
            Vec3 velocity = direction.scale(speed).add(randomSpread(random, 0.035D));
            Particle particle = Minecraft.getInstance().particleEngine.createParticle(random.nextFloat() < 0.34F ? hotMetal : coolMetal, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
            if (particle != null) {
                particle.setLifetime(9 + random.nextInt(indoor ? 14 : 11));
            }
            level.addParticle(ParticleTypes.CRIT, position.x, position.y, position.z, velocity.x * 0.95D, velocity.y * 0.95D, velocity.z * 0.95D);
            if (random.nextFloat() < 0.45F) {
                level.addParticle(ParticleTypes.FIREWORK, position.x, position.y, position.z, velocity.x * 0.6D, velocity.y * 0.6D, velocity.z * 0.6D);
            }
        }

        int chunkCount = reserveParticles(reduceForGraphics(Minecraft.getInstance(), Mth.clamp((int) (radius * 4.2F), 32, indoor ? 72 : 60)));
        ItemParticleOption shrapnelChip = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.IRON_NUGGET));
        for (int i = 0; i < chunkCount; i++) {
            Vec3 direction = randomDirection(random);
            double speed = 0.18D + random.nextDouble() * 0.36D;
            Vec3 position = origin.add(direction.scale(0.16D + random.nextDouble() * 0.32D)).add(0.0D, 0.12D + random.nextDouble() * 0.35D, 0.0D);
            Vec3 velocity = direction.scale(speed).add(0.0D, 0.04D + random.nextDouble() * 0.08D, 0.0D);
            level.addParticle(shrapnelChip, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
        }
    }

    private static void playLayeredExplosionAudio(ClientLevel level, RandomSource random, Player player, Vec3 origin, double distance, boolean indoor) {
        if (!reserveSounds(distance < 32.0D ? 4 : 2)) {
            return;
        }
        double maxDistance = Math.max(16.0D, BallisticBlockConfig.FRAG_EXPLOSION_SOUND_DISTANCE.get());
        float falloff = (float) Mth.clamp(1.0D - distance / maxDistance, 0.0D, 1.0D);
        float close = (float) Math.pow(Mth.clamp(1.0D - distance / 48.0D, 0.0D, 1.0D), 0.65D);
        float baseVolume = Mth.clamp(0.22F + falloff * 1.35F, 0.0F, 2.2F);
        float pitchJitter = random.nextFloat() * 0.08F;
        boolean closeListener = close > 0.02F;
        double listenerX = player.getX();
        double listenerY = player.getEyeY();
        double listenerZ = player.getZ();

        if (closeListener) {
            level.playLocalSound(listenerX, listenerY, listenerZ, ModSounds.FRAG_EXPLOSION_CLOSE.get(), SoundSource.PLAYERS, 2.35F * close, 0.92F + pitchJitter, false);
        } else if (BallisticBlockConfig.ENABLE_EXPLOSION_ECHO.get()) {
            level.playLocalSound(listenerX, listenerY, listenerZ, ModSounds.FRAG_EXPLOSION_FAR.get(), SoundSource.PLAYERS, baseVolume * 1.2F, 0.8F + random.nextFloat() * 0.08F, false);
        }
        if (indoor && BallisticBlockConfig.ENABLE_INDOOR_EXPLOSION_REVERB.get() && distance < 96.0D) {
            float reverb = (float) Mth.clamp((1.0D - distance / 96.0D) * 0.72D, 0.08D, 0.72D);
            level.playLocalSound(listenerX, listenerY, listenerZ, ModSounds.FRAG_EXPLOSION_INDOOR.get(), SoundSource.PLAYERS, reverb * 1.35F, 0.86F + random.nextFloat() * 0.08F, false);
        }
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

    private static boolean reserveSounds(int requested) {
        long currentSecond = System.currentTimeMillis() / 1000L;
        if (currentSecond != soundLimiterSecond) {
            soundLimiterSecond = currentSecond;
            soundsThisSecond = 0;
        }
        int max = 10;
        if (soundsThisSecond + requested > max) {
            return false;
        }
        soundsThisSecond += requested;
        return true;
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

    private static Vec3 randomDirection(RandomSource random) {
        double yaw = random.nextDouble() * Math.PI * 2.0D;
        double y = random.nextDouble() * 0.72D - 0.18D;
        return new Vec3(Math.cos(yaw), y, Math.sin(yaw)).normalize();
    }

    private static boolean isIndoor(ClientLevel level, Vec3 origin) {
        BlockPos pos = BlockPos.containing(origin);
        if (!level.canSeeSky(pos)) {
            return true;
        }
        int blocked = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (var direction : net.minecraft.core.Direction.values()) {
            for (int distance = 1; distance <= 5; distance++) {
                mutable.set(pos).move(direction, distance);
                BlockState state = level.getBlockState(mutable);
                if (!state.isAir() && (state.isSolidRender(level, mutable) || state.getSoundType() == SoundType.GLASS)) {
                    blocked++;
                    break;
                }
            }
        }
        return blocked >= 4;
    }

    private static Vec3 randomSpread(RandomSource random, double amount) {
        return new Vec3(
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount
        );
    }
}
