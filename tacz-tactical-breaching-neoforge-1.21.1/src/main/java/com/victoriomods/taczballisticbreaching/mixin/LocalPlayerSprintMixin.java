package com.victoriomods.taczballisticbreaching.mixin;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.victoriomods.taczballisticbreaching.BallisticBlockConfig;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.tacz.guns.client.gameplay.LocalPlayerSprint", remap = false)
public abstract class LocalPlayerSprintMixin {
    @Shadow(remap = false)
    private LocalPlayer player;

    @Inject(method = "getProcessedSprintStatus", at = @At("HEAD"), cancellable = true, require = 0)
    private void taczTacticalBreaching$keepPlayerSprintingWhenAllowed(boolean sprinting, CallbackInfoReturnable<Boolean> cir) {
        if (!sprinting) {
            return;
        }
        IGunOperator operator = IGunOperator.fromLivingEntity(player);
        if (operator.getSynIsAiming()) {
            return;
        }
        ReloadState.StateType stateType = operator.getSynReloadState().getStateType();
        if (stateType.isReloading() && !stateType.isReloadFinishing()) {
            if (BallisticBlockConfig.ALLOW_RELOAD_WHILE_SPRINTING.get()) {
                cir.setReturnValue(true);
            }
            return;
        }
        if (BallisticBlockConfig.ALLOW_SHOOT_WHILE_SPRINTING.get()) {
            cir.setReturnValue(true);
        }
    }
}
