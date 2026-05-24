package com.victoriomods.taczballisticbreaching;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class ClientMuzzleSmokeHandler {
    private static final double MAX_VISIBLE_DISTANCE_SQR = 64.0D * 64.0D;
    private static final long BUILDUP_WINDOW_MILLIS = 1400L;
    private static final double CAMPFIRE_SMOKE_DENSITY_MULTIPLIER = 0.38D;
    private static long limiterSecond = -1L;
    private static int particlesThisSecond;
    private static final Map<Integer, FireStreak> FIRE_STREAKS = new HashMap<>();

    private ClientMuzzleSmokeHandler() {
    }

    public static void handle(MuzzleSmokePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null) {
            return;
        }

        Vec3 muzzle = new Vec3(packet.x(), packet.y(), packet.z());
        if (player.distanceToSqr(muzzle) > MAX_VISIBLE_DISTANCE_SQR) {
            return;
        }

        RandomSource random = level.random;
        Vec3 forward = new Vec3(packet.forwardX(), packet.forwardY(), packet.forwardZ());
        forward = forward.lengthSqr() < 0.001D ? new Vec3(0.0D, 0.0D, 1.0D) : forward.normalize();
        double heatSmokeMultiplier = ClientWeaponHeatHandler.recordShot(packet, muzzle, forward);
        DynamicGunLightManager.addMuzzleFlash(muzzle.add(forward.scale(0.08D)), packet);
        ClientGunshotAcousticsHandler.shot(packet, muzzle, forward);
        triggerCameraShake(packet, muzzle);
        if (packet.automatic()) {
            ClientSuppressionEffectsHandler.triggerSustainedFire(muzzle, packet.highCaliber(), packet.suppressed());
        }

        if (!BallisticBlockConfig.ENABLE_MUZZLE_SMOKE.get()) {
            return;
        }

        int amount = (int) Math.ceil(calculateAmount(packet) * heatSmokeMultiplier * CAMPFIRE_SMOKE_DENSITY_MULTIPLIER);
        amount = reduceForParticleSettings(minecraft, amount);
        int reserved = reserveParticleBudget(amount + 1);
        if (reserved <= 0) {
            return;
        }

        int smokeCount = Math.min(amount, reserved);
        spawnSmoke(level, random, muzzle, forward, smokeCount, packet);
        ClientIndoorSmokeManager.addWeaponSmoke(packet, muzzle, forward, smokeCount, heatSmokeMultiplier);
        BurnMarkManager.addHeatResidue(packet, muzzle, forward, heatSmokeMultiplier);
        if (reserved > smokeCount) {
            spawnMuzzleFlash(level, random, muzzle, forward);
        }
    }

    private static void triggerCameraShake(MuzzleSmokePacket packet, Vec3 muzzle) {
        MuzzleSmokeNetwork.WeaponKind kind = MuzzleSmokeNetwork.WeaponKind.byId(packet.weaponKind());
        if (kind == MuzzleSmokeNetwork.WeaponKind.SHOTGUN) {
            ClientCameraShakeHandler.shotgun(muzzle, packet.suppressed(), packet.highCaliber());
            return;
        }
        if (kind == MuzzleSmokeNetwork.WeaponKind.SNIPER || packet.highCaliber()) {
            ClientCameraShakeHandler.shotgun(muzzle, packet.suppressed(), true);
        }
        if (packet.automatic()) {
            ClientCameraShakeHandler.heavyAutomatic(muzzle, packet.suppressed());
        }
    }

    private static int calculateAmount(MuzzleSmokePacket packet) {
        int amount = BallisticBlockConfig.MUZZLE_SMOKE_AMOUNT.get();
        MuzzleSmokeNetwork.WeaponKind kind = MuzzleSmokeNetwork.WeaponKind.byId(packet.weaponKind());
        amount = switch (kind) {
            case PISTOL -> Math.max(1, (int) Math.ceil(amount * 0.65D));
            case RIFLE -> amount;
            case SHOTGUN, SNIPER -> (int) Math.ceil(amount * 1.35D);
        };
        if (packet.highCaliber()) {
            amount = (int) Math.ceil(amount * 1.25D);
        }
        if (packet.suppressed()) {
            amount = Math.max(1, (int) Math.floor(amount * 0.38D));
        }
        if (packet.automatic() && BallisticBlockConfig.DYNAMIC_SMOKE_BUILDUP.get()) {
            amount = (int) Math.ceil(amount * getBuildupMultiplier(packet.shooterId()));
        }
        return Math.min(amount, 14);
    }

    private static double getBuildupMultiplier(int shooterId) {
        long now = System.currentTimeMillis();
        cleanupStreaks(now);
        FireStreak streak = FIRE_STREAKS.get(shooterId);
        if (streak == null || now - streak.lastShotMillis > BUILDUP_WINDOW_MILLIS) {
            FIRE_STREAKS.put(shooterId, new FireStreak(now, 1));
            return 1.0D;
        }
        int shots = Math.min(streak.shots + 1, 12);
        FIRE_STREAKS.put(shooterId, new FireStreak(now, shots));
        return 1.0D + shots * 0.11D;
    }

    private static void cleanupStreaks(long now) {
        Iterator<Map.Entry<Integer, FireStreak>> iterator = FIRE_STREAKS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue().lastShotMillis > BUILDUP_WINDOW_MILLIS * 3L) {
                iterator.remove();
            }
        }
    }

    private static int reduceForParticleSettings(Minecraft minecraft, int amount) {
        String particleMode = String.valueOf(minecraft.options.particles().get()).toLowerCase(java.util.Locale.ROOT);
        if (particleMode.contains("minimal")) {
            return Math.max(1, amount / 3);
        }
        if (particleMode.contains("decreased")) {
            return Math.max(1, amount / 2);
        }
        return amount;
    }

    private static int reserveParticleBudget(int requested) {
        int maxPerSecond = BallisticBlockConfig.MAX_SMOKE_PARTICLES_PER_SECOND.get();
        if (maxPerSecond <= 0 || requested <= 0) {
            return 0;
        }
        long currentSecond = System.currentTimeMillis() / 1000L;
        if (currentSecond != limiterSecond) {
            limiterSecond = currentSecond;
            particlesThisSecond = 0;
        }
        int available = Math.max(0, maxPerSecond - particlesThisSecond);
        int reserved = Math.min(requested, available);
        particlesThisSecond += reserved;
        return reserved;
    }

    private static void spawnSmoke(ClientLevel level, RandomSource random, Vec3 muzzle, Vec3 forward, int count, MuzzleSmokePacket packet) {
        float baseScale = BallisticBlockConfig.MUZZLE_SMOKE_SCALE.get().floatValue();
        int baseLifetime = Math.max(4, (int) Math.ceil(BallisticBlockConfig.MUZZLE_SMOKE_LIFETIME_TICKS.get() * 0.62D));
        double indoorMultiplier = isIndoor(level, muzzle) ? 1.08D : 0.86D;

        for (int i = 0; i < count; i++) {
            Vec3 offset = randomSpread(random, 0.035D).add(forward.scale(0.025D + random.nextDouble() * 0.04D));
            Vec3 velocity = forward.scale(0.018D + random.nextDouble() * 0.035D)
                    .add(0.0D, 0.018D + random.nextDouble() * 0.028D, 0.0D)
                    .add(randomSpread(random, 0.018D));
            Particle particle = Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, muzzle.x + offset.x, muzzle.y + offset.y, muzzle.z + offset.z, velocity.x, velocity.y, velocity.z);
            if (particle != null) {
                float grey = 0.58F + random.nextFloat() * 0.18F;
                particle.setColor(grey, grey, grey);
                float scale = baseScale * (0.42F + random.nextFloat() * 0.32F);
                if (packet.suppressed()) {
                    scale *= 0.72F;
                }
                if (packet.highCaliber()) {
                    scale *= 1.08F;
                }
                particle.scale(scale);
                int lifetime = (int) Math.round((baseLifetime + random.nextInt(7) - 3) * indoorMultiplier);
                particle.setLifetime(Math.max(3, lifetime));
            }
        }
    }

    private static void spawnMuzzleFlash(ClientLevel level, RandomSource random, Vec3 muzzle, Vec3 forward) {
        if (level.getMaxLocalRawBrightness(BlockPos.containing(muzzle)) > 8 && random.nextFloat() > 0.35F) {
            return;
        }
        Vec3 position = muzzle.add(forward.scale(0.035D));
        DustParticleOptions glow = new DustParticleOptions(new Vector3f(1.0F, 0.62F, 0.18F), 0.65F + random.nextFloat() * 0.35F);
        Particle particle = Minecraft.getInstance().particleEngine.createParticle(glow, position.x, position.y, position.z, forward.x * 0.015D, forward.y * 0.015D, forward.z * 0.015D);
        if (particle != null) {
            particle.setLifetime(2 + random.nextInt(2));
        }
    }

    private static boolean isIndoor(ClientLevel level, Vec3 position) {
        BlockPos pos = BlockPos.containing(position);
        return level.canSeeSky(pos) ? false : level.getMaxLocalRawBrightness(pos) < 12;
    }

    private static Vec3 randomSpread(RandomSource random, double amount) {
        return new Vec3(
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount
        );
    }

    private record FireStreak(long lastShotMillis, int shots) {
    }
}
