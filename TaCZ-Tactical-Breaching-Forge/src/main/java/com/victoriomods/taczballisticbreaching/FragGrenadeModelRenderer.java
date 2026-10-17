package com.victoriomods.taczballisticbreaching;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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

final class FragGrenadeModelRenderer {
    private static final ResourceLocation GRENADE_TEXTURE = new ResourceLocation(TaczTacticalBreaching.MOD_ID, "textures/entity/v40_frag_grenade.png");
    private static final ResourceLocation GRENADE_MESH = new ResourceLocation(TaczTacticalBreaching.MOD_ID, "models/entity/v40_frag_grenade.mesh");
    private static final RenderType GRENADE_TRIANGLES = FragGrenadeRenderTypes.v40Triangles(GRENADE_TEXTURE);
    private static MeshData mesh;
    private static boolean meshLoadAttempted;
    private static boolean textureChecked;

    private FragGrenadeModelRenderer() {
    }

    static ResourceLocation textureLocation() {
        return GRENADE_TEXTURE;
    }

    static void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ItemDisplayContext context, float rollDegrees) {
        render(poseStack, buffer, packedLight, context, rollDegrees, -1);
    }

    static void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ItemDisplayContext context, float rollDegrees, int fuseTicks) {
        warnIfTextureMissing();
        poseStack.pushPose();
        float scale = context == ItemDisplayContext.GUI ? 1.0F : 0.66F;
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rollDegrees * 0.22F));

        int modelLight = readableLight(packedLight);
        MeshData loadedMesh = mesh();
        if (loadedMesh == null) {
            renderFallback(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(GRENADE_TEXTURE)), modelLight);
        } else {
            renderMesh(loadedMesh, poseStack, buffer.getBuffer(GRENADE_TRIANGLES), modelLight);
        }

        if (fuseTicks > 0 && fuseTicks <= 20 && fuseTicks % 6 < 4) {
            float pulse = 1.0F - fuseTicks / 20.0F;
            VertexConsumer glowConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(GRENADE_TEXTURE));
            box(poseStack, glowConsumer, LightTexture.FULL_BRIGHT, -0.018F, 0.315F, -0.018F, 0.018F, 0.365F, 0.018F, 1.0F, 0.38F + pulse * 0.22F, 0.08F);
        }
        poseStack.popPose();
    }

    private static void renderMesh(MeshData mesh, PoseStack poseStack, VertexConsumer consumer, int light) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        float[] vertices = mesh.vertices();
        for (int i = 0; i + 8 < vertices.length; i += 9) {
            vertex(consumer, pose, normal, light,
                    vertices[i], vertices[i + 1], vertices[i + 2],
                    vertices[i + 6], vertices[i + 7], vertices[i + 8],
                    0.0F, 0.0F,
                    vertices[i + 3], vertices[i + 4], vertices[i + 5]);
        }
    }

    private static MeshData mesh() {
        if (meshLoadAttempted) {
            return mesh;
        }
        meshLoadAttempted = true;
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(GRENADE_MESH);
        if (resource.isEmpty()) {
            TaczTacticalBreaching.LOGGER.warn("Missing V40 frag grenade mesh: {}", GRENADE_MESH);
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
                if (parts.length != 9) {
                    TaczTacticalBreaching.LOGGER.warn("Skipping malformed V40 frag grenade mesh line: {}", trimmed);
                    continue;
                }
                for (String part : parts) {
                    values.add(Float.parseFloat(part));
                }
            }
        } catch (IOException | NumberFormatException exception) {
            TaczTacticalBreaching.LOGGER.warn("Could not load V40 frag grenade mesh: {}", GRENADE_MESH, exception);
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
        box(poseStack, consumer, light, -0.13F, -0.28F, -0.13F, 0.13F, 0.18F, 0.13F, 0.18F, 0.30F, 0.11F);
        box(poseStack, consumer, light, -0.18F, 0.16F, -0.12F, 0.18F, 0.29F, 0.12F, 0.05F, 0.05F, 0.045F);
        box(poseStack, consumer, light, -0.06F, 0.29F, -0.06F, 0.06F, 0.34F, 0.06F, 0.62F, 0.52F, 0.28F);
    }

    private static void warnIfTextureMissing() {
        if (textureChecked) {
            return;
        }
        textureChecked = true;
        try {
            if (Minecraft.getInstance().getResourceManager().getResource(GRENADE_TEXTURE).isEmpty()) {
                TaczTacticalBreaching.LOGGER.warn("Missing V40 frag grenade texture: {}", GRENADE_TEXTURE);
            }
        } catch (RuntimeException exception) {
            TaczTacticalBreaching.LOGGER.warn("Could not verify V40 frag grenade texture: {}", GRENADE_TEXTURE, exception);
        }
    }

    private static int readableLight(int packedLight) {
        int block = Math.max(LightTexture.block(packedLight), 6);
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
