package com.victoriomods.taczballisticbreaching;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ImpactFxPacket(double x, double y, double z, double normalX, double normalY, double normalZ, int materialId, boolean glassPane, boolean penetrationExit, boolean shotgunImpact, boolean highCaliberImpact, boolean armorPiercingImpact, boolean automaticImpact, boolean finalBreak) {
    static void encode(ImpactFxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat((float) packet.normalX);
        buffer.writeFloat((float) packet.normalY);
        buffer.writeFloat((float) packet.normalZ);
        buffer.writeVarInt(packet.materialId);
        buffer.writeBoolean(packet.glassPane);
        buffer.writeBoolean(packet.penetrationExit);
        buffer.writeBoolean(packet.shotgunImpact);
        buffer.writeBoolean(packet.highCaliberImpact);
        buffer.writeBoolean(packet.armorPiercingImpact);
        buffer.writeBoolean(packet.automaticImpact);
        buffer.writeBoolean(packet.finalBreak);
    }

    static ImpactFxPacket decode(FriendlyByteBuf buffer) {
        return new ImpactFxPacket(
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
                buffer.readBoolean(),
                buffer.readBoolean()
        );
    }

    static void handle(ImpactFxPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientImpactFxHandler.handle(packet));
        context.get().setPacketHandled(true);
    }
}
