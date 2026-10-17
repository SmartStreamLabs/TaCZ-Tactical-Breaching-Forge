package com.victoriomods.taczballisticbreaching;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class ClientWeaponHeatHandler {
    private static final double MAX_VISIBLE_DISTANCE_SQR = 56.0D * 56.0D;
    private static final int MAX_ACTIVE_HEAT_SOURCES = 28;
    private static final Map<Integer, HeatState> HEAT = new HashMap<>();

    private ClientWeaponHeatHandler() {
    }

    public static double recordShot(MuzzleSmokePacket packet, Vec3 muzzle, Vec3 forward) {
        if (!BallisticBlockConfig.ENABLE_WEAPON_HEAT.get()) {
            return 1.0D;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.player.distanceToSqr(muzzle) > MAX_VISIBLE_DISTANCE_SQR) {
            return 1.0D;
        }

        long now = System.currentTimeMillis();
        HeatState state = HEAT.computeIfAbsent(packet.shooterId(), ignored -> new HeatState());
        state.muzzle = muzzle;
        state.forward = forward;
        state.localPlayer = minecraft.player.getId() == packet.shooterId();
        state.kind = MuzzleSmokeNetwork.WeaponKind.byId(packet.weaponKind());
        state.automatic = packet.automatic();
        state.suppressed = packet.suppressed();
        state.highCaliber = packet.highCaliber();
        if (state.localPlayer) {
            state.heldItem = minecraft.player.getMainHandItem().copy();
        }
        state.lastShotMillis = now;

        double add = heatPerShot(state);
        state.heat = Math.min(BallisticBlockConfig.MAX_HEAT_LEVEL.get(), state.heat + add);
        trimHeatSources();
        double heatRatio = heatRatio(state);
        if (heatRatio > 0.92D && BallisticBlockConfig.ENABLE_BARREL_SMOKE.get()) {
            spawnOverheatBurst(minecraft.level, minecraft.level.random, state);
        }
        return 1.0D + heatRatio * (packet.automatic() ? 0.55D : 0.28D);
    }

    public static void tick() {
        if (!BallisticBlockConfig.ENABLE_WEAPON_HEAT.get() || HEAT.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null) {
            return;
        }

        long now = System.currentTimeMillis();
        RandomSource random = level.random;
        Iterator<Map.Entry<Integer, HeatState>> iterator = HEAT.entrySet().iterator();
        while (iterator.hasNext()) {
            HeatState state = iterator.next().getValue();
            if (state.localPlayer && !isStillHoldingSourceWeapon(player, state)) {
                iterator.remove();
                continue;
            }
            updateLocalMuzzle(player, state);
            cool(level, state);
            if (state.heat <= 0.05D || now - state.lastShotMillis > heatMemoryMillis(state)) {
                iterator.remove();
                continue;
            }
            if (player.distanceToSqr(state.muzzle) > MAX_VISIBLE_DISTANCE_SQR) {
                continue;
            }
            double ratio = heatRatio(state);
            if (ratio > 0.48D && BallisticBlockConfig.ENABLE_BARREL_SMOKE.get()) {
                spawnBarrelSmoke(level, random, state, ratio);
            }
            if (ratio > 0.46D && BallisticBlockConfig.ENABLE_HEAT_DISTORTION.get()) {
                spawnHeatHaze(level, random, state, ratio);
            }
            if (ratio > 0.82D && BallisticBlockConfig.ENABLE_HOT_BARREL_GLOW.get()) {
                spawnHotGlow(level, random, state, ratio);
            }
            maybeCoolingTick(level, random, state, ratio, now);
        }
    }

    public static void clear() {
        HEAT.clear();
    }

    private static double heatPerShot(HeatState state) {
        double heat = switch (state.kind) {
            case PISTOL -> 2.0D;
            case RIFLE -> 3.6D;
            case SHOTGUN -> 4.4D;
            case SNIPER -> 6.5D;
        };
        if (state.automatic) {
            heat *= state.kind == MuzzleSmokeNetwork.WeaponKind.RIFLE ? 1.25D : 1.55D;
        }
        if (state.highCaliber) {
            heat *= 1.35D;
        }
        if (state.suppressed) {
            heat *= 1.45D;
        }
        return heat;
    }

    private static void cool(ClientLevel level, HeatState state) {
        double cooling = 0.18D * BallisticBlockConfig.WEAPON_COOLING_SPEED.get();
        long sinceShot = System.currentTimeMillis() - state.lastShotMillis;
        if (sinceShot > 900L) {
            cooling *= 1.85D;
        }
        if (state.suppressed) {
            cooling *= 0.58D;
        }
        if (level.isRainingAt(BlockPos.containing(state.muzzle))) {
            cooling *= 2.4D;
        }
        state.heat = Math.max(0.0D, state.heat - cooling);
    }

    private static void updateLocalMuzzle(Player player, HeatState state) {
        if (!state.localPlayer) {
            return;
        }
        Vec3 forward = player.getLookAngle();
        forward = forward.lengthSqr() < 0.001D ? state.forward : forward.normalize();
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 0.0001D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        double length = state.kind == MuzzleSmokeNetwork.WeaponKind.PISTOL ? 0.48D : 0.72D;
        state.forward = forward;
        state.muzzle = player.getEyePosition()
                .add(forward.scale(length))
                .add(right.scale(0.055D))
                .add(0.0D, -0.08D, 0.0D);
    }

    private static boolean isStillHoldingSourceWeapon(Player player, HeatState state) {
        if (state.heldItem.isEmpty()) {
            return false;
        }
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        return sameItem(mainHand, state.heldItem) || sameItem(offHand, state.heldItem);
    }

    private static boolean sameItem(ItemStack current, ItemStack original) {
        return !current.isEmpty() && !original.isEmpty() && current.is(original.getItem());
    }

    private static long heatMemoryMillis(HeatState state) {
        if (state.localPlayer) {
            return 45000L;
        }
        return state.suppressed ? 16000L : 10000L;
    }

    private static void spawnBarrelSmoke(ClientLevel level, RandomSource random, HeatState state, double ratio) {
        double chance = (0.025D + ratio * 0.075D) * graphicsMultiplier();
        if (random.nextDouble() > chance) {
            return;
        }
        Vec3 pos = state.muzzle.add(state.forward.scale(-0.04D)).add(randomSpread(random, 0.025D));
        Vec3 velocity = randomSpread(random, 0.012D).add(0.0D, 0.025D + random.nextDouble() * 0.028D, 0.0D).add(state.forward.scale(0.008D));
        ClientMuzzleSmokeRenderer.spawnAmbient(pos, velocity, (float) (0.12D + ratio * 0.12D),
                (int) (8 + ratio * 12) + random.nextInt(5), random);
    }

    private static void spawnHeatHaze(ClientLevel level, RandomSource random, HeatState state, double ratio) {
        int maxDistortions = isReducedGraphics() ? 5 : 12;
        if (countVeryHotSources() > maxDistortions || random.nextDouble() > (0.06D + ratio * 0.12D) * BallisticBlockConfig.HEAT_DISTORTION_STRENGTH.get()) {
            return;
        }
        Vec3 pos = state.muzzle.add(0.0D, 0.05D + random.nextDouble() * 0.08D, 0.0D).add(state.forward.scale(-0.03D)).add(randomSpread(random, 0.02D));
        ClientMuzzleSmokeRenderer.spawnAmbient(pos, new Vec3(0.0D, 0.02D, 0.0D),
                (float) (0.05D + ratio * 0.04D), 5 + random.nextInt(4), random);
    }

    private static void spawnHotGlow(ClientLevel level, RandomSource random, HeatState state, double ratio) {
        double chance = state.suppressed ? 0.22D : 0.13D;
        if (random.nextDouble() > chance * graphicsMultiplier()) {
            return;
        }
        double glow = BallisticBlockConfig.BARREL_GLOW_INTENSITY.get() * ratio;
        Vec3 pos = state.muzzle.add(state.forward.scale(state.suppressed ? -0.13D : -0.04D)).add(randomSpread(random, 0.012D));
        if (glow > 0.34D && level.getMaxLocalRawBrightness(BlockPos.containing(pos)) < 10) {
            DynamicGunLightManager.addBreachingFlash(pos, ImpactMaterial.METAL, state.highCaliber);
        }
    }

    private static void spawnOverheatBurst(ClientLevel level, RandomSource random, HeatState state) {
        if (random.nextFloat() > 0.35F) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            spawnBarrelSmoke(level, random, state, 1.0D);
        }
    }

    private static void maybeCoolingTick(ClientLevel level, RandomSource random, HeatState state, double ratio, long now) {
    }

    private static double heatRatio(HeatState state) {
        return Mth.clamp(state.heat / Math.max(1.0D, BallisticBlockConfig.MAX_HEAT_LEVEL.get()), 0.0D, 1.0D);
    }

    private static void trimHeatSources() {
        while (HEAT.size() > MAX_ACTIVE_HEAT_SOURCES) {
            Integer oldest = null;
            long oldestTime = Long.MAX_VALUE;
            for (Map.Entry<Integer, HeatState> entry : HEAT.entrySet()) {
                if (entry.getValue().lastShotMillis < oldestTime) {
                    oldest = entry.getKey();
                    oldestTime = entry.getValue().lastShotMillis;
                }
            }
            if (oldest == null) {
                return;
            }
            HEAT.remove(oldest);
        }
    }

    private static int countVeryHotSources() {
        int count = 0;
        for (HeatState state : HEAT.values()) {
            if (heatRatio(state) > 0.46D) {
                count++;
            }
        }
        return count;
    }

    private static boolean isReducedGraphics() {
        Minecraft minecraft = Minecraft.getInstance();
        String particles = String.valueOf(minecraft.options.particles().get()).toLowerCase(java.util.Locale.ROOT);
        String graphics = String.valueOf(minecraft.options.graphicsMode().get()).toLowerCase(java.util.Locale.ROOT);
        return particles.contains("minimal") || particles.contains("decreased") || graphics.contains("fast");
    }

    private static double graphicsMultiplier() {
        return isReducedGraphics() ? 0.45D : 1.0D;
    }

    private static Vec3 randomSpread(RandomSource random, double amount) {
        return new Vec3(
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount
        );
    }

    private static final class HeatState {
        private Vec3 muzzle = Vec3.ZERO;
        private Vec3 forward = new Vec3(0.0D, 0.0D, 1.0D);
        private MuzzleSmokeNetwork.WeaponKind kind = MuzzleSmokeNetwork.WeaponKind.RIFLE;
        private boolean automatic;
        private boolean suppressed;
        private boolean highCaliber;
        private boolean localPlayer;
        private ItemStack heldItem = ItemStack.EMPTY;
        private double heat;
        private long lastShotMillis;
        private long lastTickSoundMillis;
    }
}
