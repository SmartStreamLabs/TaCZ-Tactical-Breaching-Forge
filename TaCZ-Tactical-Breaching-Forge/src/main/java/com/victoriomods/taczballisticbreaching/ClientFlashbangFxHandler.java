package com.victoriomods.taczballisticbreaching;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class ClientFlashbangFxHandler {
    private static float flashTicks;
    private static float flashDuration;
    private static float flashStrength;
    private static float directStrength;
    private static float blurStrength;
    private static float vignetteTicks;
    private static float tinnitusTicks;
    private static float tinnitusDuration;
    private static long limiterSecond = -1L;
    private static int detonationsThisSecond;

    private ClientFlashbangFxHandler() {
    }

    public static void handle(FlashbangDetonationFxPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null || !BallisticBlockConfig.ENABLE_FLASHBANG.get()) {
            return;
        }
        Vec3 origin = new Vec3(packet.x(), packet.y(), packet.z());
        double distance = player.getEyePosition().distanceTo(origin);
        playAudio(level, player, origin, distance, packet);
        if (distance < 96.0D && reserveDetonation()) {
            spawnDetonationParticles(level, level.random, origin, packet.indoor());
        }

        if (packet.effectStrength() > 0.01F) {
            flashStrength = Math.max(flashStrength, Mth.clamp(packet.effectStrength(), 0.0F, 1.0F));
            directStrength = Math.max(directStrength, Mth.clamp(packet.directStrength(), 0.0F, 1.0F));
            flashDuration = Math.max(1.0F, packet.flashDurationTicks());
            flashTicks = Math.max(flashTicks, flashDuration);
            blurStrength = Math.max(blurStrength, packet.blurStrength() * packet.effectStrength());
            vignetteTicks = Math.max(vignetteTicks, Math.min(28.0F, packet.flashDurationTicks() * 0.55F));
            if (BallisticBlockConfig.ENABLE_FLASHBANG_INDOOR_REVERB.get() && packet.indoor()) {
                vignetteTicks = Math.max(vignetteTicks, Math.min(36.0F, packet.flashDurationTicks() * 0.68F));
            }
            if (packet.effectStrength() > 0.28F) {
                tinnitusDuration = Math.max(1.0F, packet.tinnitusDurationTicks() * Mth.clamp(packet.effectStrength(), 0.2F, 1.0F));
                tinnitusTicks = Math.max(tinnitusTicks, tinnitusDuration);
            }
            ClientCameraShakeHandler.flashbang(origin, packet.effectStrength());
        } else {
            ClientCameraShakeHandler.flashbang(origin, 0.16F);
        }

        ClientIndoorSmokeManager.illuminate(origin, packet.indoor() ? 1.35D : 0.9D, 1.0F, 0.96F, 0.8F);
        ClientNightstickFlashlightHandler.explosion(origin);
    }

    public static void tick() {
        if (flashTicks > 0.0F) {
            flashTicks = Math.max(0.0F, flashTicks - 1.0F);
            if (flashTicks <= 0.0F) {
                flashStrength = 0.0F;
                directStrength = 0.0F;
            }
        }
        if (vignetteTicks > 0.0F) {
            vignetteTicks = Math.max(0.0F, vignetteTicks - 1.0F);
        }
        if (tinnitusTicks > 0.0F) {
            tinnitusTicks = Math.max(0.0F, tinnitusTicks - 1.0F);
        }
        blurStrength *= 0.88F;
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (flashTicks > 0.0F && flashDuration > 0.0F && flashStrength > 0.01F) {
            float age = 1.0F - Mth.clamp(flashTicks / flashDuration, 0.0F, 1.0F);
            float attack = age < 0.08F ? age / 0.08F : 1.0F;
            float fade = flashTicks / flashDuration;
            fade = fade * fade;
            float maxAlpha = 0.32F + directStrength * 0.54F;
            int alpha = (int) (Mth.clamp(flashStrength * attack * fade * maxAlpha, 0.0F, 0.92F) * 255.0F);
            if (alpha > 1) {
                graphics.fill(0, 0, width, height, argb(alpha, 255, 255, 255));
            }
        }
        if (vignetteTicks > 0.0F || blurStrength > 0.01F || tinnitusTicks > 0.0F) {
            float vignette = Mth.clamp(vignetteTicks / 36.0F, 0.0F, 1.0F);
            float ringing = tinnitusDuration <= 0.0F ? 0.0F : Mth.clamp(tinnitusTicks / tinnitusDuration, 0.0F, 1.0F);
            float intensity = Mth.clamp(Math.max(vignette, blurStrength) + ringing * 0.22F, 0.0F, 1.0F);
            int edge = Math.max(10, Math.min(width, height) / 12);
            int corner = Math.max(edge, Math.min(width, height) / 6);
            int edgeAlpha = (int) (intensity * 44.0F);
            int cornerAlpha = (int) (intensity * 34.0F);
            graphics.fill(0, 0, width, edge, argb(edgeAlpha, 228, 232, 236));
            graphics.fill(0, height - edge, width, height, argb(edgeAlpha, 228, 232, 236));
            graphics.fill(0, 0, edge, height, argb(edgeAlpha, 228, 232, 236));
            graphics.fill(width - edge, 0, width, height, argb(edgeAlpha, 228, 232, 236));
            graphics.fill(0, 0, corner, corner, argb(cornerAlpha, 210, 216, 224));
            graphics.fill(width - corner, 0, width, corner, argb(cornerAlpha, 210, 216, 224));
            graphics.fill(0, height - corner, corner, height, argb(cornerAlpha, 210, 216, 224));
            graphics.fill(width - corner, height - corner, width, height, argb(cornerAlpha, 210, 216, 224));
        }
    }

    public static void clear() {
        flashTicks = 0.0F;
        flashDuration = 0.0F;
        flashStrength = 0.0F;
        directStrength = 0.0F;
        blurStrength = 0.0F;
        vignetteTicks = 0.0F;
        tinnitusTicks = 0.0F;
        tinnitusDuration = 0.0F;
        limiterSecond = -1L;
        detonationsThisSecond = 0;
    }

    private static void playAudio(ClientLevel level, Player player, Vec3 origin, double distance, FlashbangDetonationFxPacket packet) {
        double maxDistance = 56.0D;
        float distanceScale = (float) Mth.clamp(1.0D - distance / maxDistance, 0.0D, 1.0D);
        float pressure = Mth.clamp(0.16F + distanceScale * 1.2F, 0.0F, 1.35F);
        float pitch = 0.96F + level.random.nextFloat() * 0.08F;
        level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), ModSounds.FLASHBANG_DETONATE.get(), SoundSource.PLAYERS, pressure, pitch, false);
        if (packet.indoor() && BallisticBlockConfig.ENABLE_FLASHBANG_INDOOR_REVERB.get() && distance < 42.0D) {
            float reverb = (float) Mth.clamp((1.0D - distance / 42.0D) * 0.85D, 0.0D, 0.9D);
            level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), ModSounds.FLASHBANG_INDOOR.get(), SoundSource.PLAYERS, reverb, 0.9F + level.random.nextFloat() * 0.08F, false);
        }
        if (packet.effectStrength() > 0.24F && packet.tinnitusDurationTicks() > 0) {
            float ringVolume = Mth.clamp(packet.effectStrength() * 0.72F, 0.08F, 0.72F);
            level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), ModSounds.FLASHBANG_RING.get(), SoundSource.PLAYERS, ringVolume, 1.0F, false);
        }
    }

    private static void spawnDetonationParticles(ClientLevel level, RandomSource random, Vec3 origin, boolean indoor) {
        DustParticleOptions flash = new DustParticleOptions(new Vector3f(1.0F, 0.96F, 0.78F), 1.05F);
        DustParticleOptions spark = new DustParticleOptions(new Vector3f(1.0F, 0.78F, 0.25F), 0.55F);
        for (int i = 0; i < 14; i++) {
            Vec3 direction = randomDirection(random);
            Vec3 position = origin.add(direction.scale(0.05D + random.nextDouble() * 0.16D)).add(0.0D, random.nextDouble() * 0.22D, 0.0D);
            Vec3 velocity = direction.scale(0.08D + random.nextDouble() * 0.22D).add(0.0D, 0.025D + random.nextDouble() * 0.05D, 0.0D);
            Particle particle = Minecraft.getInstance().particleEngine.createParticle(i < 7 ? flash : spark, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
            if (particle != null) {
                particle.setLifetime(4 + random.nextInt(5));
            }
        }
        int smokeCount = indoor ? 10 : 6;
        for (int i = 0; i < smokeCount; i++) {
            Vec3 direction = randomDirection(random);
            Vec3 position = origin.add(direction.scale(random.nextDouble() * 0.25D)).add(0.0D, random.nextDouble() * 0.28D, 0.0D);
            Vec3 velocity = direction.scale(0.015D + random.nextDouble() * 0.035D).add(0.0D, 0.012D + random.nextDouble() * 0.025D, 0.0D);
            Particle particle = Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
            if (particle != null) {
                particle.setColor(0.62F, 0.62F, 0.6F);
                particle.scale(indoor ? 0.2F + random.nextFloat() * 0.12F : 0.16F + random.nextFloat() * 0.08F);
                particle.setLifetime(indoor ? 24 + random.nextInt(14) : 15 + random.nextInt(10));
            }
        }
        for (int i = 0; i < 6; i++) {
            Vec3 direction = randomDirection(random);
            level.addParticle(ParticleTypes.END_ROD, origin.x, origin.y + 0.1D, origin.z, direction.x * 0.08D, Math.abs(direction.y) * 0.04D, direction.z * 0.08D);
        }
    }

    private static boolean reserveDetonation() {
        long currentSecond = System.currentTimeMillis() / 1000L;
        if (currentSecond != limiterSecond) {
            limiterSecond = currentSecond;
            detonationsThisSecond = 0;
        }
        if (detonationsThisSecond >= 8) {
            return false;
        }
        detonationsThisSecond++;
        return true;
    }

    private static Vec3 randomDirection(RandomSource random) {
        double yaw = random.nextDouble() * Math.PI * 2.0D;
        double y = random.nextDouble() * 0.5D - 0.08D;
        return new Vec3(Math.cos(yaw), y, Math.sin(yaw)).normalize();
    }

    private static int argb(int alpha, int red, int green, int blue) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (red << 16) | (green << 8) | blue;
    }
}
