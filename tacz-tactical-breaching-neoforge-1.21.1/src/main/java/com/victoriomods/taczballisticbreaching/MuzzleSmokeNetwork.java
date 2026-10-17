package com.victoriomods.taczballisticbreaching;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

final class MuzzleSmokeNetwork {
    private static final double SEND_RADIUS = 72.0D;

    private MuzzleSmokeNetwork() {
    }

    static void send(ServerLevel level, LivingEntity shooter, ItemStack gunStack) {
        send(level, shooter, inspectGun(gunStack));
    }

    static void sendSuperb(ServerLevel level, LivingEntity shooter, ItemStack gunStack, boolean suppressed) {
        GunSmokeInfo base = fromGunId(BuiltInRegistries.ITEM.getKey(gunStack.getItem()), gunStack);
        send(level, shooter, new GunSmokeInfo(base.kind(), base.automatic(), suppressed, base.highCaliber()));
    }

    private static void send(ServerLevel level, LivingEntity shooter, GunSmokeInfo info) {
        if (!BallisticBlockConfig.ENABLE_MUZZLE_SMOKE.get()
                && !BallisticBlockConfig.ENABLE_DYNAMIC_GUN_LIGHTING.get()
                && !BallisticBlockConfig.ENABLE_WEAPON_HEAT.get()) {
            return;
        }

        Vec3 forward = shooter.getLookAngle();
        if (forward.lengthSqr() < 0.001D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            forward = forward.normalize();
        }
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 0.0001D) {
            float yaw = shooter.getYRot() * Mth.DEG_TO_RAD;
            right = new Vec3(Mth.cos(yaw), 0.0D, Mth.sin(yaw));
        } else {
            right = right.normalize();
        }

        Vec3 muzzle = shooter.getEyePosition()
                .add(forward.scale(info.kind() == WeaponKind.PISTOL ? 0.48D : 0.72D))
                .add(right.scale(0.055D))
                .add(0.0D, -0.08D, 0.0D);

