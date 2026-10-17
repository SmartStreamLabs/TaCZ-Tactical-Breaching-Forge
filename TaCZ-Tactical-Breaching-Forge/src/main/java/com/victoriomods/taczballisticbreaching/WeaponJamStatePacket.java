package com.victoriomods.taczballisticbreaching;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record WeaponJamStatePacket(boolean jammed, float condition, int clearProgressTicks, int clearTotalTicks) {
    static void encode(WeaponJamStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.jammed);
        buffer.writeFloat(packet.condition);
        buffer.writeVarInt(packet.clearProgressTicks);
        buffer.writeVarInt(packet.clearTotalTicks);
    }

    static WeaponJamStatePacket decode(FriendlyByteBuf buffer) {
        return new WeaponJamStatePacket(
                buffer.readBoolean(),
                buffer.readFloat(),
                buffer.readVarInt(),
                buffer.readVarInt()
        );
    }

    static void handle(WeaponJamStatePacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientWeaponJamHandler.handleState(packet));
        context.get().setPacketHandled(true);
    }
}
