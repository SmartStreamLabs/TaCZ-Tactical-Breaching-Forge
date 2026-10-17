package com.victoriomods.taczballisticbreaching;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ConfigSyncPacket(String key, String value) implements CustomPacketPayload {
    private static final int MAX_STRING_LENGTH = 4096;
    public static final Type<ConfigSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TaczTacticalBreaching.MOD_ID, "config_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> encode(packet, buffer),
            ConfigSyncPacket::decode
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void encode(ConfigSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.key, MAX_STRING_LENGTH);
        buffer.writeUtf(packet.value, MAX_STRING_LENGTH);
    }

    static ConfigSyncPacket decode(FriendlyByteBuf buffer) {
        return new ConfigSyncPacket(buffer.readUtf(MAX_STRING_LENGTH), buffer.readUtf(MAX_STRING_LENGTH));
    }

    static void handle(ConfigSyncPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> ConfigCommandHandler.applyRemoteValue(packet.key, packet.value));
        }
    }
}
