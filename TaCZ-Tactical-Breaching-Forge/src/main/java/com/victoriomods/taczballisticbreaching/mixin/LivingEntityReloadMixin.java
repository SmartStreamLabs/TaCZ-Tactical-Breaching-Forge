package com.victoriomods.taczballisticbreaching.mixin;

import com.tacz.guns.entity.shooter.LivingEntityShoot;
import com.victoriomods.taczballisticbreaching.BallisticBlockConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.tacz.guns.entity.shooter.LivingEntityReload", remap = false)
public abstract class LivingEntityReloadMixin {
    @Redirect(
            method = "lambda$reload$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tacz/guns/entity/shooter/LivingEntityShoot;getShootCoolDown()J"
            ),
            require = 0
    )
    private long taczTacticalBreaching$allowInstantReloadAfterShot(LivingEntityShoot shoot) {
        if (BallisticBlockConfig.ALLOW_INSTANT_RELOAD_AFTER_SHOT.get()) {
            return 0L;
        }
        return shoot.getShootCoolDown();
    }
}
