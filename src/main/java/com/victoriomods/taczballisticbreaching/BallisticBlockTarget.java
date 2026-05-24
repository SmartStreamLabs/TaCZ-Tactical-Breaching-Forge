package com.victoriomods.taczballisticbreaching;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

record BallisticBlockTarget(BlockPos damagePos, BlockState damageState, BlockPos hitPos, BlockState hitState) {
    static BallisticBlockTarget fromHit(Level level, BlockPos hitPos, BlockState hitState) {
        BlockPos damagePos = hitPos;
        BlockState damageState = hitState;

        if (hitState.getBlock() instanceof DoorBlock && hitState.hasProperty(DoorBlock.HALF) && hitState.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            BlockPos lowerPos = hitPos.below();
            BlockState lowerState = level.getBlockState(lowerPos);
            if (lowerState.getBlock() == hitState.getBlock()) {
                damagePos = lowerPos;
                damageState = lowerState;
            }
        }

        return new BallisticBlockTarget(damagePos.immutable(), damageState, hitPos.immutable(), hitState);
    }
}
