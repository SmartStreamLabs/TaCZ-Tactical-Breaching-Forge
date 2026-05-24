package com.victoriomods.taczballisticbreaching;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;

final class ImpactFxNetwork {
    private static final double SEND_RADIUS = 64.0D;

    private ImpactFxNetwork() {
    }

    static void sendImpact(ServerLevel level, Vec3 location, Direction direction, BlockState state, BlockPos pos, boolean shotgunImpact, boolean highCaliberImpact, boolean armorPiercingImpact, boolean automaticImpact, boolean finalBreak) {
        sendImpact(level, location, direction, state, pos, shotgunImpact, highCaliberImpact, armorPiercingImpact, automaticImpact, finalBreak, false);
    }

    static void sendImpact(ServerLevel level, Vec3 location, Direction direction, BlockState state, BlockPos pos, boolean shotgunImpact, boolean highCaliberImpact, boolean armorPiercingImpact, boolean automaticImpact, boolean finalBreak, boolean penetrationExit) {
        ImpactMaterial material = ImpactMaterial.classify(state);
        boolean smokeEnabled = BallisticBlockConfig.ENABLE_IMPACT_SMOKE.get()
                && (!BallisticBlockConfig.IMPACT_SMOKE_ONLY_ON_HARD_BLOCKS.get() || ImpactMaterial.isHardEnoughForSmoke(level, pos, state));
        boolean woodEnabled = material == ImpactMaterial.WOOD && BallisticBlockConfig.ENABLE_WOOD_SPLINTERS.get();
        boolean metalEnabled = material == ImpactMaterial.METAL && BallisticBlockConfig.ENABLE_METAL_SPARKS.get();
        boolean glassShardsEnabled = material == ImpactMaterial.GLASS && BallisticBlockConfig.ENABLE_GLASS_SHARDS.get();
        boolean soundEnabled = (material == ImpactMaterial.WOOD && BallisticBlockConfig.ENABLE_WOOD_IMPACT_SOUND.get())
                || (material == ImpactMaterial.METAL && BallisticBlockConfig.ENABLE_METAL_RICOCHET_SOUND.get())
                || (BallisticBlockConfig.ENABLE_RICOCHET_SOUNDS.get() && (material == ImpactMaterial.METAL || material == ImpactMaterial.STONE));
        if (!BallisticBlockConfig.ENABLE_BULLET_DECALS.get() && !smokeEnabled && !woodEnabled && !metalEnabled && !glassShardsEnabled && !soundEnabled) {
            return;
        }

        ImpactFxPacket packet = new ImpactFxPacket(
                location.x,
                location.y,
                location.z,
                direction.getStepX(),
                direction.getStepY(),
                direction.getStepZ(),
                material.id(),
                isGlassPane(state),
                penetrationExit,
                shotgunImpact,
                highCaliberImpact,
                armorPiercingImpact,
                automaticImpact,
                finalBreak
        );
        ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(location.x, location.y, location.z, SEND_RADIUS, level.dimension())), packet);
    }

    private static boolean isGlassPane(BlockState state) {
        if (state.getBlock() instanceof StainedGlassPaneBlock) {
            return true;
        }
        var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        String path = id == null ? "" : id.getPath().toLowerCase(Locale.ROOT);
        return path.equals("glass_pane") || path.endsWith("_glass_pane");
    }
}
