package com.victoriomods.taczballisticbreaching;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "18";

    private ModNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(ImpactFxPacket.TYPE, ImpactFxPacket.STREAM_CODEC, ImpactFxPacket::handle);
        registrar.playToClient(MuzzleSmokePacket.TYPE, MuzzleSmokePacket.STREAM_CODEC, MuzzleSmokePacket::handle);
        registrar.playToClient(ArmorHitFxPacket.TYPE, ArmorHitFxPacket.STREAM_CODEC, ArmorHitFxPacket::handle);
        registrar.playToClient(BloodHitFxPacket.TYPE, BloodHitFxPacket.STREAM_CODEC, BloodHitFxPacket::handle);
        registrar.playToServer(NightstickFlashlightPacket.TYPE, NightstickFlashlightPacket.STREAM_CODEC, NightstickFlashlightPacket::handle);
        registrar.playToClient(FragExplosionFxPacket.TYPE, FragExplosionFxPacket.STREAM_CODEC, FragExplosionFxPacket::handle);
        registrar.playToClient(FlashbangDetonationFxPacket.TYPE, FlashbangDetonationFxPacket.STREAM_CODEC, FlashbangDetonationFxPacket::handle);
        registrar.playToClient(HandcuffSyncPacket.TYPE, HandcuffSyncPacket.STREAM_CODEC, HandcuffSyncPacket::handle);
        registrar.playToClient(DistantGunshotPacket.TYPE, DistantGunshotPacket.STREAM_CODEC, DistantGunshotPacket::handle);
        registrar.playToClient(ConfigSyncPacket.TYPE, ConfigSyncPacket.STREAM_CODEC, ConfigSyncPacket::handle);
        registrar.playToServer(ConfigUpdatePacket.TYPE, ConfigUpdatePacket.STREAM_CODEC, ConfigUpdatePacket::handle);
    }

    public static void sendToPlayersNear(ServerLevel level, Vec3 position, double radius, CustomPacketPayload payload) {
        double radiusSqr = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(position) <= radiusSqr) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
}
