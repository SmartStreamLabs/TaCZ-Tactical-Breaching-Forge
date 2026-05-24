package com.victoriomods.taczballisticbreaching;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

final class FlashbangGrenadeModelRenderer {
    private static final ResourceLocation FLASHBANG_TEXTURE = new ResourceLocation(TaczTacticalBreaching.MOD_ID, "textures/entity/flashbang_grenade.png");
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
        float scale = context == ItemDisplayContext.GUI ? 1.18F : 0.78F;
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.YP.rotationDegrees(rollDegrees * 0.18F));
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(FLASHBANG_TEXTURE));
        int modelLight = readableLight(packedLight);

        // Matte black octagonal cylinder: compact, readable, and less cube-like than a single block.
        box(poseStack, consumer, modelLight, -0.142F, -0.52F, -0.208F, 0.142F, 0.24F, 0.208F, 0.012F, 0.014F, 0.015F);
        box(poseStack, consumer, modelLight, -0.208F, -0.52F, -0.142F, 0.208F, 0.24F, 0.142F, 0.016F, 0.018F, 0.019F);
        box(poseStack, consumer, modelLight, -0.176F, -0.52F, -0.176F, 0.176F, 0.24F, 0.176F, 0.021F, 0.023F, 0.024F);
        box(poseStack, consumer, modelLight, -0.184F, -0.545F, -0.184F, 0.184F, -0.5F, 0.184F, 0.048F, 0.052F, 0.052F);
        box(poseStack, consumer, modelLight, -0.194F, 0.22F, -0.194F, 0.194F, 0.268F, 0.194F, 0.048F, 0.052F, 0.052F);
        box(poseStack, consumer, modelLight, -0.176F, 0.268F, -0.176F, 0.176F, 0.305F, 0.176F, 0.034F, 0.037F, 0.037F);

        // Body grooves, raised vertical ribs, white warning bands, and worn metal edges.
        for (int i = 0; i < 8; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(i * 45.0F));
            box(poseStack, consumer, modelLight, -0.017F, -0.49F, -0.219F, 0.017F, 0.205F, -0.198F, 0.057F, 0.061F, 0.059F);
            box(poseStack, consumer, modelLight, -0.016F, -0.365F, -0.224F, 0.016F, -0.338F, -0.198F, 0.82F, 0.84F, 0.83F);
            box(poseStack, consumer, modelLight, -0.016F, -0.19F, -0.224F, 0.016F, -0.162F, -0.198F, 0.86F, 0.88F, 0.88F);
            box(poseStack, consumer, modelLight, -0.015F, 0.035F, -0.222F, 0.015F, 0.061F, -0.198F, 0.86F, 0.88F, 0.88F);
            box(poseStack, consumer, modelLight, -0.01F, -0.515F, -0.223F, 0.01F, -0.497F, -0.198F, 0.30F, 0.31F, 0.29F);
            box(poseStack, consumer, modelLight, -0.01F, 0.213F, -0.223F, 0.01F, 0.232F, -0.198F, 0.36F, 0.37F, 0.34F);
            poseStack.popPose();
        }
        box(poseStack, consumer, modelLight, -0.13F, -0.315F, -0.222F, 0.13F, -0.292F, -0.199F, 0.93F, 0.94F, 0.93F);
        box(poseStack, consumer, modelLight, -0.118F, 0.01F, -0.222F, 0.118F, 0.031F, -0.199F, 0.93F, 0.94F, 0.93F);
        box(poseStack, consumer, modelLight, -0.095F, -0.42F, -0.222F, -0.045F, -0.395F, -0.199F, 0.93F, 0.94F, 0.93F);
        box(poseStack, consumer, modelLight, -0.026F, -0.42F, -0.222F, 0.026F, -0.395F, -0.199F, 0.93F, 0.94F, 0.93F);
        box(poseStack, consumer, modelLight, 0.045F, -0.42F, -0.222F, 0.095F, -0.395F, -0.199F, 0.93F, 0.94F, 0.93F);
        addWearMarks(poseStack, consumer, modelLight);

        // Layered fuze assembly with visible vent openings instead of a simple flat cap.
        box(poseStack, consumer, modelLight, -0.13F, 0.305F, -0.13F, 0.13F, 0.36F, 0.13F, 0.075F, 0.078F, 0.074F);
        box(poseStack, consumer, modelLight, -0.102F, 0.36F, -0.102F, 0.102F, 0.438F, 0.102F, 0.038F, 0.041F, 0.040F);
        box(poseStack, consumer, modelLight, -0.076F, 0.438F, -0.076F, 0.076F, 0.535F, 0.076F, 0.028F, 0.030F, 0.030F);
        box(poseStack, consumer, modelLight, -0.128F, 0.518F, -0.096F, 0.128F, 0.592F, 0.096F, 0.06F, 0.063F, 0.058F);
        box(poseStack, consumer, modelLight, -0.11F, 0.592F, -0.073F, 0.11F, 0.637F, 0.073F, 0.13F, 0.132F, 0.12F);
        for (int i = 0; i < 8; i++) {
            float angle = (float) (i * Math.PI * 2.0D / 8.0D);
            topVent(poseStack, consumer, modelLight, Mth.cos(angle) * 0.105F, Mth.sin(angle) * 0.105F, 0.025F);
        }
        topVent(poseStack, consumer, modelLight, 0.0F, 0.0F, 0.028F);

        // Larger, stepped safety spoon/lever. The segments offset slightly to imply a tactical curved lever.
        box(poseStack, consumer, modelLight, 0.118F, -0.2F, -0.07F, 0.228F, 0.16F, 0.07F, 0.105F, 0.11F, 0.105F);
        box(poseStack, consumer, modelLight, 0.102F, 0.12F, -0.066F, 0.222F, 0.39F, 0.066F, 0.13F, 0.135F, 0.128F);
        box(poseStack, consumer, modelLight, 0.072F, 0.345F, -0.06F, 0.205F, 0.535F, 0.06F, 0.18F, 0.185F, 0.172F);
        box(poseStack, consumer, modelLight, 0.134F, -0.155F, 0.073F, 0.205F, 0.105F, 0.09F, 0.74F, 0.75F, 0.69F);
        box(poseStack, consumer, modelLight, 0.124F, 0.145F, 0.071F, 0.196F, 0.35F, 0.088F, 0.83F, 0.84F, 0.78F);
        box(poseStack, consumer, modelLight, 0.055F, 0.49F, -0.08F, 0.214F, 0.57F, 0.08F, 0.24F, 0.245F, 0.225F);
        box(poseStack, consumer, modelLight, 0.066F, 0.57F, -0.053F, 0.19F, 0.625F, 0.053F, 0.46F, 0.45F, 0.39F);

        // Bigger, thicker pull ring with a real loop silhouette.
        box(poseStack, consumer, modelLight, 0.17F, 0.575F, -0.026F, 0.47F, 0.63F, 0.026F, 0.82F, 0.80F, 0.68F);
        box(poseStack, consumer, modelLight, 0.17F, 0.73F, -0.026F, 0.47F, 0.785F, 0.026F, 0.86F, 0.84F, 0.70F);
        box(poseStack, consumer, modelLight, 0.165F, 0.59F, -0.026F, 0.225F, 0.765F, 0.026F, 0.68F, 0.66F, 0.56F);
        box(poseStack, consumer, modelLight, 0.415F, 0.59F, -0.026F, 0.475F, 0.765F, 0.026F, 0.72F, 0.70F, 0.58F);
        box(poseStack, consumer, modelLight, 0.225F, 0.63F, 0.027F, 0.415F, 0.73F, 0.04F, 0.26F, 0.25F, 0.22F);

        drawBodyLabels(poseStack, buffer, modelLight);

        if (fuseTicks > 0 && fuseTicks <= 12 && fuseTicks % 4 < 2) {
            float pulse = 1.0F - fuseTicks / 12.0F;
            box(poseStack, consumer, LightTexture.FULL_BRIGHT, -0.018F, 0.598F, -0.018F, 0.018F, 0.64F, 0.018F, 1.0F, 0.88F + pulse * 0.1F, 0.58F);
        }
        poseStack.popPose();
    }

    private static void drawBodyLabels(PoseStack poseStack, MultiBufferSource buffer, int light) {
        Font font = Minecraft.getInstance().font;
        poseStack.pushPose();
        poseStack.translate(0.0F, -0.05F, -0.226F);
        poseStack.scale(0.00355F, -0.00355F, 0.00355F);
        drawCentered(font, poseStack, buffer, "FLASH BANG", -55.0F, 0xF1F3F2, light);
        drawCentered(font, poseStack, buffer, "WARNING", -20.0F, 0xE7ECEB, light);
        drawCentered(font, poseStack, buffer, "CN0182-20", 23.0F, 0xDDE2E2, light);
        drawCentered(font, poseStack, buffer, "POLICE", 54.0F, 0xDDE2E2, light);
        poseStack.popPose();
    }

    private static void topVent(PoseStack poseStack, VertexConsumer consumer, int light, float x, float z, float radius) {
        box(poseStack, consumer, light, x - radius, 0.305F, z - radius, x + radius, 0.313F, z + radius, 0.54F, 0.55F, 0.50F);
        box(poseStack, consumer, light, x - radius * 0.58F, 0.313F, z - radius * 0.58F, x + radius * 0.58F, 0.319F, z + radius * 0.58F, 0.002F, 0.002F, 0.002F);
    }

    private static void addWearMarks(PoseStack poseStack, VertexConsumer consumer, int light) {
        box(poseStack, consumer, light, -0.172F, -0.485F, -0.225F, -0.105F, -0.472F, -0.198F, 0.42F, 0.43F, 0.39F);
        box(poseStack, consumer, light, 0.072F, -0.255F, -0.224F, 0.152F, -0.241F, -0.198F, 0.32F, 0.33F, 0.30F);
        box(poseStack, consumer, light, -0.035F, 0.165F, -0.224F, 0.052F, 0.178F, -0.198F, 0.38F, 0.39F, 0.35F);
        box(poseStack, consumer, light, -0.219F, -0.055F, -0.04F, -0.198F, -0.02F, 0.062F, 0.26F, 0.27F, 0.25F);
        box(poseStack, consumer, light, 0.198F, -0.39F, -0.05F, 0.219F, -0.36F, 0.08F, 0.34F, 0.35F, 0.32F);
    }

    private static void drawCentered(Font font, PoseStack poseStack, MultiBufferSource buffer, String text, float y, int color, int light) {
        font.drawInBatch(text, -font.width(text) / 2.0F, y, color, false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, light);
    }

    private static void warnIfTextureMissing() {
        if (textureChecked) {
            return;
        }
        textureChecked = true;
        try {
            if (Minecraft.getInstance().getResourceManager().getResource(FLASHBANG_TEXTURE).isEmpty()) {
                TaczTacticalBreaching.LOGGER.warn("Missing flashbang grenade entity texture: {}", FLASHBANG_TEXTURE);
            }
        } catch (RuntimeException exception) {
            TaczTacticalBreaching.LOGGER.warn("Could not verify flashbang grenade entity texture: {}", FLASHBANG_TEXTURE, exception);
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
        face(consumer, pose, normal, light, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, r * 1.12F, g * 1.12F, b * 1.12F, 0.0F, 1.0F, 0.0F);
        face(consumer, pose, normal, light, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r * 0.66F, g * 0.66F, b * 0.66F, 0.0F, -1.0F, 0.0F);
        face(consumer, pose, normal, light, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, r * 0.96F, g * 0.96F, b * 0.96F, 1.0F, 0.0F, 0.0F);
        face(consumer, pose, normal, light, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, r * 0.92F, g * 0.92F, b * 0.92F, -1.0F, 0.0F, 0.0F);
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
