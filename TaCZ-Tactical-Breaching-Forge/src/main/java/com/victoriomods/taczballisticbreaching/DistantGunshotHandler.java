package com.victoriomods.taczballisticbreaching;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class DistantGunshotHandler {
    private static final double SPEED_OF_SOUND_BLOCKS_PER_SECOND = 343.0D;
    private static final double SUPPRESSED_DISTANT_EXTRA_RANGE_CAP = 260.0D;
    private static final Map<BattleKey, ShotWindow> BATTLE_CACHE = new HashMap<>();
    private static final Map<UUID, PlayerSoundWindow> PLAYER_LIMITERS = new HashMap<>();
    private static final Map<UUID, Long> RECENT_DISPATCH_TICKS = new HashMap<>();
    private static boolean commandDebugLogging;

    private DistantGunshotHandler() {
    }

    @SubscribeEvent
    public static void onGunFire(GunFireEvent event) {
        handleGunEvent("GunFireEvent", event.getShooter(), event.getGunItemStack(), event.getLogicalSide());
    }

    @SubscribeEvent
    public static void onGunShoot(GunShootEvent event) {
        handleGunEvent("GunShootEvent", event.getShooter(), event.getGunItemStack(), event.getLogicalSide());
    }

    static void onSuperbShot(net.minecraft.world.entity.LivingEntity shooter, ItemStack gunStack, boolean suppressed) {
        if (!(shooter.level() instanceof ServerLevel)) {
            return;
        }
        ResourceLocation gunId = BuiltInRegistries.ITEM.getKey(gunStack.getItem());
        WeaponInfo base = fromGunId(gunId, gunStack);
        handleGunEvent("SuperbWarfareShootEvent", shooter, gunStack, LogicalSide.SERVER,
                new WeaponInfo(base.kind(), base.automatic(), suppressed, base.highCaliber()));
    }

    private static void handleGunEvent(String hookName, net.minecraft.world.entity.LivingEntity shooter, ItemStack gunStack, LogicalSide side) {
        handleGunEvent(hookName, shooter, gunStack, side, null);
    }

    private static void handleGunEvent(String hookName, net.minecraft.world.entity.LivingEntity shooter, ItemStack gunStack, LogicalSide side, WeaponInfo externalInfo) {
        if (side != LogicalSide.SERVER) {
            return;
        }
        if (!(shooter.level() instanceof ServerLevel level)) {
            return;
        }

        DistantGunshotAssetVerifier.verifyOnce();
        Vec3 shotPosition = shooter.position();
        String shooterName = shooter.getName().getString();
        String gunItemId = gunItemId(gunStack);
        TaczTacticalBreaching.LOGGER.info("[DistantGunshots] Shot detected shooter={} gunItemId={} shotPosition={} {} {} hook={}", shooterName, gunItemId, format(shotPosition.x), format(shotPosition.y), format(shotPosition.z), hookName);
        if (isDuplicateSameTick(shooter, level)) {
            TaczTacticalBreaching.LOGGER.info("[DistantGunshots] packet sent no reason=duplicate_same_tick hook={} shooter={}", hookName, shooterName);
            return;
        }
        if (!BallisticBlockConfig.ENABLE_DISTANT_GUNSHOTS.get()) {
            TaczTacticalBreaching.LOGGER.info("[DistantGunshots] packet sent no reason=disabled_in_config shooter={}", shooterName);
            return;
        }

        long now = System.currentTimeMillis();
        WeaponInfo info = externalInfo == null ? inspectGun(gunStack) : externalInfo;
        if (info.suppressed && suppressorDistanceMultiplier() <= 0.0D) {
            TaczTacticalBreaching.LOGGER.info("[DistantGunshots] packet sent no reason=suppressed_weapon_disabled shooter={} gunItemId={}", shooterName, gunItemId);
            return;
        }
        boolean activeBattle = recordBattleShot(level, shotPosition, now);
        cleanupBattleCache(now);

        double configuredNormalRange = Math.max(BallisticBlockConfig.DISTANT_NORMAL_RANGE.get(), BallisticBlockConfig.MIN_DISTANT_GUNSHOT_DISTANCE.get());
        double normalRange = Math.max(0.0D, configuredNormalRange);
        if (debugEnabled()) {
            normalRange = Math.min(normalRange, 20.0D);
        }
        double distantRange = Math.max(normalRange + 1.0D, BallisticBlockConfig.DISTANT_RANGE.get());
        double farRange = Math.max(distantRange + 1.0D, BallisticBlockConfig.DISTANT_FAR_RANGE.get());
        double maxDistance = effectiveMaxDistance(normalRange, info);
        TaczTacticalBreaching.LOGGER.info("[DistantGunshots] Config minDistance={} maxDistance={} volume={} delayEnabled={} debug={} suppressed={} suppressorMultiplier={}", format(normalRange), format(maxDistance), format(BallisticBlockConfig.DISTANT_GUNSHOT_VOLUME.get()), BallisticBlockConfig.ENABLE_SOUND_DELAY.get(), debugEnabled(), info.suppressed, format(suppressorDistanceMultiplier()));

        for (ServerPlayer listener : level.players()) {
            double distanceSqr = listener.distanceToSqr(shotPosition);
            double distance = Math.sqrt(distanceSqr);
            String listenerName = listener.getGameProfile().getName();
            TaczTacticalBreaching.LOGGER.info("[DistantGunshots] Listener {} distance={}", listenerName, format(distance));
            if (distance < normalRange) {
                TaczTacticalBreaching.LOGGER.info("[DistantGunshots] listener={} distance={} chosenSound=none packet sent no reason=below_min_distance minDistance={}", listenerName, format(distance), format(normalRange));
                continue;
            }
            if (distance > maxDistance) {
                TaczTacticalBreaching.LOGGER.info("[DistantGunshots] listener={} distance={} chosenSound=none packet sent no reason=above_max_distance maxDistance={}", listenerName, format(distance), format(maxDistance));
                continue;
            }
            DistantGunshotPacket packet = packetFor(level, shotPosition, info, distance, normalRange, distantRange, farRange, maxDistance, false, false, soundGunId(gunStack));
            if (packet != null && send(listener, packet)) {
                TaczTacticalBreaching.LOGGER.info("[DistantGunshots] listener={} distance={} chosenSound={} packet sent yes delayMs={} volume={} gunItemId={}", listenerName, format(distance), DistantGunshotSoundType.byId(packet.soundType()), packet.delayMillis(), format(packet.volume()), gunItemId);
                debug(level, "shot={} listener={} distance={} delayMs={} sound={} battle={}", shotPosition, listenerName, Mth.floor(distance), packet.delayMillis(), DistantGunshotSoundType.byId(packet.soundType()), activeBattle);
            } else if (packet == null) {
                TaczTacticalBreaching.LOGGER.info("[DistantGunshots] listener={} distance={} chosenSound=none packet sent no reason=packet_not_built", listenerName, format(distance));
            } else {
                TaczTacticalBreaching.LOGGER.info("[DistantGunshots] listener={} distance={} chosenSound={} packet sent no reason=sound_limiter", listenerName, format(distance), DistantGunshotSoundType.byId(packet.soundType()));
            }
            if (activeBattle && BallisticBlockConfig.ENABLE_BATTLE_DETECTION.get() && distance >= distantRange && level.random.nextFloat() < 0.22F) {
                DistantGunshotPacket burst = packetFor(level, shotPosition, info.asBattleBurst(), distance, normalRange, distantRange, farRange, maxDistance, true, false, soundGunId(gunStack));
                if (burst != null) {
                    send(listener, burst);
                }
            }
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("taczbreach")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("distantgunshots")
                        .then(Commands.literal("reload")
                                .executes(context -> {
                                    BATTLE_CACHE.clear();
                                    context.getSource().sendSuccess(() -> Component.literal("Distant gunshot runtime cache reloaded. Config values are read live."), true);
                                    return 1;
                                }))
                        .then(Commands.literal("debug")
                                .executes(context -> {
                                    commandDebugLogging = !commandDebugLogging;
                                    context.getSource().sendSuccess(() -> Component.literal("Distant gunshot debug logging: " + (debugEnabled() ? "enabled" : "disabled")), true);
                                    return 1;
                                }))
                        .then(Commands.literal("test")
                                .executes(context -> runTest(context.getSource())))));
    }

    static void clearRuntimeState() {
        BATTLE_CACHE.clear();
        PLAYER_LIMITERS.clear();
        RECENT_DISPATCH_TICKS.clear();
        commandDebugLogging = false;
    }

    private static int runTest(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        DistantGunshotAssetVerifier.verifyOnce();
        Vec3 direction = player.getLookAngle();
        if (direction.lengthSqr() < 0.0001D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 shotPosition = player.getEyePosition().add(direction.normalize().scale(32.0D));
        TestSound[] tests = {
                new TestSound("300", DistantGunshotSoundType.NEAR_ECHO, 0),
                new TestSound("1000", DistantGunshotSoundType.DISTANT_ECHO, 1000),
                new TestSound("3000", DistantGunshotSoundType.FAR_ECHO, 2000),
                new TestSound("7000", DistantGunshotSoundType.EXTREME_ECHO, 3000)
        };
        for (TestSound test : tests) {
            DistantGunshotPacket packet = new DistantGunshotPacket(shotPosition.x, shotPosition.y, shotPosition.z, test.type.id(), 3.0F, 1.0F, test.delayMillis, 1, 80, "");
            sendDirect(player, packet);
            TaczTacticalBreaching.LOGGER.info("[DistantGunshots] test command packet sent yes listener={} distanceLabel={} chosenSound={} originalShotPosition={} {} {} volume=3.00 delayMs={}", player.getGameProfile().getName(), test.distanceLabel, test.type, format(shotPosition.x), format(shotPosition.y), format(shotPosition.z), test.delayMillis);
        }
        source.sendSuccess(() -> Component.literal("Playing distant gunshot test sounds: 300, 1000, 3000, 7000 blocks."), false);
        if (DistantGunshotAssetVerifier.hasMissingAssets()) {
            source.sendFailure(Component.literal("Missing distant gunshot sound asset. Check latest.log."));
        }
        return tests.length;
    }

    private static DistantGunshotPacket packetFor(ServerLevel level, Vec3 shotPosition, WeaponInfo info, double distance, double normalRange, double distantRange, double farRange, double maxDistance, boolean battleBurst, boolean forcePlay, String gunId) {
        Band band = Band.forDistance(distance, distantRange, farRange, maxDistance);
        if (band == Band.NONE) {
            return null;
        }
        if (!forcePlay && band == Band.EXTREME && BallisticBlockConfig.ENABLE_RANDOM_ECHOES.get() && level.random.nextFloat() < 0.30F) {
            return null;
        }

        DistantGunshotSoundType type = selectSound(info, band, distance, normalRange, distantRange, farRange, maxDistance);
        float volume = volumeFor(info, band, distance, normalRange, distantRange, farRange, maxDistance, battleBurst);
        if (volume <= 0.01F) {
            return null;
        }
        float pitch = pitchFor(level, info, band, battleBurst);
        int delay = delayFor(level, distance, band);
        int burstCount = battleBurst ? 3 + level.random.nextInt(info.automatic ? 5 : 3) : 1;
        int burstSpacing = burstCount > 1 ? 90 + level.random.nextInt(150) : 80;
        return new DistantGunshotPacket(shotPosition.x, shotPosition.y + 1.35D, shotPosition.z, type.id(), volume, pitch, delay, burstCount, burstSpacing, gunId);
    }

    private static String soundGunId(ItemStack stack) {
        IGun gun = IGun.getIGunOrNull(stack);
        ResourceLocation id = gun == null ? BuiltInRegistries.ITEM.getKey(stack.getItem()) : gun.getGunId(stack);
        return id == null ? "" : id.toString();
    }

    private static DistantGunshotSoundType selectSound(WeaponInfo info, Band band, double distance, double normalRange, double distantRange, double farRange, double maxDistance) {
        double nearBreak = (normalRange + distantRange) * 0.5D;
        double distantBreak = (distantRange + farRange) * 0.5D;
        double farBreak = (farRange + maxDistance) * 0.5D;
        if (distance < nearBreak) {
            return DistantGunshotSoundType.NEAR_ECHO;
        }
        if (distance < distantBreak) {
            return DistantGunshotSoundType.DISTANT_ECHO;
        }
        if (distance < farBreak) {
            return DistantGunshotSoundType.FAR_ECHO;
        }
        if (band == Band.EXTREME) {
            return DistantGunshotSoundType.EXTREME_ECHO;
        }
        if (band == Band.FAR) {
            return DistantGunshotSoundType.FAR_ECHO;
        }
        if (band == Band.DISTANT) {
            return DistantGunshotSoundType.DISTANT_ECHO;
        }
        if (info.automatic) {
            return DistantGunshotSoundType.MACHINEGUN_FAR;
        }
        return switch (info.kind) {
            case PISTOL -> band == Band.EXTREME ? DistantGunshotSoundType.RIFLE_ECHO : DistantGunshotSoundType.PISTOL_POP;
            case SHOTGUN -> DistantGunshotSoundType.SHOTGUN_ECHO;
            case SNIPER -> DistantGunshotSoundType.SNIPER_ECHO;
            case RIFLE -> band == Band.DISTANT ? DistantGunshotSoundType.RIFLE_POP : DistantGunshotSoundType.RIFLE_ECHO;
        };
    }

    private static float volumeFor(WeaponInfo info, Band band, double distance, double normalRange, double distantRange, double farRange, double extremeRange, boolean battleBurst) {
        double bandMultiplier = switch (band) {
            case DISTANT -> BallisticBlockConfig.DISTANT_VOLUME_MULTIPLIER.get();
            case FAR -> BallisticBlockConfig.FAR_VOLUME_MULTIPLIER.get();
            case EXTREME -> BallisticBlockConfig.EXTREME_VOLUME_MULTIPLIER.get();
            case NONE -> 0.0D;
        };
        double multiplier = BallisticBlockConfig.DISTANT_GUNSHOT_VOLUME.get() * bandMultiplier;
        double bandStart = switch (band) {
            case DISTANT -> normalRange;
            case FAR -> distantRange;
            case EXTREME -> farRange;
            case NONE -> distance;
        };
        double bandEnd = switch (band) {
            case DISTANT -> distantRange;
            case FAR -> farRange;
            case EXTREME -> extremeRange;
            case NONE -> distance + 1.0D;
        };
        double fade = 1.0D - Mth.clamp((distance - bandStart) / Math.max(1.0D, bandEnd - bandStart), 0.0D, 1.0D) * 0.65D;
        double weaponScale = switch (info.kind) {
            case PISTOL -> 0.62D;
            case RIFLE -> 0.92D;
            case SHOTGUN -> 0.82D;
            case SNIPER -> 1.35D;
        };
        if (info.highCaliber) {
            weaponScale *= 1.22D;
        }
        if (info.suppressed) {
            weaponScale *= suppressorDistanceMultiplier();
        }
        if (battleBurst) {
            weaponScale *= 0.72D;
        }
        return (float) Mth.clamp(multiplier * fade * weaponScale, 0.0D, 1.25D);
    }

    private static float pitchFor(ServerLevel level, WeaponInfo info, Band band, boolean battleBurst) {
        double pitch = switch (info.kind) {
            case PISTOL -> 1.08D;
            case RIFLE -> 1.0D;
            case SHOTGUN -> 0.92D;
            case SNIPER -> 0.88D;
        };
        if (info.automatic) {
            pitch = 1.02D;
        }
        if (band == Band.EXTREME) {
            pitch *= 0.97D;
        } else if (band == Band.FAR) {
            pitch *= 0.98D;
        }
        if (battleBurst) {
            pitch *= 0.99D;
        }
        pitch += level.random.nextDouble() * 0.16D - 0.08D;
        return (float) Mth.clamp(pitch, 0.85D, 1.15D);
    }

    private static int delayFor(ServerLevel level, double distance, Band band) {
        if (!BallisticBlockConfig.ENABLE_SOUND_DELAY.get()) {
            return 0;
        }
        double seconds = distance / SPEED_OF_SOUND_BLOCKS_PER_SECOND;
        int jitter = 0;
        if (BallisticBlockConfig.ENABLE_RANDOM_ECHOES.get()) {
            jitter = switch (band) {
                case DISTANT -> level.random.nextInt(301);
                case FAR -> 300 + level.random.nextInt(701);
                case EXTREME -> 1000 + level.random.nextInt(2001);
                case NONE -> 0;
            };
        }
        return Mth.clamp((int) Math.round(seconds * 1000.0D) + jitter, 0, 30000);
    }

    private static double effectiveMaxDistance(double normalRange, WeaponInfo info) {
        double maxDistance = Math.max(normalRange + 1.0D, BallisticBlockConfig.MAX_DISTANT_GUNSHOT_DISTANCE.get());
        if (info.suppressed) {
            double multiplier = suppressorDistanceMultiplier();
            if (multiplier <= 0.0D) {
                return normalRange;
            }
            maxDistance *= multiplier;
            maxDistance = Math.min(maxDistance, normalRange + SUPPRESSED_DISTANT_EXTRA_RANGE_CAP);
        }
        return Math.max(normalRange + 1.0D, maxDistance);
    }

    private static double suppressorDistanceMultiplier() {
        return Mth.clamp(BallisticBlockConfig.SUPPRESSOR_DISTANCE_MULTIPLIER.get(), 0.0D, 1.0D);
    }

    private static boolean isDuplicateSameTick(net.minecraft.world.entity.LivingEntity shooter, ServerLevel level) {
        UUID shooterId = shooter.getUUID();
        long gameTime = level.getGameTime();
        Long previousTick = RECENT_DISPATCH_TICKS.put(shooterId, gameTime);
        RECENT_DISPATCH_TICKS.entrySet().removeIf(entry -> gameTime - entry.getValue() > 100L);
        return previousTick != null && previousTick == gameTime;
    }

    private static boolean recordBattleShot(ServerLevel level, Vec3 shotPosition, long now) {
        if (!BallisticBlockConfig.ENABLE_BATTLE_DETECTION.get()) {
            return false;
        }
        BattleKey key = new BattleKey(level.dimension().location().toString(), Mth.floor(shotPosition.x) >> 4, Mth.floor(shotPosition.z) >> 4);
        ShotWindow window = BATTLE_CACHE.computeIfAbsent(key, ignored -> new ShotWindow());
        long cutoff = now - BallisticBlockConfig.BATTLE_WINDOW_SECONDS.get() * 1000L;
        window.shots.addLast(now);
        window.lastShotMillis = now;
        while (!window.shots.isEmpty() && window.shots.peekFirst() < cutoff) {
            window.shots.removeFirst();
        }
        if (window.shots.size() >= BallisticBlockConfig.BATTLE_THRESHOLD_SHOTS.get()) {
            window.activeUntilMillis = now + 10000L;
        }
        return now <= window.activeUntilMillis;
    }

    private static void cleanupBattleCache(long now) {
        long maxAge = Math.max(30000L, BallisticBlockConfig.BATTLE_WINDOW_SECONDS.get() * 3000L);
        Iterator<Map.Entry<BattleKey, ShotWindow>> iterator = BATTLE_CACHE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BattleKey, ShotWindow> entry = iterator.next();
            if (now - entry.getValue().lastShotMillis > maxAge) {
                iterator.remove();
            }
        }
    }

    private static boolean send(ServerPlayer player, DistantGunshotPacket packet) {
        long now = System.currentTimeMillis();
        cleanupPlayerLimiters(now);
        if (!reservePlayerSound(player, Math.max(1, packet.burstCount()), now)) {
            return false;
        }
        sendDirect(player, packet);
        return true;
    }

    private static void sendDirect(ServerPlayer player, DistantGunshotPacket packet) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private static boolean reservePlayerSound(ServerPlayer player, int count, long now) {
        int max = BallisticBlockConfig.MAX_DISTANT_GUNSHOT_SOUNDS_PER_SECOND.get();
        if (max <= 0) {
            return false;
        }
        PlayerSoundWindow window = PLAYER_LIMITERS.computeIfAbsent(player.getUUID(), ignored -> new PlayerSoundWindow());
        long second = now / 1000L;
        if (window.second != second) {
            window.second = second;
            window.sounds = 0;
        }
        window.lastSeenMillis = now;
        if (window.sounds + count > max) {
            return false;
        }
        window.sounds += count;
        return true;
    }

    private static void cleanupPlayerLimiters(long now) {
        PLAYER_LIMITERS.entrySet().removeIf(entry -> now - entry.getValue().lastSeenMillis > 5000L);
    }

    private static WeaponInfo inspectGun(ItemStack gunStack) {
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null) {
            return new WeaponInfo(MuzzleSmokeNetwork.WeaponKind.RIFLE, false, false, false);
        }
        ResourceLocation gunId = gun.getGunId(gunStack);
        try {
            return TimelessAPI.getCommonGunIndex(gunId)
                    .map(index -> fromGunIndex(index, gunId, gunStack))
                    .orElseGet(() -> fromGunId(gunId, gunStack));
        } catch (RuntimeException exception) {
            TaczTacticalBreaching.LOGGER.debug("Could not inspect TaCZ gun index {} for distant gunshots", gunId, exception);
            return fromGunId(gunId, gunStack);
        }
    }

    private static WeaponInfo fromGunIndex(CommonGunIndex index, ResourceLocation gunId, ItemStack gunStack) {
        String type = index.getType() == null ? "" : index.getType().toLowerCase(Locale.ROOT);
        ResourceLocation ammoId = index.getGunData().getAmmoId();
        String ammoPath = ammoId == null ? "" : ammoId.getPath().toLowerCase(Locale.ROOT);
        MuzzleSmokeNetwork.WeaponKind kind = MuzzleSmokeNetwork.WeaponKind.RIFLE;
        if (type.contains("shotgun") || index.getBulletData().getBulletAmount() > 1 || ammoPath.contains("12g")) {
            kind = MuzzleSmokeNetwork.WeaponKind.SHOTGUN;
        } else if (type.contains("pistol") || ammoPath.contains("9mm") || ammoPath.contains("45acp") || ammoPath.contains("357") || ammoPath.contains("50ae")) {
            kind = MuzzleSmokeNetwork.WeaponKind.PISTOL;
        } else if (containsAny(gunId, "m107", "m95", "sniper", "awm", "barrett") || ammoPath.contains("50") || ammoPath.contains("338")) {
            kind = MuzzleSmokeNetwork.WeaponKind.SNIPER;
        }
        boolean automatic = containsAny(gunId, "smg", "lmg", "mg", "ak", "m4", "m16", "scar", "g36", "vector", "minigun", "auto");
        boolean highCaliber = kind == MuzzleSmokeNetwork.WeaponKind.SHOTGUN || kind == MuzzleSmokeNetwork.WeaponKind.SNIPER || containsAny(gunId, "50", "bmg", "127", "338", "762", "308") || ammoPath.contains("50") || ammoPath.contains("338") || ammoPath.contains("762") || ammoPath.contains("308");
        return new WeaponInfo(kind, automatic, isSuppressed(gunId, gunStack), highCaliber);
    }

    private static WeaponInfo fromGunId(ResourceLocation gunId, ItemStack gunStack) {
        MuzzleSmokeNetwork.WeaponKind kind = MuzzleSmokeNetwork.WeaponKind.RIFLE;
        if (containsAny(gunId, "shotgun", "m870", "m1014", "spas", "aa12", "db_")) {
            kind = MuzzleSmokeNetwork.WeaponKind.SHOTGUN;
        } else if (containsAny(gunId, "glock", "pistol", "m1911", "m9", "p320", "cz75", "b93r", "deagle", "mk23", "rhino", "taurus")) {
            kind = MuzzleSmokeNetwork.WeaponKind.PISTOL;
        } else if (containsAny(gunId, "m107", "m95", "sniper", "awm", "barrett")) {
            kind = MuzzleSmokeNetwork.WeaponKind.SNIPER;
        }
        boolean automatic = containsAny(gunId, "smg", "lmg", "mg", "ak", "m4", "m16", "scar", "g36", "vector", "minigun", "auto");
        boolean highCaliber = kind == MuzzleSmokeNetwork.WeaponKind.SHOTGUN || kind == MuzzleSmokeNetwork.WeaponKind.SNIPER || containsAny(gunId, "50", "bmg", "127", "338", "762", "308");
        return new WeaponInfo(kind, automatic, isSuppressed(gunId, gunStack), highCaliber);
    }

    private static boolean isSuppressed(ResourceLocation gunId, ItemStack gunStack) {
        if (containsAny(gunId, "suppressed", "suppressor", "silencer", "_sd", "-sd", "sd_")) {
            return true;
        }
        ResourceLocation muzzleId = muzzleAttachmentId(gunStack);
        if (isSuppressorId(muzzleId)) {
            return true;
        }
        String tagText = gunStack.getTag() == null ? "" : gunStack.getTag().toString().toLowerCase(Locale.ROOT);
        return containsAnyText(tagText, "suppressor", "silencer", "muzzle_suppressor", "muzzle_silencer");
    }

    private static ResourceLocation muzzleAttachmentId(ItemStack gunStack) {
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null) {
            return null;
        }
        try {
            ResourceLocation id = gun.getAttachmentId(gunStack, AttachmentType.MUZZLE);
            if (id != null) {
                return id;
            }
            return gun.getBuiltInAttachmentId(gunStack, AttachmentType.MUZZLE);
        } catch (RuntimeException | LinkageError exception) {
            TaczTacticalBreaching.LOGGER.debug("Could not inspect TaCZ muzzle attachment for suppressor detection", exception);
            return null;
        }
    }

    private static boolean isSuppressorId(ResourceLocation muzzleId) {
        if (muzzleId == null) {
            return false;
        }
        String text = muzzleId.toString().toLowerCase(Locale.ROOT);
        return containsAnyText(text, "suppressor", "silencer", "suppressed", "muffler", "moderator", "muzzle_suppressor", "pbs", "nt4", "osprey", "omega", "_sd", "-sd", "/sd", "sd_");
    }

    private static boolean containsAny(ResourceLocation id, String... needles) {
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase(Locale.ROOT);
        for (String needle : needles) {
            if (path.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAnyText(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String gunItemId(ItemStack gunStack) {
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun != null) {
            return String.valueOf(gun.getGunId(gunStack));
        }
        return String.valueOf(BuiltInRegistries.ITEM.getKey(gunStack.getItem()));
    }

    private static boolean debugEnabled() {
        return commandDebugLogging || BallisticBlockConfig.DISTANT_GUNSHOT_DEBUG_LOGGING.get();
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static void debug(ServerLevel level, String message, Object... args) {
        if (debugEnabled()) {
            Object[] values = new Object[args.length + 1];
            values[0] = level.dimension().location();
            System.arraycopy(args, 0, values, 1, args.length);
            TaczTacticalBreaching.LOGGER.info("[DistantGunshots:{}] " + message, values);
        }
    }

    private enum Band {
        NONE,
        DISTANT,
        FAR,
        EXTREME;

        static Band forDistance(double distance, double distantRange, double farRange, double extremeRange) {
            if (distance <= distantRange) {
                return DISTANT;
            }
            if (distance <= farRange) {
                return FAR;
            }
            if (distance <= extremeRange) {
                return EXTREME;
            }
            return NONE;
        }
    }

    private record BattleKey(String dimension, int chunkX, int chunkZ) {
    }

    private static final class ShotWindow {
        private final ArrayDeque<Long> shots = new ArrayDeque<>();
        private long lastShotMillis;
        private long activeUntilMillis;
    }

    private static final class PlayerSoundWindow {
        private long second = -1L;
        private int sounds;
        private long lastSeenMillis;
    }

    private record WeaponInfo(MuzzleSmokeNetwork.WeaponKind kind, boolean automatic, boolean suppressed, boolean highCaliber) {
        private WeaponInfo asBattleBurst() {
            return new WeaponInfo(kind, true, suppressed, highCaliber);
        }
    }

    private record TestSound(String distanceLabel, DistantGunshotSoundType type, int delayMillis) {
    }

}
