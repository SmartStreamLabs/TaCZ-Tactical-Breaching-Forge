package com.victoriomods.taczballisticbreaching;

import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

final class TaczBulletDamageCompat {
    static final String PENETRATION_DAMAGE_MULTIPLIER_KEY = TaczTacticalBreaching.MOD_ID + ":penetration_damage_multiplier";
    static final String GLASS_PASSTHROUGH_MULTIPLIER_KEY = TaczTacticalBreaching.MOD_ID + ":glass_passthrough_multiplier";

    private static boolean lookupComplete;
    private static boolean unavailableLogged;
    private static Method setShotDamageMultiplierMethod;

    private TaczBulletDamageCompat() {
    }

    static float getStoredMultiplier(EntityKineticBullet bullet, String key) {
        CompoundTag data = bullet.getPersistentData();
        return data.contains(key) ? data.getFloat(key) : 1.0F;
    }

    static void storeMultiplierAndTryApply(EntityKineticBullet bullet, String key, float multiplier) {
        bullet.getPersistentData().putFloat(key, Math.max(0.0F, multiplier));
        tryApplyShotDamageMultiplier(bullet, combinedStoredMultiplier(bullet));
    }

    static float getDamageWithStoredMultipliers(EntityKineticBullet bullet, Vec3 hitLocation) {
        return Math.max(0.0F, bullet.getDamage(hitLocation) * combinedStoredMultiplier(bullet));
    }

    private static float combinedStoredMultiplier(EntityKineticBullet bullet) {
        CompoundTag data = bullet.getPersistentData();
        float penetration = data.contains(PENETRATION_DAMAGE_MULTIPLIER_KEY) ? data.getFloat(PENETRATION_DAMAGE_MULTIPLIER_KEY) : 1.0F;
        float glass = data.contains(GLASS_PASSTHROUGH_MULTIPLIER_KEY) ? data.getFloat(GLASS_PASSTHROUGH_MULTIPLIER_KEY) : 1.0F;
        return Math.max(0.0F, penetration * glass);
    }

    private static void tryApplyShotDamageMultiplier(EntityKineticBullet bullet, float multiplier) {
        Method method = findSetShotDamageMultiplierMethod();
        if (method == null) {
            logUnavailableOnce();
            return;
        }
        try {
            method.invoke(bullet, multiplier);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logUnavailableOnce();
            TaczTacticalBreaching.LOGGER.debug("Could not invoke TaCZ bullet damage multiplier API; using fallback damage reduction.", exception);
        }
    }

    private static Method findSetShotDamageMultiplierMethod() {
        if (lookupComplete) {
            return setShotDamageMultiplierMethod;
        }
        lookupComplete = true;
        try {
            setShotDamageMultiplierMethod = EntityKineticBullet.class.getMethod("setShotDamageMultiplier", float.class);
        } catch (NoSuchMethodException exception) {
            setShotDamageMultiplierMethod = null;
        }
        return setShotDamageMultiplierMethod;
    }

    private static void logUnavailableOnce() {
        if (unavailableLogged) {
            return;
        }
        unavailableLogged = true;
        TaczTacticalBreaching.LOGGER.warn("TaCZ EntityKineticBullet#setShotDamageMultiplier(float) is unavailable; using internal penetration damage fallback only.");
    }
}
