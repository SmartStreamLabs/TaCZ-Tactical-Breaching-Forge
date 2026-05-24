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

    private ModItems() {
    }
}
