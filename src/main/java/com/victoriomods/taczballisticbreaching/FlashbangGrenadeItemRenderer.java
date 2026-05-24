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
public class FlashbangGrenadeItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static FlashbangGrenadeItemRenderer instance;

    private FlashbangGrenadeItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static FlashbangGrenadeItemRenderer instance() {
        if (instance == null) {
            instance = new FlashbangGrenadeItemRenderer();
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.5D, 0.5D, 0.5D);
            poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(34.0F));
        } else if (context.firstPerson()) {
            poseStack.translate(0.55D, 0.35D, 0.28D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-26.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(7.0F));
        } else {
            poseStack.translate(0.5D, 0.44D, 0.5D);
            poseStack.mulPose(Axis.YP.rotationDegrees(24.0F));
        }
        FlashbangGrenadeModelRenderer.render(poseStack, buffer, packedLight, context, 0.0F);
        poseStack.popPose();
    }
}
