package com.victoriomods.taczballisticbreaching;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FragGrenadeItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static FragGrenadeItemRenderer instance;

    private FragGrenadeItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static FragGrenadeItemRenderer instance() {
        if (instance == null) {
            instance = new FragGrenadeItemRenderer();
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.5D, 0.5D, 0.5D);
            poseStack.mulPose(Axis.XP.rotationDegrees(-22.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(38.0F));
        } else if (context.firstPerson()) {
            poseStack.translate(0.55D, 0.36D, 0.28D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-28.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(8.0F));
        } else {
            poseStack.translate(0.5D, 0.42D, 0.5D);
            poseStack.mulPose(Axis.YP.rotationDegrees(25.0F));
        }
        FragGrenadeModelRenderer.render(poseStack, buffer, packedLight, context, 0.0F);
        poseStack.popPose();
    }
}
