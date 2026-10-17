package com.victoriomods.taczballisticbreaching;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class ClientMuzzleSmokeHandler {
    private static final double MAX_VISIBLE_DISTANCE_SQR = 64.0D * 64.0D;
    private static final long BUILDUP_WINDOW_MILLIS = 1400L;
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

        double quality = switch (BallisticBlockConfig.SMOKE_QUALITY.get()) {
            case "LOW" -> 0.5D;
            case "HIGH" -> 1.5D;
            case "ULTRA" -> 2.0D;
            default -> 1.0D;
        };
        int amount = (int) Math.ceil(calculateAmount(packet) * quality * Math.min(1.35D, heatSmokeMultiplier));
        double distance = player.distanceToSqr(muzzle);
        if (distance > 32.0D * 32.0D) amount = Math.max(1, amount / 3);
        else if (distance > 16.0D * 16.0D) amount = Math.max(1, amount / 2);
        amount = reduceForParticleSettings(minecraft, amount);
        int reserved = reserveParticleBudget(amount);
        if (reserved <= 0) {
            return;
        }

        int smokeCount = Math.min(amount, reserved);
        spawnSmoke(level, random, muzzle, forward, smokeCount, packet);
        ClientIndoorSmokeManager.addWeaponSmoke(packet, muzzle, forward, smokeCount, heatSmokeMultiplier);
        BurnMarkManager.addHeatResidue(packet, muzzle, forward, heatSmokeMultiplier);
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
        int maxPerSecond = Math.min(BallisticBlockConfig.MAX_SMOKE_PARTICLES_PER_SECOND.get(), 60);
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
        ClientMuzzleSmokeRenderer.spawn(muzzle, forward, count, packet, isIndoor(level, muzzle), random);
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
