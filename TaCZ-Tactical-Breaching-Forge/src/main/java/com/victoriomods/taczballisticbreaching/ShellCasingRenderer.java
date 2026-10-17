package com.victoriomods.taczballisticbreaching;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.model.BedrockAmmoModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShellCasingRenderer extends EntityRenderer<ShellCasingEntity> {
    private final ItemRenderer itemRenderer;

    public ShellCasingRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.035F;
    }

    @Override
    public void render(ShellCasingEntity casing, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.035D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, casing.yRotO, casing.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, casing.xRotO, casing.getXRot())));
        poseStack.mulPose(Axis.ZP.rotationDegrees(casing.getRenderRoll(partialTick)));
        renderTaczShellModel(casing, poseStack, buffer, packedLight);
        poseStack.popPose();
        super.render(casing, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private void renderTaczShellModel(ShellCasingEntity casing, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        TimelessAPI.getClientAmmoIndex(casing.getAmmoId()).ifPresentOrElse(ammoIndex -> {
            BedrockAmmoModel model = ammoIndex.getShellModel();
            ResourceLocation texture = ammoIndex.getShellTextureLocation();
            if (model == null || texture == null) {
                renderFallbackItem(casing, poseStack, buffer, packedLight);
                return;
            }
            poseStack.pushPose();
            poseStack.translate(0.0D, -1.5D, 0.0D);
            model.render(poseStack, ItemDisplayContext.GROUND, RenderType.entityCutout(texture), packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }, () -> renderFallbackItem(casing, poseStack, buffer, packedLight));
    }

    private void renderFallbackItem(ShellCasingEntity casing, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float scale = casing.getCasingType() == CasingType.SHOTGUN ? 0.62F : 0.48F;
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        this.itemRenderer.renderStatic(casing.getRenderStack(), ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, casing.level(), casing.getId());
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(ShellCasingEntity casing) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
