package com.victoriomods.taczballisticbreaching.mixin;

import com.victoriomods.taczballisticbreaching.BallisticBlockConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(targets = "com.tacz.guns.client.gameplay.LocalPlayerReload", remap = false)
public abstract class LocalPlayerReloadMixin {
    @ModifyConstant(method = "lambda$reload$2", constant = @Constant(longValue = 100L), require = 0)
    private long taczTacticalBreaching$allowInstantReloadAfterShot(long original) {
        if (BallisticBlockConfig.ALLOW_INSTANT_RELOAD_AFTER_SHOT.get()) {
            return 0L;
        }
        return original;
    }
}
