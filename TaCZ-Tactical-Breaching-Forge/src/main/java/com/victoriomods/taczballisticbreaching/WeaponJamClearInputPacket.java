package com.victoriomods.taczballisticbreaching;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record WeaponJamClearInputPacket(boolean pressed) {
    static void encode(WeaponJamClearInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.pressed);
    }

    static WeaponJamClearInputPacket decode(FriendlyByteBuf buffer) {
        return new WeaponJamClearInputPacket(buffer.readBoolean());
    }

    static void handle(WeaponJamClearInputPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                WeaponJamHandler.setClearInput(player, packet.pressed);
            }
        });
        context.get().setPacketHandled(true);
    }
}
