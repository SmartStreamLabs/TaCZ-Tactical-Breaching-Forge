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

public record ImpactFxPacket(double x, double y, double z, double normalX, double normalY, double normalZ, int materialId, boolean glassPane, boolean penetrationExit, boolean shotgunImpact, boolean highCaliberImpact, boolean armorPiercingImpact, boolean automaticImpact, boolean finalBreak) implements CustomPacketPayload {
    public static final Type<ImpactFxPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TaczTacticalBreaching.MOD_ID, "impact_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ImpactFxPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> encode(packet, buffer),
            ImpactFxPacket::decode
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void encode(ImpactFxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat((float) packet.normalX);
        buffer.writeFloat((float) packet.normalY);
        buffer.writeFloat((float) packet.normalZ);
        buffer.writeVarInt(packet.materialId);
        buffer.writeBoolean(packet.glassPane);
        buffer.writeBoolean(packet.penetrationExit);
        buffer.writeBoolean(packet.shotgunImpact);
        buffer.writeBoolean(packet.highCaliberImpact);
        buffer.writeBoolean(packet.armorPiercingImpact);
        buffer.writeBoolean(packet.automaticImpact);
        buffer.writeBoolean(packet.finalBreak);
    }

    static ImpactFxPacket decode(FriendlyByteBuf buffer) {
        return new ImpactFxPacket(
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
                buffer.readBoolean(),
                buffer.readBoolean()
        );
    }

    static void handle(ImpactFxPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> ClientImpactFxHandler.handle(packet));
        }
    }
}
