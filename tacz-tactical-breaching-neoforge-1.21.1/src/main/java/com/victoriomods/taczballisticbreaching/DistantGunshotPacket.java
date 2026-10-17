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

public record DistantGunshotPacket(double x, double y, double z, int soundType, float volume, float pitch, int delayMillis, int burstCount, int burstSpacingMillis, String gunId) implements CustomPacketPayload {
    public static final Type<DistantGunshotPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TaczTacticalBreaching.MOD_ID, "distant_gunshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DistantGunshotPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> encode(packet, buffer),
            DistantGunshotPacket::decode
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void encode(DistantGunshotPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeVarInt(packet.soundType);
        buffer.writeFloat(packet.volume);
        buffer.writeFloat(packet.pitch);
        buffer.writeVarInt(packet.delayMillis);
        buffer.writeVarInt(packet.burstCount);
        buffer.writeVarInt(packet.burstSpacingMillis);
        buffer.writeUtf(packet.gunId, 128);
    }

    static DistantGunshotPacket decode(FriendlyByteBuf buffer) {
        return new DistantGunshotPacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readVarInt(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readUtf(128)
        );
    }

    static void handle(DistantGunshotPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> ClientDistantGunshotHandler.handle(packet));
        }
    }
}
