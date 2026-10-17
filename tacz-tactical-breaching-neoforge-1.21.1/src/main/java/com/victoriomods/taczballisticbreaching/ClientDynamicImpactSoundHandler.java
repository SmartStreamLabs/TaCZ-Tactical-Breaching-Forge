package com.victoriomods.taczballisticbreaching;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientDynamicImpactSoundHandler {
    private ClientDynamicImpactSoundHandler() {
    }

    // The old material voices were built from vanilla block/entity sounds. Keep the visual pipeline active but play no fallback audio.
    public static void playBlockImpact(ImpactFxPacket packet, ImpactMaterial material) {
    }

    public static void playArmorImpact(ArmorHitFxPacket packet) {
    }

    public static boolean enabled() {
        return true;
    }
}
