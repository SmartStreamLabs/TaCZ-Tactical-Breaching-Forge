package com.victoriomods.taczballisticbreaching;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

final class FragGrenadeModelRenderer {
    private static final ResourceLocation GRENADE_TEXTURE = new ResourceLocation(TaczTacticalBreaching.MOD_ID, "textures/entity/frag_grenade.png");
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
        float scale = context == ItemDisplayContext.GUI ? 1.35F : 0.92F;
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rollDegrees * 0.22F));
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(GRENADE_TEXTURE));
        int modelLight = readableLight(packedLight);

        // Compact tactical 3D grenade: layered body, raised grooves, metal spoon, pin, and pull ring.
        box(poseStack, consumer, modelLight, -0.19F, -0.31F, -0.14F, 0.19F, 0.25F, 0.14F, 0.34F, 0.42F, 0.25F);
        box(poseStack, consumer, modelLight, -0.155F, -0.35F, -0.112F, 0.155F, 0.29F, 0.112F, 0.28F, 0.37F, 0.22F);
        box(poseStack, consumer, modelLight, -0.118F, -0.385F, -0.088F, 0.118F, 0.32F, 0.088F, 0.33F, 0.42F, 0.26F);
        box(poseStack, consumer, modelLight, -0.158F, 0.18F, -0.118F, 0.158F, 0.225F, 0.118F, 0.42F, 0.49F, 0.29F);
        box(poseStack, consumer, modelLight, -0.126F, 0.25F, -0.102F, 0.126F, 0.395F, 0.102F, 0.18F, 0.18F, 0.16F);
        box(poseStack, consumer, modelLight, -0.052F, 0.38F, -0.052F, 0.052F, 0.525F, 0.052F, 0.50F, 0.48F, 0.39F);
        box(poseStack, consumer, modelLight, -0.03F, 0.505F, -0.026F, 0.03F, 0.565F, 0.026F, 0.80F, 0.63F, 0.28F);

        // Safety lever/spoon and hinge, kept visibly metallic instead of disappearing into black.
        box(poseStack, consumer, modelLight, 0.108F, -0.265F, -0.042F, 0.195F, 0.325F, 0.042F, 0.58F, 0.59F, 0.54F);
        box(poseStack, consumer, modelLight, 0.122F, -0.208F, 0.044F, 0.18F, 0.285F, 0.06F, 0.86F, 0.86F, 0.78F);
        box(poseStack, consumer, modelLight, 0.096F, -0.245F, -0.046F, 0.126F, 0.255F, -0.058F, 0.82F, 0.83F, 0.75F);
        box(poseStack, consumer, modelLight, 0.086F, 0.265F, -0.052F, 0.182F, 0.335F, 0.052F, 0.44F, 0.45F, 0.40F);
        box(poseStack, consumer, modelLight, 0.102F, 0.335F, -0.036F, 0.166F, 0.382F, 0.036F, 0.66F, 0.63F, 0.50F);

        // Pull ring made from several thin cuboids so it reads as a real 3D ring in flight.
        box(poseStack, consumer, modelLight, 0.172F, 0.35F, -0.021F, 0.36F, 0.393F, 0.021F, 0.92F, 0.86F, 0.66F);
        box(poseStack, consumer, modelLight, 0.318F, 0.292F, -0.021F, 0.366F, 0.455F, 0.021F, 0.82F, 0.76F, 0.58F);
        box(poseStack, consumer, modelLight, 0.215F, 0.448F, -0.018F, 0.348F, 0.488F, 0.018F, 0.94F, 0.88F, 0.68F);
        box(poseStack, consumer, modelLight, 0.198F, 0.292F, -0.018F, 0.242F, 0.38F, 0.018F, 0.78F, 0.72F, 0.55F);
        box(poseStack, consumer, modelLight, 0.236F, 0.385F, 0.022F, 0.315F, 0.43F, 0.034F, 0.40F, 0.38F, 0.32F);

        // Raised tactical grooves and subtle edge highlights on the body.
        for (int i = -2; i <= 2; i++) {
            float y = i * 0.115F - 0.04F;
            box(poseStack, consumer, modelLight, -0.207F, y - 0.012F, -0.147F, 0.207F, y + 0.012F, 0.147F, 0.20F, 0.27F, 0.15F);
            box(poseStack, consumer, modelLight, -0.166F, y + 0.014F, -0.151F, 0.166F, y + 0.022F, -0.137F, 0.52F, 0.58F, 0.38F);
        }
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(i * 90.0F));
            box(poseStack, consumer, modelLight, -0.014F, -0.32F, -0.169F, 0.014F, 0.22F, -0.135F, 0.20F, 0.27F, 0.15F);
            box(poseStack, consumer, modelLight, -0.01F, -0.235F, -0.174F, 0.01F, 0.17F, -0.166F, 0.54F, 0.60F, 0.39F);
            poseStack.popPose();
        }
        box(poseStack, consumer, modelLight, -0.195F, -0.285F, 0.122F, -0.158F, 0.19F, 0.153F, 0.58F, 0.64F, 0.41F);
        box(poseStack, consumer, modelLight, 0.158F, -0.285F, 0.122F, 0.195F, 0.19F, 0.153F, 0.25F, 0.32F, 0.19F);
        box(poseStack, consumer, modelLight, -0.085F, -0.33F, 0.118F, 0.085F, -0.295F, 0.152F, 0.52F, 0.58F, 0.36F);
        box(poseStack, consumer, modelLight, -0.07F, 0.02F, 0.143F, 0.07F, 0.085F, 0.158F, 0.68F, 0.72F, 0.44F);
        box(poseStack, consumer, modelLight, -0.15F, -0.19F, 0.154F, -0.035F, -0.155F, 0.166F, 0.74F, 0.64F, 0.18F);
        box(poseStack, consumer, modelLight, -0.14F, -0.105F, 0.154F, -0.11F, -0.078F, 0.166F, 0.83F, 0.83F, 0.70F);
        box(poseStack, consumer, modelLight, -0.095F, -0.105F, 0.154F, -0.065F, -0.078F, 0.166F, 0.83F, 0.83F, 0.70F);
        box(poseStack, consumer, modelLight, -0.05F, -0.105F, 0.154F, -0.02F, -0.078F, 0.166F, 0.83F, 0.83F, 0.70F);
        box(poseStack, consumer, modelLight, 0.045F, -0.33F, 0.154F, 0.092F, -0.305F, 0.166F, 0.86F, 0.86F, 0.76F);
        box(poseStack, consumer, modelLight, 0.112F, -0.12F, 0.154F, 0.153F, -0.095F, 0.166F, 0.62F, 0.66F, 0.46F);

        if (fuseTicks > 0 && fuseTicks <= 20 && fuseTicks % 6 < 4) {
            float pulse = 1.0F - fuseTicks / 20.0F;
            box(poseStack, consumer, LightTexture.FULL_BRIGHT, -0.018F, 0.56F, -0.018F, 0.018F, 0.605F, 0.018F, 1.0F, 0.38F + pulse * 0.22F, 0.08F);
        }
        poseStack.popPose();
    }

    private static void warnIfTextureMissing() {
        if (textureChecked) {
            return;
        }
        textureChecked = true;
        try {
            if (Minecraft.getInstance().getResourceManager().getResource(GRENADE_TEXTURE).isEmpty()) {
                TaczTacticalBreaching.LOGGER.warn("Missing frag grenade entity texture: {}", GRENADE_TEXTURE);
            }
        } catch (RuntimeException exception) {
            TaczTacticalBreaching.LOGGER.warn("Could not verify frag grenade entity texture: {}", GRENADE_TEXTURE, exception);
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
}
