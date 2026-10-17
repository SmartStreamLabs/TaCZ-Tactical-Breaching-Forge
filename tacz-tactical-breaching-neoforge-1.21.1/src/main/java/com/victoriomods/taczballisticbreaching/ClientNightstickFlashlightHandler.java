package com.victoriomods.taczballisticbreaching;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class ClientNightstickFlashlightHandler {
    private static final KeyMapping TOGGLE_FLASHLIGHT = new KeyMapping(
            "key.tacz_tactical_breaching.toggle_nightstick_flashlight",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.tacz_tactical_breaching"
    );
    private static final double MAX_RENDER_DISTANCE_SQR = 80.0D * 80.0D;
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
        sendServerState(enabled && hasNightstick());
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

        Beam beam = currentBeam(level, player);
        if (beam.origin.distanceToSqr(camera.getPosition()) > MAX_RENDER_DISTANCE_SQR) {
            return;
        }
        if (!ClientEffectRenderSafety.canRenderAt(camera.getPosition(), beam.end)) {
            return;
        }
        renderBeam(poseStack, camera, beam, level);
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
    }

    private static boolean hasNightstick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return false;
        }
        return NightstickAttachmentUtil.hasNightstick(player);
    }

    private static void sendServerState(boolean active) {
        if (active == lastSentEnabled) {
            return;
        }
        lastSentEnabled = active;
        ModNetwork.sendToServer(new NightstickFlashlightPacket(active));
    }

    private static Beam currentBeam(ClientLevel level, LocalPlayer player) {
        Vec3 forward = player.getLookAngle();
        forward = forward.lengthSqr() < 0.001D ? new Vec3(0.0D, 0.0D, 1.0D) : forward.normalize();
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        right = right.lengthSqr() < 0.001D ? new Vec3(1.0D, 0.0D, 0.0D) : right.normalize();
        Vec3 origin = player.getEyePosition()
                .add(forward.scale(0.42D))
                .add(right.scale(0.075D))
                .add(0.0D, -0.105D, 0.0D);
        double range = BallisticBlockConfig.FLASHLIGHT_RANGE.get();
        BlockHitResult hit = level.clip(new ClipContext(origin, origin.add(forward.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : origin.add(forward.scale(range));
        Vec3 normal = hit.getType() == HitResult.Type.BLOCK ? Vec3.atLowerCornerOf(hit.getDirection().getNormal()).normalize() : forward.scale(-1.0D);
        return new Beam(origin, end, forward, normal, hit.getType() == HitResult.Type.BLOCK);
    }

    private static void illuminateSmoke() {
        long now = System.currentTimeMillis();
        if (now - lastSmokeLightMillis < 70L || !BallisticBlockConfig.ENABLE_NIGHTSTICK_DYNAMIC_LIGHTING.get()) {
            return;
        }
        lastSmokeLightMillis = now;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return;
        }
        Beam beam = currentBeam(level, player);
        double intensity = BallisticBlockConfig.FLASHLIGHT_INTENSITY.get() * flickerScale(level);
        int samples = Math.max(4, Math.min(10, BallisticBlockConfig.FLASHLIGHT_RANGE.get() / 2));
        Vec3 direction = beam.end.subtract(beam.origin);
        for (int i = 1; i <= samples; i++) {
            double t = i / (double) samples;
            Vec3 pos = beam.origin.add(direction.scale(t));
            ClientIndoorSmokeManager.illuminate(pos, intensity * (1.0D - t * 0.42D), 1.0F, 0.92F, 0.72F);
        }
    }

    private static void renderBeam(PoseStack poseStack, Camera camera, Beam beam, ClientLevel level) {
        Vec3 cameraPos = camera.getPosition();
        double length = beam.origin.distanceTo(beam.end);
        if (length <= 0.05D) {
            return;
        }
        float darkness = darknessBoost(level, beam);
        float flicker = flickerScale(level);
        float intensity = (float) BallisticBlockConfig.FLASHLIGHT_INTENSITY.get().doubleValue() * darkness * flicker;
        if (intensity <= 0.01F) {
            return;
        }

        ClientEffectRenderSafety.beginDepthTestedTranslucent();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        Vec3 forward = beam.forward;
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        right = right.lengthSqr() < 0.001D ? new Vec3(1.0D, 0.0D, 0.0D) : right.normalize();
        Vec3 up = right.cross(forward).normalize();
        Vec3 start = beam.origin.subtract(cameraPos);
        Vec3 end = beam.end.subtract(cameraPos);
        double endRadius = Math.min(1.85D, 0.18D + length * 0.115D);
        addConeFace(buffer, matrix, start, end, right, endRadius, 1.0F, 0.92F, 0.70F, 0.115F * intensity);
        addConeFace(buffer, matrix, start, end, up, endRadius * 0.82D, 1.0F, 0.92F, 0.70F, 0.075F * intensity);
        addCore(buffer, matrix, start, end, right, up, length, intensity);
        if (beam.hitBlock) {
            renderHotspot(buffer, matrix, cameraPos, beam, right, up, intensity, endRadius);
        }

        var meshData = buffer.build();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }
        ClientEffectRenderSafety.endDepthTestedTranslucent();
    }

    private static void addConeFace(BufferBuilder buffer, Matrix4f matrix, Vec3 start, Vec3 end, Vec3 axis, double radius, float r, float g, float b, float alpha) {
        Vec3 edge = axis.scale(radius);
        vertex(buffer, matrix, start, r, g, b, alpha * 0.08F);
        vertex(buffer, matrix, end.add(edge), r, g, b, 0.0F);
        vertex(buffer, matrix, end, r, g, b, alpha);
        vertex(buffer, matrix, end.subtract(edge), r, g, b, 0.0F);
    }

    private static void addCore(BufferBuilder buffer, Matrix4f matrix, Vec3 start, Vec3 end, Vec3 right, Vec3 up, double length, float intensity) {
        double width = Math.min(0.34D, 0.035D + length * 0.018D);
        Vec3 offset = right.add(up).normalize().scale(width);
        vertex(buffer, matrix, start.subtract(offset.scale(0.18D)), 1.0F, 0.96F, 0.78F, 0.12F * intensity);
        vertex(buffer, matrix, start.add(offset.scale(0.18D)), 1.0F, 0.96F, 0.78F, 0.12F * intensity);
        vertex(buffer, matrix, end.add(offset), 1.0F, 0.95F, 0.72F, 0.035F * intensity);
        vertex(buffer, matrix, end.subtract(offset), 1.0F, 0.95F, 0.72F, 0.035F * intensity);
    }

    private static void renderHotspot(BufferBuilder buffer, Matrix4f matrix, Vec3 cameraPos, Beam beam, Vec3 right, Vec3 up, float intensity, double radius) {
        Vec3 center = beam.end.add(beam.normal.scale(0.006D)).subtract(cameraPos);
        Vec3 tx = right.scale(radius * 0.46D);
        Vec3 by = up.scale(radius * 0.46D);
        vertex(buffer, matrix, center.subtract(tx).subtract(by), 1.0F, 0.92F, 0.68F, 0.0F);
        vertex(buffer, matrix, center.add(tx).subtract(by), 1.0F, 0.92F, 0.68F, 0.0F);
        vertex(buffer, matrix, center.add(tx.scale(0.18D)).add(by.scale(0.18D)), 1.0F, 0.95F, 0.76F, 0.34F * intensity);
        vertex(buffer, matrix, center.subtract(tx).add(by), 1.0F, 0.92F, 0.68F, 0.0F);
    }

    private static float darknessBoost(ClientLevel level, Beam beam) {
        BlockPos pos = BlockPos.containing(beam.end);
        int brightness = level.getMaxLocalRawBrightness(pos);
        boolean indoor = !level.canSeeSky(pos);
        float boost = indoor ? 1.28F : 0.72F;
        if (brightness <= 7) {
            boost += 0.62F;
        } else if (brightness >= 13) {
            boost *= 0.42F;
        }
        return Mth.clamp(boost, 0.18F, 2.1F);
    }

    private static float flickerScale(ClientLevel level) {
        long remaining = flickerUntilMillis - System.currentTimeMillis();
        if (remaining <= 0L) {
            return 1.0F;
        }
        float wave = 0.72F + Mth.sin(level.getGameTime() * 1.7F) * 0.22F;
        return Mth.clamp(wave, 0.42F, 1.0F);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 position, float r, float g, float b, float a) {
        buffer.addVertex(matrix, (float) position.x, (float) position.y, (float) position.z).setColor(r, g, b, a);
    }

    private record Beam(Vec3 origin, Vec3 end, Vec3 forward, Vec3 normal, boolean hitBlock) {
    }
}
