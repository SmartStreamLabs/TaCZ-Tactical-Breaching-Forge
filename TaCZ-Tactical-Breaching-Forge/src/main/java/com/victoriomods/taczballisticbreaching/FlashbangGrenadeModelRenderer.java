package com.victoriomods.taczballisticbreaching;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class FlashbangGrenadeModelRenderer {
    private static final ResourceLocation FLASHBANG_TEXTURE = new ResourceLocation(TaczTacticalBreaching.MOD_ID, "textures/entity/low_poly_flashbang.png");
    private static final ResourceLocation FLASHBANG_MESH = new ResourceLocation(TaczTacticalBreaching.MOD_ID, "models/entity/low_poly_flashbang.mesh");
    private static final RenderType FLASHBANG_TRIANGLES = FragGrenadeRenderTypes.flashbangTriangles(FLASHBANG_TEXTURE);
    private static MeshData mesh;
    private static boolean meshLoadAttempted;
    private static boolean textureChecked;

    private FlashbangGrenadeModelRenderer() {
    }

    static ResourceLocation textureLocation() {
        return FLASHBANG_TEXTURE;
    }

    static void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ItemDisplayContext context, float rollDegrees) {
        render(poseStack, buffer, packedLight, context, rollDegrees, -1);
    }

    static void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ItemDisplayContext context, float rollDegrees, int fuseTicks) {
        warnIfTextureMissing();
        poseStack.pushPose();
        float scale = context == ItemDisplayContext.GUI ? 0.92F : 0.58F;
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.YP.rotationDegrees(rollDegrees * 0.18F));

        int modelLight = readableLight(packedLight);
        MeshData loadedMesh = mesh();
        if (loadedMesh == null) {
            renderFallback(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(FLASHBANG_TEXTURE)), modelLight);
        } else {
            renderMesh(loadedMesh, poseStack, buffer.getBuffer(FLASHBANG_TRIANGLES), modelLight);
        }

        if (fuseTicks > 0 && fuseTicks <= 12 && fuseTicks % 4 < 2) {
            float pulse = 1.0F - fuseTicks / 12.0F;
            VertexConsumer glowConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(FLASHBANG_TEXTURE));
            box(poseStack, glowConsumer, LightTexture.FULL_BRIGHT, -0.018F, 0.42F, -0.018F, 0.018F, 0.47F, 0.018F, 1.0F, 0.9F + pulse * 0.08F, 0.62F);
        }
        poseStack.popPose();
    }

    private static void renderMesh(MeshData mesh, PoseStack poseStack, VertexConsumer consumer, int light) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        float[] vertices = mesh.vertices();
        for (int i = 0; i + 10 < vertices.length; i += 11) {
            vertex(consumer, pose, normal, light,
                    vertices[i], vertices[i + 1], vertices[i + 2],
                    vertices[i + 6], vertices[i + 7], vertices[i + 8],
                    vertices[i + 9], vertices[i + 10],
                    vertices[i + 3], vertices[i + 4], vertices[i + 5]);
        }
    }

    private static MeshData mesh() {
        if (meshLoadAttempted) {
            return mesh;
        }
        meshLoadAttempted = true;
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(FLASHBANG_MESH);
        if (resource.isEmpty()) {
            TaczTacticalBreaching.LOGGER.warn("Missing low-poly flashbang mesh: {}", FLASHBANG_MESH);
            return null;
        }
        List<Float> values = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] parts = trimmed.split("\\s+");
                if (parts.length != 11) {
                    TaczTacticalBreaching.LOGGER.warn("Skipping malformed low-poly flashbang mesh line: {}", trimmed);
                    continue;
                }
                for (String part : parts) {
                    values.add(Float.parseFloat(part));
                }
            }
        } catch (IOException | NumberFormatException exception) {
            TaczTacticalBreaching.LOGGER.warn("Could not load low-poly flashbang mesh: {}", FLASHBANG_MESH, exception);
            return null;
        }
        float[] bakedVertices = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            bakedVertices[i] = values.get(i);
        }
        mesh = new MeshData(bakedVertices);
        return mesh;
    }

    private static void renderFallback(PoseStack poseStack, VertexConsumer consumer, int light) {
        box(poseStack, consumer, light, -0.12F, -0.35F, -0.12F, 0.12F, 0.31F, 0.12F, 0.025F, 0.027F, 0.028F);
        box(poseStack, consumer, light, -0.16F, 0.28F, -0.10F, 0.16F, 0.42F, 0.10F, 0.045F, 0.046F, 0.043F);
        box(poseStack, consumer, light, -0.04F, 0.42F, -0.04F, 0.04F, 0.50F, 0.04F, 0.78F, 0.70F, 0.46F);
    }

    private static void warnIfTextureMissing() {
        if (textureChecked) {
            return;
        }
        textureChecked = true;
        try {
            if (Minecraft.getInstance().getResourceManager().getResource(FLASHBANG_TEXTURE).isEmpty()) {
                TaczTacticalBreaching.LOGGER.warn("Missing low-poly flashbang texture: {}", FLASHBANG_TEXTURE);
            }
        } catch (RuntimeException exception) {
            TaczTacticalBreaching.LOGGER.warn("Could not verify low-poly flashbang texture: {}", FLASHBANG_TEXTURE, exception);
        }
    }

    private static int readableLight(int packedLight) {
        int block = Math.max(LightTexture.block(packedLight), 7);
        int sky = Math.max(LightTexture.sky(packedLight), 5);
        return LightTexture.pack(block, sky);
    }

    private static void box(PoseStack poseStack, VertexConsumer consumer, int light, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        face(consumer, pose, normal, light, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, 0.0F, 0.0F, 1.0F);
        face(consumer, pose, normal, light, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, r * 0.84F, g * 0.84F, b * 0.84F, 0.0F, 0.0F, -1.0F);
        face(consumer, pose, normal, light, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, r * 1.08F, g * 1.08F, b * 1.08F, 0.0F, 1.0F, 0.0F);
        face(consumer, pose, normal, light, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r * 0.72F, g * 0.72F, b * 0.72F, 0.0F, -1.0F, 0.0F);
        face(consumer, pose, normal, light, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, r * 0.94F, g * 0.94F, b * 0.94F, 1.0F, 0.0F, 0.0F);
        face(consumer, pose, normal, light, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, r * 0.91F, g * 0.91F, b * 0.91F, -1.0F, 0.0F, 0.0F);
    }

    private static void face(VertexConsumer consumer, Matrix4f pose, Matrix3f normalMatrix, int light, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float nx, float ny, float nz) {
        vertex(consumer, pose, normalMatrix, light, x1, y1, z1, r, g, b, 0.0F, 0.0F, nx, ny, nz);
        vertex(consumer, pose, normalMatrix, light, x2, y2, z2, r, g, b, 1.0F, 0.0F, nx, ny, nz);
        vertex(consumer, pose, normalMatrix, light, x3, y3, z3, r, g, b, 1.0F, 1.0F, nx, ny, nz);
        vertex(consumer, pose, normalMatrix, light, x4, y4, z4, r, g, b, 0.0F, 1.0F, nx, ny, nz);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normalMatrix, int light, float x, float y, float z, float r, float g, float b, float u, float v, float nx, float ny, float nz) {
        consumer.vertex(pose, x, y, z)
                .color(Mth.clamp(r, 0.0F, 1.0F), Mth.clamp(g, 0.0F, 1.0F), Mth.clamp(b, 0.0F, 1.0F), 1.0F)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normalMatrix, nx, ny, nz)
                .endVertex();
    }

    private record MeshData(float[] vertices) {
    }
}
