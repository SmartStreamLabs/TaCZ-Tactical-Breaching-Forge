package com.victoriomods.taczballisticbreaching;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TaczTacticalBreaching.MOD_ID);

    public static final RegistryObject<SoundEvent> FRAG_EXPLOSION_CLOSE = register("frag_explosion_close");
    public static final RegistryObject<SoundEvent> FRAG_EXPLOSION_FAR = register("frag_explosion_far");
    public static final RegistryObject<SoundEvent> FRAG_EXPLOSION_INDOOR = register("frag_explosion_indoor");
    public static final RegistryObject<SoundEvent> FRAG_EXPLOSION_DEBRIS = register("frag_explosion_debris");
    public static final RegistryObject<SoundEvent> FRAG_THROW = register("frag_throw");
    public static final RegistryObject<SoundEvent> FRAG_BOUNCE = register("frag_bounce");
    public static final RegistryObject<SoundEvent> FRAG_ROLL = register("frag_roll");
    public static final RegistryObject<SoundEvent> FLASHBANG_DETONATE = register("flashbang_detonate");
    public static final RegistryObject<SoundEvent> FLASHBANG_INDOOR = register("flashbang_indoor");
    public static final RegistryObject<SoundEvent> FLASHBANG_RING = register("flashbang_ring");
    public static final RegistryObject<SoundEvent> FLASHBANG_THROW = register("flashbang_throw");
    public static final RegistryObject<SoundEvent> FLASHBANG_BOUNCE = register("flashbang_bounce");
    public static final RegistryObject<SoundEvent> FLASHBANG_ROLL = register("flashbang_roll");

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = new ResourceLocation(TaczTacticalBreaching.MOD_ID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
