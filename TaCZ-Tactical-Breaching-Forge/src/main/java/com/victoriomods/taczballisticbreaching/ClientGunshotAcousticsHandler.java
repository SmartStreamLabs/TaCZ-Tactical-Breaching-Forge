package com.victoriomods.taczballisticbreaching;

import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientGunshotAcousticsHandler {
    private ClientGunshotAcousticsHandler() {
    }

    // TaCZ already supplies the firearm audio. Legacy vanilla explosion, crossbow, and thunder echo layers are intentionally silent.
    public static void shot(MuzzleSmokePacket packet, Vec3 muzzle, Vec3 forward) {
    }

    public static void tick() {
    }

    public static void clear() {
    }
}
