package com.victoriomods.taczballisticbreaching;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TaczTacticalBreaching.MOD_ID);

    public static final RegistryObject<EntityType<ShellCasingEntity>> SHELL_CASING = ENTITY_TYPES.register("shell_casing", () ->
            EntityType.Builder.<ShellCasingEntity>of(ShellCasingEntity::new, MobCategory.MISC)
                    .noSave()
                    .noSummon()
                    .sized(0.12F, 0.08F)
                    .clientTrackingRange(32)
                    .updateInterval(10)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("shell_casing")
    );

    public static final RegistryObject<EntityType<DroppedMagazineEntity>> DROPPED_MAGAZINE = ENTITY_TYPES.register("dropped_magazine", () ->
            EntityType.Builder.<DroppedMagazineEntity>of(DroppedMagazineEntity::new, MobCategory.MISC)
                    .noSummon()
                    .sized(0.24F, 0.12F)
                    .clientTrackingRange(48)
                    .updateInterval(4)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("dropped_magazine")
    );

    public static final RegistryObject<EntityType<FragGrenadeEntity>> FRAG_GRENADE = ENTITY_TYPES.register("frag_grenade", () ->
            EntityType.Builder.<FragGrenadeEntity>of(FragGrenadeEntity::new, MobCategory.MISC)
                    .sized(0.18F, 0.18F)
                    .clientTrackingRange(96)
                    .updateInterval(2)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("frag_grenade")
    );

    public static final RegistryObject<EntityType<FlashbangGrenadeEntity>> FLASHBANG_GRENADE = ENTITY_TYPES.register("flashbang_grenade", () ->
            EntityType.Builder.<FlashbangGrenadeEntity>of(FlashbangGrenadeEntity::new, MobCategory.MISC)
                    .sized(0.22F, 0.34F)
                    .clientTrackingRange(96)
                    .updateInterval(2)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("flashbang_grenade")
    );

    private ModEntities() {
    }
}
