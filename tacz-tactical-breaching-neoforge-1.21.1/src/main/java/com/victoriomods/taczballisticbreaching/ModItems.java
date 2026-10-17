package com.victoriomods.taczballisticbreaching;

import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TaczTacticalBreaching.MOD_ID);

    public static final DeferredItem<Item> FRAG_GRENADE = ITEMS.register("frag_grenade", () ->
            new FragGrenadeItem(new Item.Properties().stacksTo(16))
    );

    public static final DeferredItem<Item> FLASHBANG_GRENADE = ITEMS.register("flashbang_grenade", () ->
            new FlashbangGrenadeItem(new Item.Properties().stacksTo(16))
    );

    public static final DeferredItem<Item> DROPPED_MAGAZINE = ITEMS.register("dropped_magazine", () ->
            new DroppedMagazineItem(new Item.Properties().stacksTo(1))
    );

    public static final DeferredItem<Item> HANDCUFFS = ITEMS.register("handcuffs", () ->
            new Item(new Item.Properties().stacksTo(16))
    );

    public static final DeferredItem<Item> HANDCUFF_KEY = ITEMS.register("handcuff_key", () ->
            new Item(new Item.Properties().stacksTo(16))
    );

    public static final DeferredItem<Item> GAS_MASK = ITEMS.register("gas_mask", () ->
            new GasMaskItem(new Item.Properties())
    );

    public static final DeferredItem<Item> BASIC_FILTER = ITEMS.register("basic_filter", () ->
            new GasMaskFilterItem(GasMaskFilterType.BASIC, new Item.Properties())
    );

    public static final DeferredItem<Item> MILITARY_FILTER = ITEMS.register("military_filter", () ->
            new GasMaskFilterItem(GasMaskFilterType.MILITARY, new Item.Properties())
    );

    public static final DeferredItem<Item> ADVANCED_NBC_FILTER = ITEMS.register("advanced_nbc_filter", () ->
            new GasMaskFilterItem(GasMaskFilterType.ADVANCED_NBC, new Item.Properties())
    );

    private ModItems() {
    }
}
