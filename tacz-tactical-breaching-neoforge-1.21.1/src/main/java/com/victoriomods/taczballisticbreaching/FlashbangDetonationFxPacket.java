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

public record FlashbangDetonationFxPacket(
        double x,
        double y,
        double z,
        float effectStrength,
        float directStrength,
        int flashDurationTicks,
        int tinnitusDurationTicks,
        float blurStrength,
        boolean indoor,
        boolean lineOfSight
) implements CustomPacketPayload {
    public static final Type<FlashbangDetonationFxPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TaczTacticalBreaching.MOD_ID, "flashbang_detonation_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlashbangDetonationFxPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> encode(packet, buffer),
            FlashbangDetonationFxPacket::decode
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void encode(FlashbangDetonationFxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat(packet.effectStrength);
        buffer.writeFloat(packet.directStrength);
        buffer.writeVarInt(packet.flashDurationTicks);
        buffer.writeVarInt(packet.tinnitusDurationTicks);
        buffer.writeFloat(packet.blurStrength);
        buffer.writeBoolean(packet.indoor);
        buffer.writeBoolean(packet.lineOfSight);
    }

    static FlashbangDetonationFxPacket decode(FriendlyByteBuf buffer) {
        return new FlashbangDetonationFxPacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readFloat(),
                buffer.readBoolean(),
                buffer.readBoolean()
        );
    }

    static void handle(FlashbangDetonationFxPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> ClientFlashbangFxHandler.handle(packet));
        }
    }
}
