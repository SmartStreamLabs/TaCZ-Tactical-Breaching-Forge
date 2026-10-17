package com.victoriomods.taczballisticbreaching;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ConfigUpdatePacket(String key, String value) implements CustomPacketPayload {
    private static final int MAX_KEY_LENGTH = 256;
    private static final int MAX_VALUE_LENGTH = 4096;
    public static final Type<ConfigUpdatePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TaczTacticalBreaching.MOD_ID, "config_update")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigUpdatePacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> encode(packet, buffer),
            ConfigUpdatePacket::decode
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

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

    static void handle(ConfigUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ConfigCommandHandler.applyClientUpdate(player, packet.key, packet.value);
            }
        });
    }
}
