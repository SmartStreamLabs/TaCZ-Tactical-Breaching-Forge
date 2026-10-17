package com.victoriomods.taczballisticbreaching;

import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;

final class MaterialPenetration {
    private static final String PENETRATION_COUNT_KEY = TaczTacticalBreaching.MOD_ID + ":penetration_count";
    private static final int MAX_PENETRATIONS_PER_BULLET = 6;
    private static final double STEP = 0.1D;

    private MaterialPenetration() {
    }

    static boolean tryPenetrate(ServerLevel level, EntityKineticBullet bullet, BlockState state, BlockPos pos, Vec3 hitLocation, Direction hitDirection, boolean shotgunImpact, boolean highCaliberImpact, boolean armorPiercingImpact, boolean automaticImpact) {
        if (!BallisticBlockConfig.ENABLE_MATERIAL_PENETRATION.get() || state.isAir() || BallisticBlockRules.isGlassTarget(state)) {
            return false;
        }

        CompoundTag data = bullet.getPersistentData();
        int count = data.getInt(PENETRATION_COUNT_KEY);
        if (count >= MAX_PENETRATIONS_PER_BULLET) {
            return false;
        }

        Vec3 direction = bullet.getDeltaMovement();
        if (direction.lengthSqr() < 1.0E-5D) {
            direction = new Vec3(hitDirection.getStepX(), hitDirection.getStepY(), hitDirection.getStepZ());
        }
        direction = direction.normalize();

        PenetrationMaterial material = PenetrationMaterial.classify(state);
        if (material == null) {
            return false;
        }
        double capability = BallisticBlockConfig.MAX_PENETRATION_DEPTH.get() * material.configMultiplier() * caliberMultiplier(bullet, shotgunImpact, highCaliberImpact, armorPiercingImpact);
        if (capability <= 0.02D) {
            return false;
        }

        TraceResult trace = traceExit(level, hitLocation, direction, material);
        if (trace == null || trace.depth > capability) {
            return false;
        }

        double depthFactor = Math.max(0.0D, trace.depth);
        double damageMultiplier = Math.max(0.05D, 1.0D - BallisticBlockConfig.DAMAGE_LOSS_PER_BLOCK.get() * material.lossScale() * depthFactor);
        double velocityMultiplier = Math.max(0.08D, 1.0D - BallisticBlockConfig.VELOCITY_LOSS_PER_BLOCK.get() * material.lossScale() * depthFactor);
        if (armorPiercingImpact) {
            damageMultiplier = Math.min(1.0D, damageMultiplier + 0.18D);
            velocityMultiplier = Math.min(1.0D, velocityMultiplier + 0.16D);
        }

        float currentDamageMultiplier = TaczBulletDamageCompat.getStoredMultiplier(bullet, TaczBulletDamageCompat.PENETRATION_DAMAGE_MULTIPLIER_KEY);
        float updatedDamageMultiplier = (float) Math.max(0.0D, currentDamageMultiplier * damageMultiplier);
        TaczBulletDamageCompat.storeMultiplierAndTryApply(bullet, TaczBulletDamageCompat.PENETRATION_DAMAGE_MULTIPLIER_KEY, updatedDamageMultiplier);
        data.putInt(PENETRATION_COUNT_KEY, count + 1);
        bullet.setDeltaMovement(bullet.getDeltaMovement().scale(velocityMultiplier));
        bullet.setPos(trace.exit.x + direction.x * 0.035D, trace.exit.y + direction.y * 0.035D, trace.exit.z + direction.z * 0.035D);
        tryHitEntityAfterPenetration(level, bullet, trace.exit.add(direction.scale(0.04D)), direction);

        if (BallisticBlockConfig.ENABLE_EXIT_DECALS.get() || BallisticBlockConfig.ENABLE_PENETRATION_PARTICLES.get()) {
            Direction exitDirection = Direction.getNearest(direction.x, direction.y, direction.z);
            ImpactFxNetwork.sendImpact(level, trace.exit, exitDirection, state, trace.exitPos, shotgunImpact, highCaliberImpact, armorPiercingImpact, automaticImpact, false, true);
        }
        return true;
    }

