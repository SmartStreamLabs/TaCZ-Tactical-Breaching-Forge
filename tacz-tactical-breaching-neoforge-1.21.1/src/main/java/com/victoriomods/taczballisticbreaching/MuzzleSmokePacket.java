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

public record MuzzleSmokePacket(double x, double y, double z, double forwardX, double forwardY, double forwardZ, int shooterId, int weaponKind, boolean automatic, boolean suppressed, boolean highCaliber) implements CustomPacketPayload {
    public static final Type<MuzzleSmokePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TaczTacticalBreaching.MOD_ID, "muzzle_smoke"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MuzzleSmokePacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> encode(packet, buffer),
            MuzzleSmokePacket::decode
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void encode(MuzzleSmokePacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat((float) packet.forwardX);
        buffer.writeFloat((float) packet.forwardY);
        buffer.writeFloat((float) packet.forwardZ);
        buffer.writeVarInt(packet.shooterId);
        buffer.writeVarInt(packet.weaponKind);
        buffer.writeBoolean(packet.automatic);
        buffer.writeBoolean(packet.suppressed);
        buffer.writeBoolean(packet.highCaliber);
    }

    static MuzzleSmokePacket decode(FriendlyByteBuf buffer) {
        return new MuzzleSmokePacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean()
        );
    }

    static void handle(MuzzleSmokePacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> ClientMuzzleSmokeHandler.handle(packet));
        }
    }
}
