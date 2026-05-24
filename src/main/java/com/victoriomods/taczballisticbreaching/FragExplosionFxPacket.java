package com.victoriomods.taczballisticbreaching;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record FragExplosionFxPacket(double x, double y, double z, float radius) {
    static void encode(FragExplosionFxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat(packet.radius);
    }

    static FragExplosionFxPacket decode(FriendlyByteBuf buffer) {
        return new FragExplosionFxPacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat()
        );
    }

    static void handle(FragExplosionFxPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFragExplosionFxHandler.handle(packet));
        context.get().setPacketHandled(true);
    }
}
