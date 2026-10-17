package com.victoriomods.taczballisticbreaching;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FragGrenadeEntity extends Entity implements IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<Integer> DATA_FUSE_TICKS = SynchedEntityData.defineId(FragGrenadeEntity.class, EntityDataSerializers.INT);
    private static final double GRAVITY = 0.045D;
    private static final double REST_SPEED_SQR = 0.0007D;
    private static final double MAX_SAFE_RADIUS = 6.0D;
    private static final int MAX_SAFE_FRAGMENT_COUNT = 64;
    private static final double MAX_SAFE_DAMAGE = 12.0D;
    private static final double BLOCK_DAMAGE_RADIUS = 1.65D;
    private static final int MAX_BROKEN_BLOCKS = 6;
    private int fuseTicks;
    private UUID ownerUuid;
    private float spinX;
    private float spinY;
    private float spinZ;
    private float roll;
    private float rollO;
    private int lastBounceSoundTick = -20;
    private int lastRollSoundTick = -30;

    public FragGrenadeEntity(EntityType<? extends FragGrenadeEntity> type, Level level) {
        super(type, level);
        this.fuseTicks = BallisticBlockConfig.FRAG_FUSE_TIME_TICKS.get();
        this.entityData.set(DATA_FUSE_TICKS, this.fuseTicks);
    }

    public FragGrenadeEntity(Level level, LivingEntity owner) {
        this(ModEntities.FRAG_GRENADE.get(), level);
        this.ownerUuid = owner.getUUID();
        setPos(owner.getX(), owner.getEyeY() - 0.16D, owner.getZ());
        this.spinX = this.random.nextFloat() * 11.0F - 5.5F;
        this.spinY = this.random.nextFloat() * 14.0F - 7.0F;
        this.spinZ = this.random.nextFloat() * 13.0F - 6.5F;
        this.setYRot(this.random.nextFloat() * 360.0F);
        this.setXRot(this.random.nextFloat() * 360.0F);
        this.roll = this.random.nextFloat() * 360.0F;
        this.rollO = this.roll;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_FUSE_TICKS, BallisticBlockConfig.FRAG_FUSE_TIME_TICKS.get());
    }

    public void shootFromPlayer(Player player, float xRot, float yRot, float angle, float velocity, float inaccuracy) {
        float f = -Mth.sin(yRot * ((float) Math.PI / 180F)) * Mth.cos(xRot * ((float) Math.PI / 180F));
        float f1 = -Mth.sin((xRot + angle) * ((float) Math.PI / 180F));
        float f2 = Mth.cos(yRot * ((float) Math.PI / 180F)) * Mth.cos(xRot * ((float) Math.PI / 180F));
        Vec3 motion = new Vec3(f, f1, f2).normalize()
                .add(this.random.triangle(0.0D, 0.017D * inaccuracy), this.random.triangle(0.0D, 0.017D * inaccuracy), this.random.triangle(0.0D, 0.017D * inaccuracy))
                .scale(velocity)
                .add(player.getDeltaMovement().scale(0.42D));
        setDeltaMovement(motion);
    }

    @Override
    public void tick() {
        super.tick();
        this.xRotO = getXRot();
        this.yRotO = getYRot();
        this.rollO = this.roll;

        if (!level().isClientSide()) {
            fuseTicks--;
            this.entityData.set(DATA_FUSE_TICKS, fuseTicks);
            if (fuseTicks <= 0) {
                explode();
                return;
            }
        } else {
            preExplosionClientEffects();
        }

        Vec3 velocity = getDeltaMovement();
        if (!onGround()) {
            velocity = velocity.add(0.0D, -GRAVITY, 0.0D);
        }
        move(MoverType.SELF, velocity);

        if (horizontalCollision || verticalCollision) {
            velocity = bounce(velocity);
            playBounceSound(velocity);
        } else {
            velocity = velocity.scale(0.992D);
        }
        if (onGround()) {
            velocity = velocity.multiply(0.76D, 1.0D, 0.76D);
            playRollingSound(velocity);
            if (velocity.lengthSqr() < REST_SPEED_SQR) {
                velocity = Vec3.ZERO;
            }
        }
        setDeltaMovement(velocity);
        if (velocity.lengthSqr() > REST_SPEED_SQR) {
            float speedScale = (float) Mth.clamp(velocity.length() * 2.15D, 0.28D, 1.0D);
            setXRot(Mth.wrapDegrees(getXRot() + spinX * speedScale));
            setYRot(Mth.wrapDegrees(getYRot() + spinY * speedScale));
            this.roll = Mth.wrapDegrees(this.roll + spinZ * speedScale);
            if (onGround()) {
                spinX *= 0.965F;
                spinY *= 0.94F;
                spinZ *= 0.965F;
            }
        } else {
            spinX *= 0.9F;
            spinY *= 0.9F;
            spinZ *= 0.9F;
        }
    }

    private Vec3 bounce(Vec3 velocity) {
        double x = horizontalCollision ? -velocity.x * 0.42D : velocity.x;
        double y = verticalCollision ? -velocity.y * 0.46D : velocity.y;
        double z = horizontalCollision ? -velocity.z * 0.42D : velocity.z;
        return new Vec3(x, y, z).scale(0.86D);
    }

    private void playBounceSound(Vec3 velocity) {
        if (level().isClientSide() || tickCount - lastBounceSoundTick < 6 || velocity.lengthSqr() < 0.018D) {
            return;
        }
        lastBounceSoundTick = tickCount;
        float volume = (float) Mth.clamp(velocity.length() * 0.34D, 0.16D, 0.36D);
        level().playSound(null, blockPosition(), ModSounds.FRAG_BOUNCE.get(), SoundSource.PLAYERS, volume, 0.86F + random.nextFloat() * 0.18F);
    }

    private void playRollingSound(Vec3 velocity) {
        if (level().isClientSide() || tickCount - lastRollSoundTick < 14 || velocity.horizontalDistanceSqr() < 0.004D) {
            return;
        }
        lastRollSoundTick = tickCount;
        float volume = (float) Mth.clamp(velocity.horizontalDistance() * 0.16D, 0.035D, 0.12D);
        level().playSound(null, blockPosition(), ModSounds.FRAG_ROLL.get(), SoundSource.PLAYERS, volume, 0.82F + random.nextFloat() * 0.14F);
    }

    private void preExplosionClientEffects() {
        int syncedFuse = getClientFuseTicks();
        if (syncedFuse <= 0 || syncedFuse > 20) {
            return;
        }
        double urgency = 1.0D - syncedFuse / 20.0D;
        Vec3 top = position().add(0.0D, getBbHeight() * 0.92D, 0.0D);
        if (random.nextFloat() < 0.35F + urgency * 0.35F) {
            Vec3 velocity = randomSpread(0.012D).add(0.0D, 0.012D + random.nextDouble() * 0.025D, 0.0D);
            level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, top.x + random.triangle(0.0D, 0.035D), top.y, top.z + random.triangle(0.0D, 0.035D), velocity.x, velocity.y, velocity.z);
        }
        if (syncedFuse % 5 < 3) {
            DustParticleOptions ember = new DustParticleOptions(new Vector3f(1.0F, 0.36F + (float) urgency * 0.18F, 0.08F), 0.45F);
            Vec3 velocity = randomSpread(0.01D).add(0.0D, 0.01D, 0.0D);
            level().addParticle(ember, top.x + random.triangle(0.0D, 0.025D), top.y + 0.03D, top.z + random.triangle(0.0D, 0.025D), velocity.x, velocity.y, velocity.z);
        }
    }

    private void explode() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }
        Vec3 center = position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
        applyBlastDamage(serverLevel, center);
        applyFragmentDamage(serverLevel, center);
        float radius = getEffectiveRadius();
        double soundDistance = BallisticBlockConfig.FRAG_EXPLOSION_SOUND_DISTANCE.get();
        ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(center.x, center.y, center.z, soundDistance, serverLevel.dimension())), new FragExplosionFxPacket(center.x, center.y, center.z, radius));
        if (BallisticBlockConfig.ENABLE_FRAG_BLOCK_DAMAGE.get()) {
            breakNearbyBlocks(serverLevel, center);
        }
        discard();
    }

    private void breakNearbyBlocks(ServerLevel level, Vec3 center) {
        int broken = 0;
        BlockPos origin = BlockPos.containing(center);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-2, -2, -2), origin.offset(2, 2, 2))) {
            Vec3 target = Vec3.atCenterOf(pos);
            if (center.distanceToSqr(target) > BLOCK_DAMAGE_RADIUS * BLOCK_DAMAGE_RADIUS) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            if (state.isAir() || hardness < 0.0F || hardness > 2.0F || state.hasBlockEntity() || !state.getFluidState().isEmpty()) {
                continue;
            }
            HitResult hit = level.clip(new ClipContext(center, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() == HitResult.Type.BLOCK && !((BlockHitResult) hit).getBlockPos().equals(pos)) {
                continue;
            }
            if (level.destroyBlock(pos, true, this) && ++broken >= MAX_BROKEN_BLOCKS) {
                return;
            }
        }
    }

    private void applyFragmentDamage(ServerLevel level, Vec3 center) {
        int count = getEffectiveFragmentCount();
        if (count <= 0 || getEffectiveDamage() <= 0.0D) {
            return;
        }
        double radius = getEffectiveRadius();
        Entity owner = getOwner();
        List<Entity> targets = level.getEntities(this, new AABB(center, center).inflate(radius), Entity::isAlive);
        Map<Entity, Integer> hits = new HashMap<>();
        Set<BlockPos> brokenGlass = new HashSet<>();
        for (int i = 0; i < count; i++) {
            Vec3 dir = randomFragmentDirection();
            Vec3 end = center.add(dir.scale(radius));
            double blockLimit = radius;
            if (BallisticBlockConfig.ENABLE_FRAGMENT_LINE_OF_SIGHT.get()) {
                HitResult blockHit = level.clip(new ClipContext(center, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                if (blockHit.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult hit = (BlockHitResult) blockHit;
                    if (tryBreakFragileGlass(level, hit.getBlockPos(), brokenGlass)) {
                        blockLimit = radius;
                    } else {
                        blockLimit = Math.max(0.0D, center.distanceTo(blockHit.getLocation()) - 0.08D);
                    }
                }
            }
            Entity best = null;
            double bestDistance = blockLimit;
            for (Entity target : targets) {
                AABB box = target.getBoundingBox().inflate(target instanceof HangingEntity ? 0.08D : 0.18D);
                var clip = box.clip(center, center.add(dir.scale(blockLimit)));
                if (clip.isPresent()) {
                    double dist = center.distanceTo(clip.get());
                    if (dist < bestDistance && !hasSolidCoverBetween(level, center, clip.get())) {
                        bestDistance = dist;
                        best = target;
                    }
                }
            }
            if (best != null) {
                hits.merge(best, 1, Integer::sum);
            }
        }
        DamageSource source = damageSources().explosion(this, owner instanceof LivingEntity living ? living : null);
        for (Map.Entry<Entity, Integer> entry : hits.entrySet()) {
            Entity target = entry.getKey();
            double distance = target.distanceTo(this);
            double falloff = 1.0D - Mth.clamp(distance / radius, 0.0D, 0.92D);
            double hitScale = Math.min(1.45D, 0.42D + entry.getValue() / Math.max(1.0D, count * 0.12D));
            float damage = (float) (getEffectiveDamage() * falloff * hitScale);
            if (target instanceof LivingEntity livingTarget) {
                if (hasArmor(livingTarget)) {
                    damage *= 0.72F;
                }
            } else if (target instanceof HangingEntity) {
                damage = Math.max(damage, 10.0F);
            } else {
                damage = Math.max(damage * 0.65F, 2.0F);
            }
            if (damage > 0.35F) {
                hurtEntity(target, source, damage);
            }
        }
    }

    private void applyBlastDamage(ServerLevel level, Vec3 center) {
        double radius = getEffectiveRadius();
        double damageBase = getEffectiveDamage();
        if (damageBase <= 0.0D) {
            return;
        }
        Entity owner = getOwner();
        DamageSource source = damageSources().explosion(this, owner instanceof LivingEntity living ? living : null);
        List<Entity> targets = level.getEntities(this, new AABB(center, center).inflate(radius), Entity::isAlive);
        for (Entity target : targets) {
            double distance = Math.max(0.35D, target.distanceTo(this));
            double falloff = 1.0D - Mth.clamp(distance / radius, 0.0D, 1.0D);
            if (falloff <= 0.0D) {
                continue;
            }

            if (hasSolidCoverBetween(level, center, target.getBoundingBox().getCenter())
                    || hasSolidCoverBetween(level, center, new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.18D, target.getZ()))
                    || hasSolidCoverBetween(level, center, new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.82D, target.getZ()))) {
                continue;
            }

            float damage = (float) (damageBase * 0.9D * falloff);
            if (target instanceof LivingEntity livingTarget) {
                if (hasArmor(livingTarget)) {
                    damage *= 0.82F;
                }
            } else if (target instanceof HangingEntity) {
                damage = Math.max(damage, 20.0F);
            } else {
                damage = Math.max(damage * 0.7F, 3.0F);
            }
            if (damage > 0.35F) {
                hurtEntity(target, source, damage);
            }
        }
    }

    private void hurtEntity(Entity target, DamageSource source, float damage) {
        boolean damaged = target.hurt(source, damage);
        if (!damaged && target instanceof Player player && !player.getAbilities().invulnerable) {
            target.hurt(damageSources().generic(), damage);
        }
    }

    private boolean hasSolidCoverBetween(ServerLevel level, Vec3 start, Vec3 end) {
        if (!BallisticBlockConfig.ENABLE_FRAGMENT_LINE_OF_SIGHT.get()) {
            return false;
        }
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() < 0.0001D) {
            return false;
        }
        Vec3 adjustedStart = start.add(direction.normalize().scale(0.08D));
        HitResult blockHit = level.clip(new ClipContext(adjustedStart, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() != HitResult.Type.BLOCK || adjustedStart.distanceTo(blockHit.getLocation()) + 0.08D >= adjustedStart.distanceTo(end)) {
            return false;
        }
        BlockHitResult hit = (BlockHitResult) blockHit;
        return !isFragileGlass(level.getBlockState(hit.getBlockPos()));
    }

    private boolean tryBreakFragileGlass(ServerLevel level, BlockPos pos, Set<BlockPos> brokenGlass) {
        if (!brokenGlass.add(pos)) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        if (!isFragileGlass(state)) {
            return false;
        }
        level.destroyBlock(pos, true, this);
        return true;
    }

    private boolean isFragileGlass(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.getSoundType() == SoundType.GLASS) {
            return true;
        }
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return path.contains("glass") || path.contains("pane");
    }

    private float getEffectiveRadius() {
        return (float) Mth.clamp(BallisticBlockConfig.FRAG_EXPLOSION_RADIUS.get(), 1.0D, MAX_SAFE_RADIUS);
    }

    private int getEffectiveFragmentCount() {
        int configured = BallisticBlockConfig.FRAG_COUNT.get();
        if (configured <= 0) {
            return 0;
        }
        return Mth.clamp(configured, 1, MAX_SAFE_FRAGMENT_COUNT);
    }

    private double getEffectiveDamage() {
        double configured = BallisticBlockConfig.FRAG_DAMAGE.get();
        if (configured <= 0.0D) {
            return 0.0D;
        }
        return Mth.clamp(configured * 1.5D, 0.0D, MAX_SAFE_DAMAGE);
    }

    private Vec3 randomFragmentDirection() {
        double yaw = random.nextDouble() * Math.PI * 2.0D;
        double y = random.nextDouble() * 1.55D - 0.52D;
        double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - y * y * 0.42D));
        return new Vec3(Math.cos(yaw) * horizontal, y, Math.sin(yaw) * horizontal).normalize();
    }

    private boolean hasArmor(LivingEntity entity) {
        for (var stack : entity.getArmorSlots()) {
            if (!stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private Entity getOwner() {
        if (ownerUuid != null && level() instanceof ServerLevel serverLevel) {
            return serverLevel.getEntity(ownerUuid);
        }
        return null;
    }

    public float getRenderRoll(float partialTick) {
        return Mth.lerp(partialTick, this.rollO, this.roll);
    }

    public int getClientFuseTicks() {
        return this.entityData.get(DATA_FUSE_TICKS);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        fuseTicks = tag.getInt("Fuse");
        this.entityData.set(DATA_FUSE_TICKS, fuseTicks);
        if (tag.hasUUID("Owner")) {
            ownerUuid = tag.getUUID("Owner");
        }
        spinX = tag.getFloat("SpinX");
        spinY = tag.getFloat("SpinY");
        spinZ = tag.getFloat("SpinZ");
        roll = tag.getFloat("Roll");
        rollO = roll;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Fuse", fuseTicks);
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
        tag.putFloat("SpinX", spinX);
        tag.putFloat("SpinY", spinY);
        tag.putFloat("SpinZ", spinZ);
        tag.putFloat("Roll", roll);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeFloat(spinX);
        buffer.writeFloat(spinY);
        buffer.writeFloat(spinZ);
        buffer.writeFloat(roll);
        buffer.writeVarInt(fuseTicks);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        spinX = buffer.readFloat();
        spinY = buffer.readFloat();
        spinZ = buffer.readFloat();
        roll = buffer.readFloat();
        rollO = roll;
        fuseTicks = buffer.readVarInt();
        this.entityData.set(DATA_FUSE_TICKS, fuseTicks);
    }

    private Vec3 randomSpread(double amount) {
        return new Vec3(
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount,
                (random.nextDouble() - 0.5D) * amount
        );
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
