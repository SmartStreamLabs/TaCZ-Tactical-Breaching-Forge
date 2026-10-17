package com.victoriomods.taczballisticbreaching;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TaczTacticalBreaching.MOD_ID);

    public static final RegistryObject<Item> FRAG_GRENADE = ITEMS.register("frag_grenade", () ->
            new FragGrenadeItem(new Item.Properties().stacksTo(16))
    );

    public static final RegistryObject<Item> FLASHBANG_GRENADE = ITEMS.register("flashbang_grenade", () ->
            new FlashbangGrenadeItem(new Item.Properties().stacksTo(16))
    );

    public static final RegistryObject<Item> DROPPED_MAGAZINE = ITEMS.register("dropped_magazine", () ->
            new DroppedMagazineItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> HANDCUFFS = ITEMS.register("handcuffs", () ->
            new Item(new Item.Properties().stacksTo(16))
    );

    public static final RegistryObject<Item> HANDCUFF_KEY = ITEMS.register("handcuff_key", () ->
            new Item(new Item.Properties().stacksTo(16))
    );

    public static final RegistryObject<Item> SOFT_ARMOR_PLATE = ITEMS.register("soft_armor_plate", () ->
            new ArmorPlateItem(ArmorPlateType.SOFT, new Item.Properties())
    );

    public static final RegistryObject<Item> CERAMIC_PLATE = ITEMS.register("ceramic_plate", () ->
            new ArmorPlateItem(ArmorPlateType.CERAMIC, new Item.Properties())
    );

    public static final RegistryObject<Item> HEAVY_PLATE = ITEMS.register("heavy_plate", () ->
            new ArmorPlateItem(ArmorPlateType.HEAVY, new Item.Properties())
    );

    public static final RegistryObject<Item> MILITARY_GRADE_PLATE = ITEMS.register("military_grade_plate", () ->
            new ArmorPlateItem(ArmorPlateType.MILITARY, new Item.Properties())
    );

    public static final RegistryObject<Item> GAS_MASK = ITEMS.register("gas_mask", () ->
            new GasMaskItem(new Item.Properties())
    );

    public static final RegistryObject<Item> BASIC_FILTER = ITEMS.register("basic_filter", () ->
            new GasMaskFilterItem(GasMaskFilterType.BASIC, new Item.Properties())
    );

    public static final RegistryObject<Item> MILITARY_FILTER = ITEMS.register("military_filter", () ->
            new GasMaskFilterItem(GasMaskFilterType.MILITARY, new Item.Properties())
    );

    public static final RegistryObject<Item> ADVANCED_NBC_FILTER = ITEMS.register("advanced_nbc_filter", () ->
            new GasMaskFilterItem(GasMaskFilterType.ADVANCED_NBC, new Item.Properties())
    );

    private ModItems() {
    }
}
