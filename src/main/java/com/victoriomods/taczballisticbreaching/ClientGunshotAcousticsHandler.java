package com.victoriomods.taczballisticbreaching;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayDeque;
import java.util.Iterator;

@OnlyIn(Dist.CLIENT)
public final class ClientGunshotAcousticsHandler {
    private static final int MAX_ECHO_LAYERS_PER_SECOND = 28;
    private static final int MAX_QUEUED_ECHOES = 96;
    private static final long CLOSE_SHOT_COOLDOWN_MILLIS = 28L;
    private static final ArrayDeque<ScheduledEcho> ECHOES = new ArrayDeque<>();
    private static long limiterSecond = -1L;
    private static int echoesThisSecond;
    private static long lastCloseShotMillis;

    private ClientGunshotAcousticsHandler() {
    }

    public static void shot(MuzzleSmokePacket packet, Vec3 muzzle, Vec3 forward) {
        if (!BallisticBlockConfig.ENABLE_BULLET_CRACK_ECHO.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null) {
            return;
        }
        double maxDistance = BallisticBlockConfig.MAX_ECHO_DISTANCE.get();
        double distance = player.getEyePosition().distanceTo(muzzle);
        if (distance > maxDistance) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastCloseShotMillis < CLOSE_SHOT_COOLDOWN_MILLIS && packet.automatic() && !packet.highCaliber()) {
            return;
        }
        lastCloseShotMillis = now;

        Environment environment = Environment.detect(level, muzzle);
        MuzzleSmokeNetwork.WeaponKind kind = MuzzleSmokeNetwork.WeaponKind.byId(packet.weaponKind());
        double caliber = caliberScale(kind, packet.highCaliber());
        double suppressed = packet.suppressed() ? 0.18D : 1.0D;
        double distanceScale = 1.0D - Mth.clamp(distance / maxDistance, 0.0D, 1.0D);
        double baseVolume = Math.max(0.035D, distanceScale) * caliber * suppressed;
        if (level.isRainingAt(BlockPos.containing(muzzle))) {
            baseVolume *= 0.78D;
        }

        if (baseVolume <= 0.025D) {
            return;
        }

