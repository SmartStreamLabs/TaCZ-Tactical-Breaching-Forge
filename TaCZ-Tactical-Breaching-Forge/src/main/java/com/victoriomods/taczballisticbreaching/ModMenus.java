package com.victoriomods.taczballisticbreaching;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, TaczTacticalBreaching.MOD_ID);

    public static final RegistryObject<MenuType<MagazineLoaderMenu>> MAGAZINE_LOADER = MENU_TYPES.register("magazine_loader", () ->
            IForgeMenuType.create(MagazineLoaderMenu::new)
    );

    private ModMenus() {
    }
}
