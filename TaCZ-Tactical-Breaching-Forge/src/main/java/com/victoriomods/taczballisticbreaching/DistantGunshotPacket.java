package com.victoriomods.taczballisticbreaching;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DistantGunshotPacket(double x, double y, double z, int soundType, float volume, float pitch, int delayMillis, int burstCount, int burstSpacingMillis, String gunId) {
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

    static void handle(DistantGunshotPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientDistantGunshotHandler.handle(packet));
        context.get().setPacketHandled(true);
    }
}
