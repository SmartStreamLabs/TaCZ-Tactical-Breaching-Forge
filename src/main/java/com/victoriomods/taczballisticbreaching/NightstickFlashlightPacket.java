package com.victoriomods.taczballisticbreaching;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record NightstickFlashlightPacket(boolean enabled) {
    static void encode(NightstickFlashlightPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.enabled);
    }

    static NightstickFlashlightPacket decode(FriendlyByteBuf buffer) {
        return new NightstickFlashlightPacket(buffer.readBoolean());
    }

    static void handle(NightstickFlashlightPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                NightstickFlashlightLightHandler.setEnabled(player, packet.enabled);
            }
        });
        ctx.setPacketHandled(true);
    }
}
