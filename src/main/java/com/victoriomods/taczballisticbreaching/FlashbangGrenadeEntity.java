package com.victoriomods.taczballisticbreaching;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.UUID;

public class FlashbangGrenadeEntity extends net.minecraft.world.entity.Entity implements IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<Integer> DATA_FUSE_TICKS = SynchedEntityData.defineId(FlashbangGrenadeEntity.class, EntityDataSerializers.INT);
    private static final double GRAVITY = 0.045D;
    private static final double REST_SPEED_SQR = 0.00055D;
    private static final double SOUND_RADIUS = 56.0D;
    private int fuseTicks;
    private UUID ownerUuid;
    private float spinX;
    private float spinY;
    private float spinZ;
    private float roll;
    private float rollO;
    private int lastBounceSoundTick = -20;
    private int lastRollSoundTick = -30;

    public FlashbangGrenadeEntity(EntityType<? extends FlashbangGrenadeEntity> type, Level level) {
        super(type, level);
        this.fuseTicks = BallisticBlockConfig.FLASHBANG_FUSE_TICKS.get();
        this.entityData.set(DATA_FUSE_TICKS, this.fuseTicks);
    }

    public FlashbangGrenadeEntity(Level level, LivingEntity owner) {
        this(ModEntities.FLASHBANG_GRENADE.get(), level);
        this.ownerUuid = owner.getUUID();
        setPos(owner.getX(), owner.getEyeY() - 0.14D, owner.getZ());
        this.spinX = this.random.nextFloat() * 10.0F - 5.0F;
        this.spinY = this.random.nextFloat() * 12.0F - 6.0F;
        this.spinZ = this.random.nextFloat() * 12.0F - 6.0F;
        this.setYRot(this.random.nextFloat() * 360.0F);
        this.setXRot(this.random.nextFloat() * 360.0F);
        this.roll = this.random.nextFloat() * 360.0F;
        this.rollO = this.roll;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_FUSE_TICKS, BallisticBlockConfig.FLASHBANG_FUSE_TICKS.get());
    }

    public void shootFromPlayer(Player player, float xRot, float yRot, float angle, float velocity, float inaccuracy) {
        float x = -Mth.sin(yRot * ((float) Math.PI / 180F)) * Mth.cos(xRot * ((float) Math.PI / 180F));
        float y = -Mth.sin((xRot + angle) * ((float) Math.PI / 180F));
        float z = Mth.cos(yRot * ((float) Math.PI / 180F)) * Mth.cos(xRot * ((float) Math.PI / 180F));
        Vec3 motion = new Vec3(x, y, z).normalize()
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
                detonate();
                return;
            }
        } else {
            preDetonationClientEffects();
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
            velocity = velocity.multiply(0.78D, 1.0D, 0.78D);
            playRollingSound(velocity);
            if (velocity.lengthSqr() < REST_SPEED_SQR) {
                velocity = Vec3.ZERO;
            }
        }
        setDeltaMovement(velocity);
        if (velocity.lengthSqr() > REST_SPEED_SQR) {
            float speedScale = (float) Mth.clamp(velocity.length() * 2.0D, 0.25D, 1.0D);
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
        double x = horizontalCollision ? -velocity.x * 0.38D : velocity.x;
        double y = verticalCollision ? -velocity.y * 0.42D : velocity.y;
        double z = horizontalCollision ? -velocity.z * 0.38D : velocity.z;
        return new Vec3(x, y, z).scale(0.86D);
    }

    private void playBounceSound(Vec3 velocity) {
        if (level().isClientSide() || tickCount - lastBounceSoundTick < 6 || velocity.lengthSqr() < 0.014D) {
            return;
        }
        lastBounceSoundTick = tickCount;
        float volume = (float) Mth.clamp(velocity.length() * 0.28D, 0.12D, 0.3D);
        level().playSound(null, blockPosition(), ModSounds.FLASHBANG_BOUNCE.get(), SoundSource.PLAYERS, volume, 0.92F + random.nextFloat() * 0.14F);
    }

    private void playRollingSound(Vec3 velocity) {
        if (level().isClientSide() || tickCount - lastRollSoundTick < 16 || velocity.horizontalDistanceSqr() < 0.004D) {
            return;
        }
        lastRollSoundTick = tickCount;
        float volume = (float) Mth.clamp(velocity.horizontalDistance() * 0.13D, 0.03D, 0.1D);
        level().playSound(null, blockPosition(), ModSounds.FLASHBANG_ROLL.get(), SoundSource.PLAYERS, volume, 0.9F + random.nextFloat() * 0.12F);
    }

    private void preDetonationClientEffects() {
        int syncedFuse = getClientFuseTicks();
        if (syncedFuse <= 0 || syncedFuse > 16) {
            return;
        }
        double urgency = 1.0D - syncedFuse / 16.0D;
        Vec3 top = position().add(0.0D, getBbHeight() * 0.9D, 0.0D);
        if (random.nextFloat() < 0.22F + urgency * 0.38F) {
            Vec3 velocity = randomSpread(0.01D).add(0.0D, 0.008D + random.nextDouble() * 0.016D, 0.0D);
            level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, top.x + random.triangle(0.0D, 0.025D), top.y, top.z + random.triangle(0.0D, 0.025D), velocity.x, velocity.y, velocity.z);
        }
        if (syncedFuse % 4 < 2) {
            DustParticleOptions flashPrimer = new DustParticleOptions(new Vector3f(1.0F, 0.95F, 0.72F), 0.38F);
            Vec3 velocity = randomSpread(0.008D).add(0.0D, 0.008D, 0.0D);
            level().addParticle(flashPrimer, top.x + random.triangle(0.0D, 0.018D), top.y + 0.03D, top.z + random.triangle(0.0D, 0.018D), velocity.x, velocity.y, velocity.z);
        }
    }

    private void detonate() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }
        Vec3 center = position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
        double effectRadius = Math.max(1.0D, BallisticBlockConfig.FLASHBANG_RADIUS.get());
        double packetRadius = Math.max(SOUND_RADIUS, effectRadius * 2.5D);
        boolean indoor = isIndoor(serverLevel, center);
        for (ServerPlayer player : serverLevel.players()) {
            if (!player.level().dimension().equals(serverLevel.dimension())) {
                continue;
            }
            double distance = player.getEyePosition().distanceTo(center);
            if (distance > packetRadius) {
                continue;
            }
            FlashStrength strength = calculateStrength(serverLevel, player, center, distance, effectRadius);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new FlashbangDetonationFxPacket(
                    center.x,
                    center.y,
                    center.z,
                    strength.effect(),
                    strength.direct(),
                    BallisticBlockConfig.FLASH_DURATION_TICKS.get(),
                    BallisticBlockConfig.FLASHBANG_TINNITUS_DURATION_TICKS.get(),
                    BallisticBlockConfig.FLASHBANG_BLUR_STRENGTH.get().floatValue(),
                    indoor,
                    strength.lineOfSight()
            ));
        }
        discard();
    }

    private FlashStrength calculateStrength(ServerLevel level, ServerPlayer player, Vec3 center, double distance, double radius) {
        if (distance > radius) {
            return new FlashStrength(0.0F, 0.0F, false);
        }
        Vec3 eye = player.getEyePosition();
        Vec3 toFlash = center.subtract(eye);
        if (toFlash.lengthSqr() < 0.0001D) {
            return new FlashStrength(1.0F, 1.0F, true);
        }

        CoverResult cover = coverBetween(level, eye, center);
        if (BallisticBlockConfig.ENABLE_FLASHBANG_LINE_OF_SIGHT.get() && cover.solidBlocked()) {
            return new FlashStrength(0.0F, 0.0F, false);
        }

        double distanceFactor = 1.0D - Mth.clamp(distance / radius, 0.0D, 1.0D);
        Vec3 direction = toFlash.normalize();
        double dot = Mth.clamp(player.getLookAngle().normalize().dot(direction), -1.0D, 1.0D);
        double direct = Mth.clamp((dot + 0.1D) / 1.1D, 0.0D, 1.0D);
        double lookScale = 0.28D + direct * 0.72D;
        double coverScale = cover.glassReduced() ? 0.42D : 1.0D;
        double effect = Math.pow(distanceFactor, 0.72D) * lookScale * coverScale;
        return new FlashStrength((float) Mth.clamp(effect, 0.0D, 1.0D), (float) Mth.clamp(direct * coverScale, 0.0D, 1.0D), !cover.solidBlocked());
    }

    private CoverResult coverBetween(ServerLevel level, Vec3 eye, Vec3 center) {
        if (!BallisticBlockConfig.ENABLE_FLASHBANG_LINE_OF_SIGHT.get()) {
            return CoverResult.CLEAR;
        }
        HitResult hit = level.clip(new ClipContext(eye, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return CoverResult.CLEAR;
        }
        BlockHitResult blockHit = (BlockHitResult) hit;
        double hitDistance = eye.distanceTo(blockHit.getLocation());
        if (hitDistance + 0.12D >= eye.distanceTo(center)) {
            return CoverResult.CLEAR;
        }
        BlockState state = level.getBlockState(blockHit.getBlockPos());
        if (isGlassLike(state)) {
            return CoverResult.GLASS;
        }
        return CoverResult.SOLID;
    }

    private boolean isGlassLike(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.getSoundType() == SoundType.GLASS) {
            return true;
        }
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return path.contains("glass") || path.contains("pane");
    }

    private boolean isIndoor(ServerLevel level, Vec3 origin) {
        BlockPos pos = BlockPos.containing(origin);
        if (!level.canSeeSky(pos)) {
            return true;
        }
        int blocked = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (var direction : net.minecraft.core.Direction.values()) {
            for (int distance = 1; distance <= 5; distance++) {
                mutable.set(pos).move(direction, distance);
                BlockState state = level.getBlockState(mutable);
                if (!state.isAir() && (state.isSolidRender(level, mutable) || state.getSoundType() == SoundType.GLASS)) {
                    blocked++;
                    break;
                }
            }
        }
        return blocked >= 4;
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

    private record FlashStrength(float effect, float direct, boolean lineOfSight) {
    }

    private record CoverResult(boolean solidBlocked, boolean glassReduced) {
        static final CoverResult CLEAR = new CoverResult(false, false);
        static final CoverResult SOLID = new CoverResult(true, false);
        static final CoverResult GLASS = new CoverResult(false, true);
    }
}
