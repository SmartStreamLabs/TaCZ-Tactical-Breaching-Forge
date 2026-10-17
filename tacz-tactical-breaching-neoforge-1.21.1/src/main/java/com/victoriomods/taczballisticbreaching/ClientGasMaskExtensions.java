package com.victoriomods.taczballisticbreaching;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

@OnlyIn(Dist.CLIENT)
final class ClientGasMaskExtensions {
    private static final HumanoidModel<LivingEntity> GAS_MASK_MODEL = new HumanoidModel<>(createLayer().bakeRoot());
    private static boolean loggedRenderer;

    private ClientGasMaskExtensions() {
    }

    static IClientItemExtensions create() {
        TaczTacticalBreaching.LOGGER.debug("Gas mask client extension registered with custom head armor model.");
        return new IClientItemExtensions() {
            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (equipmentSlot != EquipmentSlot.HEAD) {
                    return original;
                }
                HumanoidModel model = GAS_MASK_MODEL;
                original.copyPropertiesTo(model);
                GAS_MASK_MODEL.setAllVisible(false);
                GAS_MASK_MODEL.head.visible = true;
                GAS_MASK_MODEL.hat.visible = false;
                GAS_MASK_MODEL.head.copyFrom(original.head);
                if (!loggedRenderer) {
                    loggedRenderer = true;
                    TaczTacticalBreaching.LOGGER.debug("Rendering gas mask with custom wearable head model on {} using armor texture layer tacz_tactical_breaching:gas_mask.", livingEntity.getType().builtInRegistryHolder().key().location());
                }
                return GAS_MASK_MODEL;
            }
        };
    }

    private static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.0F), 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("mask_shell", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.5F, -8.25F, -5.15F, 9.0F, 8.2F, 1.9F, new CubeDeformation(0.05F)), PartPose.ZERO);
        head.addOrReplaceChild("left_lens", CubeListBuilder.create()
                .texOffs(0, 12)
                .addBox(-3.8F, -6.85F, -5.55F, 3.1F, 2.4F, 0.45F, new CubeDeformation(0.02F)), PartPose.ZERO);
        head.addOrReplaceChild("right_lens", CubeListBuilder.create()
                .texOffs(0, 12)
                .addBox(0.7F, -6.85F, -5.55F, 3.1F, 2.4F, 0.45F, new CubeDeformation(0.02F)), PartPose.ZERO);
        head.addOrReplaceChild("center_filter", CubeListBuilder.create()
                .texOffs(20, 12)
                .addBox(-1.45F, -4.25F, -5.85F, 2.9F, 2.65F, 1.0F, new CubeDeformation(0.02F)), PartPose.ZERO);
        head.addOrReplaceChild("left_filter", CubeListBuilder.create()
                .texOffs(30, 12)
                .addBox(-4.95F, -4.35F, -4.85F, 1.3F, 2.1F, 1.3F, new CubeDeformation(0.02F)), PartPose.ZERO);
        head.addOrReplaceChild("right_filter", CubeListBuilder.create()
                .texOffs(30, 12)
                .addBox(3.65F, -4.35F, -4.85F, 1.3F, 2.1F, 1.3F, new CubeDeformation(0.02F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
