package com.victoriomods.taczballisticbreaching;

import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class ClientSuppressionSoundHandler {
    private static final double TRACK_RADIUS = 24.0D;
    private static final long BULLET_MEMORY_MILLIS = 2500L;
    private static final long TINNITUS_WINDOW_MILLIS = 2600L;
    private static final Map<Integer, Long> TRIGGERED_BULLETS = new HashMap<>();
    private static long limiterSecond = -1L;
    private static int soundsThisSecond;
    private static int recentHeavyPasses;
    private static long lastHeavyPassMillis;
    private static long lastTinnitusMillis;

    private ClientSuppressionSoundHandler() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null || !BallisticBlockConfig.ENABLE_SUPPRESSION_SOUNDS.get()) {
            return;
        }

        long now = System.currentTimeMillis();
        cleanup(now);
        double radius = Math.max(BallisticBlockConfig.SUPPRESSION_RADIUS.get(), BallisticBlockConfig.SUPPRESSION_EFFECT_RADIUS.get());
        AABB area = player.getBoundingBox().inflate(TRACK_RADIUS);
        for (EntityKineticBullet bullet : level.getEntitiesOfClass(EntityKineticBullet.class, area, bullet -> bullet.isAlive() && !TRIGGERED_BULLETS.containsKey(bullet.getId()))) {
            if (!BallisticBlockConfig.ENABLE_SHOOTER_SUPPRESSION.get() && isLocalShooter(bullet, player)) {
                continue;
            }
            Flyby flyby = closestFlyby(bullet, player, radius);
            if (flyby == null) {
                continue;
            }
            TRIGGERED_BULLETS.put(bullet.getId(), now);
            playSuppression(level, player, bullet, flyby, radius);
        }

    }

    public static void clear() {
        TRIGGERED_BULLETS.clear();
        soundsThisSecond = 0;
        limiterSecond = -1L;
        recentHeavyPasses = 0;
        lastHeavyPassMillis = 0L;
        lastTinnitusMillis = 0L;
    }

    private static Flyby closestFlyby(EntityKineticBullet bullet, Player player, double radius) {
        Vec3 current = bullet.position();
        Vec3 previous = current.subtract(bullet.getDeltaMovement());
        Vec3 head = player.getEyePosition();
        Vec3 body = player.position().add(0.0D, player.getBbHeight() * 0.55D, 0.0D);

        ClosestPoint headPass = closestPointOnSegment(previous, current, head);
        ClosestPoint bodyPass = closestPointOnSegment(previous, current, body);
        ClosestPoint closest = headPass.distance <= bodyPass.distance ? headPass : bodyPass;
        if (closest.distance > radius) {
            return null;
        }
        return new Flyby(closest.position, closest.distance);
    }

    private static ClosestPoint closestPointOnSegment(Vec3 start, Vec3 end, Vec3 point) {
        Vec3 segment = end.subtract(start);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 1.0E-5D) {
            return new ClosestPoint(start, start.distanceTo(point));
        }
        double t = point.subtract(start).dot(segment) / lengthSqr;
        t = Mth.clamp(t, 0.0D, 1.0D);
        Vec3 closest = start.add(segment.scale(t));
        return new ClosestPoint(closest, closest.distanceTo(point));
    }

    private static void playSuppression(ClientLevel level, Player player, EntityKineticBullet bullet, Flyby flyby, double radius) {
        if (!reserveSound()) {
            return;
        }

        RandomSource random = level.random;
        boolean highCaliber = isHighCaliber(bullet);
        boolean smallCaliber = isSmallCaliber(bullet);
        double closeness = 1.0D - Mth.clamp(flyby.distance / radius, 0.0D, 1.0D);
        float volume = BallisticBlockConfig.SUPPRESSION_VOLUME.get().floatValue() * (0.35F + (float) closeness * 0.95F);
        float pitch = 1.05F + random.nextFloat() * 0.3F;
        if (highCaliber) {
            volume *= 1.35F;
            pitch -= 0.28F + random.nextFloat() * 0.08F;
        } else if (smallCaliber) {
            volume *= 0.82F;
            pitch += 0.12F + random.nextFloat() * 0.1F;
        }
        if (isIndoor(level, player)) {
            volume *= 1.18F;
        }

        SoundEvent sound = chooseFlybySound(random, highCaliber, smallCaliber, closeness);
        level.playLocalSound(flyby.position.x, flyby.position.y, flyby.position.z, sound, SoundSource.PLAYERS, Math.min(volume, 4.0F), Mth.clamp(pitch, 0.65F, 1.55F), false);
        ClientSuppressionEffectsHandler.triggerFlyby(flyby.position, (float) closeness, highCaliber, false);

        if (highCaliber && closeness > 0.78D) {
            ClientCameraShakeHandler.suppression(flyby.position, (float) closeness, true);
            registerHeavySuppression(level, player, random, flyby.position);
        }
    }

    private static SoundEvent chooseFlybySound(RandomSource random, boolean highCaliber, boolean smallCaliber, double closeness) {
        if (highCaliber || closeness > 0.82D) {
            return random.nextBoolean() ? SoundEvents.FIREWORK_ROCKET_BLAST : SoundEvents.GENERIC_EXPLODE;
        }
        if (smallCaliber) {
            return random.nextBoolean() ? SoundEvents.ARROW_SHOOT : SoundEvents.CROSSBOW_SHOOT;
        }
        int variant = random.nextInt(3);
        if (variant == 0) {
            return SoundEvents.TRIDENT_THROW;
        }
        if (variant == 1) {
            return SoundEvents.CROSSBOW_SHOOT;
        }
        return SoundEvents.ARROW_SHOOT;
    }

    private static void registerHeavySuppression(ClientLevel level, Player player, RandomSource random, Vec3 position) {
        long now = System.currentTimeMillis();
        if (now - lastHeavyPassMillis > TINNITUS_WINDOW_MILLIS) {
            recentHeavyPasses = 0;
        }
        recentHeavyPasses++;
        lastHeavyPassMillis = now;
        if (recentHeavyPasses >= 3 && now - lastTinnitusMillis > 3500L) {
            lastTinnitusMillis = now;
            if (BallisticBlockConfig.ENABLE_SUPPRESSION_RINGING.get()) {
                return;
            }
            level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.18F, 1.65F + random.nextFloat() * 0.18F, false);
            level.playLocalSound(position.x, position.y, position.z, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.12F, 1.9F, false);
        }
    }

    private static boolean reserveSound() {
        int maxPerSecond = BallisticBlockConfig.SUPPRESSION_MAX_SOUNDS_PER_SECOND.get();
        if (maxPerSecond <= 0) {
            return false;
        }
        long currentSecond = System.currentTimeMillis() / 1000L;
        if (currentSecond != limiterSecond) {
            limiterSecond = currentSecond;
            soundsThisSecond = 0;
        }
        if (soundsThisSecond >= maxPerSecond) {
            return false;
        }
        soundsThisSecond++;
        return true;
    }

    private static void cleanup(long now) {
        Iterator<Map.Entry<Integer, Long>> iterator = TRIGGERED_BULLETS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue() > BULLET_MEMORY_MILLIS) {
                iterator.remove();
            }
        }
    }

    private static boolean isLocalShooter(EntityKineticBullet bullet, Player player) {
        Entity owner = bullet.getOwner();
        return owner != null && owner.getId() == player.getId();
    }

    private static boolean isIndoor(ClientLevel level, Player player) {
        return !level.canSeeSky(player.blockPosition()) && level.getMaxLocalRawBrightness(player.blockPosition()) < 12;
    }

    private static boolean isHighCaliber(EntityKineticBullet bullet) {
        return containsAny(bullet.getGunId(), "50", "bmg", "127", "m107", "m95", "338", "lapua", "762", "7_62", "7.62", "308", "sniper")
                || containsAny(bullet.getAmmoId(), "50", "bmg", "127", "338", "lapua", "762", "7_62", "7.62", "308", "12g");
    }

    private static boolean isSmallCaliber(EntityKineticBullet bullet) {
        return containsAny(bullet.getAmmoId(), "9mm", "45acp", "380", "22lr", "57", "5_7")
                || containsAny(bullet.getGunId(), "pistol", "glock", "m1911", "m9", "smg", "vector");
    }

    private static boolean containsAny(ResourceLocation id, String... needles) {
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase(java.util.Locale.ROOT);
        for (String needle : needles) {
            if (path.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private record ClosestPoint(Vec3 position, double distance) {
    }

    private record Flyby(Vec3 position, double distance) {
    }
}
