package com.victoriomods.taczballisticbreaching;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientSuppressionEffectsHandler {
    private static long limiterSecond = -1L;
    private static int effectsThisSecond;
    private static float blurIntensity;
    private static float stressIntensity;
    private static float directionX;
    private static float directionY;
    private static int ringingTicks;
    private static int ringingDuration;

    private ClientSuppressionEffectsHandler() {
    }

    public static void triggerExplosion(Vec3 source) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || !reserveEffect()) {
            return;
        }
        double distance = player.getEyePosition().distanceTo(source);
        double radius = Math.max(6.0D, BallisticBlockConfig.CAMERA_SHAKE_MAX_DISTANCE.get());
        float closeness = (float) (1.0D - Mth.clamp(distance / radius, 0.0D, 1.0D));
        if (closeness <= 0.02F) {
            return;
        }
        addVisualPressure(player, source, Mth.clamp(closeness * 1.35F, 0.0F, 1.0F));
        startRinging((int) (BallisticBlockConfig.EAR_RINGING_DURATION_TICKS.get() * (1.0F + closeness)), 0.28F + closeness * 0.3F);
    }

    public static void triggerSustainedFire(Vec3 source, boolean highCaliber, boolean suppressed) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || suppressed || !reserveEffect()) {
            return;
        }
        double radius = BallisticBlockConfig.SUPPRESSION_EFFECT_RADIUS.get() + 6.0D;
        double distance = player.getEyePosition().distanceTo(source);
        if (distance > radius || !isIndoor(level, player)) {
            return;
        }
        float closeness = (float) (1.0D - Mth.clamp(distance / radius, 0.0D, 1.0D));
        addVisualPressure(player, source, closeness * (highCaliber ? 0.42F : 0.26F));
        if (closeness > 0.55F) {
            startRinging(BallisticBlockConfig.EAR_RINGING_DURATION_TICKS.get() / 2, 0.12F + closeness * 0.12F);
        }
    }

    public static void tick() {
        blurIntensity *= 0.84F;
        stressIntensity *= 0.88F;
        directionX *= 0.86F;
        directionY *= 0.86F;
        if (ringingTicks > 0) {
            ringingTicks--;
        }
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (BallisticBlockConfig.ENABLE_STRESS_EFFECTS.get() && stressIntensity > 0.01F) {
            int alpha = (int) (Mth.clamp(stressIntensity, 0.0F, 0.45F) * 32.0F);
            graphics.fill(0, 0, width, height, argb(alpha, 112, 118, 124));
        }
        if (BallisticBlockConfig.ENABLE_SUPPRESSION_BLUR.get() && blurIntensity > 0.01F) {
            float strength = (float) (blurIntensity * BallisticBlockConfig.SUPPRESSION_BLUR_STRENGTH.get());
            int alpha = (int) (Mth.clamp(strength, 0.0F, 0.28F) * 255.0F);
            int sideAlpha = (int) (alpha * 0.62F);
            int edge = Math.max(12, Math.min(width, height) / 11);
            int corner = Math.max(edge, Math.min(width, height) / 6);
            int color = argb(alpha, 95, 103, 110);
            int sideColor = argb(sideAlpha, 95, 103, 110);
            graphics.fill(0, 0, width, edge, sideColor);
            graphics.fill(0, height - edge, width, height, sideColor);
            graphics.fill(0, 0, edge, height, sideColor);
            graphics.fill(width - edge, 0, width, height, sideColor);
            if (directionX < -0.12F) {
                graphics.fill(0, 0, corner, height, color);
            } else if (directionX > 0.12F) {
                graphics.fill(width - corner, 0, width, height, color);
            }
            if (directionY < -0.18F) {
                graphics.fill(0, 0, width, corner, color);
            } else if (directionY > 0.18F) {
                graphics.fill(0, height - corner, width, height, color);
            }
        }
    }

    public static void clear() {
        blurIntensity = 0.0F;
        stressIntensity = 0.0F;
        directionX = 0.0F;
        directionY = 0.0F;
        ringingTicks = 0;
        ringingDuration = 0;
        limiterSecond = -1L;
        effectsThisSecond = 0;
    }

    private static void addVisualPressure(Player player, Vec3 source, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        if (BallisticBlockConfig.ENABLE_SUPPRESSION_BLUR.get()) {
            blurIntensity = Math.max(blurIntensity, Math.min(1.0F, amount));
        }
        if (BallisticBlockConfig.ENABLE_STRESS_EFFECTS.get()) {
            stressIntensity = Math.max(stressIntensity, Math.min(0.55F, amount * 0.55F));
        }
        Vec3 toSource = source.subtract(player.getEyePosition()).normalize();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() > 0.001D) {
            right = right.normalize();
            directionX = (float) Mth.clamp(toSource.dot(right), -1.0D, 1.0D);
            Vec3 up = right.cross(look).normalize();
            directionY = (float) Mth.clamp(toSource.dot(up), -1.0D, 1.0D);
        }
    }

    private static void startRinging(int duration, float volume) {
        if (!BallisticBlockConfig.ENABLE_SUPPRESSION_RINGING.get() || duration <= 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        int cappedDuration = Math.min(160, Math.max(duration, ringingTicks));
        if (cappedDuration <= ringingTicks && ringingTicks > ringingDuration * 0.5F) {
            return;
        }
        ringingTicks = cappedDuration;
        ringingDuration = cappedDuration;
    }

    private static boolean reserveEffect() {
        int maxPerSecond = BallisticBlockConfig.MAX_SUPPRESSION_EFFECTS_PER_SECOND.get();
        if (maxPerSecond <= 0) {
            return false;
        }
        long currentSecond = System.currentTimeMillis() / 1000L;
        if (currentSecond != limiterSecond) {
            limiterSecond = currentSecond;
            effectsThisSecond = 0;
        }
        if (effectsThisSecond >= maxPerSecond) {
            return false;
        }
        effectsThisSecond++;
        return true;
    }

    private static boolean isIndoor(ClientLevel level, Player player) {
        BlockPos pos = player.blockPosition();
        return !level.canSeeSky(pos) && level.getMaxLocalRawBrightness(pos) < 12;
    }

    private static int argb(int alpha, int red, int green, int blue) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (red << 16) | (green << 8) | blue;
    }
}