        playImmediateCrack(level, muzzle, kind, packet, environment, baseVolume, distance);
        scheduleEnvironmentEchoes(level, muzzle, kind, packet, environment, baseVolume, distance);
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            clear();
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<ScheduledEcho> iterator = ECHOES.iterator();
        while (iterator.hasNext()) {
            ScheduledEcho echo = iterator.next();
            if (now < echo.playAtMillis) {
                continue;
            }
            iterator.remove();
            if (reserveEcho()) {
                level.playLocalSound(echo.x, echo.y, echo.z, echo.sound, SoundSource.PLAYERS, echo.volume, echo.pitch, false);
            }
        }
    }

    public static void clear() {
        ECHOES.clear();
        limiterSecond = -1L;
        echoesThisSecond = 0;
        lastCloseShotMillis = 0L;
    }

    private static void playImmediateCrack(ClientLevel level, Vec3 muzzle, MuzzleSmokeNetwork.WeaponKind kind, MuzzleSmokePacket packet, Environment environment, double baseVolume, double distance) {
        if (!reserveEcho()) {
            return;
        }
        RandomSource random = level.random;
        boolean sniperLike = kind == MuzzleSmokeNetwork.WeaponKind.SNIPER || packet.highCaliber();
        boolean rifleLike = kind == MuzzleSmokeNetwork.WeaponKind.RIFLE || sniperLike;
        float volume = (float) Mth.clamp(baseVolume * (sniperLike ? 0.86D : rifleLike ? 0.58D : 0.32D), 0.02D, 1.55D);
        float pitch = (float) Mth.clamp((sniperLike ? 0.92D : rifleLike ? 1.15D : 1.42D) + random.nextDouble() * 0.18D - 0.06D, 0.55D, 1.8D);
        if (environment == Environment.FOREST) {
            volume *= BallisticBlockConfig.FOREST_ECHO_REDUCTION.get().floatValue();
            pitch *= 0.94F;
        }
        if (level.isRainingAt(BlockPos.containing(muzzle)) || level.getRainLevel(1.0F) > 0.25F) {
            pitch *= 0.93F;
        }
        SoundEvent crack = sniperLike ? SoundEvents.FIREWORK_ROCKET_LARGE_BLAST : rifleLike ? SoundEvents.FIREWORK_ROCKET_BLAST : SoundEvents.CROSSBOW_SHOOT;
        level.playLocalSound(muzzle.x, muzzle.y, muzzle.z, crack, SoundSource.PLAYERS, volume, pitch, false);
        if (distance > 42.0D && sniperLike) {
            enqueue(muzzle, SoundEvents.LIGHTNING_BOLT_THUNDER, volume * 0.22F, 1.65F + random.nextFloat() * 0.12F, 180L + random.nextInt(150));
        }
    }

    private static void scheduleEnvironmentEchoes(ClientLevel level, Vec3 muzzle, MuzzleSmokeNetwork.WeaponKind kind, MuzzleSmokePacket packet, Environment environment, double baseVolume, double distance) {
        if (environment != Environment.INDOOR && !BallisticBlockConfig.ENABLE_OUTDOOR_ECHO.get()) {
            return;
        }
        if (environment == Environment.INDOOR && !BallisticBlockConfig.ENABLE_INDOOR_REVERB.get()) {
            return;
        }

        RandomSource random = level.random;
        boolean sniperLike = kind == MuzzleSmokeNetwork.WeaponKind.SNIPER || packet.highCaliber();
        double envScale = switch (environment) {
            case INDOOR -> BallisticBlockConfig.INDOOR_REVERB_INTENSITY.get();
            case URBAN -> BallisticBlockConfig.URBAN_ECHO_INTENSITY.get();
            case VALLEY -> BallisticBlockConfig.VALLEY_ECHO_INTENSITY.get();
            case FOREST -> BallisticBlockConfig.FOREST_ECHO_REDUCTION.get();
            case OPEN -> 0.42D;
        };
        if (level.isRainingAt(BlockPos.containing(muzzle)) || level.getRainLevel(1.0F) > 0.25F) {
            envScale *= 0.76D;
        }
        if (packet.suppressed()) {
            envScale *= 0.28D;
        }

        int layers = switch (environment) {
            case INDOOR -> 4;
            case URBAN -> 5;
            case VALLEY -> sniperLike ? 5 : 3;
            case FOREST -> 2;
            case OPEN -> sniperLike ? 2 : 1;
        };
        if (packet.automatic() && !sniperLike) {
            layers = Math.max(1, layers - 2);
        }
        double weaponScale = sniperLike ? 1.28D : kind == MuzzleSmokeNetwork.WeaponKind.RIFLE ? 0.9D : kind == MuzzleSmokeNetwork.WeaponKind.SHOTGUN ? 0.78D : 0.48D;
        float startVolume = (float) Mth.clamp(baseVolume * envScale * weaponScale, 0.0D, 1.35D);
        if (startVolume <= 0.025F) {
            return;
        }

        for (int i = 0; i < layers; i++) {
            double falloff = Math.pow(0.66D, i);
            long delay = delayFor(environment, i, distance, random);
            float volume = (float) Mth.clamp(startVolume * falloff * (0.84D + random.nextDouble() * 0.24D), 0.015D, 1.2D);
            float pitch = pitchFor(environment, kind, i, random);
            SoundEvent sound = echoSoundFor(environment, kind, sniperLike, i);
            Vec3 echoPos = offsetEchoPosition(muzzle, environment, i, random);
            enqueue(echoPos, sound, volume, pitch, delay);
        }
    }

    private static long delayFor(Environment environment, int layer, double distance, RandomSource random) {
        return switch (environment) {
            case INDOOR -> 45L + layer * 54L + random.nextInt(34);
            case URBAN -> 70L + layer * 82L + random.nextInt(55);
            case VALLEY -> 180L + layer * 260L + (long) (distance * 1.8D) + random.nextInt(150);
            case FOREST -> 120L + layer * 145L + random.nextInt(90);
            case OPEN -> 210L + layer * 220L + random.nextInt(120);
        };
    }

    private static float pitchFor(Environment environment, MuzzleSmokeNetwork.WeaponKind kind, int layer, RandomSource random) {
        double base = switch (kind) {
            case SNIPER -> 0.78D;
            case RIFLE -> 0.96D;
            case SHOTGUN -> 0.72D;
            case PISTOL -> 1.18D;
        };
        base += switch (environment) {
            case INDOOR -> 0.08D;
            case URBAN -> 0.0D;
            case VALLEY -> -0.18D;
            case FOREST -> -0.10D;
            case OPEN -> -0.06D;
        };
        base -= layer * 0.035D;
        base += random.nextDouble() * 0.12D - 0.04D;
        return (float) Mth.clamp(base, 0.45D, 1.65D);
    }

    private static SoundEvent echoSoundFor(Environment environment, MuzzleSmokeNetwork.WeaponKind kind, boolean sniperLike, int layer) {
        if (environment == Environment.INDOOR) {
            return layer % 2 == 0 ? SoundEvents.GENERIC_EXPLODE : SoundEvents.FIREWORK_ROCKET_BLAST;
        }
        if (environment == Environment.VALLEY) {
            return sniperLike || layer > 1 ? SoundEvents.LIGHTNING_BOLT_THUNDER : SoundEvents.FIREWORK_ROCKET_LARGE_BLAST;
        }
        if (environment == Environment.URBAN) {
            return layer % 2 == 0 ? SoundEvents.FIREWORK_ROCKET_BLAST : SoundEvents.TRIDENT_THUNDER;
        }
        if (environment == Environment.FOREST) {
            return kind == MuzzleSmokeNetwork.WeaponKind.PISTOL ? SoundEvents.CROSSBOW_SHOOT : SoundEvents.FIREWORK_ROCKET_BLAST;
        }
        return sniperLike ? SoundEvents.FIREWORK_ROCKET_LARGE_BLAST : SoundEvents.FIREWORK_ROCKET_BLAST;
    }

    private static Vec3 offsetEchoPosition(Vec3 muzzle, Environment environment, int layer, RandomSource random) {
        double radius = switch (environment) {
            case INDOOR -> 2.0D + layer * 1.0D;
            case URBAN -> 7.0D + layer * 3.5D;
            case VALLEY -> 24.0D + layer * 18.0D;
            case FOREST -> 10.0D + layer * 5.0D;
            case OPEN -> 18.0D + layer * 12.0D;
        };
        double angle = random.nextDouble() * Math.PI * 2.0D;
        return muzzle.add(Math.cos(angle) * radius, environment == Environment.VALLEY ? 5.0D + layer * 2.0D : random.nextDouble() * 2.0D, Math.sin(angle) * radius);
    }

    private static double caliberScale(MuzzleSmokeNetwork.WeaponKind kind, boolean highCaliber) {
        double scale = switch (kind) {
            case PISTOL -> 0.48D;
            case RIFLE -> 0.84D;
            case SHOTGUN -> 0.72D;
            case SNIPER -> 1.28D;
        };
        return highCaliber ? scale * 1.35D : scale;
    }

    private static void enqueue(Vec3 position, SoundEvent sound, float volume, float pitch, long delayMillis) {
        if (volume <= 0.01F || ECHOES.size() >= MAX_QUEUED_ECHOES) {
            return;
        }
        ECHOES.addLast(new ScheduledEcho(position.x, position.y, position.z, sound, volume, pitch, System.currentTimeMillis() + delayMillis));
    }

    private static boolean reserveEcho() {
        long currentSecond = System.currentTimeMillis() / 1000L;
        if (currentSecond != limiterSecond) {
            limiterSecond = currentSecond;
            echoesThisSecond = 0;
        }
        if (echoesThisSecond >= MAX_ECHO_LAYERS_PER_SECOND) {
            return false;
        }
        echoesThisSecond++;
        return true;
    }

    private record ScheduledEcho(double x, double y, double z, SoundEvent sound, float volume, float pitch, long playAtMillis) {
    }

    private enum Environment {
        INDOOR,
        URBAN,
        VALLEY,
        FOREST,
        OPEN;

        static Environment detect(ClientLevel level, Vec3 position) {
            BlockPos pos = BlockPos.containing(position);
            if (isIndoor(level, pos)) {
                return INDOOR;
            }
            int urban = 0;
            int forest = 0;
            int terrainWalls = 0;
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            for (Direction direction : directions) {
                for (int distance = 2; distance <= 14; distance += 2) {
                    mutable.set(pos).move(direction, distance);
                    BlockState state = level.getBlockState(mutable);
                    if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) {
                        forest++;
                    }
                    if (!state.isAir() && state.isSolidRender(level, mutable)) {
                        terrainWalls++;
                        if (buildingColumnScore(level, mutable.immutable()) >= 3) {
                            urban++;
                        }
                        break;
                    }
                }
            }
            if (urban >= 2) {
                return URBAN;
            }
            if (forest >= 3) {
                return FOREST;
            }
            if (terrainWalls >= 2 && level.canSeeSky(pos)) {
                return VALLEY;
            }
            return OPEN;
        }

        private static boolean isIndoor(ClientLevel level, BlockPos pos) {
            if (level.canSeeSky(pos)) {
                return false;
            }
            int blocked = 0;
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            for (Direction direction : Direction.values()) {
                for (int distance = 1; distance <= 5; distance++) {
                    mutable.set(pos).move(direction, distance);
                    BlockState state = level.getBlockState(mutable);
                    if (!state.isAir() && state.isSolidRender(level, mutable)) {
                        blocked++;
                        break;
                    }
                }
            }
            return blocked >= 3;
        }

        private static int buildingColumnScore(ClientLevel level, BlockPos base) {
            int solid = 0;
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            for (int y = 0; y <= 6; y++) {
                mutable.set(base).move(Direction.UP, y);
                BlockState state = level.getBlockState(mutable);
                if (!state.isAir() && state.isSolidRender(level, mutable)) {
                    solid++;
                }
            }
            return solid;
        }
    }
}
