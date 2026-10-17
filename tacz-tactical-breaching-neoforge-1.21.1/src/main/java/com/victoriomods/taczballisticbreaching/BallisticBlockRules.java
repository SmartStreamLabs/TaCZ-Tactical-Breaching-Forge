package com.victoriomods.taczballisticbreaching;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.TintedGlassBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

final class BallisticBlockRules {
    private BallisticBlockRules() {
    }

    static boolean isDamageable(BlockState state) {
        if (state.isAir() || matchesBlockList(state, BallisticBlockConfig.BLOCK_BLACKLIST.get())) {
            return false;
        }
        if (BallisticBlockConfig.USE_WHITELIST.get()) {
            return matchesBlockList(state, BallisticBlockConfig.BLOCK_WHITELIST.get());
        }
        return isDoorLike(state) || isGlassTarget(state) || matchesBlockList(state, BallisticBlockConfig.BLOCK_WHITELIST.get());
    }

    static boolean isGlassTarget(BlockState state) {
        return getGlassKind(state) != GlassKind.NONE;
    }

    static double getMaxHp(BlockState state) {
        OptionalDouble override = findConfiguredDouble(state, BallisticBlockConfig.BLOCK_HP_OVERRIDES.get());
        if (override.isPresent()) {
            return Math.max(override.getAsDouble(), 1.0D);
        }

        GlassKind glassKind = getGlassKind(state);
        if (glassKind == GlassKind.TINTED) {
            return BallisticBlockConfig.TINTED_GLASS_HP.get();
        }
        if (glassKind == GlassKind.PANE) {
            return BallisticBlockConfig.GLASS_PANE_HP.get();
        }
        if (glassKind == GlassKind.BLOCK) {
            return BallisticBlockConfig.GLASS_BLOCK_HP.get();
        }

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = id == null ? "" : id.getPath().toLowerCase(Locale.ROOT);

        if (state.is(Blocks.IRON_DOOR)) {
            return BallisticBlockConfig.IRON_DOOR_HP.get();
        }
        if (state.is(Blocks.IRON_TRAPDOOR)) {
            return BallisticBlockConfig.IRON_TRAPDOOR_HP.get();
        }
        if (state.is(net.minecraft.tags.BlockTags.WOODEN_DOORS)) {
            return BallisticBlockConfig.WOODEN_DOOR_HP.get();
        }
        if (state.is(net.minecraft.tags.BlockTags.WOODEN_TRAPDOORS)) {
            return BallisticBlockConfig.WOODEN_TRAPDOOR_HP.get();
        }
        if (path.contains("glass")) {
            return BallisticBlockConfig.GLASS_DOOR_HP.get();
        }
        return BallisticBlockConfig.OTHER_SUPPORTED_BLOCK_HP.get();
    }

    static double getConfiguredMultiplier(ResourceLocation id, List<? extends String> entries, double fallback) {
        if (id == null) {
            return fallback;
        }
        for (String entry : entries) {
            ParsedAssignment parsed = ParsedAssignment.parse(entry);
            if (parsed == null || parsed.tag()) {
                continue;
            }
            ResourceLocation configuredId = ResourceLocation.tryParse(parsed.key());
            if (id.equals(configuredId)) {
                return parsed.value();
            }
        }
        return fallback;
    }

    private static boolean isDoorLike(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof DoorBlock || block instanceof TrapDoorBlock) {
            return true;
        }
        if (state.is(net.minecraft.tags.BlockTags.DOORS) || state.is(net.minecraft.tags.BlockTags.TRAPDOORS)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase(Locale.ROOT);
        return path.contains("door") || path.contains("trapdoor") || path.contains("glass_door") || path.contains("glass_trapdoor");
    }

    private static GlassKind getGlassKind(BlockState state) {
        Block block = state.getBlock();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        String path = id == null ? "" : id.getPath().toLowerCase(Locale.ROOT);

        if (block instanceof TintedGlassBlock || state.is(Blocks.TINTED_GLASS)) {
            return GlassKind.TINTED;
        }
        if (block instanceof StainedGlassPaneBlock || path.equals("glass_pane") || path.endsWith("_glass_pane")) {
            return GlassKind.PANE;
        }
        if (block instanceof StainedGlassBlock || path.equals("glass") || path.endsWith("_stained_glass")) {
            return GlassKind.BLOCK;
        }
        return GlassKind.NONE;
    }

    private static OptionalDouble findConfiguredDouble(BlockState state, List<? extends String> entries) {
        for (String entry : entries) {
            ParsedAssignment parsed = ParsedAssignment.parse(entry);
            if (parsed == null) {
                continue;
            }
            if (parsed.matches(state)) {
                return OptionalDouble.of(parsed.value());
            }
        }
        return OptionalDouble.empty();
    }

    private static boolean matchesBlockList(BlockState state, List<? extends String> entries) {
        for (String entry : entries) {
            ParsedTarget target = ParsedTarget.parse(entry);
            if (target != null && target.matches(state)) {
                return true;
            }
        }
        return false;
    }

    private enum GlassKind {
        NONE,
        BLOCK,
        PANE,
        TINTED
    }

    private record ParsedTarget(boolean tag, String key) {
        static ParsedTarget parse(String raw) {
            if (raw == null) {
                return null;
            }
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            boolean isTag = trimmed.startsWith("#");
            String key = isTag ? trimmed.substring(1) : trimmed;
            return key.isEmpty() ? null : new ParsedTarget(isTag, key);
        }

        boolean matches(BlockState state) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id == null) {
                return false;
            }
            if (tag) {
                return state.is(TagKey.create(Registries.BLOCK, id));
            }
            return state.is(BuiltInRegistries.BLOCK.get(id));
        }
    }

    private record ParsedAssignment(boolean tag, String key, double value) {
        static ParsedAssignment parse(String raw) {
            if (raw == null) {
                return null;
            }
            String[] parts = raw.trim().split("=", 2);
            if (parts.length != 2) {
                return null;
            }
            ParsedTarget target = ParsedTarget.parse(parts[0]);
            if (target == null) {
                return null;
            }
            try {
                double value = Double.parseDouble(parts[1].trim());
                if (!Double.isFinite(value)) {
                    return null;
                }
                return new ParsedAssignment(target.tag(), target.key(), value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        boolean matches(BlockState state) {
            return new ParsedTarget(tag, key).matches(state);
        }
    }
}
