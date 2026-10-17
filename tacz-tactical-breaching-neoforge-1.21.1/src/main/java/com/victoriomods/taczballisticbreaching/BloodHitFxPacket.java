package com.victoriomods.taczballisticbreaching;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record BloodHitFxPacket(double x, double y, double z, double dirX, double dirY, double dirZ, int targetId, boolean highCaliber, boolean shotgun, boolean armorPiercing, boolean armored, boolean helmetHit, float armorReduction, float damageAmount) implements CustomPacketPayload {
    public static final Type<BloodHitFxPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TaczTacticalBreaching.MOD_ID, "blood_hit_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BloodHitFxPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> encode(packet, buffer),
            BloodHitFxPacket::decode
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void encode(BloodHitFxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat((float) packet.dirX);
        buffer.writeFloat((float) packet.dirY);
        buffer.writeFloat((float) packet.dirZ);
        buffer.writeVarInt(packet.targetId);
        buffer.writeBoolean(packet.highCaliber);
        buffer.writeBoolean(packet.shotgun);
        buffer.writeBoolean(packet.armorPiercing);
        buffer.writeBoolean(packet.armored);
        buffer.writeBoolean(packet.helmetHit);
        buffer.writeFloat(packet.armorReduction);
        buffer.writeFloat(packet.damageAmount);
    }

    static BloodHitFxPacket decode(FriendlyByteBuf buffer) {
        return new BloodHitFxPacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    static void handle(BloodHitFxPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> ClientBloodHitFxHandler.handle(packet));
        }
    }
}
