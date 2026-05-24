package com.victoriomods.taczballisticbreaching;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class NightstickFlashlightLightHandler {
    private static final Map<UUID, LightState> STATES = new HashMap<>();
    private static final Set<BlockPos> PLACED_LIGHTS = new HashSet<>();
    private static final int UPDATE_INTERVAL_TICKS = 2;

    private NightstickFlashlightLightHandler() {
    }

    static void setEnabled(ServerPlayer player, boolean enabled) {
        if (!BallisticBlockConfig.ENABLE_NIGHTSTICK_FLASHLIGHT.get() || !BallisticBlockConfig.ENABLE_NIGHTSTICK_DYNAMIC_LIGHTING.get()) {
            enabled = false;
        }
        LightState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new LightState());
        state.enabled = enabled && NightstickAttachmentUtil.hasNightstick(player);
        if (!state.enabled) {
            clearPlayer(player.serverLevel(), player.getUUID(), state);
        }
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        LightState state = STATES.get(player.getUUID());
        if (state == null || !state.enabled) {
            return;
        }
        if (!BallisticBlockConfig.ENABLE_NIGHTSTICK_FLASHLIGHT.get() || !BallisticBlockConfig.ENABLE_NIGHTSTICK_DYNAMIC_LIGHTING.get() || !NightstickAttachmentUtil.hasNightstick(player)) {
            state.enabled = false;
            clearPlayer(player.serverLevel(), player.getUUID(), state);
            return;
        }
        if (player.tickCount % UPDATE_INTERVAL_TICKS != 0) {
            return;
        }
        updateLights(player, state);
    }

    @SubscribeEvent
    public static void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cleanupAllPlacedLights(player.serverLevel());
            cleanupNearbyStrayLights(player);
            STATES.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LightState state = STATES.remove(player.getUUID());
            if (state != null) {
                clearPlayer(player.serverLevel(), player.getUUID(), state);
            }
        }
    }

    @SubscribeEvent
    public static void levelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            cleanupAllPlacedLights(level);
            STATES.clear();
        }
    }

    private static void updateLights(ServerPlayer player, LightState state) {
        ServerLevel level = player.serverLevel();
        List<BlockPos> next = computeLightPositions(player);
        Set<BlockPos> nextSet = new HashSet<>(next);
        for (BlockPos old : state.positions) {
            if (!nextSet.contains(old)) {
                removeIfUnused(level, player.getUUID(), old);
            }
        }
        state.positions.clear();
        state.positions.addAll(next);
        for (int i = 0; i < next.size(); i++) {
            BlockPos pos = next.get(i);
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState current = level.getBlockState(pos);
            if (!current.isAir() && !current.is(Blocks.LIGHT)) {
                continue;
            }
            int light = Math.max(8, 15 - i * 2);
            BlockState lightState = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, light);
            if (!current.is(Blocks.LIGHT) || current.getValue(LightBlock.LEVEL) != light) {
                level.setBlock(pos, lightState, 3);
            }
            PLACED_LIGHTS.add(pos.immutable());
        }
    }

    private static List<BlockPos> computeLightPositions(ServerPlayer player) {
        List<BlockPos> positions = new ArrayList<>();
        Vec3 forward = player.getLookAngle();
        forward = forward.lengthSqr() < 0.001D ? new Vec3(0.0D, 0.0D, 1.0D) : forward.normalize();
        Vec3 origin = player.getEyePosition().add(forward.scale(0.65D)).add(0.0D, -0.12D, 0.0D);
        double range = BallisticBlockConfig.FLASHLIGHT_RANGE.get();
        BlockHitResult hit = player.level().clip(new ClipContext(origin, origin.add(forward.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double length = hit.getType() == HitResult.Type.BLOCK ? Math.max(1.0D, origin.distanceTo(hit.getLocation()) - 0.35D) : range;
        double step = 3.0D;
        for (double d = 1.8D; d <= length; d += step) {
            BlockPos pos = BlockPos.containing(origin.add(forward.scale(d)));
            if (!positions.contains(pos)) {
                positions.add(pos);
            }
            if (positions.size() >= 6) {
                break;
            }
        }
        return positions;
    }

    private static void clearPlayer(ServerLevel level, UUID playerId, LightState state) {
        for (BlockPos pos : state.positions) {
            removeIfUnused(level, playerId, pos);
        }
        state.positions.clear();
    }

    private static void removeIfUnused(ServerLevel level, UUID owner, BlockPos pos) {
        if (isUsedByOther(owner, pos)) {
            return;
        }
        if (level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.LIGHT)) {
            level.removeBlock(pos, false);
        }
        PLACED_LIGHTS.remove(pos);
    }

    private static boolean isUsedByOther(UUID owner, BlockPos pos) {
        for (Map.Entry<UUID, LightState> entry : STATES.entrySet()) {
            if (!entry.getKey().equals(owner) && entry.getValue().positions.contains(pos)) {
                return true;
            }
        }
        return false;
    }

    private static void cleanupAllPlacedLights(ServerLevel level) {
        for (BlockPos pos : new ArrayList<>(PLACED_LIGHTS)) {
            if (level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.LIGHT)) {
                level.removeBlock(pos, false);
            }
            PLACED_LIGHTS.remove(pos);
        }
    }

    private static void cleanupNearbyStrayLights(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        int radius = Math.max(10, BallisticBlockConfig.FLASHLIGHT_RANGE.get() + 4);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -radius; z <= radius; z++) {
                    mutable.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (level.hasChunkAt(mutable) && level.getBlockState(mutable).is(Blocks.LIGHT)) {
                        level.removeBlock(mutable, false);
                    }
                }
            }
        }
    }

    private static final class LightState {
        private boolean enabled;
        private final List<BlockPos> positions = new ArrayList<>();
    }
}
