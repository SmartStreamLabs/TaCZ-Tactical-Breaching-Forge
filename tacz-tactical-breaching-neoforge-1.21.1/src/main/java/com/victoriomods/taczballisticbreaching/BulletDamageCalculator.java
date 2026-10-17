package com.victoriomods.taczballisticbreaching;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

final class BulletDamageCalculator {
    private BulletDamageCalculator() {
    }

    static double calculate(EntityKineticBullet bullet, Vec3 hitLocation) {
        double damage = Math.max(bullet.getDamage(hitLocation), 0.0F);
        ResourceLocation gunId = bullet.getGunId();
        ResourceLocation ammoId = bullet.getAmmoId();

        damage *= BallisticBlockConfig.DAMAGE_MULTIPLIER.get();
        damage *= BallisticBlockRules.getConfiguredMultiplier(gunId, BallisticBlockConfig.GUN_DAMAGE_MULTIPLIERS.get(), 1.0D);
        damage *= BallisticBlockRules.getConfiguredMultiplier(ammoId, BallisticBlockConfig.AMMO_DAMAGE_MULTIPLIERS.get(), 1.0D);

        if (isShotgun(gunId, ammoId) && isCloseRange(bullet, hitLocation)) {
            damage *= BallisticBlockConfig.SHOTGUN_CLOSE_RANGE_MULTIPLIER.get();
        }

        return damage;
    }

    static boolean isShotgunImpact(EntityKineticBullet bullet, Vec3 hitLocation) {
        return isShotgun(bullet.getGunId(), bullet.getAmmoId()) && isCloseRange(bullet, hitLocation);
    }

    static boolean isHighCaliber(EntityKineticBullet bullet) {
        ResourceLocation gunId = bullet.getGunId();
        ResourceLocation ammoId = bullet.getAmmoId();
        return containsAny(gunId, "50", "bmg", "127", "m107", "m95", "338", "lapua", "762", "7_62", "7.62", "308")
                || containsAny(ammoId, "50", "bmg", "127", "338", "lapua", "762", "7_62", "7.62", "308");
    }

    static boolean isArmorPiercing(EntityKineticBullet bullet) {
        return containsAny(bullet.getAmmoId(), "ap", "armor_piercing", "armour_piercing", "fmj", "steel_core")
                || containsAny(bullet.getGunId(), "ap", "anti_material", "m107", "m95");
    }

    static boolean isAutomaticWeapon(EntityKineticBullet bullet) {
        return containsAny(bullet.getGunId(), "smg", "lmg", "mg", "ak", "m4", "m16", "scar", "g36", "vector", "minigun", "auto");
    }

    private static boolean isShotgun(ResourceLocation gunId, ResourceLocation ammoId) {
        try {
            if (TimelessAPI.getCommonGunIndex(gunId).map(BulletDamageCalculator::isShotgunIndex).orElse(false)) {
                return true;
            }
        } catch (RuntimeException exception) {
            TaczTacticalBreaching.LOGGER.debug("Could not inspect TaCZ gun index {}", gunId, exception);
        }
        return ammoId != null && ammoId.getPath().toLowerCase(java.util.Locale.ROOT).contains("12g");
    }

    private static boolean isShotgunIndex(CommonGunIndex index) {
        return "shotgun".equalsIgnoreCase(index.getType()) || index.getBulletData().getBulletAmount() > 1;
    }

    private static boolean isCloseRange(EntityKineticBullet bullet, Vec3 hitLocation) {
        Entity owner = bullet.getOwner();
        if (owner == null) {
            return false;
        }
        double closeRange = BallisticBlockConfig.SHOTGUN_CLOSE_RANGE.get();
        return owner.position().distanceTo(hitLocation) <= closeRange;
    }

    private static boolean containsAny(ResourceLocation id, String... needles) {
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase(java.util.Locale.ROOT);
        for (String needle : needles) {
            if (path.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
