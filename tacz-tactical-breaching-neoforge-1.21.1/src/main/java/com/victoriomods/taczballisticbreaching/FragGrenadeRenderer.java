package com.victoriomods.taczballisticbreaching;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FragGrenadeRenderer extends EntityRenderer<FragGrenadeEntity> {
    private static final float WORLD_SCALE = 0.58F;

    public FragGrenadeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.1F;
    }

    @Override
    public void render(FragGrenadeEntity grenade, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.08D, 0.0D);
        poseStack.scale(WORLD_SCALE, WORLD_SCALE, WORLD_SCALE);
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, grenade.yRotO, grenade.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, grenade.xRotO, grenade.getXRot())));
        poseStack.mulPose(Axis.ZP.rotationDegrees(grenade.getRenderRoll(partialTick)));
        FragGrenadeModelRenderer.render(poseStack, buffer, packedLight, ItemDisplayContext.GROUND, grenade.getRenderRoll(partialTick), grenade.getClientFuseTicks());
        poseStack.popPose();
        super.render(grenade, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(FragGrenadeEntity entity) {
        return FragGrenadeModelRenderer.textureLocation();
    }
}
