package com.victoriomods.taczballisticbreaching;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

final class BloodHitFxNetwork {
    private static final double SEND_RADIUS = 64.0D;

    private BloodHitFxNetwork() {
    }

    static void send(ServerLevel level, Entity target, Vec3 location, Vec3 direction, boolean highCaliber, boolean shotgun, boolean armorPiercing, boolean armored, boolean helmetHit, float armorReduction) {
        if (!BallisticBlockConfig.ENABLE_BLOOD_MIST.get() && !BallisticBlockConfig.ENABLE_WALL_SPLATTER.get()) {
            return;
        }
        Vec3 normalized = direction.lengthSqr() < 0.001D ? target.getLookAngle() : direction.normalize();
        BloodHitFxPacket packet = new BloodHitFxPacket(
                location.x,
                location.y,
                location.z,
                normalized.x,
                normalized.y,
                normalized.z,
                target.getId(),
                highCaliber,
                shotgun,
                armorPiercing,
                armored,
                helmetHit,
                armorReduction
        );
        ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(location.x, location.y, location.z, SEND_RADIUS, level.dimension())), packet);
    }
}
