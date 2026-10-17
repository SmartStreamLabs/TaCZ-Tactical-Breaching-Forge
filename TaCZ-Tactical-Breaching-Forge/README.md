# TaCZ Tactical Breaching

Forge 1.20.1 addon for Timeless & Classics Guns: Zero.

Bullets from TaCZ damage supported doors, trapdoors, glass blocks, glass panes, and configured door-like blocks on the server. Damage is stored temporarily per block, with impact particles, sounds, crack progress, and natural block breaking when HP reaches zero.

## Configuration

After first launch, edit `config/tacz_tactical_breaching-common.toml`.

- `block_hp_overrides`: per-block or per-tag HP entries such as `minecraft:oak_door=24` or `#minecraft:wooden_doors=24`.
- `glass_block_hp`, `glass_pane_hp`, `tinted_glass_hp`: default HP for glass targets.
- `damage_multiplier`: global bullet-to-block damage multiplier.
- `gun_damage_multipliers`: per TaCZ gun ID multipliers.
- `ammo_damage_multipliers`: per TaCZ ammo/caliber ID multipliers.
- `glass_passthrough_enabled`: lets bullets continue through glass after impact.
- `glass_passthrough_damage_multiplier`: cumulative damage multiplier applied to a bullet after glass passthrough.
- `enableShellCasings`, `casingLifetime`, `maxNearbyCasings`, `enableCasingSounds`: casing ejection performance and sound controls.
- `enableImpactSmoke`, `impactSmokeAmount`, `impactSmokeSize`, `impactSmokeLifetimeTicks`, `impactSmokeOnlyOnHardBlocks`: subtle bullet impact smoke.
- `enableMetalSparks`, `metalSparkAmount`, `metalSparkLifetimeTicks`, `enableMetalRicochetSound`: metal impact sparks and ping sounds.
- `enableWoodSplinters`, `woodSplinterAmount`, `woodSplinterLifetimeTicks`, `enableWoodImpactSound`: wooden impact splinters, dust, and sharp crack sounds.
- `enableBulletDecals`, `maxDecalsPerChunk`, `bulletDecalLifetimeSeconds`, `bulletDecalSize`, `enableMaterialSpecificDecals`: persistent client-side bullet holes and material-specific impact decals.
- `enableRicochetSounds`, `ricochetSoundChanceMetal`, `ricochetSoundChanceHardBlock`, `ricochetVolume`, `ricochetMaxSoundsPerSecond`, `ricochetMaxDistance`: client-side bullet ping/chip ricochet sound controls.
- `enableMuzzleSmoke`, `muzzleSmokeAmount`, `muzzleSmokeLifetimeTicks`, `muzzleSmokeScale`, `dynamicSmokeBuildup`, `maxSmokeParticlesPerSecond`: client-side muzzle smoke after firing.
- `enableIndoorSmoke`, `indoorSmokeMultiplier`, `smokeDissipationSpeedIndoor`, `smokeDissipationSpeedOutdoor`, `maxIndoorSmokeDensity`, `enableFlashlightSmokeInteraction`, `enableLaserSmokeVisibility`: client-side room smoke accumulation, lingering haze, spreading, and light interaction indoors.
- `enableGlassShards`, `glassShardAmount`, `glassShardLifetimeSeconds`, `glassShardFadeOut`, `maxGlassShardsPerChunk`: temporary client-side glass shard debris.
- `enableSuppressionSounds`, `suppressionRadius`, `suppressionVolume`, `suppressionMaxSoundsPerSecond`, `enableShooterSuppression`: client-side bullet flyby/suppression sounds near the player.
- `enableDynamicGunLighting`, `enableSparkLighting`, `muzzleFlashLightIntensity`, `sparkLightIntensity`, `dynamicLightRange`, `reducedLightingForSuppressors`: client-side tactical muzzle/spark/breaching light flashes.
- `enableScreenDust`, `screenDustRadius`, `screenDustIntensity`, `screenDustDurationTicks`, `screenDustMaxTriggersPerSecond`, `screenDustIndoorMultiplier`: subtle screen-edge dust/debris from nearby bullet impacts.
- `enableWeaponHeat`, `enableBarrelSmoke`, `enableHeatDistortion`, `enableHotBarrelGlow`, `weaponCoolingSpeed`, `maxHeatLevel`, `heatDistortionStrength`, `barrelGlowIntensity`: client-side sustained-fire heat, hot barrel smoke, shimmer, glow, and cooling ticks.
- `enableMaterialPenetration`, `maxPenetrationDepth`, material penetration multipliers, `damageLossPerBlock`, `velocityLossPerBlock`, `enableExitDecals`, `enablePenetrationParticles`: server-side material penetration with client-side exit visuals.
- `enableCameraShake`, `cameraShakeIntensity`, `cameraShakeMaxDistance`, `cameraShakeDurationTicks`, `enableExplosionShake`, `enableShotgunShake`, `enableSuppressionShake`, `enableBreachingShake`: subtle client-side camera shake for nearby heavy shots, flybys, explosions, and breaching events.
- `enableArmorHitEffects`, `enableArmorRicochet`, `enableArmorSparks`, `armorRicochetChance`, `armorSparkAmount`, `armorHitVolume`: client-side armor hit sparks/smoke/sounds plus optional weak-caliber ricochet mitigation.
- `enableBloodMist`, `enableWallSplatter`, `bloodMistIntensity`, `bloodSplatterChance`, `bloodDecalLifetimeSeconds`, `maxBloodDecalsPerChunk`: subtle client-side entity hit mist and restrained fading wall/floor splatter decals.
- `enableDynamicImpactSounds`, `enableIndoorReverb`, `enableDistanceMuffling`, `impactSoundMaxDistance`, `impactSoundVolume`, `dynamicImpactVariation`: context-aware client-side impact audio for material, distance, caliber, armor, and indoor/outdoor space.
- `enableBulletCrackEcho`, `enableOutdoorEcho`, `urbanEchoIntensity`, `indoorReverbIntensity`, `valleyEchoIntensity`, `forestEchoReduction`, `maxEchoDistance`: client-side supersonic crack echoes and environmental gunshot acoustics for indoor, urban, valley, forest, and open areas.
- `enableSuppressionBlur`, `enableSuppressionRinging`, `enableStressEffects`, `suppressionBlurStrength`, `suppressionEffectRadius`, `earRingingDurationTicks`, `maxSuppressionEffectsPerSecond`: subtle client-only suppression vignette, stress tint, and controlled ringing.
- `enablePhysicalDebris`, `debrisLifetimeSeconds`, `maxDebrisPerChunk`, `woodDebrisAmount`, `glassDebrisAmount`, `metalDebrisAmount`, `concreteDebrisAmount`: client-side physical debris chunks for destroyed doors, glass, metal, and stone/concrete targets.
- `enableShockwaveDust`, `enableIndoorPressureEffect`, `enableDebrisFalloff`, `shockwaveDustIntensity`, `pressureEffectStrength`, `dustLifetimeTicks`, `maxShockwaveParticles`: client-side indoor explosion shockwave dust, pressure feedback, falling ceiling debris, and temporary haze.
- `enableBurnMarks`, `enableExplosionScorch`, `enableBreachScorch`, `enableHeatResidue`, `burnMarkLifetimeSeconds`, `maxBurnMarksPerChunk`, `burnMarkOpacity`: client-side scorch/burn decals from explosions, breaching, and sustained heavy muzzle heat.
- `enableNightstickFlashlight`, `flashlightIntensity`, `flashlightRange`, `enableDynamicLighting`: client-side tactical flashlight beam for the TaCZ Nightstick Compact Laser attachment, with smoke/haze illumination and explosion flicker.
- `enableFragGrenade`, `fragFuseTimeTicks`, `fragExplosionRadius`, `fragDamage`, `fragCount`, `enableFragmentLineOfSight`, `enableBlockDamage`, `enableFragSmoke`, `enableFragDebris`, `enableIndoorPressure`, `enableCeilingDust`, `enableEarRinging`: throwable 3D frag grenade, fragmentation damage, fuse, and tactical explosion feedback. Item id: `tacz_tactical_breaching:frag_grenade`.
- `maxImpactParticlesPerSecond`: client-side limiter for smoke, dust, sparks, and tiny glass shards.
- `use_whitelist`, `block_whitelist`, `block_blacklist`: restrict or exclude blocks.
- `drop_items`: whether broken blocks drop items.

TaCZ is a required dependency. If it is missing, Forge reports the missing dependency instead of loading this addon.

## Credits

- Frag grenade visual model: "V40 Mini-Grenade (Low-Poly)" by GoldbergR, licensed under Creative Commons Attribution 4.0. Source: https://skfb.ly/pLnGY
- Flashbang visual model: "Low-Poly Flashbang" by William_In_3D, licensed under Creative Commons Attribution 4.0. Source: https://skfb.ly/o6zHR
