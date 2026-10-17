package com.victoriomods.taczballisticbreaching;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class ClientArmorHitFxHandler {
    private static final double MAX_VISIBLE_DISTANCE_SQR = 64.0D * 64.0D;
    private static final int MAX_PARTICLES_PER_SECOND = 160;
    private static final int MAX_SOUNDS_PER_SECOND = 14;
    private static long particleLimiterSecond = -1L;
    private static int particlesThisSecond;
    private static long soundLimiterSecond = -1L;
    private static int soundsThisSecond;

    private ClientArmorHitFxHandler() {
    }

    public static void handle(ArmorHitFxPacket packet) {
        if (!BallisticBlockConfig.ENABLE_ARMOR_HIT_EFFECTS.get()) {
            return;
        }
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

        RandomSource random = level.random;
        Vec3 normal = new Vec3(packet.normalX(), packet.normalY(), packet.normalZ());
        normal = normal.lengthSqr() < 0.001D ? new Vec3(0.0D, 1.0D, 0.0D) : normal.normalize();
        spawnParticles(level, random, impact, normal, packet);
        if (ClientDynamicImpactSoundHandler.enabled()) {
            ClientDynamicImpactSoundHandler.playArmorImpact(packet);
        } else {
            playSounds(level, random, impact, packet);
        }
        DynamicGunLightManager.addSparkFlash(impact);
        if (packet.deflected() || packet.highCaliber() || packet.vehicleHit()) {
            ClientCameraShakeHandler.closeImpact(impact, packet.highCaliber() || packet.vehicleHit());
        }
        visuallyStaggerTarget(level, packet);
    }

    private static void spawnParticles(ClientLevel level, RandomSource random, Vec3 impact, Vec3 normal, ArmorHitFxPacket packet) {
        int sparkCount = BallisticBlockConfig.ENABLE_ARMOR_SPARKS.get() ? BallisticBlockConfig.ARMOR_SPARK_AMOUNT.get() : 0;
        if (packet.vehicleHit()) {
            sparkCount = (int) Math.ceil(sparkCount * 1.65D);
        }
        if (packet.highCaliber()) {
            sparkCount = (int) Math.ceil(sparkCount * 1.35D);
        }
        if (packet.armorPiercing()) {
            sparkCount = Math.max(2, (int) Math.floor(sparkCount * 0.72D));
        }
        int smokeCount = packet.vehicleHit() || packet.highCaliber() ? 4 : 2;
        int debrisCount = packet.deflected() ? 4 : 2;
        int streakCount = packet.deflected() && BallisticBlockConfig.ENABLE_ARMOR_RICOCHET.get() ? 1 : 0;
        int budget = reserveParticles(sparkCount + smokeCount + debrisCount + streakCount);
        if (budget <= 0) {
            return;
        }
        int sparks = Math.min(sparkCount, budget);
        spawnSparks(level, random, impact, normal, sparks, packet);
        budget -= sparks;
        int smoke = Math.min(smokeCount, budget);
        spawnSmoke(level, random, impact, normal, smoke, packet);
        budget -= smoke;
        int debris = Math.min(debrisCount, budget);
        spawnMetalDebris(level, random, impact, normal, debris);
        budget -= debris;
        if (budget > 0 && streakCount > 0) {
            spawnRicochetStreak(level, random, impact, normal);
        }
    }

    private static void spawnSparks(ClientLevel level, RandomSource random, Vec3 impact, Vec3 normal, int count, ArmorHitFxPacket packet) {
        for (int i = 0; i < count; i++) {
            Vector3f color = random.nextBoolean() ? new Vector3f(1.0F, 0.68F, 0.08F) : new Vector3f(1.0F, 0.92F, 0.25F);
            DustParticleOptions spark = new DustParticleOptions(color, 0.52F + random.nextFloat() * 0.38F);
            double speed = packet.deflected() ? 0.18D : 0.11D;
            if (packet.vehicleHit()) {
                speed *= 1.25D;
            }
            Vec3 tangent = randomSpread(random, 0.09D);
            Vec3 velocity = normal.scale(speed + random.nextDouble() * speed).add(tangent);
            Vec3 offset = normal.scale(0.05D).add(randomSpread(random, 0.035D));
            Particle particle = Minecraft.getInstance().particleEngine.createParticle(spark, impact.x + offset.x, impact.y + offset.y, impact.z + offset.z, velocity.x, velocity.y, velocity.z);
            if (particle != null) {
                particle.setLifetime(3 + random.nextInt(packet.deflected() ? 6 : 4));
            }
        }
    }

    private static void spawnSmoke(ClientLevel level, RandomSource random, Vec3 impact, Vec3 normal, int count, ArmorHitFxPacket packet) {
        for (int i = 0; i < count; i++) {
            Vec3 velocity = normal.scale(0.012D + random.nextDouble() * 0.018D).add(randomSpread(random, 0.012D)).add(0.0D, 0.006D, 0.0D);
            Particle particle = Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, impact.x, impact.y, impact.z, velocity.x, velocity.y, velocity.z);
            if (particle != null) {
                float grey = 0.42F + random.nextFloat() * 0.18F;
                particle.setColor(grey, grey, grey);
                particle.scale((packet.vehicleHit() ? 0.62F : 0.45F) + random.nextFloat() * 0.22F);
                particle.setLifetime(8 + random.nextInt(8));
            }
        }
    }

    private static void spawnMetalDebris(ClientLevel level, RandomSource random, Vec3 impact, Vec3 normal, int count) {
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(0.42F, 0.40F, 0.36F), 0.44F);
        for (int i = 0; i < count; i++) {
            Vec3 velocity = normal.scale(0.035D + random.nextDouble() * 0.04D).add(randomSpread(random, 0.035D));
            level.addParticle(dust, impact.x, impact.y, impact.z, velocity.x, velocity.y, velocity.z);
        }
    }

    private static void spawnRicochetStreak(ClientLevel level, RandomSource random, Vec3 impact, Vec3 normal) {
        DustParticleOptions streak = new DustParticleOptions(new Vector3f(1.0F, 0.82F, 0.28F), 0.85F);
        Vec3 direction = normal.add(randomSpread(random, 0.36D)).normalize();
        for (int i = 0; i < 4; i++) {
            Vec3 pos = impact.add(direction.scale(0.08D * i));
            level.addParticle(streak, pos.x, pos.y, pos.z, direction.x * 0.16D, direction.y * 0.16D, direction.z * 0.16D);
        }
    }

    private static void playSounds(ClientLevel level, RandomSource random, Vec3 impact, ArmorHitFxPacket packet) {
        if (!reserveSound()) {
            return;
        }
        float baseVolume = BallisticBlockConfig.ARMOR_HIT_VOLUME.get().floatValue();
        float volume = baseVolume * (0.72F + random.nextFloat() * 0.35F);
        float pitch = 0.86F + random.nextFloat() * 0.28F;
        if (packet.vehicleHit()) {
            volume *= 1.35F;
            pitch -= 0.14F;
        }
        if (packet.highCaliber()) {
            volume *= 1.22F;
            pitch -= 0.08F;
        }
        if (packet.helmetHit()) {
            volume *= 1.12F;
            pitch += 0.22F;
        }
        SoundEvent thud = packet.vehicleHit() ? SoundEvents.ANVIL_LAND : SoundEvents.ARMOR_EQUIP_IRON.value();
        level.playLocalSound(impact.x, impact.y, impact.z, thud, SoundSource.PLAYERS, Math.min(volume, 4.0F), Mth.clamp(pitch, 0.65F, 1.55F), false);
        if (packet.deflected() || random.nextFloat() < (packet.weakCaliber() ? 0.42F : 0.18F)) {
            float pingPitch = packet.helmetHit() ? 1.45F + random.nextFloat() * 0.25F : 0.95F + random.nextFloat() * 0.35F;
            level.playLocalSound(impact.x, impact.y, impact.z, choosePing(random, packet), SoundSource.PLAYERS, Math.min(volume * 0.72F, 3.0F), pingPitch, false);
        }
    }

    private static SoundEvent choosePing(RandomSource random, ArmorHitFxPacket packet) {
        if (packet.vehicleHit()) {
            return random.nextBoolean() ? SoundEvents.CHAIN_HIT : SoundEvents.ANVIL_LAND;
        }
        if (packet.helmetHit()) {
            return random.nextBoolean() ? SoundEvents.SHIELD_BLOCK : SoundEvents.CHAIN_HIT;
        }
        return random.nextBoolean() ? SoundEvents.CHAIN_HIT : SoundEvents.TRIDENT_HIT;
    }

    private static void visuallyStaggerTarget(ClientLevel level, ArmorHitFxPacket packet) {
        Entity target = level.getEntity(packet.targetId());
        if (target == null || !(packet.highCaliber() || packet.vehicleHit() || packet.deflected())) {
            return;
        }
        target.hurtMarked = true;
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
        int available = Math.max(0, MAX_PARTICLES_PER_SECOND - particlesThisSecond);
        int reserved = Math.min(requested, available);
        particlesThisSecond += reserved;
        return reserved;
    }

    private static boolean reserveSound() {
        long currentSecond = System.currentTimeMillis() / 1000L;
        if (currentSecond != soundLimiterSecond) {
            soundLimiterSecond = currentSecond;
            soundsThisSecond = 0;
        }
        if (soundsThisSecond >= MAX_SOUNDS_PER_SECOND) {
            return false;
        }
        soundsThisSecond++;
        return true;
    }

    private static Vec3 randomSpread(RandomSource random, double amount) {
        return new Vec3(
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount
        );
    }
}