    static boolean tryHitEntityAfterPenetration(ServerLevel level, EntityKineticBullet bullet, Vec3 exit, Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-5D) {
            return false;
        }
        Vec3 normalized = direction.normalize();
        double traceDistance = Math.max(1.5D, Math.min(6.0D, bullet.getDeltaMovement().length() * 1.5D));
        Vec3 end = exit.add(normalized.scale(traceDistance));
        AABB area = bullet.getBoundingBox().move(normalized.scale(traceDistance * 0.5D)).inflate(1.0D);
        Entity owner = bullet.getOwner();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, bullet, exit, end, area, entity -> {
            if (!entity.isAlive() || entity.isSpectator() || !entity.isPickable()) {
                return false;
            }
            if (owner != null && entity.getId() == owner.getId()) {
                return false;
            }
            return true;
        });
        if (hit == null) {
            return false;
        }
        Entity target = hit.getEntity();
        float damage = TaczBulletDamageCompat.getDamageWithStoredMultipliers(bullet, hit.getLocation());
        if (damage <= 0.0F) {
            return false;
        }
        boolean hurt = target.hurt(level.damageSources().thrown(bullet, owner), damage);
        if (hurt) {
            bullet.setPos(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
            if (target instanceof LivingEntity living) {
                living.invulnerableTime = 0;
            }
        }
        return hurt;
    }

    private static TraceResult traceExit(ServerLevel level, Vec3 hitLocation, Vec3 direction, PenetrationMaterial material) {
        double maxDepth = BallisticBlockConfig.MAX_PENETRATION_DEPTH.get();
        Vec3 start = hitLocation.add(direction.scale(0.03D));
        BlockPos lastSolidPos = BlockPos.containing(start);
        double traveled = 0.0D;
        boolean sawSolid = false;
        for (double d = STEP; d <= maxDepth + STEP; d += STEP) {
            Vec3 sample = start.add(direction.scale(d));
            BlockPos samplePos = BlockPos.containing(sample);
            BlockState sampleState = level.getBlockState(samplePos);
            if (sampleState.isAir() || sampleState.getCollisionShape(level, samplePos).isEmpty()) {
                if (sawSolid) {
                    return new TraceResult(sample, lastSolidPos, Math.max(STEP, traveled));
                }
                continue;
            }
            sawSolid = true;
            lastSolidPos = samplePos;
            traveled = d;
            PenetrationMaterial sampleMaterial = PenetrationMaterial.classify(sampleState);
            if (sampleMaterial == null) {
                return null;
            }
            if (sampleMaterial.ordinal() > material.ordinal()) {
                material = sampleMaterial;
            }
        }
        return null;
    }

    private static double caliberMultiplier(EntityKineticBullet bullet, boolean shotgunImpact, boolean highCaliberImpact, boolean armorPiercingImpact) {
        ResourceLocation ammoId = bullet.getAmmoId();
        ResourceLocation gunId = bullet.getGunId();
        String ammo = ammoId == null ? "" : ammoId.getPath().toLowerCase(Locale.ROOT);
        String gun = gunId == null ? "" : gunId.getPath().toLowerCase(Locale.ROOT);
        double multiplier = 0.75D;
        if (ammo.contains("9mm") || ammo.contains("45acp") || gun.contains("pistol")) {
            multiplier = 0.45D;
        } else if (ammo.contains("556") || ammo.contains("5_56") || ammo.contains("5.56")) {
            multiplier = 0.9D;
        } else if (ammo.contains("762") || ammo.contains("7_62") || ammo.contains("7.62") || ammo.contains("308")) {
            multiplier = 1.25D;
        } else if (ammo.contains("12g") || shotgunImpact) {
            multiplier = ammo.contains("slug") ? 0.95D : 0.38D;
        }
        if (highCaliberImpact || ammo.contains("50") || ammo.contains("338")) {
            multiplier *= 1.45D;
        }
        if (armorPiercingImpact || ammo.contains("ap") || ammo.contains("fmj") || ammo.contains("steel_core")) {
            multiplier *= 1.45D;
        }
        return multiplier;
    }

    private record TraceResult(Vec3 exit, BlockPos exitPos, double depth) {
    }

    private enum PenetrationMaterial {
        WOOD,
        STONE,
        METAL,
        REINFORCED;

        static PenetrationMaterial classify(BlockState state) {
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
            String path = id == null ? "" : id.getPath().toLowerCase(Locale.ROOT);
            SoundType sound = state.getSoundType();
            if (state.is(Blocks.OBSIDIAN)
                    || state.is(Blocks.CRYING_OBSIDIAN)
                    || state.is(Blocks.REINFORCED_DEEPSLATE)
                    || state.is(Blocks.NETHERITE_BLOCK)
                    || path.contains("reinforced")
                    || path.contains("armored")
                    || path.contains("armoured")
                    || path.contains("vault")
                    || path.contains("blast")
                    || path.contains("obsidian")) {
                return REINFORCED;
            }
            ImpactMaterial impactMaterial = ImpactMaterial.classify(state);
            if (impactMaterial == ImpactMaterial.METAL) {
                return METAL;
            }
            if (impactMaterial == ImpactMaterial.WOOD
                    || state.is(BlockTags.PLANKS)
                    || state.is(BlockTags.LOGS)
                    || state.is(BlockTags.FENCES)
                    || sound == SoundType.WOOD
                    || path.contains("crate")) {
                return WOOD;
            }
            if (impactMaterial == ImpactMaterial.STONE
                    || state.is(BlockTags.WALLS)
                    || sound == SoundType.STONE
                    || sound == SoundType.DEEPSLATE
                    || path.contains("stone")
                    || path.contains("concrete")
                    || path.contains("brick")
                    || path.contains("terracotta")
                    || path.contains("wall")
                    || path.contains("deepslate")) {
                return STONE;
            }
            return null;
        }

        double configMultiplier() {
            return switch (this) {
                case WOOD -> BallisticBlockConfig.WOOD_PENETRATION_MULTIPLIER.get();
                case STONE -> BallisticBlockConfig.STONE_PENETRATION_MULTIPLIER.get();
                case METAL -> BallisticBlockConfig.METAL_PENETRATION_MULTIPLIER.get();
                case REINFORCED -> BallisticBlockConfig.REINFORCED_PENETRATION_MULTIPLIER.get();
            };
        }

        double lossScale() {
            return switch (this) {
                case WOOD -> 0.65D;
                case STONE -> 1.0D;
                case METAL -> 1.65D;
                case REINFORCED -> 3.0D;
            };
        }
    }
}
