package com.victoriomods.taczballisticbreaching;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;

enum ImpactMaterial {
    STONE(0),
    WOOD(1),
    GLASS(2),
    METAL(3),
    GENERIC(4),
    DIRT(5);

    private final int id;

    ImpactMaterial(int id) {
        this.id = id;
    }

    int id() {
        return id;
    }

    static ImpactMaterial byId(int id) {
        for (ImpactMaterial material : values()) {
            if (material.id == id) {
                return material;
            }
        }
        return GENERIC;
    }

    static ImpactMaterial classify(BlockState state) {
        if (BallisticBlockRules.isGlassTarget(state)) {
            return GLASS;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = id == null ? "" : id.getPath().toLowerCase(Locale.ROOT);
        SoundType sound = state.getSoundType();

        if (isVanillaMetal(state) || sound == SoundType.METAL || sound == SoundType.COPPER || path.contains("iron") || path.contains("steel") || path.contains("metal") || path.contains("copper") || path.contains("vault") || path.contains("bars") || path.contains("barrier_door") || path.contains("grate") || path.contains("anvil")) {
            return METAL;
        }
        if (state.is(BlockTags.PLANKS)
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.WOODEN_DOORS)
                || state.is(BlockTags.WOODEN_TRAPDOORS)
                || state.is(BlockTags.FENCES)
                || sound == SoundType.WOOD
                || sound == SoundType.BAMBOO_WOOD
                || sound == SoundType.CHERRY_WOOD
                || path.contains("wood")
                || path.contains("plank")
                || path.contains("log")
                || path.contains("crate")
                || path.contains("barrel")
                || path.contains("fence")) {
            return WOOD;
        }
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE) || sound == SoundType.STONE || path.contains("stone") || path.contains("concrete") || path.contains("brick") || path.contains("deepslate")) {
            return STONE;
        }
        if (sound == SoundType.GRAVEL
                || sound == SoundType.SAND
                || path.contains("dirt")
                || path.contains("sand")
                || path.contains("gravel")
                || path.contains("mud")
                || path.contains("clay")
                || path.contains("soil")) {
            return DIRT;
        }
        return GENERIC;
    }

    private static boolean isVanillaMetal(BlockState state) {
        return state.is(Blocks.IRON_DOOR)
                || state.is(Blocks.IRON_TRAPDOOR)
                || state.is(Blocks.IRON_BLOCK)
                || state.is(Blocks.RAW_IRON_BLOCK)
                || state.is(Blocks.COPPER_BLOCK)
                || state.is(Blocks.EXPOSED_COPPER)
                || state.is(Blocks.WEATHERED_COPPER)
                || state.is(Blocks.OXIDIZED_COPPER)
                || state.is(Blocks.IRON_BARS)
                || state.is(Blocks.CHAIN)
                || state.is(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE)
                || state.is(Blocks.HOPPER)
                || state.is(Blocks.CAULDRON)
                || state.is(Blocks.ANVIL)
                || state.is(Blocks.CHIPPED_ANVIL)
                || state.is(Blocks.DAMAGED_ANVIL);
    }

    static boolean isHardEnoughForSmoke(Level level, BlockPos pos, BlockState state) {
        ImpactMaterial material = classify(state);
        if (material == STONE || material == METAL) {
            return true;
        }
        float destroySpeed = state.getDestroySpeed(level, pos);
        return destroySpeed < 0.0F || destroySpeed >= 1.5F;
    }
}
