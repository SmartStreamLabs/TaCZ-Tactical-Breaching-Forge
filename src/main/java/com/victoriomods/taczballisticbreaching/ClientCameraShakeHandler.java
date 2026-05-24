package com.victoriomods.taczballisticbreaching;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientCameraShakeHandler {
    private static final long MIN_TRIGGER_INTERVAL_MILLIS = 55L;
    private static float ticksRemaining;
    private static float durationTicks;
    private static float strength;
    private static float frequency;
    private static float rollWeight;
    private static float pitchWeight;
    private static long lastTriggerMillis;

    private ClientCameraShakeHandler() {
    }

    public static void shotgun(Vec3 source, boolean suppressed, boolean highCaliber) {
        if (!BallisticBlockConfig.ENABLE_SHOTGUN_SHAKE.get()) {
            return;
        }
        trigger(source, 0.24F * (highCaliber ? 1.25F : 1.0F) * (suppressed ? 0.45F : 1.0F), 0.75F, 1.0F, 2.2F, 0.8F);
    }

    public static void heavyAutomatic(Vec3 source, boolean suppressed) {
        if (!BallisticBlockConfig.ENABLE_SHOTGUN_SHAKE.get()) {
            return;
        }
        trigger(source, 0.08F * (suppressed ? 0.45F : 1.0F), 0.55F, 0.55F, 3.0F, 0.45F);
    }

    public static void suppression(Vec3 source, float closeness, boolean highCaliber) {
        if (!BallisticBlockConfig.ENABLE_SUPPRESSION_SHAKE.get()) {
            return;
        }
        trigger(source, (highCaliber ? 0.28F : 0.12F) * Mth.clamp(closeness, 0.0F, 1.0F), 0.9F, 0.85F, 3.8F, 0.7F);
    }

    public static void breaching(Vec3 source, boolean heavy) {
        if (!BallisticBlockConfig.ENABLE_BREACHING_SHAKE.get()) {
            return;
        }
        trigger(source, heavy ? 0.34F : 0.2F, 0.8F, 0.95F, 2.0F, heavy ? 1.1F : 0.85F);
    }

    public static void closeImpact(Vec3 source, boolean heavy) {
        trigger(source, heavy ? 0.16F : 0.075F, 0.5F, 0.6F, 2.6F, 0.55F);
    }

    public static void explosion(Vec3 source) {
        if (!BallisticBlockConfig.ENABLE_EXPLOSION_SHAKE.get()) {
            return;
        }
        trigger(source, 0.55F, 1.0F, 1.0F, 1.35F, 1.65F);
    }

    public static void flashbang(Vec3 source, float effectStrength) {
        trigger(source, 0.12F * Mth.clamp(effectStrength, 0.12F, 1.0F), 0.36F, 0.42F, 2.6F, 0.7F);
    }

    public static void tick() {
        if (ticksRemaining > 0.0F) {
            ticksRemaining = Math.max(0.0F, ticksRemaining - 1.0F);
            if (ticksRemaining <= 0.0F) {
                strength = 0.0F;
            }
        }
    }

    public static ShakeOffset offset(float partialTick) {
        if (!BallisticBlockConfig.ENABLE_CAMERA_SHAKE.get() || ticksRemaining <= 0.0F || strength <= 0.001F) {
            return ShakeOffset.NONE;
        }
        float age = durationTicks - ticksRemaining + partialTick;
        float fade = Mth.clamp(ticksRemaining / Math.max(1.0F, durationTicks), 0.0F, 1.0F);
        fade *= fade;
        float phase = age * frequency;
        float base = strength * fade;
        float roll = Mth.clamp(Mth.sin(phase * 1.17F) * base * rollWeight, -0.75F, 0.75F);
        float pitch = Mth.clamp(Mth.sin(phase * 0.83F + 1.3F) * base * pitchWeight * 0.42F, -0.35F, 0.35F);
        return new ShakeOffset(pitch, roll);
    }

    public static void clear() {
        ticksRemaining = 0.0F;
        durationTicks = 0.0F;
        strength = 0.0F;
        frequency = 0.0F;
        rollWeight = 0.0F;
        pitchWeight = 0.0F;
        lastTriggerMillis = 0L;
    }

    private static void trigger(Vec3 source, float baseStrength, float roll, float pitch, float freq, float durationMultiplier) {
        if (!BallisticBlockConfig.ENABLE_CAMERA_SHAKE.get() || baseStrength <= 0.0F) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastTriggerMillis < MIN_TRIGGER_INTERVAL_MILLIS && baseStrength < strength * 0.9F) {
            return;
        }

        double maxDistance = BallisticBlockConfig.CAMERA_SHAKE_MAX_DISTANCE.get();
        double distance = player.getEyePosition().distanceTo(source);
        if (distance > maxDistance) {
            return;
        }

        float distanceScale = (float) (1.0D - Mth.clamp(distance / maxDistance, 0.0D, 1.0D));
        float finalStrength = baseStrength * distanceScale * distanceScale * BallisticBlockConfig.CAMERA_SHAKE_INTENSITY.get().floatValue();
        finalStrength = Mth.clamp(finalStrength, 0.0F, 0.72F);
        if (finalStrength <= 0.006F) {
            return;
        }

        lastTriggerMillis = now;
        strength = Math.max(strength * 0.65F, finalStrength);
        durationTicks = Math.max(2.0F, BallisticBlockConfig.CAMERA_SHAKE_DURATION_TICKS.get() * durationMultiplier);
        ticksRemaining = Math.max(ticksRemaining, durationTicks);
        frequency = freq;
        rollWeight = roll;
        pitchWeight = pitch;
    }

    public record ShakeOffset(float pitch, float roll) {
        static final ShakeOffset NONE = new ShakeOffset(0.0F, 0.0F);
    }
}
