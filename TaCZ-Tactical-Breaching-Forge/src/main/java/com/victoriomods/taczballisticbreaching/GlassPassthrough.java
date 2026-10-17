package com.victoriomods.taczballisticbreaching;

import com.tacz.guns.entity.EntityKineticBullet;

final class GlassPassthrough {
    private GlassPassthrough() {
    }

    static void apply(EntityKineticBullet bullet) {
        float current = TaczBulletDamageCompat.getStoredMultiplier(bullet, TaczBulletDamageCompat.GLASS_PASSTHROUGH_MULTIPLIER_KEY);
        float updated = (float) Math.max(0.0D, current * BallisticBlockConfig.GLASS_PASSTHROUGH_DAMAGE_MULTIPLIER.get());
        TaczBulletDamageCompat.storeMultiplierAndTryApply(bullet, TaczBulletDamageCompat.GLASS_PASSTHROUGH_MULTIPLIER_KEY, updated);
    }
}
