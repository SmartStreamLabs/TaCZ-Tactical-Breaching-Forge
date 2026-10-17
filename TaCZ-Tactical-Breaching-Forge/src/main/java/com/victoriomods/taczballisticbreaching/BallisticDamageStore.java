package com.victoriomods.taczballisticbreaching;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

final class BallisticDamageStore {
    private static final Map<DamageKey, DamageEntry> DAMAGE = new HashMap<>();

    private BallisticDamageStore() {
    }

    static double applyDamage(ServerLevel level, BlockPos pos, Block block, double maxHp, double damage, long gameTime) {
        DamageKey key = DamageKey.of(level.dimension(), pos);
        DamageEntry entry = DAMAGE.get(key);
        if (entry == null || entry.block() != block) {
            entry = new DamageEntry(block, maxHp, 0.0D, gameTime, progressId(key));
        }

        double totalDamage = Math.min(maxHp, entry.damage() + Math.max(damage, 0.0D));
        DAMAGE.put(key, new DamageEntry(block, maxHp, totalDamage, gameTime, entry.progressId()));
        return Math.max(maxHp - totalDamage, 0.0D);
    }

    static void clear(ServerLevel level, BlockPos pos) {
        DamageKey key = DamageKey.of(level.dimension(), pos);
        DamageEntry removed = DAMAGE.remove(key);
        if (removed != null) {
            clearProgress(level, pos, removed);
        }
    }

    static void tick(ServerLevel level, long gameTime) {
        int timeout = BallisticBlockConfig.DAMAGE_TIMEOUT_TICKS.get();
        ResourceLocation dimension = level.dimension().location();
        Iterator<Map.Entry<DamageKey, DamageEntry>> iterator = DAMAGE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<DamageKey, DamageEntry> mapEntry = iterator.next();
            DamageKey key = mapEntry.getKey();
            DamageEntry entry = mapEntry.getValue();
            if (!key.dimension().equals(dimension)) {
                continue;
            }
            boolean expired = gameTime - entry.lastHitGameTime() > timeout;
            boolean changed = level.getBlockState(key.pos()).getBlock() != entry.block();
            if (expired || changed) {
                iterator.remove();
                clearProgress(level, key.pos(), entry);
            }
        }
    }

    static void clearAll() {
        DAMAGE.clear();
    }

    static void updateProgress(ServerLevel level, BlockPos pos, double remainingHp, double maxHp) {
        if (!BallisticBlockConfig.SHOW_CRACK_PROGRESS.get()) {
            return;
        }
        DamageKey key = DamageKey.of(level.dimension(), pos);
        DamageEntry entry = DAMAGE.get(key);
        if (entry == null) {
            return;
        }
        double brokenRatio = 1.0D - Math.max(remainingHp, 0.0D) / Math.max(maxHp, 1.0D);
        int stage = Math.min(9, Math.max(0, (int) Math.floor(brokenRatio * 10.0D)));
        level.destroyBlockProgress(entry.progressId(), pos, stage);
    }

    private static void clearProgress(ServerLevel level, BlockPos pos, DamageEntry entry) {
        if (BallisticBlockConfig.SHOW_CRACK_PROGRESS.get()) {
            level.destroyBlockProgress(entry.progressId(), pos, -1);
        }
    }

    private static int progressId(DamageKey key) {
        return Objects.hash(TaczTacticalBreaching.MOD_ID, key.dimension(), key.pos());
    }

    private record DamageKey(ResourceLocation dimension, BlockPos pos) {
        static DamageKey of(ResourceKey<Level> dimension, BlockPos pos) {
            return new DamageKey(dimension.location(), pos.immutable());
        }
    }

    private record DamageEntry(Block block, double maxHp, double damage, long lastHitGameTime, int progressId) {
    }
}
