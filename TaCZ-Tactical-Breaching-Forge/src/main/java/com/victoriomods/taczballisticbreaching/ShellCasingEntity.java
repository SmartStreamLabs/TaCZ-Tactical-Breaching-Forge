package com.victoriomods.taczballisticbreaching;

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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public class ShellCasingEntity extends Entity implements IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<Integer> CASING_TYPE = SynchedEntityData.defineId(ShellCasingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> AMMO_ID = SynchedEntityData.defineId(ShellCasingEntity.class, EntityDataSerializers.STRING);
    private static final double REST_SPEED_SQR = 0.00018D;
    private static final double SOUND_SPEED_SQR = 0.012D;

    private float spinX;
    private float spinY;
    private float spinZ;
    private float roll;
    private float rollO;
    private int lastImpactSoundTick = -20;
    private boolean settled;

    public ShellCasingEntity(EntityType<? extends ShellCasingEntity> type, Level level) {
        super(type, level);
    }

    public ShellCasingEntity(Level level, CasingType type, ResourceLocation ammoId, Vec3 position, Vec3 velocity, float spinX, float spinY, float spinZ) {
        this(ModEntities.SHELL_CASING.get(), level);
        setCasingType(type);
        setAmmoId(ammoId);
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
        this.entityData.define(CASING_TYPE, CasingType.RIFLE.id());
        this.entityData.define(AMMO_ID, "tacz:556x45");
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide
                && this.tickCount > BallisticBlockConfig.CASING_LIFETIME.get()
                && !(this.settled && BallisticBlockConfig.PERSISTENT_SETTLED_CASINGS.get())) {
            discard();
            return;
        }

        this.xRotO = getXRot();
        this.yRotO = getYRot();
        this.rollO = this.roll;

        if (this.settled && onGround()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }

        Vec3 velocity = getDeltaMovement();
        CasingType type = getCasingType();
        if (!onGround()) {
            velocity = velocity.add(0.0D, -type.gravity(), 0.0D);
        }

        move(MoverType.SELF, velocity);

        if (this.horizontalCollision || this.verticalCollision) {
            velocity = bounce(velocity, type);
            maybePlayImpactSound(velocity);
        } else {
            velocity = velocity.scale(0.985D);
        }

        if (onGround()) {
            velocity = velocity.multiply(0.78D, 1.0D, 0.78D);
            if (velocity.lengthSqr() < REST_SPEED_SQR) {
                velocity = Vec3.ZERO;
                this.settled = true;
                this.spinX *= 0.6F;
                this.spinY *= 0.6F;
                this.spinZ *= 0.6F;
            }
        }

        setDeltaMovement(velocity);
        if (velocity != Vec3.ZERO) {
            setXRot(wrapDegrees(getXRot() + this.spinX));
            setYRot(wrapDegrees(getYRot() + this.spinY));
            this.roll = wrapDegrees(this.roll + this.spinZ);
        }
    }

    private Vec3 bounce(Vec3 velocity, CasingType type) {
        double x = this.horizontalCollision ? -velocity.x * type.bounce() : velocity.x;
        double y = this.verticalCollision ? -velocity.y * type.bounce() : velocity.y;
        double z = this.horizontalCollision ? -velocity.z * type.bounce() : velocity.z;
        return new Vec3(x, y, z).scale(0.82D);
    }

    private void maybePlayImpactSound(Vec3 velocity) {
    }

    public CasingType getCasingType() {
        return CasingType.byId(this.entityData.get(CASING_TYPE));
    }

    public ItemStack getRenderStack() {
        return new ItemStack(getCasingType().fallbackItem());
    }

    public ResourceLocation getAmmoId() {
        ResourceLocation parsed = ResourceLocation.tryParse(this.entityData.get(AMMO_ID));
        return parsed == null ? new ResourceLocation("tacz", "556x45") : parsed;
    }

    public float getRenderRoll(float partialTick) {
        return Mth.lerp(partialTick, this.rollO, this.roll);
    }

    private void setCasingType(CasingType type) {
        this.entityData.set(CASING_TYPE, type.id());
    }

    private void setAmmoId(ResourceLocation ammoId) {
        ResourceLocation safeAmmoId = ammoId == null ? new ResourceLocation("tacz", "556x45") : ammoId;
        this.entityData.set(AMMO_ID, safeAmmoId.toString());
    }

    private static float wrapDegrees(float value) {
        return Mth.wrapDegrees(value);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setCasingType(CasingType.byId(tag.getInt("CasingType")));
        setAmmoId(ResourceLocation.tryParse(tag.getString("AmmoId")));
        this.spinX = tag.getFloat("SpinX");
        this.spinY = tag.getFloat("SpinY");
        this.spinZ = tag.getFloat("SpinZ");
        this.roll = tag.getFloat("Roll");
        this.rollO = this.roll;
        this.settled = tag.getBoolean("Settled");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("CasingType", getCasingType().id());
        tag.putString("AmmoId", getAmmoId().toString());
        tag.putFloat("SpinX", this.spinX);
        tag.putFloat("SpinY", this.spinY);
        tag.putFloat("SpinZ", this.spinZ);
        tag.putFloat("Roll", this.roll);
        tag.putBoolean("Settled", this.settled);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeVarInt(getCasingType().id());
        buffer.writeResourceLocation(getAmmoId());
        buffer.writeFloat(this.spinX);
        buffer.writeFloat(this.spinY);
        buffer.writeFloat(this.spinZ);
        buffer.writeFloat(this.roll);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        setCasingType(CasingType.byId(buffer.readVarInt()));
        setAmmoId(buffer.readResourceLocation());
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
        return false;
    }
}
