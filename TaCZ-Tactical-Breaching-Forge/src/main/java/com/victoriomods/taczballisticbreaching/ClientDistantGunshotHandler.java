package com.victoriomods.taczballisticbreaching;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.sound.GunSoundInstance;
import com.tacz.guns.sound.SoundManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayDeque;
import java.util.Iterator;

@OnlyIn(Dist.CLIENT)
public final class ClientDistantGunshotHandler {
    private static final int MAX_QUEUED_SOUNDS = 160;
    private static final double DIRECTIONAL_PROXY_DISTANCE = 32.0D;
    private static final ArrayDeque<ScheduledGunshot> QUEUE = new ArrayDeque<>();
    private static long limiterSecond = -1L;
    private static int soundsThisSecond;

    private ClientDistantGunshotHandler() {
    }

    public static void handle(DistantGunshotPacket packet) {
        DistantGunshotAssetVerifier.verifyOnce();
        DistantGunshotSoundType type = DistantGunshotSoundType.byId(packet.soundType());
        TaczTacticalBreaching.LOGGER.info("[DistantGunshots] Received distant gunshot packet originalShotPosition={} {} {} chosenSound={} volume={} pitch={} delay={}", format(packet.x()), format(packet.y()), format(packet.z()), type, format(packet.volume()), format(packet.pitch()), packet.delayMillis());
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null) {
            TaczTacticalBreaching.LOGGER.warn("[DistantGunshots] Client ignored packet: client level is null");
            return;
        }
        if (player == null) {
            TaczTacticalBreaching.LOGGER.warn("[DistantGunshots] Client ignored packet: local player is null");
            return;
        }
        if (packet.volume() <= 0.0F) {
            TaczTacticalBreaching.LOGGER.warn("[DistantGunshots] Client ignored packet: volume <= 0");
            return;
        }

        Vec3 shotPosition = new Vec3(packet.x(), packet.y(), packet.z());
        Vec3 audiblePosition = directionalProxy(player, shotPosition);
        long baseTime = System.currentTimeMillis() + packet.delayMillis();
        int burstCount = Math.max(1, Math.min(packet.burstCount(), 8));
        int spacing = Math.max(25, Math.min(packet.burstSpacingMillis(), 500));
        SoundEvent sound = soundFor(type);
        TaczTacticalBreaching.LOGGER.info("[DistantGunshots] fake sound position={} {} {} chosen sound event={} queuedSounds={} spacingMs={}", format(audiblePosition.x), format(audiblePosition.y), format(audiblePosition.z), sound.getLocation(), burstCount, spacing);

