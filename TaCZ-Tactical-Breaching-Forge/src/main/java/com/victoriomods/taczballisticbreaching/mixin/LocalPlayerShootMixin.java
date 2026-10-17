package com.victoriomods.taczballisticbreaching.mixin;

import com.tacz.guns.api.entity.IGunOperator;
import com.victoriomods.taczballisticbreaching.BallisticBlockConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.tacz.guns.client.gameplay.LocalPlayerShoot", remap = false)
public abstract class LocalPlayerShootMixin {
    @Redirect(
            method = "shoot",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tacz/guns/api/entity/IGunOperator;getSynSprintTime()F"
            ),
            require = 0
    )
    private float taczTacticalBreaching$allowShootWhileSprinting(IGunOperator operator) {
        if (BallisticBlockConfig.ALLOW_SHOOT_WHILE_SPRINTING.get()) {
            return 0.0F;
        }
        return operator.getSynSprintTime();
    }
}
