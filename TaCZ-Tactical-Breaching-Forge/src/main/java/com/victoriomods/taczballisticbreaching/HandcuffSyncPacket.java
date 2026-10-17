package com.victoriomods.taczballisticbreaching;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record HandcuffSyncPacket(int entityId, boolean restrained) {
    static void encode(HandcuffSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeBoolean(packet.restrained);
    }

    static HandcuffSyncPacket decode(FriendlyByteBuf buffer) {
        return new HandcuffSyncPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    static void handle(HandcuffSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandcuffState.handle(packet));
        context.get().setPacketHandled(true);
    }
}