        RandomSource random = level.random;
        for (int i = 0; i < burstCount && QUEUE.size() < MAX_QUEUED_SOUNDS; i++) {
            float volume = packet.volume() * (float) Math.pow(0.86D, i);
            float pitch = Mth.clamp(packet.pitch() + (random.nextFloat() - 0.5F) * 0.08F, 0.45F, 1.8F);
            Vec3 variedPosition = audiblePosition.add((random.nextDouble() - 0.5D) * 1.25D, (random.nextDouble() - 0.5D) * 0.65D, (random.nextDouble() - 0.5D) * 1.25D);
            QUEUE.addLast(new ScheduledGunshot(variedPosition.x, variedPosition.y, variedPosition.z, type, volume, pitch, baseTime + (long) i * spacing, packet.gunId()));
        }
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            clear();
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<ScheduledGunshot> iterator = QUEUE.iterator();
        while (iterator.hasNext()) {
            ScheduledGunshot gunshot = iterator.next();
            if (now < gunshot.playAtMillis) {
                continue;
            }
            iterator.remove();
            if (reserveSound()) {
                SoundEvent sound = weaponSound(gunshot.gunId, gunshot.type);
                TaczTacticalBreaching.LOGGER.info("[DistantGunshots] playLocalSound called sound={} fakeSoundPosition={} {} {} volume={} pitch={}", sound.getLocation(), format(gunshot.x), format(gunshot.y), format(gunshot.z), format(gunshot.volume), format(gunshot.pitch));
                if (!playTaczSound(minecraft, gunshot)) {
                    level.playLocalSound(gunshot.x, gunshot.y, gunshot.z, sound, SoundSource.PLAYERS, gunshot.volume, gunshot.pitch, false);
                }
            } else {
                TaczTacticalBreaching.LOGGER.info("[DistantGunshots] Client sound limiter blocked distant gunshot playback");
            }
        }
    }

    public static void clear() {
        QUEUE.clear();
        limiterSecond = -1L;
        soundsThisSecond = 0;
    }

    private static Vec3 directionalProxy(Player player, Vec3 shotPosition) {
        Vec3 eye = player.getEyePosition();
        Vec3 direction = shotPosition.subtract(eye);
        if (direction.lengthSqr() < 0.0001D) {
            direction = player.getLookAngle();
        }
        direction = direction.normalize();
        return eye.add(direction.scale(DIRECTIONAL_PROXY_DISTANCE));
    }

    private static boolean reserveSound() {
        long second = System.currentTimeMillis() / 1000L;
        if (second != limiterSecond) {
            limiterSecond = second;
            soundsThisSecond = 0;
        }
        int maxPerSecond = BallisticBlockConfig.MAX_DISTANT_GUNSHOT_SOUNDS_PER_SECOND.get();
        if (maxPerSecond <= 0 || soundsThisSecond >= maxPerSecond) {
            return false;
        }
        soundsThisSecond++;
        return true;
    }

    private static SoundEvent soundFor(DistantGunshotSoundType type) {
        return switch (type) {
            case RIFLE_POP -> ModSounds.DISTANT_RIFLE_POP.get();
            case RIFLE_ECHO -> ModSounds.DISTANT_RIFLE_ECHO.get();
            case PISTOL_POP -> ModSounds.DISTANT_PISTOL_POP.get();
            case SNIPER_ECHO -> ModSounds.DISTANT_SNIPER_ECHO.get();
            case SHOTGUN_ECHO -> ModSounds.DISTANT_SHOTGUN_ECHO.get();
            case MACHINEGUN_FAR -> ModSounds.DISTANT_MACHINEGUN_FAR.get();
            case EXTREME_ECHO -> ModSounds.DISTANT_EXTREME_ECHO.get();
            case FAR_ECHO -> ModSounds.DISTANT_FAR_ECHO.get();
            case DISTANT_ECHO -> ModSounds.DISTANT_CLOSE_ECHO.get();
            case NEAR_ECHO -> ModSounds.DISTANT_NEAR_ECHO.get();
        };
    }

    private static SoundEvent weaponSound(String gunId, DistantGunshotSoundType type) {
        ResourceLocation id = ResourceLocation.tryParse(gunId);
        if (id != null && id.getNamespace().equals("superbwarfare")) {
            String suffix = switch (type) {
                case EXTREME_ECHO, FAR_ECHO -> "_veryfar";
                case DISTANT_ECHO -> "_far";
                default -> "_fire_3p";
            };
            String base = id.getPath();
            for (String candidate : new String[]{base + suffix, base + "_far", base + "_fire_3p"}) {
                ResourceLocation soundId = new ResourceLocation("superbwarfare", candidate);
                SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getOptional(soundId).orElse(null);
                if (sound != null) {
                    return sound;
                }
            }
        }
        return soundFor(type);
    }

    private static boolean playTaczSound(Minecraft minecraft, ScheduledGunshot shot) {
        ResourceLocation id = ResourceLocation.tryParse(shot.gunId);
        if (id == null || id.getNamespace().equals("superbwarfare") || minecraft.player == null) {
            return false;
        }
        ResourceLocation soundId = TimelessAPI.getClientGunIndex(id)
                .map(index -> index.getDefaultDisplay().getSounds(SoundManager.SHOOT_3P_SOUND))
                .orElse(null);
        if (soundId == null) {
            return false;
        }
        SoundEvent event = SoundEvent.createVariableRangeEvent(soundId);
        GunSoundInstance sound = new GunSoundInstance(event, SoundSource.PLAYERS, shot.volume, shot.pitch,
                minecraft.player, 10000, soundId, false) {
            {
                this.x = shot.x;
                this.y = shot.y;
                this.z = shot.z;
            }
        };
        minecraft.getSoundManager().play(sound);
        return true;
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private record ScheduledGunshot(double x, double y, double z, DistantGunshotSoundType type, float volume, float pitch, long playAtMillis, String gunId) {
    }
}
