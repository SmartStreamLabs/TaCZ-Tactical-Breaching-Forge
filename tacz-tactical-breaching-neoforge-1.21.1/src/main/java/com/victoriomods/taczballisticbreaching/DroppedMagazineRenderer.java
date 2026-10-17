package com.victoriomods.taczballisticbreaching;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DroppedMagazineRenderer extends EntityRenderer<DroppedMagazineEntity> {
    private final ItemRenderer itemRenderer;

    public DroppedMagazineRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.08F;
    }

    @Override
    public void render(DroppedMagazineEntity magazine, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.07D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, magazine.yRotO, magazine.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, magazine.xRotO, magazine.getXRot())));
        poseStack.mulPose(Axis.ZP.rotationDegrees(magazine.getRenderRoll(partialTick)));
        poseStack.scale(0.55F, 0.55F, 0.55F);
        this.itemRenderer.renderStatic(magazine.getRenderStack(), ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, magazine.level(), magazine.getId());
        poseStack.popPose();
        super.render(magazine, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(DroppedMagazineEntity magazine) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
