package com.victoriomods.taczballisticbreaching;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ConfigSyncPacket(String key, String value) {
    private static final int MAX_STRING_LENGTH = 4096;

    static void encode(ConfigSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.key, MAX_STRING_LENGTH);
        buffer.writeUtf(packet.value, MAX_STRING_LENGTH);
    }

    static ConfigSyncPacket decode(FriendlyByteBuf buffer) {
        return new ConfigSyncPacket(buffer.readUtf(MAX_STRING_LENGTH), buffer.readUtf(MAX_STRING_LENGTH));
    }

    static void handle(ConfigSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ConfigCommandHandler.applyRemoteValue(packet.key, packet.value));
        context.get().setPacketHandled(true);
    }
}
