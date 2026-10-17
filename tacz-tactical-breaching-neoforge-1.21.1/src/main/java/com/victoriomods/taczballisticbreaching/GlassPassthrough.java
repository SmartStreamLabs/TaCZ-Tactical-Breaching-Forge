package com.victoriomods.taczballisticbreaching;

import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.nbt.CompoundTag;

final class GlassPassthrough {
    private static final String MULTIPLIER_KEY = TaczTacticalBreaching.MOD_ID + ":glass_passthrough_multiplier";

    private GlassPassthrough() {
    }

    static void apply(EntityKineticBullet bullet) {
        CompoundTag data = bullet.getPersistentData();
        float current = data.contains(MULTIPLIER_KEY) ? data.getFloat(MULTIPLIER_KEY) : 1.0F;
        float updated = (float) Math.max(0.0D, current * BallisticBlockConfig.GLASS_PASSTHROUGH_DAMAGE_MULTIPLIER.get());
        data.putFloat(MULTIPLIER_KEY, updated);
        bullet.setShotDamageMultiplier(updated);
    }
}
