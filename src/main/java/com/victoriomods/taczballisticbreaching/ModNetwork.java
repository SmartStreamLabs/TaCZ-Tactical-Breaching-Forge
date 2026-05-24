package com.victoriomods.taczballisticbreaching;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "13";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TaczTacticalBreaching.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int nextMessageId;

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(ImpactFxPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ImpactFxPacket::encode)
                .decoder(ImpactFxPacket::decode)
                .consumerMainThread(ImpactFxPacket::handle)
                .add();
        CHANNEL.messageBuilder(MuzzleSmokePacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MuzzleSmokePacket::encode)
                .decoder(MuzzleSmokePacket::decode)
                .consumerMainThread(MuzzleSmokePacket::handle)
                .add();
        CHANNEL.messageBuilder(ArmorHitFxPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ArmorHitFxPacket::encode)
                .decoder(ArmorHitFxPacket::decode)
                .consumerMainThread(ArmorHitFxPacket::handle)
                .add();
        CHANNEL.messageBuilder(BloodHitFxPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BloodHitFxPacket::encode)
                .decoder(BloodHitFxPacket::decode)
                .consumerMainThread(BloodHitFxPacket::handle)
                .add();
        CHANNEL.messageBuilder(NightstickFlashlightPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(NightstickFlashlightPacket::encode)
                .decoder(NightstickFlashlightPacket::decode)
                .consumerMainThread(NightstickFlashlightPacket::handle)
                .add();
        CHANNEL.messageBuilder(FragExplosionFxPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FragExplosionFxPacket::encode)
                .decoder(FragExplosionFxPacket::decode)
                .consumerMainThread(FragExplosionFxPacket::handle)
                .add();
        CHANNEL.messageBuilder(FlashbangDetonationFxPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FlashbangDetonationFxPacket::encode)
                .decoder(FlashbangDetonationFxPacket::decode)
                .consumerMainThread(FlashbangDetonationFxPacket::handle)
                .add();
    }
}
