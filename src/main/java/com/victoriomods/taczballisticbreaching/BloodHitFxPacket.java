package com.victoriomods.taczballisticbreaching;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BloodHitFxPacket(double x, double y, double z, double dirX, double dirY, double dirZ, int targetId, boolean highCaliber, boolean shotgun, boolean armorPiercing, boolean armored, boolean helmetHit, float armorReduction) {
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
                buffer.readFloat()
        );
    }

    static void handle(BloodHitFxPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBloodHitFxHandler.handle(packet));
        context.get().setPacketHandled(true);
    }
}
