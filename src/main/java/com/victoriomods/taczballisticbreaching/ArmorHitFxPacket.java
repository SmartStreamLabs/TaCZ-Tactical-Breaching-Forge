package com.victoriomods.taczballisticbreaching;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ArmorHitFxPacket(double x, double y, double z, double normalX, double normalY, double normalZ, int targetId, boolean highCaliber, boolean armorPiercing, boolean weakCaliber, boolean helmetHit, boolean vehicleHit, boolean deflected) {
    static void encode(ArmorHitFxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat((float) packet.normalX);
        buffer.writeFloat((float) packet.normalY);
        buffer.writeFloat((float) packet.normalZ);
        buffer.writeVarInt(packet.targetId);
        buffer.writeBoolean(packet.highCaliber);
        buffer.writeBoolean(packet.armorPiercing);
        buffer.writeBoolean(packet.weakCaliber);
        buffer.writeBoolean(packet.helmetHit);
        buffer.writeBoolean(packet.vehicleHit);
        buffer.writeBoolean(packet.deflected);
    }

    static ArmorHitFxPacket decode(FriendlyByteBuf buffer) {
        return new ArmorHitFxPacket(
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
                buffer.readBoolean()
        );
    }

    static void handle(ArmorHitFxPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientArmorHitFxHandler.handle(packet));
        context.get().setPacketHandled(true);
    }
}
