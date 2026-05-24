package com.victoriomods.taczballisticbreaching;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

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
) {
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

    static void handle(FlashbangDetonationFxPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFlashbangFxHandler.handle(packet));
        context.get().setPacketHandled(true);
    }
}
