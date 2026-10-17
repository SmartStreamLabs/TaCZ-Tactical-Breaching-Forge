package com.victoriomods.taczballisticbreaching;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;

public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, TaczTacticalBreaching.MOD_ID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> GAS_MASK = ARMOR_MATERIALS.register("gas_mask", () ->
            new ArmorMaterial(
                    Map.of(
                            ArmorItem.Type.HELMET, 0,
                            ArmorItem.Type.CHESTPLATE, 0,
                            ArmorItem.Type.LEGGINGS, 0,
                            ArmorItem.Type.BOOTS, 0
                    ),
                    9,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.EMPTY,
                    java.util.List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(TaczTacticalBreaching.MOD_ID, "gas_mask"))),
                    0.0F,
                    0.0F
            )
    );

    private ModArmorMaterials() {
    }
}
