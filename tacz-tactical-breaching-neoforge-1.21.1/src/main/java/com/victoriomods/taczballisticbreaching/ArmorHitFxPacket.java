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

public record ArmorHitFxPacket(double x, double y, double z, double normalX, double normalY, double normalZ, int targetId, boolean highCaliber, boolean armorPiercing, boolean weakCaliber, boolean helmetHit, boolean vehicleHit, boolean deflected) implements CustomPacketPayload {
    public static final Type<ArmorHitFxPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TaczTacticalBreaching.MOD_ID, "armor_hit_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArmorHitFxPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> encode(packet, buffer),
            ArmorHitFxPacket::decode
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void encode(ArmorHitFxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat((float) packet.normalX);
        buffer.writeFloat((float) packet.normalY);
        buffer.writeFloat((float) packet.normalZ);
        buffer.writeVarInt(packet.targetId);
        buffer.writeBoolean(packet.highCaliber);
        buffer.writeBoolean(packet.armorPiercing);
        buffer.writeBoolean(packet.weakCaliber);
        buffer.writeBoolean(packet.helmetHit);
        buffer.writeBoolean(packet.vehicleHit);
        buffer.writeBoolean(packet.deflected);
    }

    static ArmorHitFxPacket decode(FriendlyByteBuf buffer) {
        return new ArmorHitFxPacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean()
        );
    }

    static void handle(ArmorHitFxPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> ClientArmorHitFxHandler.handle(packet));
        }
    }
}
