package com.victoriomods.taczballisticbreaching;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(BuiltInRegistries.MENU, TaczTacticalBreaching.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<MagazineLoaderMenu>> MAGAZINE_LOADER = MENU_TYPES.register("magazine_loader", () ->
            new MenuType<>((IContainerFactory<MagazineLoaderMenu>) MagazineLoaderMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );

    private ModMenus() {
    }
}
