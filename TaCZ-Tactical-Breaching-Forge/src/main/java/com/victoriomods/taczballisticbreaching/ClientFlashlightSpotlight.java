package com.victoriomods.taczballisticbreaching;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public final class ClientFlashlightSpotlight {
    private static ShaderInstance shader;
    private static RenderTarget sceneCopy;

    private ClientFlashlightSpotlight() {
    }

    public static void registerShader(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                new ResourceLocation(TaczTacticalBreaching.MOD_ID, "flashlight_spotlight"),
                DefaultVertexFormat.POSITION), loaded -> shader = loaded);
    }

    public static void render(PoseStack poseStack, Camera camera, Vec3 origin, Vec3 direction,
                              float range, float intensity) {
        if (shader == null || intensity <= 0.01F) {
            return;
        }
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        if (main.width <= 0 || main.height <= 0) {
            return;
        }
        if (sceneCopy == null || sceneCopy.width != main.width || sceneCopy.height != main.height) {
            clear();
            sceneCopy = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
            sceneCopy.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        }

        // Shade into a separate color target while reading the untouched main color/depth textures.
        sceneCopy.bindWrite(false);

        Matrix4f viewProjection = new Matrix4f(RenderSystem.getProjectionMatrix())
                .mul(poseStack.last().pose());
        Matrix4f inverse = new Matrix4f(viewProjection).invert();
        Vec3 relative = origin.subtract(camera.getPosition());
        shader.setSampler("SceneColor", main.getColorTextureId());
        shader.setSampler("SceneDepth", main.getDepthTextureId());
        shader.getUniform("InverseViewProjection").set(inverse);
        shader.getUniform("ViewProjection").set(viewProjection);
        shader.getUniform("ScreenSize").set((float) main.width, (float) main.height);
        shader.getUniform("LightOrigin").set((float) relative.x, (float) relative.y, (float) relative.z);
        shader.getUniform("LightDirection").set((float) direction.x, (float) direction.y, (float) direction.z);
        shader.getUniform("LightRange").set(range);
        shader.getUniform("LightIntensity").set(intensity);
        shader.getUniform("HotspotStrength").set(BallisticBlockConfig.FLASHLIGHT_HOTSPOT_INTENSITY.get().floatValue());
        shader.getUniform("SpillStrength").set(BallisticBlockConfig.FLASHLIGHT_SPILL_INTENSITY.get().floatValue());
        shader.getUniform("BeamSoftness").set(BallisticBlockConfig.FLASHLIGHT_BEAM_SOFTNESS.get().floatValue());
        String quality = BallisticBlockConfig.FLASHLIGHT_QUALITY.get().toUpperCase(Locale.ROOT);
        shader.getUniform("Quality").set(quality.equals("LOW") ? 0 : quality.equals("MEDIUM") ? 1
                : quality.equals("HIGH") ? 2 : 3);
        String temperature = BallisticBlockConfig.FLASHLIGHT_COLOR_TEMPERATURE.get().toUpperCase(Locale.ROOT);
        shader.getUniform("LightColor").set(temperature.equals("COOL") ? 0.86F : 1.0F,
                temperature.equals("WARM") ? 0.93F : 0.98F,
                temperature.equals("WARM") ? 0.83F : 1.0F);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        ShaderInstance previousShader = RenderSystem.getShader();
        RenderSystem.setShader(() -> shader);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        buffer.vertex(-1.0D, -1.0D, 0.0D).endVertex();
        buffer.vertex(1.0D, -1.0D, 0.0D).endVertex();
        buffer.vertex(1.0D, 1.0D, 0.0D).endVertex();
        buffer.vertex(-1.0D, 1.0D, 0.0D).endVertex();
        BufferUploader.drawWithShader(buffer.end());
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, sceneCopy.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, main.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, main.width, main.height, 0, 0, main.width, main.height,
                GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST);
        main.bindWrite(false);
        if (previousShader != null) {
            RenderSystem.setShader(() -> previousShader);
        }
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void clear() {
        if (sceneCopy != null) {
            sceneCopy.destroyBuffers();
            sceneCopy = null;
        }
    }
}
