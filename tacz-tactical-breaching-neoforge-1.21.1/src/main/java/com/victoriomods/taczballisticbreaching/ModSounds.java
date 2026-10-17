package com.victoriomods.taczballisticbreaching;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, TaczTacticalBreaching.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> FRAG_EXPLOSION_CLOSE = register("frag_explosion_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> FRAG_EXPLOSION_FAR = register("frag_explosion_far");
    public static final DeferredHolder<SoundEvent, SoundEvent> FRAG_EXPLOSION_INDOOR = register("frag_explosion_indoor");
    public static final DeferredHolder<SoundEvent, SoundEvent> FRAG_EXPLOSION_DEBRIS = register("frag_explosion_debris");
    public static final DeferredHolder<SoundEvent, SoundEvent> FRAG_THROW = register("frag_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> FRAG_BOUNCE = register("frag_bounce");
    public static final DeferredHolder<SoundEvent, SoundEvent> FRAG_ROLL = register("frag_roll");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLASHBANG_DETONATE = register("flashbang_detonate");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLASHBANG_INDOOR = register("flashbang_indoor");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLASHBANG_RING = register("flashbang_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLASHBANG_THROW = register("flashbang_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLASHBANG_BOUNCE = register("flashbang_bounce");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLASHBANG_ROLL = register("flashbang_roll");
    public static final DeferredHolder<SoundEvent, SoundEvent> DISTANT_RIFLE_POP = register("distant_rifle_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> DISTANT_RIFLE_ECHO = register("distant_rifle_echo");
    public static final DeferredHolder<SoundEvent, SoundEvent> DISTANT_PISTOL_POP = register("distant_pistol_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> DISTANT_SNIPER_ECHO = register("distant_sniper_echo");
    public static final DeferredHolder<SoundEvent, SoundEvent> DISTANT_SHOTGUN_ECHO = register("distant_shotgun_echo");
    public static final DeferredHolder<SoundEvent, SoundEvent> DISTANT_MACHINEGUN_FAR = register("distant_machinegun_far");
    public static final DeferredHolder<SoundEvent, SoundEvent> DISTANT_EXTREME_ECHO = register("distant_extreme_echo");
    public static final DeferredHolder<SoundEvent, SoundEvent> DISTANT_FAR_ECHO = register("distant_far_echo");
    public static final DeferredHolder<SoundEvent, SoundEvent> DISTANT_CLOSE_ECHO = register("distant_close_echo");
    public static final DeferredHolder<SoundEvent, SoundEvent> DISTANT_NEAR_ECHO = register("distant_near_echo");

    private ModSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TaczTacticalBreaching.MOD_ID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
