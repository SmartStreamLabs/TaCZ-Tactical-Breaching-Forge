package com.victoriomods.taczballisticbreaching;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(TaczTacticalBreaching.MOD_ID)
public class TaczTacticalBreaching {
    public static final String MOD_ID = "tacz_tactical_breaching";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TaczTacticalBreaching(IEventBus modBus, ModContainer container) {
        ModArmorMaterials.ARMOR_MATERIALS.register(modBus);
        ModEntities.ENTITY_TYPES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModMenus.MENU_TYPES.register(modBus);
        ModSounds.SOUND_EVENTS.register(modBus);
        modBus.addListener(ModNetwork::register);

        container.registerConfig(ModConfig.Type.COMMON, BallisticBlockConfig.SPEC);
        SuperbWarfareCompat.register(ModList.get().isLoaded("tacz"));
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientConfigScreenRegistration.register(container);
        }
        NeoForge.EVENT_BUS.register(HandcuffEventHandler.class);
        NeoForge.EVENT_BUS.register(GasMaskHandler.class);
        NeoForge.EVENT_BUS.register(ConfigCommandHandler.class);
        if (!ModList.get().isLoaded("tacz")) {
            LOGGER.warn("TaCZ is not loaded; TaCZ-specific breaching hooks will stay disabled.");
            return;
        }
        NeoForge.EVENT_BUS.register(BallisticBlockHitHandler.class);
        NeoForge.EVENT_BUS.register(ShellCasingSpawner.class);
        NeoForge.EVENT_BUS.register(DroppedMagazineSpawner.class);
        NeoForge.EVENT_BUS.register(MagazineReloadHandler.class);
        NeoForge.EVENT_BUS.register(ArmorHitEffectHandler.class);
        NeoForge.EVENT_BUS.register(BloodHitEffectHandler.class);
        NeoForge.EVENT_BUS.register(NightstickFlashlightLightHandler.class);
        NeoForge.EVENT_BUS.register(DistantGunshotHandler.class);
    }
}
