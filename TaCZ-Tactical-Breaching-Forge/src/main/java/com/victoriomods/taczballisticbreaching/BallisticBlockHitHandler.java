package com.victoriomods.taczballisticbreaching;

import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BallisticBlockHitHandler {
    private BallisticBlockHitHandler() {
    }

    @SubscribeEvent
    public static void onAmmoHitBlock(AmmoHitBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockHitResult hitResult = event.getHitResult();
        BallisticBlockTarget target = BallisticBlockTarget.fromHit(level, hitResult.getBlockPos(), event.getState());
        boolean shotgunImpact = BulletDamageCalculator.isShotgunImpact(event.getAmmo(), hitResult.getLocation());
        boolean highCaliberImpact = BulletDamageCalculator.isHighCaliber(event.getAmmo());
        boolean armorPiercingImpact = BulletDamageCalculator.isArmorPiercing(event.getAmmo());
        boolean automaticImpact = BulletDamageCalculator.isAutomaticWeapon(event.getAmmo());
        if (!BallisticBlockRules.isDamageable(target.damageState())) {
            ImpactFxNetwork.sendImpact(level, hitResult.getLocation(), hitResult.getDirection(), target.hitState(), target.hitPos(), shotgunImpact, highCaliberImpact, armorPiercingImpact, automaticImpact, false);
            if (MaterialPenetration.tryPenetrate(level, event.getAmmo(), target.hitState(), target.hitPos(), hitResult.getLocation(), hitResult.getDirection(), shotgunImpact, highCaliberImpact, armorPiercingImpact, automaticImpact)) {
                event.setCanceled(true);
            }
            return;
        }

        boolean glassTarget = BallisticBlockRules.isGlassTarget(target.damageState());
        double maxHp = BallisticBlockRules.getMaxHp(target.damageState());
        double damage = BulletDamageCalculator.calculate(event.getAmmo(), hitResult.getLocation());
        if (damage <= 0.0D) {
            ImpactFxNetwork.sendImpact(level, hitResult.getLocation(), hitResult.getDirection(), target.hitState(), target.hitPos(), shotgunImpact, highCaliberImpact, armorPiercingImpact, automaticImpact, false);
            return;
        }

        double remainingHp = BallisticDamageStore.applyDamage(level, target.damagePos(), target.damageState().getBlock(), maxHp, damage, level.getGameTime());
        ImpactFxNetwork.sendImpact(level, hitResult.getLocation(), hitResult.getDirection(), target.hitState(), target.hitPos(), shotgunImpact, highCaliberImpact, armorPiercingImpact, automaticImpact, remainingHp <= 0.0D);
        playImpactEffects(level, target.hitPos(), target.hitState(), hitResult.getLocation(), glassTarget);
        boolean penetrated = !glassTarget && MaterialPenetration.tryPenetrate(level, event.getAmmo(), target.damageState(), target.damagePos(), hitResult.getLocation(), hitResult.getDirection(), shotgunImpact, highCaliberImpact, armorPiercingImpact, automaticImpact);

        if (remainingHp <= 0.0D) {
            breakBlock(level, target.damagePos(), target.damageState(), event.getAmmo().getOwner(), glassTarget);
        } else {
            BallisticDamageStore.updateProgress(level, target.damagePos(), remainingHp, maxHp);
        }

        if (glassTarget && BallisticBlockConfig.GLASS_PASSTHROUGH_ENABLED.get()) {
            GlassPassthrough.apply(event.getAmmo());
            Vec3 direction = event.getAmmo().getDeltaMovement();
            if (direction.lengthSqr() < 1.0E-5D) {
                direction = new Vec3(hitResult.getDirection().getStepX(), hitResult.getDirection().getStepY(), hitResult.getDirection().getStepZ());
            }
            MaterialPenetration.tryHitEntityAfterPenetration(level, event.getAmmo(), hitResult.getLocation().add(direction.normalize().scale(0.08D)), direction);
            event.setCanceled(true);
        } else if (penetrated) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        BallisticDamageStore.tick(level, level.getGameTime());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        BallisticDamageStore.clearAll();
    }

    static void breakBlock(ServerLevel level, BlockPos pos, BlockState state, Entity breaker, boolean glassTarget) {
        BallisticDamageStore.clear(level, pos);
        if (glassTarget) {
            playGlassBreakEffects(level, pos, state);
        }
        level.destroyBlock(pos, BallisticBlockConfig.DROP_ITEMS.get(), breaker, Block.UPDATE_ALL);
    }

    static void playImpactEffects(ServerLevel level, BlockPos pos, BlockState state, Vec3 hitLocation, boolean glassTarget) {
        if (BallisticBlockConfig.SHOW_IMPACT_PARTICLES.get()) {
            if (glassTarget) {
                spawnGlassShards(level, state, hitLocation, 12);
            } else {
                level.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, state),
                        hitLocation.x,
                        hitLocation.y,
                        hitLocation.z,
                        10,
                        0.08D,
                        0.08D,
                        0.08D,
                        0.02D
                );
            }
        }

    }

    private static void playGlassBreakEffects(ServerLevel level, BlockPos pos, BlockState state) {
        Vec3 center = Vec3.atCenterOf(pos);
        if (BallisticBlockConfig.SHOW_IMPACT_PARTICLES.get()) {
            spawnGlassShards(level, state, center, 28);
        }
    }

    private static void spawnGlassShards(ServerLevel level, BlockState state, Vec3 location, int count) {
        ItemStack shardItem = new ItemStack(state.getBlock().asItem());
        if (shardItem.isEmpty()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), location.x, location.y, location.z, count, 0.12D, 0.12D, 0.12D, 0.04D);
            return;
        }
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, shardItem), location.x, location.y, location.z, count, 0.12D, 0.12D, 0.12D, 0.04D);
    }
}
