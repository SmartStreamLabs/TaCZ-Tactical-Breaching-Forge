package com.victoriomods.taczballisticbreaching;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record NightstickFlashlightPacket(boolean enabled) implements CustomPacketPayload {
    public static final Type<NightstickFlashlightPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TaczTacticalBreaching.MOD_ID, "nightstick_flashlight"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NightstickFlashlightPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> encode(packet, buffer),
            NightstickFlashlightPacket::decode
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void encode(NightstickFlashlightPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.enabled);
    }

    static NightstickFlashlightPacket decode(FriendlyByteBuf buffer) {
        return new NightstickFlashlightPacket(buffer.readBoolean());
    }

    static void handle(NightstickFlashlightPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player != null) {
                NightstickFlashlightLightHandler.setEnabled(player, packet.enabled);
            }
        });
    }
}
