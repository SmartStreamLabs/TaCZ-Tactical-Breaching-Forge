package com.victoriomods.taczballisticbreaching;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ConfigUpdatePacket(String key, String value) {
    private static final int MAX_KEY_LENGTH = 256;
    private static final int MAX_VALUE_LENGTH = 4096;

    static void encode(ConfigUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.key, MAX_KEY_LENGTH);
        buffer.writeUtf(packet.value, MAX_VALUE_LENGTH);
    }

    static ConfigUpdatePacket decode(FriendlyByteBuf buffer) {
        return new ConfigUpdatePacket(
                buffer.readUtf(MAX_KEY_LENGTH),
                buffer.readUtf(MAX_VALUE_LENGTH)
        );
    }

    static void handle(ConfigUpdatePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context networkContext = context.get();
        networkContext.enqueueWork(() -> {
            ServerPlayer sender = networkContext.getSender();
            if (sender != null) {
                ConfigCommandHandler.applyClientUpdate(sender, packet.key, packet.value);
            }
        });
        networkContext.setPacketHandled(true);
    }
}
