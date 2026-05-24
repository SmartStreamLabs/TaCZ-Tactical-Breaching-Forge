package com.victoriomods.taczballisticbreaching;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class ClientDynamicImpactSoundHandler {
    private static final int MAX_SOUNDS_PER_SECOND = 24;
    private static final long DUPLICATE_WINDOW_MILLIS = 80L;
    private static long limiterSecond = -1L;
    private static int soundsThisSecond;
    private static final Map<Long, Long> RECENT_IMPACTS = new HashMap<>();

    private ClientDynamicImpactSoundHandler() {
    }

    public static void playBlockImpact(ImpactFxPacket packet, ImpactMaterial material) {
        if (!BallisticBlockConfig.ENABLE_DYNAMIC_IMPACT_SOUNDS.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null) {
            return;
        }
        Vec3 impact = new Vec3(packet.x(), packet.y(), packet.z());
        SoundContext context = context(level, player, impact, material.id());
        if (context == null || !reserveSound() || isDuplicate(impact, material.id())) {
            return;
        }

        CaliberProfile caliber = CaliberProfile.from(packet.shotgunImpact(), packet.highCaliberImpact(), packet.armorPiercingImpact(), packet.automaticImpact());
        ImpactVoice voice = voiceForMaterial(level.random, material, caliber, packet.finalBreak(), false);
        playVoice(level, level.random, impact, voice, caliber, context);
        maybeCinematicRicochet(level, level.random, impact, material, caliber, context);
    }

    public static void playArmorImpact(ArmorHitFxPacket packet) {
        if (!BallisticBlockConfig.ENABLE_DYNAMIC_IMPACT_SOUNDS.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null) {
            return;
        }
        Vec3 impact = new Vec3(packet.x(), packet.y(), packet.z());
        SoundContext context = context(level, player, impact, 7);
        if (context == null || !reserveSound() || isDuplicate(impact, 7)) {
            return;
        }

        CaliberProfile caliber = CaliberProfile.from(false, packet.highCaliber(), packet.armorPiercing(), false);
        ImpactVoice voice = armorVoice(level.random, packet);
        playVoice(level, level.random, impact, voice, caliber, context);
        if (packet.deflected() || packet.helmetHit() || packet.vehicleHit()) {
            SoundEvent ping = packet.vehicleHit() ? SoundEvents.CHAIN_HIT : SoundEvents.SHIELD_BLOCK;
            playLayer(level, impact, ping, context.volume * 0.62F, context.pitch + (packet.helmetHit() ? 0.32F : 0.08F), SoundSource.PLAYERS);
        }
    }

    public static boolean enabled() {
        return BallisticBlockConfig.ENABLE_DYNAMIC_IMPACT_SOUNDS.get();
    }

    private static SoundContext context(ClientLevel level, Player player, Vec3 impact, int materialId) {
        double maxDistance = BallisticBlockConfig.IMPACT_SOUND_MAX_DISTANCE.get();
        double distance = player.getEyePosition().distanceTo(impact);
        if (distance > maxDistance) {
            return null;
        }
        double normalizedDistance = Mth.clamp(distance / maxDistance, 0.0D, 1.0D);
        double volume = BallisticBlockConfig.IMPACT_SOUND_VOLUME.get() * (1.0D - normalizedDistance * 0.74D);
        double pitch = 1.0D;
        if (BallisticBlockConfig.ENABLE_DISTANCE_MUFFLING.get()) {
            if (normalizedDistance > 0.55D) {
                volume *= 0.72D;
                pitch -= 0.12D + normalizedDistance * 0.08D;
            } else if (normalizedDistance < 0.18D) {
                volume *= 1.18D;
                pitch += 0.03D;
            }
        }
        boolean indoor = isIndoor(level, impact) || isIndoor(level, player.getEyePosition());
        if (indoor) {
            volume *= 1.12D;
            pitch -= 0.035D;
        }
        return new SoundContext((float) Math.min(volume, 4.0D), (float) pitch, indoor, (float) normalizedDistance, materialId);
    }

    private static ImpactVoice voiceForMaterial(RandomSource random, ImpactMaterial material, CaliberProfile caliber, boolean finalBreak, boolean armor) {
        if (armor) {
            return new ImpactVoice(SoundEvents.ARMOR_EQUIP_IRON, SoundEvents.CHAIN_HIT, 0.95F, 0.84F);
        }
        return switch (material) {
            case WOOD -> woodVoice(random, finalBreak);
            case STONE -> stoneVoice(random);
            case METAL -> metalVoice(random, caliber);
            case GLASS -> glassVoice(random, finalBreak);
            default -> new ImpactVoice(SoundEvents.STONE_HIT, SoundEvents.GRAVEL_HIT, 0.55F, 1.05F);
        };
    }

    private static ImpactVoice woodVoice(RandomSource random, boolean finalBreak) {
        SoundEvent main = finalBreak ? SoundEvents.WOOD_BREAK : SoundEvents.WOOD_HIT;
        SoundEvent layer = random.nextBoolean() ? SoundEvents.BAMBOO_WOOD_HIT : SoundEvents.WOOD_HIT;
        return new ImpactVoice(main, layer, finalBreak ? 0.88F : 0.64F, finalBreak ? 0.86F : 0.96F);
    }

    private static ImpactVoice stoneVoice(RandomSource random) {
        SoundEvent main = random.nextBoolean() ? SoundType.STONE.getHitSound() : SoundType.DEEPSLATE.getHitSound();
        SoundEvent layer = random.nextBoolean() ? SoundEvents.CALCITE_HIT : SoundEvents.GRAVEL_HIT;
        return new ImpactVoice(main, layer, 0.72F, 0.92F);
    }

    private static ImpactVoice metalVoice(RandomSource random, CaliberProfile caliber) {
        SoundEvent main = random.nextBoolean() ? SoundType.METAL.getHitSound() : SoundEvents.COPPER_HIT;
        SoundEvent layer = caliber.armorPiercing ? SoundEvents.ANVIL_LAND : SoundEvents.CHAIN_HIT;
        return new ImpactVoice(main, layer, 0.82F, caliber.highCaliber ? 0.78F : 1.04F);
    }

    private static ImpactVoice glassVoice(RandomSource random, boolean finalBreak) {
        SoundEvent main = finalBreak ? SoundEvents.GLASS_BREAK : SoundEvents.GLASS_HIT;
        SoundEvent layer = random.nextBoolean() ? SoundEvents.AMETHYST_BLOCK_HIT : SoundEvents.GLASS_FALL;
        return new ImpactVoice(main, layer, finalBreak ? 0.86F : 0.48F, finalBreak ? 1.08F : 1.32F);
    }

    private static ImpactVoice armorVoice(RandomSource random, ArmorHitFxPacket packet) {
        SoundEvent main = packet.vehicleHit() ? SoundEvents.ANVIL_LAND : SoundEvents.ARMOR_EQUIP_IRON;
        SoundEvent layer = packet.deflected() ? SoundEvents.CHAIN_HIT : (random.nextBoolean() ? SoundEvents.SHIELD_BLOCK : SoundEvents.COPPER_HIT);
        float volume = packet.vehicleHit() ? 1.18F : 0.9F;
        float pitch = packet.helmetHit() ? 1.18F : packet.vehicleHit() ? 0.72F : 0.9F;
        return new ImpactVoice(main, layer, volume, pitch);
    }

    private static void playVoice(ClientLevel level, RandomSource random, Vec3 impact, ImpactVoice voice, CaliberProfile caliber, SoundContext context) {
        float variation = BallisticBlockConfig.DYNAMIC_IMPACT_VARIATION.get() ? random.nextFloat() : 0.5F;
        float volume = context.volume * voice.volume;
        float pitch = context.pitch * voice.pitch * (0.92F + variation * 0.16F);
        if (caliber.shotgun) {
            volume *= 1.24F;
            pitch -= 0.12F;
        } else if (caliber.highCaliber) {
            volume *= 1.18F;
            pitch -= 0.08F;
        } else if (caliber.light) {
            volume *= 0.82F;
            pitch += 0.12F;
        }
        if (caliber.armorPiercing) {
            volume *= 1.1F;
            pitch += 0.04F;
        }

        playLayer(level, impact, voice.main, volume, pitch, SoundSource.BLOCKS);
        if (context.normalizedDistance < 0.64F || context.indoor) {
            playLayer(level, impact, voice.layer, volume * (context.indoor ? 0.42F : 0.26F), pitch * (context.indoor ? 0.82F : 1.06F), SoundSource.BLOCKS);
        }
        if (context.indoor && BallisticBlockConfig.ENABLE_INDOOR_REVERB.get()) {
            playLayer(level, impact, voice.main, volume * 0.24F, pitch * 0.68F, SoundSource.BLOCKS);
        }
    }

    private static void maybeCinematicRicochet(ClientLevel level, RandomSource random, Vec3 impact, ImpactMaterial material, CaliberProfile caliber, SoundContext context) {
        if (!BallisticBlockConfig.DYNAMIC_IMPACT_VARIATION.get() || material != ImpactMaterial.METAL && material != ImpactMaterial.STONE) {
            return;
        }
        double chance = material == ImpactMaterial.METAL ? 0.055D : 0.018D;
        if (caliber.armorPiercing) {
            chance += 0.02D;
        }
        if (caliber.highCaliber) {
            chance += 0.012D;
        }
        if (random.nextDouble() > chance) {
            return;
        }
        playLayer(level, impact, SoundEvents.TRIDENT_THROW, context.volume * 0.55F, 1.45F + random.nextFloat() * 0.22F, SoundSource.BLOCKS);
    }

    private static void playLayer(ClientLevel level, Vec3 impact, SoundEvent sound, float volume, float pitch, SoundSource source) {
        if (volume <= 0.01F) {
            return;
        }
        level.playLocalSound(impact.x, impact.y, impact.z, sound, source, Mth.clamp(volume, 0.0F, 4.0F), Mth.clamp(pitch, 0.45F, 1.85F), false);
    }

    private static boolean reserveSound() {
        long currentSecond = System.currentTimeMillis() / 1000L;
        if (currentSecond != limiterSecond) {
            limiterSecond = currentSecond;
            soundsThisSecond = 0;
        }
        if (soundsThisSecond >= MAX_SOUNDS_PER_SECOND) {
            return false;
        }
        soundsThisSecond++;
        return true;
    }

    private static boolean isDuplicate(Vec3 impact, int materialId) {
        long now = System.currentTimeMillis();
        cleanup(now);
        long key = hashImpact(impact, materialId);
        Long previous = RECENT_IMPACTS.get(key);
        if (previous != null && now - previous < DUPLICATE_WINDOW_MILLIS) {
            return true;
        }
        RECENT_IMPACTS.put(key, now);
        return false;
    }

    private static long hashImpact(Vec3 impact, int materialId) {
        long x = Mth.floor(impact.x * 8.0D) & 0x1FFFFFL;
        long y = Mth.floor(impact.y * 8.0D) & 0x1FFFFFL;
        long z = Mth.floor(impact.z * 8.0D) & 0x1FFFFFL;
        return ((long) materialId << 63) ^ (x << 42) ^ (y << 21) ^ z;
    }

    private static void cleanup(long now) {
        Iterator<Map.Entry<Long, Long>> iterator = RECENT_IMPACTS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue() > 1000L) {
                iterator.remove();
            }
        }
    }

    private static boolean isIndoor(ClientLevel level, Vec3 position) {
        BlockPos pos = BlockPos.containing(position);
        return !level.canSeeSky(pos) && level.getMaxLocalRawBrightness(pos) < 12;
    }

    private record SoundContext(float volume, float pitch, boolean indoor, float normalizedDistance, int materialId) {
    }

    private record ImpactVoice(SoundEvent main, SoundEvent layer, float volume, float pitch) {
    }

    private record CaliberProfile(boolean shotgun, boolean highCaliber, boolean armorPiercing, boolean automatic, boolean light) {
        static CaliberProfile from(boolean shotgun, boolean highCaliber, boolean armorPiercing, boolean automatic) {
            return new CaliberProfile(shotgun, highCaliber, armorPiercing, automatic, !shotgun && !highCaliber && !armorPiercing);
        }
    }
}
