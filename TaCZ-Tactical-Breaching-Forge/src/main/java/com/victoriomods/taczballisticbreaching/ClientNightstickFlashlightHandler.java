package com.victoriomods.taczballisticbreaching;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class ClientNightstickFlashlightHandler {
    private static final KeyMapping TOGGLE_FLASHLIGHT = new KeyMapping(
            "key.tacz_tactical_breaching.toggle_nightstick_flashlight",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K,
            "key.categories.tacz_tactical_breaching");
    private static boolean enabled;
    private static boolean lastSentEnabled;
    private static long flickerUntilMillis;
    private static long lastSmokeLightMillis;

    private ClientNightstickFlashlightHandler() {
    }

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_FLASHLIGHT);
    }

    public static void tick() {
        while (TOGGLE_FLASHLIGHT.consumeClick()) {
            if (hasNightstick()) {
                enabled = !enabled;
            }
        }
        if (!hasNightstick()) {
            enabled = false;
        }
        // Surface lighting is now client-rendered, not moving LIGHT blocks.
        sendServerState(false);
        if (enabled) {
            illuminateSmoke();
        }
    }

    public static void render(PoseStack poseStack, Camera camera) {
        if (!enabled || !BallisticBlockConfig.ENABLE_NIGHTSTICK_FLASHLIGHT.get() || !hasNightstick()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return;
        }
        float partialTick = minecraft.getFrameTime();
        Vec3 forward = player.getViewVector(partialTick).normalize();
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        right = right.lengthSqr() < 0.001D ? new Vec3(1.0D, 0.0D, 0.0D) : right.normalize();
        Vec3 origin = player.getEyePosition(partialTick).add(forward.scale(0.42D))
                .add(right.scale(0.075D)).add(0.0D, -0.105D, 0.0D);
        double range = BallisticBlockConfig.FLASHLIGHT_RANGE.get();
        BlockHitResult hit = level.clip(new ClipContext(origin, origin.add(forward.scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        float cutoff = hit.getType() == HitResult.Type.BLOCK
                ? (float) Math.min(range, origin.distanceTo(hit.getLocation()) + 1.5D)
                : (float) range;
        float intensity = (float) (BallisticBlockConfig.FLASHLIGHT_INTENSITY.get() * flickerScale(level));
        BlockPos target = BlockPos.containing(hit.getLocation());
        if (level.getMaxLocalRawBrightness(target) >= 13 && level.canSeeSky(target)) {
            intensity *= 0.45F;
        }
        ClientFlashlightSpotlight.render(poseStack, camera, origin, forward, cutoff, intensity);
    }

    public static void explosion(Vec3 origin) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.distanceToSqr(origin) < 14.0D * 14.0D) {
            flickerUntilMillis = System.currentTimeMillis() + 420L;
        }
    }

    public static void clear() {
        enabled = false;
        sendServerState(false);
        flickerUntilMillis = 0L;
        lastSmokeLightMillis = 0L;
        ClientFlashlightSpotlight.clear();
    }

    private static boolean hasNightstick() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && NightstickAttachmentUtil.hasNightstick(player);
    }

    private static void sendServerState(boolean active) {
        if (active != lastSentEnabled) {
            lastSentEnabled = active;
            ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(), new NightstickFlashlightPacket(active));
        }
    }

    private static void illuminateSmoke() {
        long now = System.currentTimeMillis();
        if (now - lastSmokeLightMillis < 70L || !BallisticBlockConfig.ENABLE_NIGHTSTICK_DYNAMIC_LIGHTING.get()) {
            return;
        }
        lastSmokeLightMillis = now;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        Vec3 origin = minecraft.player.getEyePosition();
        Vec3 forward = minecraft.player.getLookAngle();
        double intensity = BallisticBlockConfig.FLASHLIGHT_INTENSITY.get() * flickerScale(minecraft.level);
        int samples = Math.max(4, Math.min(10, BallisticBlockConfig.FLASHLIGHT_RANGE.get() / 2));
        for (int i = 1; i <= samples; i++) {
            double t = i / (double) samples;
            ClientIndoorSmokeManager.illuminate(origin.add(forward.scale(t * BallisticBlockConfig.FLASHLIGHT_RANGE.get())),
                    intensity * (1.0D - t * 0.42D), 0.96F, 0.97F, 1.0F);
        }
    }

    private static float flickerScale(ClientLevel level) {
        if (flickerUntilMillis <= System.currentTimeMillis()) {
            return 1.0F;
        }
        return Mth.clamp(0.72F + Mth.sin(level.getGameTime() * 1.7F) * 0.22F, 0.42F, 1.0F);
    }
}
