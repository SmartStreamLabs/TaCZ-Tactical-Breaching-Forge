package com.victoriomods.taczballisticbreaching;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public class DroppedMagazineEntity extends Entity implements IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<String> GUN_ID = SynchedEntityData.defineId(DroppedMagazineEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> AMMO_ID = SynchedEntityData.defineId(DroppedMagazineEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> AMMO_COUNT = SynchedEntityData.defineId(DroppedMagazineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAX_AMMO = SynchedEntityData.defineId(DroppedMagazineEntity.class, EntityDataSerializers.INT);
    private static final double REST_SPEED_SQR = 0.00012D;
    private static final double SOUND_SPEED_SQR = 0.01D;

    private float spinX;
    private float spinY;
    private float spinZ;
    private float roll;
    private float rollO;
    private int pickupDelay = 12;
    private int lastImpactSoundTick = -20;

    public DroppedMagazineEntity(EntityType<? extends DroppedMagazineEntity> type, Level level) {
        super(type, level);
    }

    public DroppedMagazineEntity(Level level, ResourceLocation gunId, ResourceLocation ammoId, int remainingAmmo, int maxAmmo, Vec3 position, Vec3 velocity, float spinX, float spinY, float spinZ) {
        this(ModEntities.DROPPED_MAGAZINE.get(), level);
        setGunId(gunId);
        setAmmoId(ammoId);
        setRemainingAmmo(remainingAmmo);
        setMaxAmmo(maxAmmo);
        setPos(position.x, position.y, position.z);
        setDeltaMovement(velocity);
        this.spinX = spinX;
        this.spinY = spinY;
        this.spinZ = spinZ;
        this.setYRot(this.random.nextFloat() * 360.0F);
        this.setXRot(this.random.nextFloat() * 360.0F);
        this.roll = this.random.nextFloat() * 360.0F;
        this.rollO = this.roll;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(GUN_ID, "tacz:unknown");
        this.entityData.define(AMMO_ID, "tacz:unknown");
        this.entityData.define(AMMO_COUNT, 0);
        this.entityData.define(MAX_AMMO, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.pickupDelay > 0) {
            this.pickupDelay--;
        }
        if (!this.level().isClientSide && this.tickCount > BallisticBlockConfig.DROPPED_MAGAZINE_DESPAWN_SECONDS.get() * 20) {
            discard();
            return;
        }

        this.xRotO = getXRot();
        this.yRotO = getYRot();
        this.rollO = this.roll;

        Vec3 velocity = getDeltaMovement();
        if (!onGround()) {
            velocity = velocity.add(0.0D, -0.035D, 0.0D);
        }

        move(MoverType.SELF, velocity);

        if (this.horizontalCollision || this.verticalCollision) {
            maybePlayImpactSound(velocity);
            maybeSpawnLandingDust(velocity);
            velocity = bounce(velocity);
        } else {
            velocity = velocity.scale(0.985D);
        }

        if (onGround()) {
            velocity = velocity.multiply(0.72D, 1.0D, 0.72D);
            if (velocity.lengthSqr() < REST_SPEED_SQR) {
                velocity = Vec3.ZERO;
                this.spinX *= 0.55F;
                this.spinY *= 0.55F;
                this.spinZ *= 0.55F;
            }
        }

        setDeltaMovement(velocity);
        if (velocity != Vec3.ZERO) {
            setXRot(wrapDegrees(getXRot() + this.spinX));
            setYRot(wrapDegrees(getYRot() + this.spinY));
            this.roll = wrapDegrees(this.roll + this.spinZ);
        }
    }

    private Vec3 bounce(Vec3 velocity) {
        double x = this.horizontalCollision ? -velocity.x * 0.36D : velocity.x;
        double y = this.verticalCollision ? -velocity.y * 0.32D : velocity.y;
        double z = this.horizontalCollision ? -velocity.z * 0.36D : velocity.z;
        return new Vec3(x, y, z).scale(0.76D);
    }

    private void maybePlayImpactSound(Vec3 velocity) {
        if (this.level().isClientSide || !BallisticBlockConfig.ENABLE_CASING_SOUNDS.get()) {
            return;
        }
        if (this.tickCount - this.lastImpactSoundTick < 8 || velocity.lengthSqr() < SOUND_SPEED_SQR) {
            return;
        }
        this.lastImpactSoundTick = this.tickCount;
        boolean polymer = isPolymerMagazine();
        this.level().playSound(null, blockPosition(), polymer ? SoundEvents.BAMBOO_WOOD_HIT : SoundEvents.COPPER_HIT, SoundSource.PLAYERS, polymer ? 0.18F : 0.22F, polymer ? 1.35F + this.random.nextFloat() * 0.2F : 1.45F + this.random.nextFloat() * 0.28F);
    }

    private void maybeSpawnLandingDust(Vec3 velocity) {
        if (!this.level().isClientSide || velocity.lengthSqr() < SOUND_SPEED_SQR) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            this.level().addParticle(ParticleTypes.POOF, getX(), getY() + 0.03D, getZ(), (this.random.nextDouble() - 0.5D) * 0.025D, 0.01D, (this.random.nextDouble() - 0.5D) * 0.025D);
        }
    }

    @Override
    public void playerTouch(Player player) {
        // Deliberately no auto-pickup: players must be able to right-click the magazine on the ground.
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            return tryPickup(player) ? InteractionResult.CONSUME : InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public boolean tryPickup(Player player) {
        if (this.pickupDelay > 0 || !BallisticBlockConfig.ALLOW_DROPPED_MAGAZINE_PICKUP.get()) {
            return false;
        }
        ItemStack stack = getRenderStack();
        if (player.getInventory().add(stack)) {
            player.take(this, 1);
            this.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.18F, 1.2F + this.random.nextFloat() * 0.2F);
            discard();
            return true;
        }
        return false;
    }

    public ItemStack getRenderStack() {
        return DroppedMagazineItem.create(getGunId(), getAmmoId(), getRemainingAmmo(), getMaxAmmo());
    }

    public ResourceLocation getGunId() {
        ResourceLocation parsed = ResourceLocation.tryParse(this.entityData.get(GUN_ID));
        return parsed == null ? new ResourceLocation("tacz", "unknown") : parsed;
    }

    public ResourceLocation getAmmoId() {
        ResourceLocation parsed = ResourceLocation.tryParse(this.entityData.get(AMMO_ID));
        return parsed == null ? new ResourceLocation("tacz", "unknown") : parsed;
    }

    public int getRemainingAmmo() {
        return Math.max(0, this.entityData.get(AMMO_COUNT));
    }

    public int getMaxAmmo() {
        return Math.max(0, this.entityData.get(MAX_AMMO));
    }

    public float getRenderRoll(float partialTick) {
        return Mth.lerp(partialTick, this.rollO, this.roll);
    }

    public void loadFromMagazineStack(ItemStack stack) {
        if (stack.getItem() instanceof DroppedMagazineItem item) {
            setGunId(DroppedMagazineItem.getGunId(stack));
            setAmmoId(item.getAmmoId(stack));
            setRemainingAmmo(item.getAmmoCount(stack));
            setMaxAmmo(item.getMaxAmmo(stack));
        }
    }

    private void setGunId(ResourceLocation gunId) {
        this.entityData.set(GUN_ID, safeId(gunId).toString());
    }

    private void setAmmoId(ResourceLocation ammoId) {
        this.entityData.set(AMMO_ID, safeId(ammoId).toString());
    }

    private void setRemainingAmmo(int remainingAmmo) {
        this.entityData.set(AMMO_COUNT, Math.max(0, remainingAmmo));
    }

    private void setMaxAmmo(int maxAmmo) {
        this.entityData.set(MAX_AMMO, Math.max(0, maxAmmo));
    }

    private boolean isPolymerMagazine() {
        String gun = getGunId().getPath().toLowerCase();
        return gun.contains("glock") || gun.contains("pistol") || gun.contains("ump") || gun.contains("vector") || gun.contains("p90") || gun.contains("mp5");
    }

    private static ResourceLocation safeId(ResourceLocation id) {
        return id == null ? new ResourceLocation("tacz", "unknown") : id;
    }

    private static float wrapDegrees(float value) {
        return Mth.wrapDegrees(value);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setGunId(ResourceLocation.tryParse(tag.getString("GunId")));
        setAmmoId(ResourceLocation.tryParse(tag.getString("AmmoId")));
        setRemainingAmmo(tag.getInt("AmmoCount"));
        setMaxAmmo(tag.getInt("MaxAmmo"));
        this.spinX = tag.getFloat("SpinX");
        this.spinY = tag.getFloat("SpinY");
        this.spinZ = tag.getFloat("SpinZ");
        this.roll = tag.getFloat("Roll");
        this.rollO = this.roll;
        this.pickupDelay = tag.getInt("PickupDelay");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("GunId", getGunId().toString());
        tag.putString("AmmoId", getAmmoId().toString());
        tag.putInt("AmmoCount", getRemainingAmmo());
        tag.putInt("MaxAmmo", getMaxAmmo());
        tag.putFloat("SpinX", this.spinX);
        tag.putFloat("SpinY", this.spinY);
        tag.putFloat("SpinZ", this.spinZ);
        tag.putFloat("Roll", this.roll);
        tag.putInt("PickupDelay", this.pickupDelay);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(getGunId());
        buffer.writeResourceLocation(getAmmoId());
        buffer.writeVarInt(getRemainingAmmo());
        buffer.writeVarInt(getMaxAmmo());
        buffer.writeFloat(this.spinX);
        buffer.writeFloat(this.spinY);
        buffer.writeFloat(this.spinZ);
        buffer.writeFloat(this.roll);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        setGunId(buffer.readResourceLocation());
        setAmmoId(buffer.readResourceLocation());
        setRemainingAmmo(buffer.readVarInt());
        setMaxAmmo(buffer.readVarInt());
        this.spinX = buffer.readFloat();
        this.spinY = buffer.readFloat();
        this.spinZ = buffer.readFloat();
        this.roll = buffer.readFloat();
        this.rollO = this.roll;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public float getPickRadius() {
        return 0.2F;
    }
}
