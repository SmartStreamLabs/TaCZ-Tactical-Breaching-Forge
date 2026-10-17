package com.victoriomods.taczballisticbreaching;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;

final class ClientSuppressedGunshotSilencer {
    private static boolean loggedTacz;
    private static boolean loggedSuperb;

    private ClientSuppressedGunshotSilencer() {
    }

    static boolean shouldMute(SoundInstance sound) {
        if (sound == null) {
            return false;
        }
        ResourceLocation id = sound.getLocation();
        String path = id.getPath();
        if ("tacz".equals(id.getNamespace()) && path.contains("silence") && path.contains("3p")) {
            if (!loggedTacz) {
                loggedTacz = true;
                TaczTacticalBreaching.LOGGER.info("Muted remote suppressed TaCZ gunshot sound {}", id);
            }
            return true;
        }
        if ("superbwarfare".equals(id.getNamespace())
                && (path.contains("fire_3p_s") || path.contains("_far_s") || path.contains("_veryfar_s"))) {
            if (!loggedSuperb) {
                loggedSuperb = true;
                TaczTacticalBreaching.LOGGER.info("Muted remote suppressed Superb Warfare gunshot sound {}", id);
            }
            return true;
        }
        return false;
    }
}
