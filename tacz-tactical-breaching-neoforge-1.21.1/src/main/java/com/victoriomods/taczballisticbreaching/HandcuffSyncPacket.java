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

public record HandcuffSyncPacket(int entityId, boolean restrained) implements CustomPacketPayload {
    public static final Type<HandcuffSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TaczTacticalBreaching.MOD_ID, "handcuff_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HandcuffSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> encode(packet, buffer),
            HandcuffSyncPacket::decode
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void encode(HandcuffSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeBoolean(packet.restrained);
    }

    static HandcuffSyncPacket decode(FriendlyByteBuf buffer) {
        return new HandcuffSyncPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    static void handle(HandcuffSyncPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> ClientHandcuffState.handle(packet));
        }
    }
}
