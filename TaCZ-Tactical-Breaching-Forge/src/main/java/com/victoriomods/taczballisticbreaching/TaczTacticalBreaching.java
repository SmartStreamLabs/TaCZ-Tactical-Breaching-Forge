package com.victoriomods.taczballisticbreaching;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(TaczTacticalBreaching.MOD_ID)
public class TaczTacticalBreaching {
    public static final String MOD_ID = "tacz_tactical_breaching";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TaczTacticalBreaching() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITY_TYPES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModMenus.MENU_TYPES.register(modBus);
        ModSounds.SOUND_EVENTS.register(modBus);
        ModNetwork.register();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BallisticBlockConfig.SPEC);
        SuperbWarfareCompat.register();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientConfigScreenRegistration::register);
        MinecraftForge.EVENT_BUS.register(BallisticBlockHitHandler.class);
        MinecraftForge.EVENT_BUS.register(ShellCasingSpawner.class);
        MinecraftForge.EVENT_BUS.register(DroppedMagazineSpawner.class);
        MinecraftForge.EVENT_BUS.register(MagazineReloadHandler.class);
        MinecraftForge.EVENT_BUS.register(WeaponJamHandler.class);
        MinecraftForge.EVENT_BUS.register(GasMaskHandler.class);
        MinecraftForge.EVENT_BUS.register(ArmorHitEffectHandler.class);
        MinecraftForge.EVENT_BUS.register(ArmorPlateHandler.class);
        MinecraftForge.EVENT_BUS.register(BloodHitEffectHandler.class);
        MinecraftForge.EVENT_BUS.register(NightstickFlashlightLightHandler.class);
        MinecraftForge.EVENT_BUS.register(HandcuffEventHandler.class);
        MinecraftForge.EVENT_BUS.register(DistantGunshotHandler.class);
        MinecraftForge.EVENT_BUS.register(ConfigCommandHandler.class);
    }
}
