package com.victoriomods.taczballisticbreaching;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class BallisticBlockConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue WOODEN_DOOR_HP;
    public static final ModConfigSpec.DoubleValue WOODEN_TRAPDOOR_HP;
    public static final ModConfigSpec.DoubleValue IRON_DOOR_HP;
    public static final ModConfigSpec.DoubleValue IRON_TRAPDOOR_HP;
    public static final ModConfigSpec.DoubleValue GLASS_BLOCK_HP;
    public static final ModConfigSpec.DoubleValue GLASS_PANE_HP;
    public static final ModConfigSpec.DoubleValue TINTED_GLASS_HP;
    public static final ModConfigSpec.DoubleValue GLASS_DOOR_HP;
    public static final ModConfigSpec.DoubleValue OTHER_SUPPORTED_BLOCK_HP;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCK_HP_OVERRIDES;

    public static final ModConfigSpec.DoubleValue DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> GUN_DAMAGE_MULTIPLIERS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> AMMO_DAMAGE_MULTIPLIERS;
    public static final ModConfigSpec.DoubleValue SHOTGUN_CLOSE_RANGE;
    public static final ModConfigSpec.DoubleValue SHOTGUN_CLOSE_RANGE_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue GLASS_PASSTHROUGH_ENABLED;
    public static final ModConfigSpec.DoubleValue GLASS_PASSTHROUGH_DAMAGE_MULTIPLIER;

    public static final ModConfigSpec.BooleanValue USE_WHITELIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCK_WHITELIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCK_BLACKLIST;
    public static final ModConfigSpec.BooleanValue DROP_ITEMS;
    public static final ModConfigSpec.IntValue DAMAGE_TIMEOUT_TICKS;
    public static final ModConfigSpec.BooleanValue SHOW_CRACK_PROGRESS;
    public static final ModConfigSpec.BooleanValue SHOW_IMPACT_PARTICLES;
    public static final ModConfigSpec.BooleanValue PLAY_IMPACT_SOUND;
    public static final ModConfigSpec.BooleanValue ENABLE_SHELL_CASINGS;
    public static final ModConfigSpec.IntValue CASING_LIFETIME;
    public static final ModConfigSpec.BooleanValue PERSISTENT_SETTLED_CASINGS;
    public static final ModConfigSpec.IntValue MAX_NEARBY_CASINGS;
    public static final ModConfigSpec.BooleanValue ENABLE_CASING_SOUNDS;
    public static final ModConfigSpec.BooleanValue DROP_MAGAZINES;
    public static final ModConfigSpec.BooleanValue ALLOW_DROPPED_MAGAZINE_PICKUP;
    public static final ModConfigSpec.BooleanValue PRESERVE_DROPPED_MAGAZINE_AMMO;
    public static final ModConfigSpec.BooleanValue ENABLE_TACTICAL_RELOAD;
    public static final ModConfigSpec.IntValue DROPPED_MAGAZINE_DESPAWN_SECONDS;
    public static final ModConfigSpec.BooleanValue FORCE_MAGAZINE_RELOADING;
    public static final ModConfigSpec.BooleanValue ALLOW_LOOSE_AMMO_RELOADING;
    public static final ModConfigSpec.BooleanValue ALLOW_MAGAZINE_RELOADING;
    public static final ModConfigSpec.BooleanValue ALLOW_SHOOT_WHILE_SPRINTING;
    public static final ModConfigSpec.BooleanValue ALLOW_RELOAD_WHILE_SPRINTING;
    public static final ModConfigSpec.BooleanValue ALLOW_INSTANT_RELOAD_AFTER_SHOT;
    public static final ModConfigSpec.BooleanValue SPRINTING_ACCURACY_PENALTY;
    public static final ModConfigSpec.DoubleValue SPRINTING_ACCURACY_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SPRINTING_RECOIL_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SPRINTING_RELOAD_SPEED_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue ENABLE_IMPACT_SMOKE;
    public static final ModConfigSpec.IntValue IMPACT_SMOKE_AMOUNT;
    public static final ModConfigSpec.DoubleValue IMPACT_SMOKE_SIZE;
    public static final ModConfigSpec.IntValue IMPACT_SMOKE_LIFETIME_TICKS;
    public static final ModConfigSpec.BooleanValue IMPACT_SMOKE_ONLY_ON_HARD_BLOCKS;
    public static final ModConfigSpec.IntValue MAX_IMPACT_PARTICLES_PER_SECOND;
    public static final ModConfigSpec.BooleanValue ENABLE_METAL_SPARKS;
    public static final ModConfigSpec.IntValue METAL_SPARK_AMOUNT;
    public static final ModConfigSpec.IntValue METAL_SPARK_LIFETIME_TICKS;
    public static final ModConfigSpec.BooleanValue ENABLE_METAL_RICOCHET_SOUND;
    public static final ModConfigSpec.BooleanValue ENABLE_WOOD_SPLINTERS;
    public static final ModConfigSpec.IntValue WOOD_SPLINTER_AMOUNT;
    public static final ModConfigSpec.IntValue WOOD_SPLINTER_LIFETIME_TICKS;
    public static final ModConfigSpec.BooleanValue ENABLE_WOOD_IMPACT_SOUND;
    public static final ModConfigSpec.BooleanValue ENABLE_BULLET_DECALS;
    public static final ModConfigSpec.BooleanValue IMPACT_DECALS_ENABLED;
    public static final ModConfigSpec.IntValue MAX_DECALS_PER_CHUNK;
    public static final ModConfigSpec.IntValue MAX_IMPACT_DECALS_PER_CHUNK;
    public static final ModConfigSpec.IntValue MAX_IMPACT_DECALS;
    public static final ModConfigSpec.IntValue BULLET_DECAL_LIFETIME_SECONDS;
    public static final ModConfigSpec.IntValue BULLET_HOLE_LIFETIME_TICKS;
    public static final ModConfigSpec.DoubleValue BULLET_DECAL_SIZE;
    public static final ModConfigSpec.BooleanValue BULLET_HOLE_RANDOM_ROTATION;
    public static final ModConfigSpec.BooleanValue BULLET_HOLE_RANDOM_SCALE;
    public static final ModConfigSpec.BooleanValue ENABLE_MATERIAL_SPECIFIC_DECALS;
    public static final ModConfigSpec.BooleanValue MATERIAL_SPECIFIC_IMPACTS;
    public static final ModConfigSpec.BooleanValue ENABLE_RICOCHET_SOUNDS;
    public static final ModConfigSpec.DoubleValue RICOCHET_SOUND_CHANCE_METAL;
    public static final ModConfigSpec.DoubleValue RICOCHET_SOUND_CHANCE_HARD_BLOCK;
    public static final ModConfigSpec.DoubleValue RICOCHET_VOLUME;
    public static final ModConfigSpec.IntValue RICOCHET_MAX_SOUNDS_PER_SECOND;
    public static final ModConfigSpec.DoubleValue RICOCHET_MAX_DISTANCE;
    public static final ModConfigSpec.BooleanValue ENABLE_MUZZLE_SMOKE;
    public static final ModConfigSpec.IntValue MUZZLE_SMOKE_AMOUNT;
    public static final ModConfigSpec.IntValue MUZZLE_SMOKE_LIFETIME_TICKS;
    public static final ModConfigSpec.DoubleValue MUZZLE_SMOKE_SCALE;
    public static final ModConfigSpec.ConfigValue<String> SMOKE_QUALITY;
    public static final ModConfigSpec.DoubleValue SMOKE_OPACITY;
    public static final ModConfigSpec.DoubleValue SMOKE_EXPANSION;
    public static final ModConfigSpec.DoubleValue SMOKE_TURBULENCE;
    public static final ModConfigSpec.DoubleValue FIRST_PERSON_SMOKE_INTENSITY;
    public static final ModConfigSpec.IntValue MAX_ACTIVE_MUZZLE_SMOKE;
    public static final ModConfigSpec.BooleanValue DYNAMIC_SMOKE_BUILDUP;
    public static final ModConfigSpec.IntValue MAX_SMOKE_PARTICLES_PER_SECOND;
    public static final ModConfigSpec.BooleanValue ENABLE_INDOOR_SMOKE;
    public static final ModConfigSpec.DoubleValue INDOOR_SMOKE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SMOKE_DISSIPATION_SPEED_INDOOR;
    public static final ModConfigSpec.DoubleValue SMOKE_DISSIPATION_SPEED_OUTDOOR;
    public static final ModConfigSpec.DoubleValue MAX_INDOOR_SMOKE_DENSITY;
    public static final ModConfigSpec.BooleanValue ENABLE_FLASHLIGHT_SMOKE_INTERACTION;
    public static final ModConfigSpec.BooleanValue ENABLE_LASER_SMOKE_VISIBILITY;
    public static final ModConfigSpec.BooleanValue ENABLE_GLASS_SHARDS;
    public static final ModConfigSpec.IntValue GLASS_SHARD_AMOUNT;
    public static final ModConfigSpec.IntValue GLASS_SHARD_LIFETIME_SECONDS;
    public static final ModConfigSpec.BooleanValue GLASS_SHARD_FADE_OUT;
    public static final ModConfigSpec.IntValue MAX_GLASS_SHARDS_PER_CHUNK;
    public static final ModConfigSpec.BooleanValue ENABLE_SUPPRESSION_SOUNDS;
    public static final ModConfigSpec.DoubleValue SUPPRESSION_RADIUS;
    public static final ModConfigSpec.DoubleValue SUPPRESSION_VOLUME;
    public static final ModConfigSpec.IntValue SUPPRESSION_MAX_SOUNDS_PER_SECOND;
    public static final ModConfigSpec.BooleanValue ENABLE_SHOOTER_SUPPRESSION;
    public static final ModConfigSpec.BooleanValue ENABLE_DYNAMIC_GUN_LIGHTING;
    public static final ModConfigSpec.BooleanValue ENABLE_SPARK_LIGHTING;
    public static final ModConfigSpec.DoubleValue MUZZLE_FLASH_LIGHT_INTENSITY;
    public static final ModConfigSpec.DoubleValue SPARK_LIGHT_INTENSITY;
    public static final ModConfigSpec.IntValue DYNAMIC_LIGHT_RANGE;
    public static final ModConfigSpec.BooleanValue REDUCED_LIGHTING_FOR_SUPPRESSORS;
    public static final ModConfigSpec.BooleanValue ENABLE_SCREEN_DUST;
    public static final ModConfigSpec.DoubleValue SCREEN_DUST_RADIUS;
    public static final ModConfigSpec.DoubleValue SCREEN_DUST_INTENSITY;
    public static final ModConfigSpec.IntValue SCREEN_DUST_DURATION_TICKS;
    public static final ModConfigSpec.IntValue SCREEN_DUST_MAX_TRIGGERS_PER_SECOND;
    public static final ModConfigSpec.DoubleValue SCREEN_DUST_INDOOR_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue ENABLE_WEAPON_HEAT;
    public static final ModConfigSpec.BooleanValue ENABLE_BARREL_SMOKE;
    public static final ModConfigSpec.BooleanValue ENABLE_HEAT_DISTORTION;
    public static final ModConfigSpec.BooleanValue ENABLE_HOT_BARREL_GLOW;
    public static final ModConfigSpec.DoubleValue WEAPON_COOLING_SPEED;
    public static final ModConfigSpec.IntValue MAX_HEAT_LEVEL;
    public static final ModConfigSpec.DoubleValue HEAT_DISTORTION_STRENGTH;
    public static final ModConfigSpec.DoubleValue BARREL_GLOW_INTENSITY;
    public static final ModConfigSpec.BooleanValue ENABLE_MATERIAL_PENETRATION;
    public static final ModConfigSpec.DoubleValue MAX_PENETRATION_DEPTH;
    public static final ModConfigSpec.DoubleValue WOOD_PENETRATION_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue STONE_PENETRATION_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue METAL_PENETRATION_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue REINFORCED_PENETRATION_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue DAMAGE_LOSS_PER_BLOCK;
    public static final ModConfigSpec.DoubleValue VELOCITY_LOSS_PER_BLOCK;
    public static final ModConfigSpec.BooleanValue ENABLE_EXIT_DECALS;
    public static final ModConfigSpec.BooleanValue ENABLE_PENETRATION_PARTICLES;
    public static final ModConfigSpec.BooleanValue ENABLE_CAMERA_SHAKE;
    public static final ModConfigSpec.DoubleValue CAMERA_SHAKE_INTENSITY;
    public static final ModConfigSpec.IntValue CAMERA_SHAKE_MAX_DISTANCE;
    public static final ModConfigSpec.IntValue CAMERA_SHAKE_DURATION_TICKS;
    public static final ModConfigSpec.BooleanValue ENABLE_EXPLOSION_SHAKE;
    public static final ModConfigSpec.BooleanValue ENABLE_SHOTGUN_SHAKE;
    public static final ModConfigSpec.BooleanValue ENABLE_SUPPRESSION_SHAKE;
    public static final ModConfigSpec.BooleanValue ENABLE_BREACHING_SHAKE;
    public static final ModConfigSpec.BooleanValue ENABLE_ARMOR_HIT_EFFECTS;
    public static final ModConfigSpec.BooleanValue ENABLE_ARMOR_RICOCHET;
    public static final ModConfigSpec.BooleanValue ENABLE_ARMOR_SPARKS;
    public static final ModConfigSpec.DoubleValue ARMOR_RICOCHET_CHANCE;
    public static final ModConfigSpec.IntValue ARMOR_SPARK_AMOUNT;
    public static final ModConfigSpec.DoubleValue ARMOR_HIT_VOLUME;
    public static final ModConfigSpec.BooleanValue ENABLE_BLOOD_MIST;
    public static final ModConfigSpec.BooleanValue BLOOD_EFFECTS_ENABLED;
    public static final ModConfigSpec.BooleanValue BLOOD_PHYSICS_ENABLED;
    public static final ModConfigSpec.BooleanValue ENABLE_WALL_SPLATTER;
    public static final ModConfigSpec.DoubleValue BLOOD_MIST_INTENSITY;
    public static final ModConfigSpec.DoubleValue BLOOD_DENSITY;
    public static final ModConfigSpec.IntValue BLOOD_LIFETIME_TICKS;
    public static final ModConfigSpec.DoubleValue BLOOD_SPLATTER_CHANCE;
    public static final ModConfigSpec.IntValue BLOOD_DECAL_LIFETIME_SECONDS;
    public static final ModConfigSpec.IntValue MAX_BLOOD_DECALS_PER_CHUNK;
    public static final ModConfigSpec.IntValue MAX_BLOOD_PARTICLES_PER_HIT;
    public static final ModConfigSpec.IntValue MAX_BLOOD_PARTICLES_PER_IMPACT;
    public static final ModConfigSpec.BooleanValue BLOOD_TRAILS_ENABLED;
    public static final ModConfigSpec.BooleanValue SODIUM_SAFE_RENDERING;
    public static final ModConfigSpec.BooleanValue ENABLE_DYNAMIC_IMPACT_SOUNDS;
    public static final ModConfigSpec.BooleanValue ENABLE_INDOOR_REVERB;
    public static final ModConfigSpec.BooleanValue ENABLE_DISTANCE_MUFFLING;
    public static final ModConfigSpec.IntValue IMPACT_SOUND_MAX_DISTANCE;
    public static final ModConfigSpec.DoubleValue IMPACT_SOUND_VOLUME;
    public static final ModConfigSpec.BooleanValue DYNAMIC_IMPACT_VARIATION;
    public static final ModConfigSpec.BooleanValue ENABLE_BULLET_CRACK_ECHO;
    public static final ModConfigSpec.BooleanValue ENABLE_OUTDOOR_ECHO;
    public static final ModConfigSpec.DoubleValue URBAN_ECHO_INTENSITY;
    public static final ModConfigSpec.DoubleValue INDOOR_REVERB_INTENSITY;
    public static final ModConfigSpec.DoubleValue VALLEY_ECHO_INTENSITY;
    public static final ModConfigSpec.DoubleValue FOREST_ECHO_REDUCTION;
    public static final ModConfigSpec.IntValue MAX_ECHO_DISTANCE;
    public static final ModConfigSpec.BooleanValue ENABLE_SUPPRESSION_BLUR;
    public static final ModConfigSpec.BooleanValue ENABLE_SUPPRESSION_RINGING;
    public static final ModConfigSpec.BooleanValue ENABLE_STRESS_EFFECTS;
    public static final ModConfigSpec.DoubleValue SUPPRESSION_BLUR_STRENGTH;
    public static final ModConfigSpec.DoubleValue SUPPRESSION_EFFECT_RADIUS;
    public static final ModConfigSpec.IntValue EAR_RINGING_DURATION_TICKS;
    public static final ModConfigSpec.IntValue MAX_SUPPRESSION_EFFECTS_PER_SECOND;
    public static final ModConfigSpec.BooleanValue ENABLE_PHYSICAL_DEBRIS;
    public static final ModConfigSpec.IntValue DEBRIS_LIFETIME_SECONDS;
    public static final ModConfigSpec.IntValue MAX_DEBRIS_PER_CHUNK;
    public static final ModConfigSpec.IntValue WOOD_DEBRIS_AMOUNT;
    public static final ModConfigSpec.IntValue GLASS_DEBRIS_AMOUNT;
    public static final ModConfigSpec.IntValue METAL_DEBRIS_AMOUNT;
    public static final ModConfigSpec.IntValue CONCRETE_DEBRIS_AMOUNT;
    public static final ModConfigSpec.BooleanValue ENABLE_SHOCKWAVE_DUST;
    public static final ModConfigSpec.BooleanValue ENABLE_INDOOR_PRESSURE_EFFECT;
    public static final ModConfigSpec.BooleanValue ENABLE_DEBRIS_FALLOFF;
    public static final ModConfigSpec.DoubleValue SHOCKWAVE_DUST_INTENSITY;
    public static final ModConfigSpec.DoubleValue PRESSURE_EFFECT_STRENGTH;
    public static final ModConfigSpec.IntValue DUST_LIFETIME_TICKS;
    public static final ModConfigSpec.IntValue MAX_SHOCKWAVE_PARTICLES;
    public static final ModConfigSpec.BooleanValue ENABLE_BURN_MARKS;
    public static final ModConfigSpec.BooleanValue ENABLE_EXPLOSION_SCORCH;
    public static final ModConfigSpec.BooleanValue ENABLE_BREACH_SCORCH;
    public static final ModConfigSpec.BooleanValue ENABLE_HEAT_RESIDUE;
    public static final ModConfigSpec.IntValue BURN_MARK_LIFETIME_SECONDS;
    public static final ModConfigSpec.IntValue MAX_BURN_MARKS_PER_CHUNK;
    public static final ModConfigSpec.DoubleValue BURN_MARK_OPACITY;
    public static final ModConfigSpec.BooleanValue ENABLE_NIGHTSTICK_FLASHLIGHT;
    public static final ModConfigSpec.DoubleValue FLASHLIGHT_INTENSITY;
    public static final ModConfigSpec.IntValue FLASHLIGHT_RANGE;
    public static final ModConfigSpec.BooleanValue ENABLE_NIGHTSTICK_DYNAMIC_LIGHTING;
    public static final ModConfigSpec.BooleanValue ENABLE_FRAG_GRENADE;
    public static final ModConfigSpec.IntValue FRAG_GRENADE_COOLDOWN_TICKS;
    public static final ModConfigSpec.DoubleValue FRAG_THROW_POWER;
    public static final ModConfigSpec.IntValue FRAG_FUSE_TIME_TICKS;
    public static final ModConfigSpec.DoubleValue FRAG_EXPLOSION_RADIUS;
    public static final ModConfigSpec.DoubleValue FRAG_DAMAGE;
    public static final ModConfigSpec.IntValue FRAG_COUNT;
    public static final ModConfigSpec.BooleanValue ENABLE_FRAGMENT_LINE_OF_SIGHT;
    public static final ModConfigSpec.BooleanValue ENABLE_FRAG_BLOCK_DAMAGE;
    public static final ModConfigSpec.BooleanValue ENABLE_FRAG_SMOKE;
    public static final ModConfigSpec.BooleanValue ENABLE_FRAG_DEBRIS;
    public static final ModConfigSpec.BooleanValue ENABLE_FRAG_INDOOR_PRESSURE;
    public static final ModConfigSpec.BooleanValue ENABLE_FRAG_CEILING_DUST;
    public static final ModConfigSpec.BooleanValue ENABLE_FRAG_EAR_RINGING;
    public static final ModConfigSpec.IntValue FRAG_EXPLOSION_SOUND_DISTANCE;
    public static final ModConfigSpec.BooleanValue ENABLE_EXPLOSION_ECHO;
    public static final ModConfigSpec.BooleanValue ENABLE_INDOOR_EXPLOSION_REVERB;
    public static final ModConfigSpec.BooleanValue ENABLE_FLASHBANG;
    public static final ModConfigSpec.IntValue FLASHBANG_COOLDOWN_TICKS;
    public static final ModConfigSpec.DoubleValue FLASHBANG_THROW_POWER;
    public static final ModConfigSpec.IntValue FLASHBANG_FUSE_TICKS;
    public static final ModConfigSpec.DoubleValue FLASHBANG_RADIUS;
    public static final ModConfigSpec.IntValue FLASH_DURATION_TICKS;
    public static final ModConfigSpec.IntValue FLASHBANG_TINNITUS_DURATION_TICKS;
    public static final ModConfigSpec.DoubleValue FLASHBANG_BLUR_STRENGTH;
    public static final ModConfigSpec.BooleanValue ENABLE_FLASHBANG_LINE_OF_SIGHT;
    public static final ModConfigSpec.BooleanValue ENABLE_FLASHBANG_INDOOR_REVERB;
    public static final ModConfigSpec.BooleanValue ENABLE_GAS_MASKS;
    public static final ModConfigSpec.BooleanValue ENABLE_FILTER_DURABILITY;
    public static final ModConfigSpec.BooleanValue ENABLE_BREATHING_SOUNDS;
    public static final ModConfigSpec.BooleanValue ENABLE_FLASHBANG_PROTECTION;
    public static final ModConfigSpec.BooleanValue ENABLE_SMOKE_PROTECTION;
    public static final ModConfigSpec.BooleanValue GAS_MASK_OVERLAY_ENABLED;
    public static final ModConfigSpec.DoubleValue GAS_MASK_OVERLAY_OPACITY;
    public static final ModConfigSpec.DoubleValue GAS_MASK_FILTER_TEXT_SCALE;
    public static final ModConfigSpec.BooleanValue GAS_MASK_FILTER_TEXT_ENABLED;
    public static final ModConfigSpec.BooleanValue HANDCUFFS_ENABLED;
    public static final ModConfigSpec.BooleanValue ALLOW_HANDCUFF_PLAYERS;
    public static final ModConfigSpec.BooleanValue ALLOW_HANDCUFF_VILLAGERS;
    public static final ModConfigSpec.DoubleValue RESTRAINED_SPEED_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue BLOCK_INVENTORY_INTERACTION;
    public static final ModConfigSpec.BooleanValue BLOCK_CHEST_INTERACTION;
    public static final ModConfigSpec.BooleanValue BLOCK_DOOR_INTERACTION;
    public static final ModConfigSpec.BooleanValue REQUIRE_KEY_TO_UNLOCK;
    public static final ModConfigSpec.BooleanValue OPERATORS_CAN_FORCE_UNLOCK;
    public static final ModConfigSpec.BooleanValue ENABLE_DISTANT_GUNSHOTS;
    public static final ModConfigSpec.IntValue DISTANT_NORMAL_RANGE;
    public static final ModConfigSpec.IntValue MIN_DISTANT_GUNSHOT_DISTANCE;
    public static final ModConfigSpec.IntValue DISTANT_RANGE;
    public static final ModConfigSpec.IntValue DISTANT_FAR_RANGE;
    public static final ModConfigSpec.IntValue DISTANT_EXTREME_RANGE;
    public static final ModConfigSpec.IntValue MAX_DISTANT_GUNSHOT_DISTANCE;
    public static final ModConfigSpec.DoubleValue DISTANT_GUNSHOT_VOLUME;
    public static final ModConfigSpec.BooleanValue ENABLE_SOUND_DELAY;
    public static final ModConfigSpec.DoubleValue SUPPRESSOR_DISTANCE_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue ENABLE_BATTLE_DETECTION;
    public static final ModConfigSpec.IntValue BATTLE_THRESHOLD_SHOTS;
    public static final ModConfigSpec.IntValue BATTLE_WINDOW_SECONDS;
    public static final ModConfigSpec.DoubleValue DISTANT_VOLUME_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue FAR_VOLUME_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue EXTREME_VOLUME_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue ENABLE_RANDOM_ECHOES;
    public static final ModConfigSpec.IntValue MAX_DISTANT_GUNSHOT_SOUNDS_PER_SECOND;
    public static final ModConfigSpec.BooleanValue USE_VANILLA_SOUNDS_FALLBACK;
    public static final ModConfigSpec.BooleanValue DISTANT_GUNSHOT_DEBUG_LOGGING;
    public static final ModConfigSpec.BooleanValue SHOW_WEAPON_CONDITION_HUD;
    public static final ModConfigSpec.BooleanValue SUPERB_WARFARE_SUPPORT;
    public static final ModConfigSpec.BooleanValue SUPERB_WARFARE_DOORS;
    public static final ModConfigSpec.BooleanValue SUPERB_WARFARE_TRAPDOORS;
    public static final ModConfigSpec.BooleanValue SUPERB_WARFARE_GLASS;
    public static final ModConfigSpec.BooleanValue SUPERB_WARFARE_IMPACTS;
    public static final ModConfigSpec.BooleanValue SUPERB_WARFARE_EXPLOSIVES;
    public static final ModConfigSpec.DoubleValue SUPERB_WARFARE_DAMAGE_MULTIPLIER;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("hp");
        WOODEN_DOOR_HP = builder.comment("Default HP for wooden doors.").defineInRange("wooden_door_hp", 24.0D, 1.0D, 100000.0D);
        WOODEN_TRAPDOOR_HP = builder.comment("Default HP for wooden trapdoors.").defineInRange("wooden_trapdoor_hp", 16.0D, 1.0D, 100000.0D);
        IRON_DOOR_HP = builder.comment("Default HP for iron doors.").defineInRange("iron_door_hp", 140.0D, 1.0D, 100000.0D);
        IRON_TRAPDOOR_HP = builder.comment("Default HP for iron trapdoors.").defineInRange("iron_trapdoor_hp", 100.0D, 1.0D, 100000.0D);
        GLASS_BLOCK_HP = builder.comment("Default HP for full glass blocks and stained glass blocks.").defineInRange("glass_block_hp", 16.0D, 1.0D, 100000.0D);
        GLASS_PANE_HP = builder.comment("Default HP for glass panes and stained glass panes.").defineInRange("glass_pane_hp", 8.0D, 1.0D, 100000.0D);
        TINTED_GLASS_HP = builder.comment("Default HP for tinted glass.").defineInRange("tinted_glass_hp", 24.0D, 1.0D, 100000.0D);
        GLASS_DOOR_HP = builder.comment("Default HP for modded glass door-like blocks.").defineInRange("glass_door_hp", 18.0D, 1.0D, 100000.0D);
        OTHER_SUPPORTED_BLOCK_HP = builder.comment("Default HP for supported blocks that are not wood, iron, or glass.").defineInRange("other_supported_block_hp", 40.0D, 1.0D, 100000.0D);
        BLOCK_HP_OVERRIDES = builder
                .comment("Per-block or per-tag HP overrides. Format: 'namespace:block=hp' or '#namespace:tag=hp'. Earlier entries win.")
                .defineListAllowEmpty("block_hp_overrides", defaultHpOverrides(), BallisticBlockConfig::isString);
        builder.pop();

        builder.push("damage");
        DAMAGE_MULTIPLIER = builder.comment("Global multiplier applied after TaCZ bullet damage is calculated.").defineInRange("damage_multiplier", 1.0D, 0.0D, 1000.0D);
        GUN_DAMAGE_MULTIPLIERS = builder
                .comment("Optional per-TaCZ-gun multipliers. Format: 'namespace:gun_id=multiplier'.")
                .defineListAllowEmpty("gun_damage_multipliers", List.of(
                        "tacz:m107=1.45",
                        "tacz:m95=1.45",
                        "tacz:rpg7=4.0"
                ), BallisticBlockConfig::isString);
        AMMO_DAMAGE_MULTIPLIERS = builder
                .comment("Optional per-TaCZ-ammo/caliber multipliers. Format: 'namespace:ammo_id=multiplier'.")
                .defineListAllowEmpty("ammo_damage_multipliers", List.of(
                        "tacz:12g=1.15",
                        "tacz:50bmg=1.5"
                ), BallisticBlockConfig::isString);
        SHOTGUN_CLOSE_RANGE = builder.comment("Distance in blocks where shotguns receive extra block damage.").defineInRange("shotgun_close_range", 7.0D, 0.0D, 256.0D);
        SHOTGUN_CLOSE_RANGE_MULTIPLIER = builder.comment("Extra multiplier for shotgun hits inside shotgun_close_range.").defineInRange("shotgun_close_range_multiplier", 1.35D, 0.0D, 100.0D);
        GLASS_PASSTHROUGH_ENABLED = builder.comment("When true, bullets keep flying after hitting glass targets.").define("glass_passthrough_enabled", true);
        GLASS_PASSTHROUGH_DAMAGE_MULTIPLIER = builder.comment("Cumulative damage multiplier applied to a bullet each time it passes through glass.").defineInRange("glass_passthrough_damage_multiplier", 0.65D, 0.0D, 1.0D);
        builder.pop();

        builder.push("rules");
        USE_WHITELIST = builder.comment("When true, only blocks in block_whitelist can be damaged.").define("use_whitelist", false);
        BLOCK_WHITELIST = builder
                .comment("Blocks/tags that may be damaged when use_whitelist is true. Format: 'namespace:block' or '#namespace:tag'.")
                .defineListAllowEmpty("block_whitelist", List.of(), BallisticBlockConfig::isString);
        BLOCK_BLACKLIST = builder
                .comment("Blocks/tags that can never be damaged. Format: 'namespace:block' or '#namespace:tag'.")
                .defineListAllowEmpty("block_blacklist", List.of(), BallisticBlockConfig::isString);
        DROP_ITEMS = builder.comment("Whether blocks broken by bullets drop their normal item drops.").define("drop_items", true);
        DAMAGE_TIMEOUT_TICKS = builder.comment("Ticks after the last hit before stored damage is forgotten.").defineInRange("damage_timeout_ticks", 400, 20, 72000);
        SHOW_CRACK_PROGRESS = builder.comment("Show vanilla crack progress while a block has stored damage.").define("show_crack_progress", true);
        SHOW_IMPACT_PARTICLES = builder.comment("Spawn block particles when a supported block is hit.").define("show_impact_particles", true);
        PLAY_IMPACT_SOUND = builder.comment("Legacy setting. Vanilla block hit sounds are no longer played by this mod.").define("play_impact_sound", false);
        builder.pop();

        builder.push("shell_casings");
        ENABLE_SHELL_CASINGS = builder.comment("Spawn visual shell casings when TaCZ guns fire.").define("enableShellCasings", true);
        CASING_LIFETIME = builder.comment("How long shell casings remain in ticks. 20 ticks = 1 second.").defineInRange("casingLifetime", 160, 20, 2400);
        PERSISTENT_SETTLED_CASINGS = builder.comment("If true, casings that have landed and stopped moving stay on the ground until removed with a command or by unloading rules.").define("persistentSettledCasings", true);
        MAX_NEARBY_CASINGS = builder.comment("Maximum casings allowed near a shooter before new casings are skipped.").defineInRange("maxNearbyCasings", 80, 0, 1000);
        ENABLE_CASING_SOUNDS = builder.comment("Legacy setting. Vanilla casing impact sounds are no longer played by this mod.").define("enableCasingSounds", false);
        builder.pop();

        builder.push("dropped_magazines");
        DROP_MAGAZINES = builder.comment("Drop a physical magazine entity when a TaCZ or supported Superb Warfare magazine-fed weapon reloads normally.").define("dropMagazines", true);
        ALLOW_DROPPED_MAGAZINE_PICKUP = builder.comment("Allow players to pick dropped magazines back up.").define("allowPickup", true);
        PRESERVE_DROPPED_MAGAZINE_AMMO = builder.comment("Preserve remaining rounds in dropped magazines and picked-up magazine items.").define("preserveAmmo", true);
        ENABLE_TACTICAL_RELOAD = builder.comment("When true, Shift + Reload stores the old magazine in inventory instead of dropping it.").define("enableTacticalReload", true);
        DROPPED_MAGAZINE_DESPAWN_SECONDS = builder.comment("How long dropped magazines remain in the world, in seconds.").defineInRange("despawnTimeSeconds", 300, 5, 3600);
        FORCE_MAGAZINE_RELOADING = builder.comment("Makes weapons require loaded magazines instead of loose ammo when explicitly enabled. Disabled by default so normal TaCZ ammo boxes/loose ammo can reload weapons.").define("forceMagazineReloading", false);
        ALLOW_LOOSE_AMMO_RELOADING = builder.comment("Allows normal TaCZ ammo reloads without requiring the custom loaded magazine system.").define("allowLooseAmmoReloading", true);
        ALLOW_MAGAZINE_RELOADING = builder.comment("Allows the custom loaded magazine system.").define("allowMagazineReloading", true);
        builder.pop();

        builder.push("sprinting_weapons");
        ALLOW_SHOOT_WHILE_SPRINTING = builder.comment("Allow TaCZ firearms to fire while the shooter is sprinting. Ammo, fire-rate, reload, jam, and server validation still apply.").define("allowShootWhileSprinting", true);
        ALLOW_RELOAD_WHILE_SPRINTING = builder.comment("Allow TaCZ firearms to start and continue reloading while the player keeps sprinting.").define("allowReloadWhileSprinting", true);
        ALLOW_INSTANT_RELOAD_AFTER_SHOT = builder.comment("Allow a reload input immediately after a valid shot instead of waiting for TaCZ's short post-shot reload delay.").define("allowInstantReloadAfterShot", true);
        SPRINTING_ACCURACY_PENALTY = builder.comment("Reserved balance toggle for applying extra spread while sprint-firing when a compatible TaCZ spread hook is available.").define("sprintingAccuracyPenalty", true);
        SPRINTING_ACCURACY_MULTIPLIER = builder.comment("Reserved sprint-firing spread multiplier. Values above 1 mean less accurate.").defineInRange("sprintingAccuracyMultiplier", 1.35D, 1.0D, 10.0D);
        SPRINTING_RECOIL_MULTIPLIER = builder.comment("Reserved sprint-firing recoil multiplier. Values above 1 mean more recoil.").defineInRange("sprintingRecoilMultiplier", 1.15D, 1.0D, 10.0D);
        SPRINTING_RELOAD_SPEED_MULTIPLIER = builder.comment("Reserved reload-speed multiplier while sprinting. 1.0 keeps normal TaCZ reload timing.").defineInRange("sprintingReloadSpeedMultiplier", 1.0D, 0.1D, 10.0D);
        builder.pop();

        builder.push("impact_smoke");
        ENABLE_IMPACT_SMOKE = builder.comment("Spawn subtle client-side smoke puffs when TaCZ bullets hit blocks.").define("enableImpactSmoke", true);
        IMPACT_SMOKE_AMOUNT = builder.comment("Base smoke particles per impact.").defineInRange("impactSmokeAmount", 6, 0, 64);
        IMPACT_SMOKE_SIZE = builder.comment("Visual scale for impact smoke particles.").defineInRange("impactSmokeSize", 0.35D, 0.05D, 2.0D);
        IMPACT_SMOKE_LIFETIME_TICKS = builder.comment("Smoke lifetime in client ticks.").defineInRange("impactSmokeLifetimeTicks", 12, 1, 80);
        IMPACT_SMOKE_ONLY_ON_HARD_BLOCKS = builder.comment("When true, smoke only appears on stone/concrete/metal and other hard blocks.").define("impactSmokeOnlyOnHardBlocks", false);
        MAX_IMPACT_PARTICLES_PER_SECOND = builder.comment("Client-side limiter for impact smoke/dust particles.").defineInRange("maxImpactParticlesPerSecond", 180, 0, 2000);
        builder.pop();

        builder.push("metal_impacts");
        ENABLE_METAL_SPARKS = builder.comment("Spawn bright directional sparks when TaCZ bullets hit metal blocks.").define("enableMetalSparks", true);
        METAL_SPARK_AMOUNT = builder.comment("Spark particles per metal impact.").defineInRange("metalSparkAmount", 8, 0, 64);
        METAL_SPARK_LIFETIME_TICKS = builder.comment("Lifetime for metal spark particles in client ticks.").defineInRange("metalSparkLifetimeTicks", 6, 1, 40);
        ENABLE_METAL_RICOCHET_SOUND = builder.comment("Play a small metallic ricochet/ping sound on metal impacts.").define("enableMetalRicochetSound", true);
        builder.pop();

        builder.push("wood_impacts");
        ENABLE_WOOD_SPLINTERS = builder.comment("Spawn directional wood splinters when TaCZ bullets hit wooden blocks.").define("enableWoodSplinters", true);
        WOOD_SPLINTER_AMOUNT = builder.comment("Wood splinter particles per wooden impact. Shotguns and high calibers scale this up.").defineInRange("woodSplinterAmount", 10, 0, 96);
        WOOD_SPLINTER_LIFETIME_TICKS = builder.comment("Lifetime for wood splinter particles in client ticks.").defineInRange("woodSplinterLifetimeTicks", 14, 1, 80);
        ENABLE_WOOD_IMPACT_SOUND = builder.comment("Play a sharp wooden crack/splinter sound on wood impacts.").define("enableWoodImpactSound", true);
        builder.pop();

        builder.push("bullet_decals");
        ENABLE_BULLET_DECALS = builder.comment("Render persistent client-side bullet hole decals at TaCZ impact positions.").define("enableBulletDecals", true);
        IMPACT_DECALS_ENABLED = builder.comment("Master toggle for client-side impact decal rendering. Existing enableBulletDecals must also be true.").define("impactDecalsEnabled", true);
        MAX_DECALS_PER_CHUNK = builder.comment("Maximum client-side bullet decals stored per chunk. Oldest decals are removed first.").defineInRange("maxDecalsPerChunk", 100, 0, 1000);
        MAX_IMPACT_DECALS_PER_CHUNK = builder.comment("Maximum client-side bullet impact decals stored per chunk. Oldest decals are removed first.").defineInRange("maxImpactDecalsPerChunk", 50, 0, 1000);
        MAX_IMPACT_DECALS = builder.comment("Global client-side cap for active impact decals across all chunks. Oldest decals are removed first.").defineInRange("maxImpactDecals", 300, 0, 5000);
        BULLET_DECAL_LIFETIME_SECONDS = builder.comment("How long bullet decals remain visible on clients, in seconds.").defineInRange("bulletDecalLifetimeSeconds", 300, 1, 3600);
        BULLET_HOLE_LIFETIME_TICKS = builder.comment("How long bullet hole decals remain visible on clients, in ticks. 20 ticks = 1 second.").defineInRange("bulletHoleLifetimeTicks", 2400, 20, 72000);
        BULLET_DECAL_SIZE = builder.comment("Base visual size for bullet decals, in blocks.").defineInRange("bulletDecalSize", 0.22D, 0.03D, 2.0D);
        BULLET_HOLE_RANDOM_ROTATION = builder.comment("Randomize bullet hole decal rotation so repeated impacts do not look identical.").define("bulletHoleRandomRotation", true);
        BULLET_HOLE_RANDOM_SCALE = builder.comment("Randomize bullet hole decal size slightly for less repetition.").define("bulletHoleRandomScale", true);
        ENABLE_MATERIAL_SPECIFIC_DECALS = builder.comment("Use material-specific decal colors and crack/chip styles.").define("enableMaterialSpecificDecals", true);
        MATERIAL_SPECIFIC_IMPACTS = builder.comment("Use material-specific particles, sounds, and decal styles for bullet impacts.").define("materialSpecificImpacts", true);
        builder.pop();

        builder.push("ricochet_sounds");
        ENABLE_RICOCHET_SOUNDS = builder.comment("Play client-side bullet ricochet/ping sounds on metal and hard block impacts.").define("enableRicochetSounds", true);
        RICOCHET_SOUND_CHANCE_METAL = builder.comment("Chance that a metal impact plays a ricochet sound.").defineInRange("ricochetSoundChanceMetal", 0.70D, 0.0D, 1.0D);
        RICOCHET_SOUND_CHANCE_HARD_BLOCK = builder.comment("Chance that a hard stone/concrete impact plays a dull ricochet/chip sound.").defineInRange("ricochetSoundChanceHardBlock", 0.25D, 0.0D, 1.0D);
        RICOCHET_VOLUME = builder.comment("Base ricochet sound volume.").defineInRange("ricochetVolume", 0.8D, 0.0D, 4.0D);
        RICOCHET_MAX_SOUNDS_PER_SECOND = builder.comment("Client-side limiter for ricochet sounds to avoid automatic fire spam.").defineInRange("ricochetMaxSoundsPerSecond", 12, 0, 128);
        RICOCHET_MAX_DISTANCE = builder.comment("Maximum distance in blocks at which ricochet sounds can play.").defineInRange("ricochetMaxDistance", 48.0D, 1.0D, 256.0D);
        builder.pop();

        builder.push("muzzle_smoke");
        ENABLE_MUZZLE_SMOKE = builder.comment("Spawn client-side muzzle smoke when TaCZ guns fire.").define("enableMuzzleSmoke", true);
        MUZZLE_SMOKE_AMOUNT = builder.comment("Base muzzle smoke wisps per shot.").defineInRange("muzzleSmokeAmount", 2, 0, 64);
        MUZZLE_SMOKE_LIFETIME_TICKS = builder.comment("Base muzzle smoke lifetime in client ticks.").defineInRange("muzzleSmokeLifetimeTicks", 10, 1, 100);
        MUZZLE_SMOKE_SCALE = builder.comment("Base visual scale for muzzle smoke particles.").defineInRange("muzzleSmokeScale", 0.22D, 0.05D, 2.0D);
        SMOKE_QUALITY = builder.comment("Muzzle smoke quality: LOW, MEDIUM, HIGH, ULTRA.").define("smokeQuality", "MEDIUM", value -> value instanceof String text && (text.equals("LOW") || text.equals("MEDIUM") || text.equals("HIGH") || text.equals("ULTRA")));
        SMOKE_OPACITY = builder.defineInRange("smokeOpacity", 0.20D, 0.01D, 1.0D);
        SMOKE_EXPANSION = builder.defineInRange("smokeExpansionSpeed", 1.0D, 0.1D, 3.0D);
        SMOKE_TURBULENCE = builder.defineInRange("smokeTurbulence", 1.0D, 0.0D, 3.0D);
        FIRST_PERSON_SMOKE_INTENSITY = builder.defineInRange("firstPersonSmokeIntensity", 0.55D, 0.0D, 1.0D);
        MAX_ACTIVE_MUZZLE_SMOKE = builder.defineInRange("maxActiveMuzzleSmoke", 96, 0, 512);
        DYNAMIC_SMOKE_BUILDUP = builder.comment("Increase smoke density during sustained automatic fire.").define("dynamicSmokeBuildup", true);
        MAX_SMOKE_PARTICLES_PER_SECOND = builder.comment("Client-side limiter for muzzle smoke particles.").defineInRange("maxSmokeParticlesPerSecond", 45, 0, 2000);
        builder.pop();

        builder.push("indoor_smoke");
        ENABLE_INDOOR_SMOKE = builder.comment("Accumulate longer-lived client-side tactical smoke in enclosed indoor spaces during gunfights.").define("enableIndoorSmoke", true);
        INDOOR_SMOKE_MULTIPLIER = builder.comment("Multiplier for smoke density gained in enclosed indoor spaces.").defineInRange("indoorSmokeMultiplier", 2.5D, 0.0D, 10.0D);
        SMOKE_DISSIPATION_SPEED_INDOOR = builder.comment("Indoor smoke dissipation speed. Lower values linger longer.").defineInRange("smokeDissipationSpeedIndoor", 0.25D, 0.01D, 5.0D);
        SMOKE_DISSIPATION_SPEED_OUTDOOR = builder.comment("Outdoor smoke dissipation speed. Higher values clears smoke faster.").defineInRange("smokeDissipationSpeedOutdoor", 1.0D, 0.01D, 5.0D);
        MAX_INDOOR_SMOKE_DENSITY = builder.comment("Maximum tracked indoor smoke density per local room cell.").defineInRange("maxIndoorSmokeDensity", 1.0D, 0.05D, 4.0D);
        ENABLE_FLASHLIGHT_SMOKE_INTERACTION = builder.comment("Allow tactical light flashes and flashlight-like beams to brighten nearby smoke haze.").define("enableFlashlightSmokeInteraction", true);
        ENABLE_LASER_SMOKE_VISIBILITY = builder.comment("Allow laser/beam-like light hints to become more visible inside smoke haze.").define("enableLaserSmokeVisibility", true);
        builder.pop();

        builder.push("glass_shards");
        ENABLE_GLASS_SHARDS = builder.comment("Spawn temporary client-side glass shard debris when bullets hit or break glass.").define("enableGlassShards", true);
        GLASS_SHARD_AMOUNT = builder.comment("Base glass shard debris count for full glass blocks. Panes spawn fewer shards.").defineInRange("glassShardAmount", 12, 0, 128);
        GLASS_SHARD_LIFETIME_SECONDS = builder.comment("How long glass shard debris remains visible on clients.").defineInRange("glassShardLifetimeSeconds", 20, 1, 300);
        GLASS_SHARD_FADE_OUT = builder.comment("Fade glass shard debris before removing it.").define("glassShardFadeOut", true);
        MAX_GLASS_SHARDS_PER_CHUNK = builder.comment("Maximum client-side glass shard debris stored per chunk. Oldest shards are removed first.").defineInRange("maxGlassShardsPerChunk", 80, 0, 1000);
        builder.pop();

        builder.push("suppression");
        ENABLE_SUPPRESSION_SOUNDS = builder.comment("Deprecated and ignored. Projectile proximity sounds have been removed; distant gunshots are handled by the distant_gunshots section.").define("enableSuppressionSounds", false);
        SUPPRESSION_RADIUS = builder.comment("Deprecated and ignored.").defineInRange("suppressionRadius", 3.0D, 0.25D, 16.0D);
        SUPPRESSION_VOLUME = builder.comment("Deprecated and ignored.").defineInRange("suppressionVolume", 1.0D, 0.0D, 4.0D);
        SUPPRESSION_MAX_SOUNDS_PER_SECOND = builder.comment("Deprecated and ignored.").defineInRange("suppressionMaxSoundsPerSecond", 10, 0, 128);
        ENABLE_SHOOTER_SUPPRESSION = builder.comment("Deprecated and ignored.").define("enableShooterSuppression", false);
        builder.pop();

        builder.push("dynamic_lighting");
        ENABLE_DYNAMIC_GUN_LIGHTING = builder.comment("Render short-lived client-side tactical light flashes from gunfire and breaching impacts.").define("enableDynamicGunLighting", true);
        ENABLE_SPARK_LIGHTING = builder.comment("Render tiny short-lived light flashes from metal ricochet sparks.").define("enableSparkLighting", true);
        MUZZLE_FLASH_LIGHT_INTENSITY = builder.comment("Base visual intensity for muzzle flash lighting.").defineInRange("muzzleFlashLightIntensity", 1.0D, 0.0D, 8.0D);
        SPARK_LIGHT_INTENSITY = builder.comment("Base visual intensity for spark light flashes.").defineInRange("sparkLightIntensity", 0.4D, 0.0D, 8.0D);
        DYNAMIC_LIGHT_RANGE = builder.comment("Maximum visual range in blocks for tactical dynamic light flashes.").defineInRange("dynamicLightRange", 7, 1, 32);
        REDUCED_LIGHTING_FOR_SUPPRESSORS = builder.comment("Reduce muzzle flash lighting for suppressed weapons.").define("reducedLightingForSuppressors", true);
        builder.pop();

        builder.push("screen_dust");
        ENABLE_SCREEN_DUST = builder.comment("Show subtle client-side screen edge dust/debris when bullets impact close to the player.").define("enableScreenDust", true);
        SCREEN_DUST_RADIUS = builder.comment("Impact distance in blocks that can trigger screen dust.").defineInRange("screenDustRadius", 5.0D, 0.5D, 24.0D);
        SCREEN_DUST_INTENSITY = builder.comment("Base screen dust opacity/intensity.").defineInRange("screenDustIntensity", 0.45D, 0.0D, 2.0D);
        SCREEN_DUST_DURATION_TICKS = builder.comment("Screen dust overlay duration in client ticks.").defineInRange("screenDustDurationTicks", 18, 1, 100);
        SCREEN_DUST_MAX_TRIGGERS_PER_SECOND = builder.comment("Client-side limiter for nearby screen dust triggers.").defineInRange("screenDustMaxTriggersPerSecond", 4, 0, 64);
        SCREEN_DUST_INDOOR_MULTIPLIER = builder.comment("Multiplier for screen dust intensity indoors/enclosed spaces.").defineInRange("screenDustIndoorMultiplier", 1.5D, 0.0D, 6.0D);
        builder.pop();

        builder.push("weapon_heat");
        ENABLE_WEAPON_HEAT = builder.comment("Track client-side weapon heat from sustained TaCZ fire.").define("enableWeaponHeat", true);
        ENABLE_BARREL_SMOKE = builder.comment("Spawn smoke from hot barrels after sustained fire.").define("enableBarrelSmoke", true);
        ENABLE_HEAT_DISTORTION = builder.comment("Spawn subtle heat shimmer/haze particles above hot barrels.").define("enableHeatDistortion", true);
        ENABLE_HOT_BARREL_GLOW = builder.comment("Spawn subtle orange/red glow particles and light from extremely hot barrels/suppressors.").define("enableHotBarrelGlow", true);
        WEAPON_COOLING_SPEED = builder.comment("Multiplier for weapon heat cooling speed. Higher values cool faster.").defineInRange("weaponCoolingSpeed", 1.0D, 0.05D, 10.0D);
        MAX_HEAT_LEVEL = builder.comment("Maximum tracked weapon heat level.").defineInRange("maxHeatLevel", 100, 10, 1000);
        HEAT_DISTORTION_STRENGTH = builder.comment("Visual strength for heat shimmer particles.").defineInRange("heatDistortionStrength", 0.35D, 0.0D, 4.0D);
        BARREL_GLOW_INTENSITY = builder.comment("Visual intensity for hot barrel/suppressor glow.").defineInRange("barrelGlowIntensity", 0.5D, 0.0D, 4.0D);
        builder.pop();

        builder.push("material_penetration");
        ENABLE_MATERIAL_PENETRATION = builder.comment("Allow TaCZ bullets to penetrate supported materials with damage and velocity loss.").define("enableMaterialPenetration", true);
        MAX_PENETRATION_DEPTH = builder.comment("Maximum block thickness in blocks that the penetration solver checks.").defineInRange("maxPenetrationDepth", 3.0D, 0.1D, 16.0D);
        WOOD_PENETRATION_MULTIPLIER = builder.comment("Penetration capability multiplier for wood-like blocks.").defineInRange("woodPenetrationMultiplier", 1.0D, 0.0D, 10.0D);
        STONE_PENETRATION_MULTIPLIER = builder.comment("Penetration capability multiplier for stone/concrete/brick-like blocks.").defineInRange("stonePenetrationMultiplier", 0.55D, 0.0D, 10.0D);
        METAL_PENETRATION_MULTIPLIER = builder.comment("Penetration capability multiplier for metal blocks.").defineInRange("metalPenetrationMultiplier", 0.25D, 0.0D, 10.0D);
        REINFORCED_PENETRATION_MULTIPLIER = builder.comment("Penetration capability multiplier for reinforced/blast-resistant blocks.").defineInRange("reinforcedPenetrationMultiplier", 0.05D, 0.0D, 10.0D);
        DAMAGE_LOSS_PER_BLOCK = builder.comment("Damage multiplier loss per block penetrated.").defineInRange("damageLossPerBlock", 0.35D, 0.0D, 1.0D);
        VELOCITY_LOSS_PER_BLOCK = builder.comment("Velocity multiplier loss per block penetrated.").defineInRange("velocityLossPerBlock", 0.30D, 0.0D, 1.0D);
        ENABLE_EXIT_DECALS = builder.comment("Spawn client-side exit decals when bullets penetrate blocks.").define("enableExitDecals", true);
        ENABLE_PENETRATION_PARTICLES = builder.comment("Spawn client-side entry/exit particles for bullet penetration.").define("enablePenetrationParticles", true);
        builder.pop();

        builder.push("camera_shake");
        ENABLE_CAMERA_SHAKE = builder.comment("Enable subtle client-side cinematic camera shake. Accessibility: set false to disable all shake.").define("enableCameraShake", true);
        CAMERA_SHAKE_INTENSITY = builder.comment("Global camera shake intensity multiplier.").defineInRange("cameraShakeIntensity", 1.0D, 0.0D, 4.0D);
        CAMERA_SHAKE_MAX_DISTANCE = builder.comment("Maximum distance in blocks for camera shake events.").defineInRange("cameraShakeMaxDistance", 18, 1, 96);
        CAMERA_SHAKE_DURATION_TICKS = builder.comment("Base duration in ticks for camera shake events.").defineInRange("cameraShakeDurationTicks", 8, 1, 80);
        ENABLE_EXPLOSION_SHAKE = builder.comment("Enable stronger rumble shake from nearby explosions.").define("enableExplosionShake", true);
        ENABLE_SHOTGUN_SHAKE = builder.comment("Enable short punchy shake from shotgun/heavy nearby fire.").define("enableShotgunShake", true);
        ENABLE_SUPPRESSION_SHAKE = builder.comment("Enable sharp jolt shake from configured pressure events.").define("enableSuppressionShake", true);
        ENABLE_BREACHING_SHAKE = builder.comment("Enable short impact shake from door/block breaching and destruction.").define("enableBreachingShake", true);
        builder.pop();

        builder.push("armor_hits");
        ENABLE_ARMOR_HIT_EFFECTS = builder.comment("Enable client-side sparks, smoke, metal debris, and sounds when TaCZ bullets hit armor.").define("enableArmorHitEffects", true);
        ENABLE_ARMOR_RICOCHET = builder.comment("Allow weak bullet hits on armor to visually ricochet and reduce damage.").define("enableArmorRicochet", true);
        ENABLE_ARMOR_SPARKS = builder.comment("Spawn bright metal sparks when TaCZ bullets hit armor.").define("enableArmorSparks", true);
        ARMOR_RICOCHET_CHANCE = builder.comment("Base chance for non-AP bullet hits on armor to ricochet or deflect. High caliber and AP rounds reduce this chance.").defineInRange("armorRicochetChance", 0.45D, 0.0D, 1.0D);
        ARMOR_SPARK_AMOUNT = builder.comment("Spark particles per armored entity or vehicle hit.").defineInRange("armorSparkAmount", 8, 0, 64);
        ARMOR_HIT_VOLUME = builder.comment("Base volume for armor hit, thud, and ricochet sounds.").defineInRange("armorHitVolume", 0.9D, 0.0D, 4.0D);
        builder.pop();

        builder.push("blood_effects");
        ENABLE_BLOOD_MIST = builder.comment("Spawn subtle client-side blood mist when TaCZ bullets hit living entities.").define("enableBloodMist", true);
        BLOOD_EFFECTS_ENABLED = builder.comment("Master toggle for all client-side blood mist, droplets, and splatter visuals.").define("bloodEffectsEnabled", true);
        BLOOD_PHYSICS_ENABLED = builder.comment("Enable lightweight client-side Blood Physics 2.0 droplets that fall, collide with blocks, and become decals.").define("bloodPhysicsEnabled", true);
        ENABLE_WALL_SPLATTER = builder.comment("Render small fading client-side blood splatter decals on nearby surfaces behind hit targets.").define("enableWallSplatter", true);
        BLOOD_MIST_INTENSITY = builder.comment("Global intensity multiplier for subtle blood mist. Keep low for restrained tactical visuals.").defineInRange("bloodMistIntensity", 0.35D, 0.0D, 2.0D);
        BLOOD_DENSITY = builder.comment("Density multiplier for client-side blood mist and droplet particles.").defineInRange("bloodDensity", 1.0D, 0.0D, 5.0D);
        BLOOD_LIFETIME_TICKS = builder.comment("Lifetime in client ticks for Blood Physics 2.0 droplets and blood decals. 20 ticks = 1 second.").defineInRange("bloodLifetimeTicks", 1200, 20, 72000);
        BLOOD_SPLATTER_CHANCE = builder.comment("Chance that a living entity bullet hit creates a small wall/floor splatter behind the target.").defineInRange("bloodSplatterChance", 0.25D, 0.0D, 1.0D);
        BLOOD_DECAL_LIFETIME_SECONDS = builder.comment("How long blood splatter decals remain visible on clients, in seconds.").defineInRange("bloodDecalLifetimeSeconds", 180, 1, 1800);
        MAX_BLOOD_DECALS_PER_CHUNK = builder.comment("Maximum client-side blood splatter decals stored per chunk. Oldest decals are removed first.").defineInRange("maxBloodDecalsPerChunk", 60, 0, 500);
        MAX_BLOOD_PARTICLES_PER_HIT = builder.comment("Maximum Blood Physics 2.0 droplets spawned for one hit before global rate limiting.").defineInRange("maxBloodParticlesPerHit", 18, 0, 128);
        MAX_BLOOD_PARTICLES_PER_IMPACT = builder.comment("Maximum blood mist/droplet particles spawned for one impact before global rate limiting.").defineInRange("maxBloodParticlesPerImpact", 48, 0, 256);
        BLOOD_TRAILS_ENABLED = builder.comment("Allow badly injured nearby living entities to occasionally drop tiny client-side blood droplets while moving. Rate-limited and off by default.").define("bloodTrailsEnabled", false);
        SODIUM_SAFE_RENDERING = builder.comment("Force conservative depth-tested rendering for custom impact visuals. Helps Sodium, Iris, Embeddium, and shader compatibility.").define("sodiumSafeRendering", true);
        builder.pop();

        builder.push("dynamic_impact_sounds");
        ENABLE_DYNAMIC_IMPACT_SOUNDS = builder.comment("Legacy setting. Vanilla material impact sound layers are no longer played by this mod.").define("enableDynamicImpactSounds", false);
        ENABLE_INDOOR_REVERB = builder.comment("Add short local echo/reverb layers for impacts in enclosed indoor spaces.").define("enableIndoorReverb", true);
        ENABLE_DISTANCE_MUFFLING = builder.comment("Lower volume and pitch for distant impacts to simulate muffling.").define("enableDistanceMuffling", true);
        IMPACT_SOUND_MAX_DISTANCE = builder.comment("Maximum distance in blocks for dynamic impact sounds.").defineInRange("impactSoundMaxDistance", 96, 4, 256);
        IMPACT_SOUND_VOLUME = builder.comment("Global dynamic impact sound volume multiplier.").defineInRange("impactSoundVolume", 1.0D, 0.0D, 4.0D);
        DYNAMIC_IMPACT_VARIATION = builder.comment("Randomize impact sound variants, pitch, volume, and rare cinematic ricochet cracks.").define("dynamicImpactVariation", true);
        builder.pop();

        builder.push("gunshot_acoustics");
        ENABLE_BULLET_CRACK_ECHO = builder.comment("Legacy setting. Vanilla gunshot crack and indoor echo layers are no longer played by this mod.").define("enableBulletCrackEcho", false);
        ENABLE_OUTDOOR_ECHO = builder.comment("Allow outdoor echo tails in open, urban, valley, and forest environments.").define("enableOutdoorEcho", true);
        URBAN_ECHO_INTENSITY = builder.comment("Echo intensity multiplier for dense urban/building areas.").defineInRange("urbanEchoIntensity", 1.0D, 0.0D, 5.0D);
        INDOOR_REVERB_INTENSITY = builder.comment("Reverb intensity multiplier for indoor gunshots and sniper cracks.").defineInRange("indoorReverbIntensity", 1.2D, 0.0D, 5.0D);
        VALLEY_ECHO_INTENSITY = builder.comment("Echo intensity multiplier for open valley/mountain-like terrain.").defineInRange("valleyEchoIntensity", 1.4D, 0.0D, 5.0D);
        FOREST_ECHO_REDUCTION = builder.comment("Multiplier applied to outdoor echoes in forests to dampen high-frequency crack tails.").defineInRange("forestEchoReduction", 0.65D, 0.0D, 1.0D);
        MAX_ECHO_DISTANCE = builder.comment("Maximum distance in blocks for client-side environmental gunshot echoes.").defineInRange("maxEchoDistance", 256, 16, 512);
        builder.pop();

        builder.push("suppression_effects");
        ENABLE_SUPPRESSION_BLUR = builder.comment("Render a very subtle edge-only pressure vignette/blur hint for explosions and sustained nearby fire. Accessibility: set false to disable visual blur.").define("enableSuppressionBlur", true);
        ENABLE_SUPPRESSION_RINGING = builder.comment("Play short fading tinnitus/ringing after heavy suppression, sniper cracks, or nearby explosions.").define("enableSuppressionRinging", true);
        ENABLE_STRESS_EFFECTS = builder.comment("Enable subtle desaturation tint and pressure feel while under heavy fire.").define("enableStressEffects", true);
        SUPPRESSION_BLUR_STRENGTH = builder.comment("Maximum visual strength for subtle edge suppression blur. Center screen remains clear.").defineInRange("suppressionBlurStrength", 0.18D, 0.0D, 1.0D);
        SUPPRESSION_EFFECT_RADIUS = builder.comment("Radius in blocks for suppression visual/stress effects around the player.").defineInRange("suppressionEffectRadius", 4.0D, 0.5D, 16.0D);
        EAR_RINGING_DURATION_TICKS = builder.comment("Base duration in ticks for ear ringing after heavy suppression events.").defineInRange("earRingingDurationTicks", 40, 1, 200);
        MAX_SUPPRESSION_EFFECTS_PER_SECOND = builder.comment("Client-side limiter for suppression visual/ringing triggers.").defineInRange("maxSuppressionEffectsPerSecond", 6, 0, 64);
        builder.pop();

        builder.push("physical_debris");
        ENABLE_PHYSICAL_DEBRIS = builder.comment("Spawn short-lived client-side physical debris when doors, glass, and destructible blocks are destroyed.").define("enablePhysicalDebris", true);
        DEBRIS_LIFETIME_SECONDS = builder.comment("How long physical debris remains visible before fading/despawning.").defineInRange("debrisLifetimeSeconds", 25, 1, 180);
        MAX_DEBRIS_PER_CHUNK = builder.comment("Maximum physical debris pieces stored per chunk. Oldest debris is removed first.").defineInRange("maxDebrisPerChunk", 120, 0, 1000);
        WOOD_DEBRIS_AMOUNT = builder.comment("Base plank/chunk debris amount for destroyed wooden blocks and doors.").defineInRange("woodDebrisAmount", 14, 0, 160);
        GLASS_DEBRIS_AMOUNT = builder.comment("Base glass chunk debris amount for destroyed glass blocks and panes.").defineInRange("glassDebrisAmount", 18, 0, 160);
        METAL_DEBRIS_AMOUNT = builder.comment("Base bent metal fragment amount for destroyed metallic blocks.").defineInRange("metalDebrisAmount", 10, 0, 160);
        CONCRETE_DEBRIS_AMOUNT = builder.comment("Base rubble fragment amount for destroyed stone/concrete blocks.").defineInRange("concreteDebrisAmount", 16, 0, 160);
        builder.pop();

        builder.push("shockwave_dust");
        ENABLE_SHOCKWAVE_DUST = builder.comment("Spawn client-side shockwave dust, haze, and drifting debris after indoor explosions and heavy breaching detonations.").define("enableShockwaveDust", true);
        ENABLE_INDOOR_PRESSURE_EFFECT = builder.comment("Apply brief client-side pressure feedback near indoor explosions.").define("enableIndoorPressureEffect", true);
        ENABLE_DEBRIS_FALLOFF = builder.comment("Spawn short falling dust/debris from ceilings and nearby surfaces after explosions.").define("enableDebrisFalloff", true);
        SHOCKWAVE_DUST_INTENSITY = builder.comment("Global intensity multiplier for explosion shockwave dust.").defineInRange("shockwaveDustIntensity", 1.0D, 0.0D, 5.0D);
        PRESSURE_EFFECT_STRENGTH = builder.comment("Strength multiplier for indoor pressure wave camera/audio visual feedback.").defineInRange("pressureEffectStrength", 0.45D, 0.0D, 2.0D);
        DUST_LIFETIME_TICKS = builder.comment("Lifetime for shockwave dust and falling ceiling dust particles.").defineInRange("dustLifetimeTicks", 80, 4, 300);
        MAX_SHOCKWAVE_PARTICLES = builder.comment("Client-side particle budget per second for shockwave dust and debris falloff.").defineInRange("maxShockwaveParticles", 180, 0, 2000);
        builder.pop();

        builder.push("burn_marks");
        ENABLE_BURN_MARKS = builder.comment("Render client-side burn/scorch decals from explosions, breaching, and heavy heat residue.").define("enableBurnMarks", true);
        ENABLE_EXPLOSION_SCORCH = builder.comment("Create large dark scorch marks after explosions.").define("enableExplosionScorch", true);
        ENABLE_BREACH_SCORCH = builder.comment("Create scorch residue around breached doors, frames, locks, and destroyed blocks.").define("enableBreachScorch", true);
        ENABLE_HEAT_RESIDUE = builder.comment("Allow sustained heavy fire, hot barrels, and muzzle blasts to lightly darken nearby surfaces.").define("enableHeatResidue", true);
        BURN_MARK_LIFETIME_SECONDS = builder.comment("How long burn/scorch decals remain visible on clients, in seconds.").defineInRange("burnMarkLifetimeSeconds", 600, 5, 7200);
        MAX_BURN_MARKS_PER_CHUNK = builder.comment("Maximum client-side burn marks stored per chunk. Oldest scorch marks are removed first.").defineInRange("maxBurnMarksPerChunk", 60, 0, 500);
        BURN_MARK_OPACITY = builder.comment("Global opacity multiplier for scorch decals.").defineInRange("burnMarkOpacity", 0.65D, 0.0D, 1.0D);
        builder.pop();

        builder.push("nightstick_flashlight");
        ENABLE_NIGHTSTICK_FLASHLIGHT = builder.comment("Upgrade the TaCZ Nightstick Compact Laser attachment into a client-side tactical flashlight and laser/light combo.").define("enableNightstickFlashlight", true);
        FLASHLIGHT_INTENSITY = builder.comment("Visual intensity multiplier for the Nightstick tactical flashlight beam.").defineInRange("flashlightIntensity", 1.0D, 0.0D, 5.0D);
        FLASHLIGHT_RANGE = builder.comment("Maximum range in blocks for the Nightstick tactical flashlight beam.").defineInRange("flashlightRange", 18, 2, 64);
        ENABLE_NIGHTSTICK_DYNAMIC_LIGHTING = builder.comment("Allow the Nightstick flashlight to feed TaCZ Tactical Breaching's client-side dynamic lighting and smoke illumination.").define("enableDynamicLighting", true);
        builder.pop();

        builder.push("frag_grenade");
        ENABLE_FRAG_GRENADE = builder.comment("Enable the TaCZ Tactical Breaching throwable frag grenade item and entity.").define("enableFragGrenade", true);
        FRAG_GRENADE_COOLDOWN_TICKS = builder.comment("Cooldown after throwing a frag grenade, in ticks. 20 ticks = 1 second.").defineInRange("fragGrenadeCooldownTicks", 40, 0, 1200);
        FRAG_THROW_POWER = builder.comment("Throw velocity for frag grenades. Higher values make grenades fly farther.").defineInRange("fragThrowPower", 1.35D, 0.2D, 3.0D);
        FRAG_FUSE_TIME_TICKS = builder.comment("Fuse time for thrown frag grenades in ticks. 20 ticks = 1 second.").defineInRange("fragFuseTimeTicks", 60, 5, 400);
        FRAG_EXPLOSION_RADIUS = builder.comment("Visual and damage radius for frag grenade explosions, in blocks. Values above 6 are safety-capped for compatibility with explosion overhauls.").defineInRange("fragExplosionRadius", 5.0D, 1.0D, 24.0D);
        FRAG_DAMAGE = builder.comment("Frag damage setting before the 1.5x entity-only boost, distance falloff and armor mitigation. Effective damage is capped at 12; explosion power and block damage are unchanged.").defineInRange("fragDamage", 6.0D, 0.0D, 200.0D);
        FRAG_COUNT = builder.comment("Number of invisible fragment rays checked when the grenade detonates. Values above 64 are safety-capped for balanced damage and performance.").defineInRange("fragCount", 48, 0, 512);
        ENABLE_FRAGMENT_LINE_OF_SIGHT = builder.comment("When true, blocks between fragments and targets block or reduce fragment hits.").define("enableFragmentLineOfSight", true);
        ENABLE_FRAG_BLOCK_DAMAGE = builder.comment("Break up to six nearby soft blocks without a vanilla explosion, including when Explosion Overhaul is installed. Existing config values are preserved.").define("enableBlockDamage", true);
        ENABLE_FRAG_SMOKE = builder.comment("Spawn client-side smoke and lingering haze from frag detonations.").define("enableFragSmoke", true);
        ENABLE_FRAG_DEBRIS = builder.comment("Spawn client-side debris, dust, sparks, and fragments from frag detonations.").define("enableFragDebris", true);
        ENABLE_FRAG_INDOOR_PRESSURE = builder.comment("Enable stronger indoor pressure/shockwave feedback for frag detonations.").define("enableIndoorPressure", true);
        ENABLE_FRAG_CEILING_DUST = builder.comment("Allow ceiling dust/falling debris after indoor frag detonations.").define("enableCeilingDust", true);
        ENABLE_FRAG_EAR_RINGING = builder.comment("Enable ear ringing/tinnitus after close frag detonations.").define("enableEarRinging", true);
        FRAG_EXPLOSION_SOUND_DISTANCE = builder.comment("Maximum client distance in blocks for layered frag explosion audio.").defineInRange("fragExplosionSoundDistance", 256, 16, 512);
        ENABLE_EXPLOSION_ECHO = builder.comment("Enable distant rolling boom tails for frag explosions.").define("enableExplosionEcho", true);
        ENABLE_INDOOR_EXPLOSION_REVERB = builder.comment("Enable close indoor reverb layers for frag explosions.").define("enableIndoorExplosionReverb", true);
        builder.pop();

        builder.push("flashbang");
        ENABLE_FLASHBANG = builder.comment("Enable the TaCZ Tactical Breaching throwable tactical flashbang item and entity.").define("enableFlashbang", true);
        FLASHBANG_COOLDOWN_TICKS = builder.comment("Cooldown after throwing a flashbang grenade, in ticks. 20 ticks = 1 second.").defineInRange("flashbangCooldownTicks", 40, 0, 1200);
        FLASHBANG_THROW_POWER = builder.comment("Throw velocity for flashbang grenades. Higher values make flashbangs fly farther.").defineInRange("flashbangThrowPower", 1.30D, 0.2D, 3.0D);
        FLASHBANG_FUSE_TICKS = builder.comment("Fuse time for thrown flashbang grenades in ticks. 20 ticks = 1 second.").defineInRange("flashbangFuseTicks", 40, 5, 400);
        FLASHBANG_RADIUS = builder.comment("Radius in blocks for flash, tinnitus, blur, and pressure effects.").defineInRange("flashbangRadius", 12.0D, 1.0D, 48.0D);
        FLASH_DURATION_TICKS = builder.comment("Duration of the white flash visual effect in ticks.").defineInRange("flashDurationTicks", 60, 1, 240);
        FLASHBANG_TINNITUS_DURATION_TICKS = builder.comment("Duration of close flashbang tinnitus/ringing in ticks.").defineInRange("tinnitusDurationTicks", 100, 1, 400);
        FLASHBANG_BLUR_STRENGTH = builder.comment("Maximum subtle blur/vignette strength caused by flashbangs.").defineInRange("blurStrength", 0.25D, 0.0D, 1.0D);
        ENABLE_FLASHBANG_LINE_OF_SIGHT = builder.comment("When true, solid walls block flashbang blinding effects. Glass reduces the effect instead of fully blocking it.").define("enableLineOfSightCheck", true);
        ENABLE_FLASHBANG_INDOOR_REVERB = builder.comment("Enable stronger indoor flashbang detonation reverb and pressure feeling.").define("enableIndoorReverb", true);
        builder.pop();

        builder.push("gas_masks");
        ENABLE_GAS_MASKS = builder.comment("Enable gas mask items, filter protection, overlay, and filter durability.").define("enableGasMasks", true);
        ENABLE_FILTER_DURABILITY = builder.comment("Drain installed gas mask filters over time while the mask is worn.").define("enableFilterDurability", true);
        ENABLE_BREATHING_SOUNDS = builder.comment("Play quiet breathing sounds while wearing a working gas mask.").define("enableBreathingSounds", true);
        ENABLE_FLASHBANG_PROTECTION = builder.comment("Reduce flashbang intensity and duration while wearing a working gas mask.").define("enableFlashbangProtection", true);
        ENABLE_SMOKE_PROTECTION = builder.comment("Reduce indoor smoke visual impairment while wearing a working gas mask.").define("enableSmokeProtection", true);
        GAS_MASK_OVERLAY_ENABLED = builder.comment("Render the client-side gas mask vignette overlay while wearing the gas mask.").define("gasMaskOverlayEnabled", true);
        GAS_MASK_OVERLAY_OPACITY = builder.comment("Opacity multiplier for the gas mask overlay.").defineInRange("gasMaskOverlayOpacity", 0.34D, 0.0D, 1.0D);
        GAS_MASK_FILTER_TEXT_SCALE = builder.comment("Scale of the bottom-left gas mask filter HUD text.").defineInRange("gasMaskFilterTextScale", 0.60D, 0.35D, 1.5D);
        GAS_MASK_FILTER_TEXT_ENABLED = builder.comment("Show the current gas mask filter percentage on the HUD.").define("gasMaskFilterTextEnabled", true);
        builder.pop();

        builder.push("handcuffs");
        HANDCUFFS_ENABLED = builder.comment("Enable the multiplayer-safe handcuffs restraint system.").define("handcuffsEnabled", true);
        ALLOW_HANDCUFF_PLAYERS = builder.comment("Allow players to restrain other players with handcuffs.").define("allowHandcuffPlayers", true);
        ALLOW_HANDCUFF_VILLAGERS = builder.comment("Allow players to restrain villagers with handcuffs.").define("allowHandcuffVillagers", true);
        RESTRAINED_SPEED_MULTIPLIER = builder.comment("Movement speed multiplier applied to restrained players.").defineInRange("restrainedSpeedMultiplier", 0.20D, 0.0D, 1.0D);
        BLOCK_INVENTORY_INTERACTION = builder.comment("Block restrained players from using inventories and item interactions.").define("blockInventoryInteraction", true);
        BLOCK_CHEST_INTERACTION = builder.comment("Block restrained players from opening chests and container blocks.").define("blockChestInteraction", true);
        BLOCK_DOOR_INTERACTION = builder.comment("Block restrained players from using doors, buttons, levers, trapdoors, gates, and similar controls.").define("blockDoorInteraction", true);
        REQUIRE_KEY_TO_UNLOCK = builder.comment("Require a Handcuff Key item to release restrained players or villagers.").define("requireKeyToUnlock", true);
        OPERATORS_CAN_FORCE_UNLOCK = builder.comment("Allow operators to force unlock restrained entities without a key.").define("operatorsCanForceUnlock", true);
        builder.pop();

        builder.push("distant_gunshots");
        ENABLE_DISTANT_GUNSHOTS = builder.comment("Enable event-driven distant TaCZ gunshot audio beyond normal Minecraft sound range.").define("enableDistantGunshots", true);
        DISTANT_NORMAL_RANGE = builder.comment("Distance in blocks where normal TaCZ gun sounds are used and no extra distant sound packet is sent.").defineInRange("normalRange", 300, 0, 10000);
        MIN_DISTANT_GUNSHOT_DISTANCE = builder.comment("Minimum shooter-to-listener distance in blocks before sending packet-based distant gunshot audio. Keep this equal to normalRange for normal gameplay.").defineInRange("minDistantGunshotDistance", 300, 0, 10000);
        DISTANT_RANGE = builder.comment("Distance in blocks for quieter nearby distant gunshot pops.").defineInRange("distantRange", 1000, 32, 12000);
        DISTANT_FAR_RANGE = builder.comment("Distance in blocks for dedicated far pop sounds with small randomized delay.").defineInRange("farRange", 3000, 64, 16000);
        DISTANT_EXTREME_RANGE = builder.comment("Distance in blocks for very distant echo sounds with heavier delay and skip chance.").defineInRange("extremeRange", 7000, 128, 32000);
        MAX_DISTANT_GUNSHOT_DISTANCE = builder.comment("Maximum shooter-to-listener distance in blocks for packet-based distant gunshot audio.").defineInRange("maxDistantGunshotDistance", 7000, 128, 32000);
        DISTANT_GUNSHOT_VOLUME = builder.comment("Global volume multiplier for all distant gunshot sounds.").defineInRange("distantGunshotVolume", 1.0D, 0.0D, 4.0D);
        ENABLE_SOUND_DELAY = builder.comment("Delay distant gunshot playback by distance / 343.0 to simulate speed of sound.").define("enableSoundDelay", true);
        SUPPRESSOR_DISTANCE_MULTIPLIER = builder.comment("Multiplier applied to maximum distant gunshot range and volume for suppressed weapons. 0 disables distant gunshot packets for suppressed weapons.").defineInRange("suppressorDistanceMultiplier", 0.0D, 0.0D, 1.0D);
        ENABLE_BATTLE_DETECTION = builder.comment("Track shot bursts per chunk so far players can hear occasional active firefight bursts.").define("enableBattleDetection", true);
        BATTLE_THRESHOLD_SHOTS = builder.comment("Shots in the battle window needed to mark a chunk as an active firefight.").defineInRange("battleThresholdShots", 10, 1, 200);
        BATTLE_WINDOW_SECONDS = builder.comment("Time window in seconds for active firefight detection.").defineInRange("battleWindowSeconds", 5, 1, 60);
        DISTANT_VOLUME_MULTIPLIER = builder.comment("Volume multiplier for 300-1000 block distant gunshots.").defineInRange("distantVolumeMultiplier", 0.5D, 0.0D, 4.0D);
        FAR_VOLUME_MULTIPLIER = builder.comment("Volume multiplier for 1000-3000 block distant gunshots.").defineInRange("farVolumeMultiplier", 0.25D, 0.0D, 4.0D);
        EXTREME_VOLUME_MULTIPLIER = builder.comment("Volume multiplier for 3000-7000 block distant echoes.").defineInRange("extremeVolumeMultiplier", 0.1D, 0.0D, 4.0D);
        ENABLE_RANDOM_ECHOES = builder.comment("Enable randomized delays, occasional skip chance, and battle burst variation.").define("enableRandomEchoes", true);
        MAX_DISTANT_GUNSHOT_SOUNDS_PER_SECOND = builder.comment("Maximum distant gunshot sounds sent/played per player per second. Higher values let automatic fire overlap more naturally at range.").defineInRange("maxSoundsPerSecond", 18, 0, 64);
        USE_VANILLA_SOUNDS_FALLBACK = builder.comment("Compatibility flag only. Must remain false: distant gunshots never fall back to Minecraft vanilla sounds.").define("useVanillaSoundsFallback", false);
        DISTANT_GUNSHOT_DEBUG_LOGGING = builder.comment("Log distant gunshot dispatch details for troubleshooting. Can also be toggled with /taczbreach distantgunshots debug.").define("debugDistantGunshots", true);
        builder.pop();

        builder.push("weapon_hud");
        SHOW_WEAPON_CONDITION_HUD = builder.comment("Show the client-side 'Weapon Condition: x%' HUD text while holding worn weapons.").define("showWeaponConditionHud", true);
        builder.pop();

        builder.push("superb_warfare");
        SUPERB_WARFARE_SUPPORT = builder.define("superbWarfareSupport", true);
        SUPERB_WARFARE_DOORS = builder.define("superbWarfareDoorBreaching", true);
        SUPERB_WARFARE_TRAPDOORS = builder.define("superbWarfareTrapdoorBreaching", true);
        SUPERB_WARFARE_GLASS = builder.define("superbWarfareGlassDamage", true);
        SUPERB_WARFARE_IMPACTS = builder.define("superbWarfareImpactEffects", true);
        SUPERB_WARFARE_EXPLOSIVES = builder.define("superbWarfareExplosiveBreaching", true);
        SUPERB_WARFARE_DAMAGE_MULTIPLIER = builder.defineInRange("superbWarfareBreachingDamageMultiplier", 1.0D, 0.0D, 100.0D);
        builder.pop();

        SPEC = builder.build();
    }

    private static List<String> defaultHpOverrides() {
        return List.of(
                "#minecraft:wooden_doors=24",
                "#minecraft:wooden_trapdoors=16",
                "minecraft:iron_door=140",
                "minecraft:iron_trapdoor=100",
                "minecraft:glass=16",
                "minecraft:glass_pane=8",
                "minecraft:tinted_glass=24"
        );
    }

    private static boolean isString(Object value) {
        return value instanceof String;
    }

    private BallisticBlockConfig() {
    }
}
