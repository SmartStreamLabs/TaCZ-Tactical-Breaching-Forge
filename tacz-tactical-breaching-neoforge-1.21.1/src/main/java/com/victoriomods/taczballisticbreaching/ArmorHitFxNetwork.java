package com.victoriomods.taczballisticbreaching;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

final class ArmorHitFxNetwork {
    private static final double SEND_RADIUS = 64.0D;

    private ArmorHitFxNetwork() {
    }

    static void send(ServerLevel level, Entity target, Vec3 location, Vec3 normal, boolean highCaliber, boolean armorPiercing, boolean weakCaliber, boolean helmetHit, boolean vehicleHit, boolean deflected) {
        if (!BallisticBlockConfig.ENABLE_ARMOR_HIT_EFFECTS.get()) {
            return;
        }
        Vec3 normalized = normal.lengthSqr() < 0.001D ? new Vec3(0.0D, 1.0D, 0.0D) : normal.normalize();
        ArmorHitFxPacket packet = new ArmorHitFxPacket(
                location.x,
                location.y,
                location.z,
                normalized.x,
                normalized.y,
                normalized.z,
                target.getId(),
                highCaliber,
                armorPiercing,
                weakCaliber,
                helmetHit,
                vehicleHit,
                deflected
        );
        ModNetwork.sendToPlayersNear(level, location, SEND_RADIUS, packet);
    }
}
