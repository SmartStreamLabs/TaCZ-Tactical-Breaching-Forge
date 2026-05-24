package com.victoriomods.taczballisticbreaching;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class BallisticBlockConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue WOODEN_DOOR_HP;
    public static final ForgeConfigSpec.DoubleValue WOODEN_TRAPDOOR_HP;
    public static final ForgeConfigSpec.DoubleValue IRON_DOOR_HP;
    public static final ForgeConfigSpec.DoubleValue IRON_TRAPDOOR_HP;
    public static final ForgeConfigSpec.DoubleValue GLASS_BLOCK_HP;
    public static final ForgeConfigSpec.DoubleValue GLASS_PANE_HP;
    public static final ForgeConfigSpec.DoubleValue TINTED_GLASS_HP;
    public static final ForgeConfigSpec.DoubleValue GLASS_DOOR_HP;
    public static final ForgeConfigSpec.DoubleValue OTHER_SUPPORTED_BLOCK_HP;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCK_HP_OVERRIDES;

    public static final ForgeConfigSpec.DoubleValue DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> GUN_DAMAGE_MULTIPLIERS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> AMMO_DAMAGE_MULTIPLIERS;
    public static final ForgeConfigSpec.DoubleValue SHOTGUN_CLOSE_RANGE;
    public static final ForgeConfigSpec.DoubleValue SHOTGUN_CLOSE_RANGE_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue GLASS_PASSTHROUGH_ENABLED;
    public static final ForgeConfigSpec.DoubleValue GLASS_PASSTHROUGH_DAMAGE_MULTIPLIER;

    public static final ForgeConfigSpec.BooleanValue USE_WHITELIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCK_WHITELIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCK_BLACKLIST;
    public static final ForgeConfigSpec.BooleanValue DROP_ITEMS;
    public static final ForgeConfigSpec.IntValue DAMAGE_TIMEOUT_TICKS;
    public static final ForgeConfigSpec.BooleanValue SHOW_CRACK_PROGRESS;
    public static final ForgeConfigSpec.BooleanValue SHOW_IMPACT_PARTICLES;
    public static final ForgeConfigSpec.BooleanValue PLAY_IMPACT_SOUND;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SHELL_CASINGS;
    public static final ForgeConfigSpec.IntValue CASING_LIFETIME;
    public static final ForgeConfigSpec.IntValue MAX_NEARBY_CASINGS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CASING_SOUNDS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_IMPACT_SMOKE;
    public static final ForgeConfigSpec.IntValue IMPACT_SMOKE_AMOUNT;
    public static final ForgeConfigSpec.DoubleValue IMPACT_SMOKE_SIZE;
    public static final ForgeConfigSpec.IntValue IMPACT_SMOKE_LIFETIME_TICKS;
    public static final ForgeConfigSpec.BooleanValue IMPACT_SMOKE_ONLY_ON_HARD_BLOCKS;
    public static final ForgeConfigSpec.IntValue MAX_IMPACT_PARTICLES_PER_SECOND;
    public static final ForgeConfigSpec.BooleanValue ENABLE_METAL_SPARKS;
    public static final ForgeConfigSpec.IntValue METAL_SPARK_AMOUNT;
    public static final ForgeConfigSpec.IntValue METAL_SPARK_LIFETIME_TICKS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_METAL_RICOCHET_SOUND;
    public static final ForgeConfigSpec.BooleanValue ENABLE_WOOD_SPLINTERS;
    public static final ForgeConfigSpec.IntValue WOOD_SPLINTER_AMOUNT;
    public static final ForgeConfigSpec.IntValue WOOD_SPLINTER_LIFETIME_TICKS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_WOOD_IMPACT_SOUND;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BULLET_DECALS;
    public static final ForgeConfigSpec.IntValue MAX_DECALS_PER_CHUNK;
    public static final ForgeConfigSpec.IntValue BULLET_DECAL_LIFETIME_SECONDS;
    public static final ForgeConfigSpec.DoubleValue BULLET_DECAL_SIZE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_MATERIAL_SPECIFIC_DECALS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_RICOCHET_SOUNDS;
    public static final ForgeConfigSpec.DoubleValue RICOCHET_SOUND_CHANCE_METAL;
    public static final ForgeConfigSpec.DoubleValue RICOCHET_SOUND_CHANCE_HARD_BLOCK;
    public static final ForgeConfigSpec.DoubleValue RICOCHET_VOLUME;
    public static final ForgeConfigSpec.IntValue RICOCHET_MAX_SOUNDS_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue RICOCHET_MAX_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_MUZZLE_SMOKE;
    public static final ForgeConfigSpec.IntValue MUZZLE_SMOKE_AMOUNT;
    public static final ForgeConfigSpec.IntValue MUZZLE_SMOKE_LIFETIME_TICKS;
    public static final ForgeConfigSpec.DoubleValue MUZZLE_SMOKE_SCALE;
    public static final ForgeConfigSpec.BooleanValue DYNAMIC_SMOKE_BUILDUP;
    public static final ForgeConfigSpec.IntValue MAX_SMOKE_PARTICLES_PER_SECOND;
    public static final ForgeConfigSpec.BooleanValue ENABLE_INDOOR_SMOKE;
    public static final ForgeConfigSpec.DoubleValue INDOOR_SMOKE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SMOKE_DISSIPATION_SPEED_INDOOR;
    public static final ForgeConfigSpec.DoubleValue SMOKE_DISSIPATION_SPEED_OUTDOOR;
    public static final ForgeConfigSpec.DoubleValue MAX_INDOOR_SMOKE_DENSITY;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FLASHLIGHT_SMOKE_INTERACTION;
    public static final ForgeConfigSpec.BooleanValue ENABLE_LASER_SMOKE_VISIBILITY;
    public static final ForgeConfigSpec.BooleanValue ENABLE_GLASS_SHARDS;
    public static final ForgeConfigSpec.IntValue GLASS_SHARD_AMOUNT;
    public static final ForgeConfigSpec.IntValue GLASS_SHARD_LIFETIME_SECONDS;
    public static final ForgeConfigSpec.BooleanValue GLASS_SHARD_FADE_OUT;
    public static final ForgeConfigSpec.IntValue MAX_GLASS_SHARDS_PER_CHUNK;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SUPPRESSION_SOUNDS;
    public static final ForgeConfigSpec.DoubleValue SUPPRESSION_RADIUS;
    public static final ForgeConfigSpec.DoubleValue SUPPRESSION_VOLUME;
    public static final ForgeConfigSpec.IntValue SUPPRESSION_MAX_SOUNDS_PER_SECOND;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SHOOTER_SUPPRESSION;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DYNAMIC_GUN_LIGHTING;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SPARK_LIGHTING;
    public static final ForgeConfigSpec.DoubleValue MUZZLE_FLASH_LIGHT_INTENSITY;
    public static final ForgeConfigSpec.DoubleValue SPARK_LIGHT_INTENSITY;
    public static final ForgeConfigSpec.IntValue DYNAMIC_LIGHT_RANGE;
    public static final ForgeConfigSpec.BooleanValue REDUCED_LIGHTING_FOR_SUPPRESSORS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SCREEN_DUST;
    public static final ForgeConfigSpec.DoubleValue SCREEN_DUST_RADIUS;
    public static final ForgeConfigSpec.DoubleValue SCREEN_DUST_INTENSITY;
    public static final ForgeConfigSpec.IntValue SCREEN_DUST_DURATION_TICKS;
    public static final ForgeConfigSpec.IntValue SCREEN_DUST_MAX_TRIGGERS_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue SCREEN_DUST_INDOOR_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue ENABLE_WEAPON_HEAT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BARREL_SMOKE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_HEAT_DISTORTION;
    public static final ForgeConfigSpec.BooleanValue ENABLE_HOT_BARREL_GLOW;
    public static final ForgeConfigSpec.DoubleValue WEAPON_COOLING_SPEED;
    public static final ForgeConfigSpec.IntValue MAX_HEAT_LEVEL;
    public static final ForgeConfigSpec.DoubleValue HEAT_DISTORTION_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue BARREL_GLOW_INTENSITY;
    public static final ForgeConfigSpec.BooleanValue ENABLE_MATERIAL_PENETRATION;
    public static final ForgeConfigSpec.DoubleValue MAX_PENETRATION_DEPTH;
    public static final ForgeConfigSpec.DoubleValue WOOD_PENETRATION_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue STONE_PENETRATION_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue METAL_PENETRATION_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue REINFORCED_PENETRATION_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_LOSS_PER_BLOCK;
    public static final ForgeConfigSpec.DoubleValue VELOCITY_LOSS_PER_BLOCK;
    public static final ForgeConfigSpec.BooleanValue ENABLE_EXIT_DECALS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_PENETRATION_PARTICLES;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CAMERA_SHAKE;
    public static final ForgeConfigSpec.DoubleValue CAMERA_SHAKE_INTENSITY;
    public static final ForgeConfigSpec.IntValue CAMERA_SHAKE_MAX_DISTANCE;
    public static final ForgeConfigSpec.IntValue CAMERA_SHAKE_DURATION_TICKS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_EXPLOSION_SHAKE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SHOTGUN_SHAKE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SUPPRESSION_SHAKE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BREACHING_SHAKE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ARMOR_HIT_EFFECTS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ARMOR_RICOCHET;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ARMOR_SPARKS;
    public static final ForgeConfigSpec.DoubleValue ARMOR_RICOCHET_CHANCE;
    public static final ForgeConfigSpec.IntValue ARMOR_SPARK_AMOUNT;
    public static final ForgeConfigSpec.DoubleValue ARMOR_HIT_VOLUME;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BLOOD_MIST;
    public static final ForgeConfigSpec.BooleanValue ENABLE_WALL_SPLATTER;
    public static final ForgeConfigSpec.DoubleValue BLOOD_MIST_INTENSITY;
    public static final ForgeConfigSpec.DoubleValue BLOOD_SPLATTER_CHANCE;
    public static final ForgeConfigSpec.IntValue BLOOD_DECAL_LIFETIME_SECONDS;
    public static final ForgeConfigSpec.IntValue MAX_BLOOD_DECALS_PER_CHUNK;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DYNAMIC_IMPACT_SOUNDS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_INDOOR_REVERB;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DISTANCE_MUFFLING;
    public static final ForgeConfigSpec.IntValue IMPACT_SOUND_MAX_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue IMPACT_SOUND_VOLUME;
    public static final ForgeConfigSpec.BooleanValue DYNAMIC_IMPACT_VARIATION;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BULLET_CRACK_ECHO;
    public static final ForgeConfigSpec.BooleanValue ENABLE_OUTDOOR_ECHO;
    public static final ForgeConfigSpec.DoubleValue URBAN_ECHO_INTENSITY;
    public static final ForgeConfigSpec.DoubleValue INDOOR_REVERB_INTENSITY;
    public static final ForgeConfigSpec.DoubleValue VALLEY_ECHO_INTENSITY;
    public static final ForgeConfigSpec.DoubleValue FOREST_ECHO_REDUCTION;
    public static final ForgeConfigSpec.IntValue MAX_ECHO_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SUPPRESSION_BLUR;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SUPPRESSION_RINGING;
    public static final ForgeConfigSpec.BooleanValue ENABLE_STRESS_EFFECTS;
    public static final ForgeConfigSpec.DoubleValue SUPPRESSION_BLUR_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue SUPPRESSION_EFFECT_RADIUS;
    public static final ForgeConfigSpec.IntValue EAR_RINGING_DURATION_TICKS;
    public static final ForgeConfigSpec.IntValue MAX_SUPPRESSION_EFFECTS_PER_SECOND;
    public static final ForgeConfigSpec.BooleanValue ENABLE_PHYSICAL_DEBRIS;
    public static final ForgeConfigSpec.IntValue DEBRIS_LIFETIME_SECONDS;
    public static final ForgeConfigSpec.IntValue MAX_DEBRIS_PER_CHUNK;
    public static final ForgeConfigSpec.IntValue WOOD_DEBRIS_AMOUNT;
    public static final ForgeConfigSpec.IntValue GLASS_DEBRIS_AMOUNT;
    public static final ForgeConfigSpec.IntValue METAL_DEBRIS_AMOUNT;
    public static final ForgeConfigSpec.IntValue CONCRETE_DEBRIS_AMOUNT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SHOCKWAVE_DUST;
    public static final ForgeConfigSpec.BooleanValue ENABLE_INDOOR_PRESSURE_EFFECT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DEBRIS_FALLOFF;
    public static final ForgeConfigSpec.DoubleValue SHOCKWAVE_DUST_INTENSITY;
    public static final ForgeConfigSpec.DoubleValue PRESSURE_EFFECT_STRENGTH;
    public static final ForgeConfigSpec.IntValue DUST_LIFETIME_TICKS;
    public static final ForgeConfigSpec.IntValue MAX_SHOCKWAVE_PARTICLES;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BURN_MARKS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_EXPLOSION_SCORCH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BREACH_SCORCH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_HEAT_RESIDUE;
    public static final ForgeConfigSpec.IntValue BURN_MARK_LIFETIME_SECONDS;
    public static final ForgeConfigSpec.IntValue MAX_BURN_MARKS_PER_CHUNK;
    public static final ForgeConfigSpec.DoubleValue BURN_MARK_OPACITY;
    public static final ForgeConfigSpec.BooleanValue ENABLE_NIGHTSTICK_FLASHLIGHT;
    public static final ForgeConfigSpec.DoubleValue FLASHLIGHT_INTENSITY;
    public static final ForgeConfigSpec.IntValue FLASHLIGHT_RANGE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_NIGHTSTICK_DYNAMIC_LIGHTING;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FRAG_GRENADE;
    public static final ForgeConfigSpec.IntValue FRAG_GRENADE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue FRAG_FUSE_TIME_TICKS;
    public static final ForgeConfigSpec.DoubleValue FRAG_EXPLOSION_RADIUS;
    public static final ForgeConfigSpec.DoubleValue FRAG_DAMAGE;
    public static final ForgeConfigSpec.IntValue FRAG_COUNT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FRAGMENT_LINE_OF_SIGHT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FRAG_BLOCK_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FRAG_SMOKE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FRAG_DEBRIS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FRAG_INDOOR_PRESSURE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FRAG_CEILING_DUST;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FRAG_EAR_RINGING;
    public static final ForgeConfigSpec.IntValue FRAG_EXPLOSION_SOUND_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_EXPLOSION_ECHO;
    public static final ForgeConfigSpec.BooleanValue ENABLE_INDOOR_EXPLOSION_REVERB;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FLASHBANG;
    public static final ForgeConfigSpec.IntValue FLASHBANG_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue FLASHBANG_FUSE_TICKS;
    public static final ForgeConfigSpec.DoubleValue FLASHBANG_RADIUS;
    public static final ForgeConfigSpec.IntValue FLASH_DURATION_TICKS;
    public static final ForgeConfigSpec.IntValue FLASHBANG_TINNITUS_DURATION_TICKS;
    public static final ForgeConfigSpec.DoubleValue FLASHBANG_BLUR_STRENGTH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FLASHBANG_LINE_OF_SIGHT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FLASHBANG_INDOOR_REVERB;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

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
        PLAY_IMPACT_SOUND = builder.comment("Play the target block's hit sound when it is damaged.").define("play_impact_sound", true);
        builder.pop();

        builder.push("shell_casings");
        ENABLE_SHELL_CASINGS = builder.comment("Spawn visual shell casings when TaCZ guns fire.").define("enableShellCasings", true);
        CASING_LIFETIME = builder.comment("How long shell casings remain in ticks. 20 ticks = 1 second.").defineInRange("casingLifetime", 160, 20, 2400);
        MAX_NEARBY_CASINGS = builder.comment("Maximum casings allowed near a shooter before new casings are skipped.").defineInRange("maxNearbyCasings", 80, 0, 1000);
        ENABLE_CASING_SOUNDS = builder.comment("Play small metal impact sounds when casings hit blocks.").define("enableCasingSounds", true);
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
        MAX_DECALS_PER_CHUNK = builder.comment("Maximum client-side bullet decals stored per chunk. Oldest decals are removed first.").defineInRange("maxDecalsPerChunk", 100, 0, 1000);
        BULLET_DECAL_LIFETIME_SECONDS = builder.comment("How long bullet decals remain visible on clients, in seconds.").defineInRange("bulletDecalLifetimeSeconds", 300, 1, 3600);
        BULLET_DECAL_SIZE = builder.comment("Base visual size for bullet decals, in blocks.").defineInRange("bulletDecalSize", 0.22D, 0.03D, 2.0D);
        ENABLE_MATERIAL_SPECIFIC_DECALS = builder.comment("Use material-specific decal colors and crack/chip styles.").define("enableMaterialSpecificDecals", true);
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
        MUZZLE_SMOKE_AMOUNT = builder.comment("Base muzzle smoke particles per shot. Campfire smoke is dense, so defaults are intentionally low.").defineInRange("muzzleSmokeAmount", 2, 0, 64);
        MUZZLE_SMOKE_LIFETIME_TICKS = builder.comment("Base muzzle smoke lifetime in client ticks.").defineInRange("muzzleSmokeLifetimeTicks", 10, 1, 100);
        MUZZLE_SMOKE_SCALE = builder.comment("Base visual scale for muzzle smoke particles.").defineInRange("muzzleSmokeScale", 0.22D, 0.05D, 2.0D);
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
        ENABLE_SUPPRESSION_SOUNDS = builder.comment("Play client-side bullet flyby/suppression sounds when TaCZ bullets pass near players.").define("enableSuppressionSounds", true);
        SUPPRESSION_RADIUS = builder.comment("Radius in blocks around the local player's head/body that can trigger flyby sounds.").defineInRange("suppressionRadius", 3.0D, 0.25D, 16.0D);
        SUPPRESSION_VOLUME = builder.comment("Base suppression/flyby sound volume.").defineInRange("suppressionVolume", 1.0D, 0.0D, 4.0D);
        SUPPRESSION_MAX_SOUNDS_PER_SECOND = builder.comment("Client-side limiter for suppression sounds.").defineInRange("suppressionMaxSoundsPerSecond", 10, 0, 128);
        ENABLE_SHOOTER_SUPPRESSION = builder.comment("When true, the shooter can hear flyby suppression from their own bullets.").define("enableShooterSuppression", false);
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
        ENABLE_SUPPRESSION_SHAKE = builder.comment("Enable sharp jolt shake from very close sniper/high caliber flybys.").define("enableSuppressionShake", true);
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
        ENABLE_WALL_SPLATTER = builder.comment("Render small fading client-side blood splatter decals on nearby surfaces behind hit targets.").define("enableWallSplatter", true);
        BLOOD_MIST_INTENSITY = builder.comment("Global intensity multiplier for subtle blood mist. Keep low for restrained tactical visuals.").defineInRange("bloodMistIntensity", 0.35D, 0.0D, 2.0D);
        BLOOD_SPLATTER_CHANCE = builder.comment("Chance that a living entity bullet hit creates a small wall/floor splatter behind the target.").defineInRange("bloodSplatterChance", 0.25D, 0.0D, 1.0D);
        BLOOD_DECAL_LIFETIME_SECONDS = builder.comment("How long blood splatter decals remain visible on clients, in seconds.").defineInRange("bloodDecalLifetimeSeconds", 180, 1, 1800);
        MAX_BLOOD_DECALS_PER_CHUNK = builder.comment("Maximum client-side blood splatter decals stored per chunk. Oldest decals are removed first.").defineInRange("maxBloodDecalsPerChunk", 40, 0, 500);
        builder.pop();

        builder.push("dynamic_impact_sounds");
        ENABLE_DYNAMIC_IMPACT_SOUNDS = builder.comment("Enable context-aware client-side bullet impact sounds for material, distance, caliber, and indoor/outdoor space.").define("enableDynamicImpactSounds", true);
        ENABLE_INDOOR_REVERB = builder.comment("Add short local echo/reverb layers for impacts in enclosed indoor spaces.").define("enableIndoorReverb", true);
        ENABLE_DISTANCE_MUFFLING = builder.comment("Lower volume and pitch for distant impacts to simulate muffling.").define("enableDistanceMuffling", true);
        IMPACT_SOUND_MAX_DISTANCE = builder.comment("Maximum distance in blocks for dynamic impact sounds.").defineInRange("impactSoundMaxDistance", 96, 4, 256);
        IMPACT_SOUND_VOLUME = builder.comment("Global dynamic impact sound volume multiplier.").defineInRange("impactSoundVolume", 1.0D, 0.0D, 4.0D);
        DYNAMIC_IMPACT_VARIATION = builder.comment("Randomize impact sound variants, pitch, volume, and rare cinematic ricochet cracks.").define("dynamicImpactVariation", true);
        builder.pop();

        builder.push("gunshot_acoustics");
        ENABLE_BULLET_CRACK_ECHO = builder.comment("Play client-side bullet crack echoes and environmental gunshot acoustic tails for TaCZ shots.").define("enableBulletCrackEcho", true);
        ENABLE_OUTDOOR_ECHO = builder.comment("Allow outdoor echo tails in open, urban, valley, and forest environments.").define("enableOutdoorEcho", true);
        URBAN_ECHO_INTENSITY = builder.comment("Echo intensity multiplier for dense urban/building areas.").defineInRange("urbanEchoIntensity", 1.0D, 0.0D, 5.0D);
        INDOOR_REVERB_INTENSITY = builder.comment("Reverb intensity multiplier for indoor gunshots and sniper cracks.").defineInRange("indoorReverbIntensity", 1.2D, 0.0D, 5.0D);
        VALLEY_ECHO_INTENSITY = builder.comment("Echo intensity multiplier for open valley/mountain-like terrain.").defineInRange("valleyEchoIntensity", 1.4D, 0.0D, 5.0D);
        FOREST_ECHO_REDUCTION = builder.comment("Multiplier applied to outdoor echoes in forests to dampen high-frequency crack tails.").defineInRange("forestEchoReduction", 0.65D, 0.0D, 1.0D);
        MAX_ECHO_DISTANCE = builder.comment("Maximum distance in blocks for client-side environmental gunshot echoes.").defineInRange("maxEchoDistance", 256, 16, 512);
        builder.pop();

        builder.push("suppression_effects");
        ENABLE_SUPPRESSION_BLUR = builder.comment("Render a very subtle edge-only suppression vignette/blur hint during near bullet flybys. Accessibility: set false to disable visual blur.").define("enableSuppressionBlur", true);
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
        FRAG_FUSE_TIME_TICKS = builder.comment("Fuse time for thrown frag grenades in ticks. 20 ticks = 1 second.").defineInRange("fragFuseTimeTicks", 60, 5, 400);
        FRAG_EXPLOSION_RADIUS = builder.comment("Visual and damage radius for frag grenade explosions, in blocks. Code enforces at least 12 blocks for frag grenades.").defineInRange("fragExplosionRadius", 12.0D, 1.0D, 24.0D);
        FRAG_DAMAGE = builder.comment("Base fragmentation damage before distance falloff and armor mitigation. Code enforces at least 80 when enabled.").defineInRange("fragDamage", 80.0D, 0.0D, 200.0D);
        FRAG_COUNT = builder.comment("Number of invisible fragment rays checked when the grenade detonates. Code enforces at least 256 when enabled.").defineInRange("fragCount", 256, 0, 512);
        ENABLE_FRAGMENT_LINE_OF_SIGHT = builder.comment("When true, blocks between fragments and targets block or reduce fragment hits.").define("enableFragmentLineOfSight", true);
        ENABLE_FRAG_BLOCK_DAMAGE = builder.comment("Allow frag grenade explosions to damage/break vanilla blocks. Disabled by default for tactical balance.").define("enableBlockDamage", false);
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
        FLASHBANG_FUSE_TICKS = builder.comment("Fuse time for thrown flashbang grenades in ticks. 20 ticks = 1 second.").defineInRange("flashbangFuseTicks", 40, 5, 400);
        FLASHBANG_RADIUS = builder.comment("Radius in blocks for flash, tinnitus, blur, and pressure effects.").defineInRange("flashbangRadius", 12.0D, 1.0D, 48.0D);
        FLASH_DURATION_TICKS = builder.comment("Duration of the white flash visual effect in ticks.").defineInRange("flashDurationTicks", 60, 1, 240);
        FLASHBANG_TINNITUS_DURATION_TICKS = builder.comment("Duration of close flashbang tinnitus/ringing in ticks.").defineInRange("tinnitusDurationTicks", 100, 1, 400);
        FLASHBANG_BLUR_STRENGTH = builder.comment("Maximum subtle blur/vignette strength caused by flashbangs.").defineInRange("blurStrength", 0.25D, 0.0D, 1.0D);
        ENABLE_FLASHBANG_LINE_OF_SIGHT = builder.comment("When true, solid walls block flashbang blinding effects. Glass reduces the effect instead of fully blocking it.").define("enableLineOfSightCheck", true);
        ENABLE_FLASHBANG_INDOOR_REVERB = builder.comment("Enable stronger indoor flashbang detonation reverb and pressure feeling.").define("enableIndoorReverb", true);
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
