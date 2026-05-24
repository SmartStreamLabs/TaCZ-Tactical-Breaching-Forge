package com.victoriomods.taczballisticbreaching;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MuzzleSmokePacket(double x, double y, double z, double forwardX, double forwardY, double forwardZ, int shooterId, int weaponKind, boolean automatic, boolean suppressed, boolean highCaliber) {
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

    static void handle(MuzzleSmokePacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientMuzzleSmokeHandler.handle(packet));
        context.get().setPacketHandled(true);
    }
}
