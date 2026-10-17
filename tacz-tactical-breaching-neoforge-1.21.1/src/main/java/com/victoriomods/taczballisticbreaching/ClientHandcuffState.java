package com.victoriomods.taczballisticbreaching;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashSet;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class ClientHandcuffState {
    private static final Set<Integer> RESTRAINED_ENTITIES = new HashSet<>();

    private ClientHandcuffState() {
    }

    public static void handle(HandcuffSyncPacket packet) {
        if (packet.restrained()) {
            RESTRAINED_ENTITIES.add(packet.entityId());
        } else {
            RESTRAINED_ENTITIES.remove(packet.entityId());
        }
    }

    public static boolean isRestrained(int entityId) {
        return RESTRAINED_ENTITIES.contains(entityId);
    }

    public static void clear() {
        RESTRAINED_ENTITIES.clear();
    }
}