        MuzzleSmokePacket packet = new MuzzleSmokePacket(
                muzzle.x,
                muzzle.y,
                muzzle.z,
                forward.x,
                forward.y,
                forward.z,
                shooter.getId(),
                info.kind().id(),
                info.automatic(),
                info.suppressed(),
                info.highCaliber()
        );
        ModNetwork.sendToPlayersNear(level, muzzle, SEND_RADIUS, packet);
    }

    private static GunSmokeInfo inspectGun(ItemStack gunStack) {
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null) {
            return new GunSmokeInfo(WeaponKind.RIFLE, false, false, false);
        }

        ResourceLocation gunId = gun.getGunId(gunStack);
        try {
            return TimelessAPI.getCommonGunIndex(gunId)
                    .map(index -> fromGunIndex(index, gunId, gunStack))
                    .orElseGet(() -> fromGunId(gunId, gunStack));
        } catch (RuntimeException exception) {
            TaczTacticalBreaching.LOGGER.debug("Could not inspect TaCZ gun index {} for muzzle smoke", gunId, exception);
            return fromGunId(gunId, gunStack);
        }
    }

    private static GunSmokeInfo fromGunIndex(CommonGunIndex index, ResourceLocation gunId, ItemStack gunStack) {
        String type = index.getType() == null ? "" : index.getType().toLowerCase(Locale.ROOT);
        ResourceLocation ammoId = index.getGunData().getAmmoId();
        String ammoPath = ammoId == null ? "" : ammoId.getPath().toLowerCase(Locale.ROOT);

        WeaponKind kind = WeaponKind.RIFLE;
        if (type.contains("shotgun") || index.getBulletData().getBulletAmount() > 1 || ammoPath.contains("12g")) {
            kind = WeaponKind.SHOTGUN;
        } else if (type.contains("pistol") || ammoPath.contains("9mm") || ammoPath.contains("45acp") || ammoPath.contains("357") || ammoPath.contains("50ae")) {
            kind = WeaponKind.PISTOL;
        } else if (containsAny(gunId, "m107", "m95", "sniper", "awm", "barrett") || ammoPath.contains("50") || ammoPath.contains("338")) {
            kind = WeaponKind.SNIPER;
        }

        boolean automatic = containsAny(gunId, "smg", "lmg", "mg", "ak", "m4", "m16", "scar", "g36", "vector", "minigun", "auto");
        boolean highCaliber = kind == WeaponKind.SHOTGUN || kind == WeaponKind.SNIPER || containsAny(gunId, "50", "bmg", "127", "338", "762", "308") || ammoPath.contains("50") || ammoPath.contains("338") || ammoPath.contains("762") || ammoPath.contains("308");
        return new GunSmokeInfo(kind, automatic, isSuppressed(gunId, gunStack), highCaliber);
    }

    private static GunSmokeInfo fromGunId(ResourceLocation gunId, ItemStack gunStack) {
        WeaponKind kind = WeaponKind.RIFLE;
        if (containsAny(gunId, "shotgun", "m870", "m1014", "spas", "aa12", "db_")) {
            kind = WeaponKind.SHOTGUN;
        } else if (containsAny(gunId, "glock", "pistol", "m1911", "m9", "p320", "cz75", "b93r", "deagle", "mk23", "rhino", "taurus")) {
            kind = WeaponKind.PISTOL;
        } else if (containsAny(gunId, "m107", "m95", "sniper", "awm", "barrett")) {
            kind = WeaponKind.SNIPER;
        }
        boolean automatic = containsAny(gunId, "smg", "lmg", "mg", "ak", "m4", "m16", "scar", "g36", "vector", "minigun", "auto");
        boolean highCaliber = kind == WeaponKind.SHOTGUN || kind == WeaponKind.SNIPER || containsAny(gunId, "50", "bmg", "127", "338", "762", "308");
        return new GunSmokeInfo(kind, automatic, isSuppressed(gunId, gunStack), highCaliber);
    }

    private static boolean isSuppressed(ResourceLocation gunId, ItemStack gunStack) {
        if (containsAny(gunId, "suppressed", "suppressor", "silencer", "_sd", "-sd", "sd_")) {
            return true;
        }
        ResourceLocation muzzleId = muzzleAttachmentId(gunStack);
        if (isSuppressorId(muzzleId)) {
            return true;
        }
        String tagText = gunStack.getComponents().toString().toLowerCase(Locale.ROOT);
        return containsAnyText(tagText, "suppressor", "silencer", "muzzle_suppressor", "muzzle_silencer");
    }

    private static ResourceLocation muzzleAttachmentId(ItemStack gunStack) {
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null) {
            return null;
        }
        try {
            ResourceLocation id = gun.getAttachmentId(gunStack, AttachmentType.MUZZLE);
            if (id != null) {
                return id;
            }
            return gun.getBuiltInAttachmentId(gunStack, AttachmentType.MUZZLE);
        } catch (RuntimeException | LinkageError exception) {
            TaczTacticalBreaching.LOGGER.debug("Could not inspect TaCZ muzzle attachment for suppressor detection", exception);
            return null;
        }
    }

    private static boolean isSuppressorId(ResourceLocation muzzleId) {
        if (muzzleId == null) {
            return false;
        }
        String text = muzzleId.toString().toLowerCase(Locale.ROOT);
        return containsAnyText(text, "suppressor", "silencer", "suppressed", "muffler", "moderator", "muzzle_suppressor", "pbs", "nt4", "osprey", "omega", "_sd", "-sd", "/sd", "sd_");
    }

    private static boolean containsAny(ResourceLocation id, String... needles) {
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase(Locale.ROOT);
        for (String needle : needles) {
            if (path.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAnyText(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    enum WeaponKind {
        PISTOL(0),
        RIFLE(1),
        SHOTGUN(2),
        SNIPER(3);

        private final int id;

        WeaponKind(int id) {
            this.id = id;
        }

        int id() {
            return id;
        }

        static WeaponKind byId(int id) {
            for (WeaponKind kind : values()) {
                if (kind.id == id) {
                    return kind;
                }
            }
            return RIFLE;
        }
    }

    private record GunSmokeInfo(WeaponKind kind, boolean automatic, boolean suppressed, boolean highCaliber) {
    }
}
