package com.victoriomods.taczballisticbreaching.mixin;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.victoriomods.taczballisticbreaching.BallisticBlockConfig;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.tacz.guns.entity.shooter.LivingEntityAim", remap = false)
public abstract class LivingEntityAimMixin {
    @Shadow(remap = false)
    private LivingEntity shooter;

    @Shadow(remap = false)
    private ShooterDataHolder data;

    @Redirect(
            method = "tickSprint",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;m_6858_(Z)V",
                    remap = false
            ),
            require = 0
    )
    private void taczTacticalBreaching$keepReloadSprintMovement(LivingEntity entity, boolean sprinting) {
        if (!sprinting && shouldKeepReloadSprint(entity)) {
            return;
        }
        entity.setSprinting(sprinting);
    }

    @Inject(method = "tickSprint", at = @At("TAIL"), require = 0)
    private void taczTacticalBreaching$suppressGunRunPoseWhenAllowed(CallbackInfo ci) {
        if (shouldSuppressGunRunPose()) {
            data.sprintTimeS = 0.0F;
        }
    }

    private static boolean shouldKeepReloadSprint(LivingEntity entity) {
        if (!BallisticBlockConfig.ALLOW_RELOAD_WHILE_SPRINTING.get()) {
            return false;
        }
        IGunOperator operator = IGunOperator.fromLivingEntity(entity);
        ReloadState.StateType stateType = operator.getSynReloadState().getStateType();
        return !operator.getSynIsAiming() && stateType.isReloading() && !stateType.isReloadFinishing();
    }

    private boolean shouldSuppressGunRunPose() {
        if (!shooter.isSprinting()) {
            return false;
        }
        IGunOperator operator = IGunOperator.fromLivingEntity(shooter);
        if (operator.getSynIsAiming()) {
            return false;
        }
        ReloadState.StateType stateType = operator.getSynReloadState().getStateType();
        if (stateType.isReloading() && !stateType.isReloadFinishing()) {
            return BallisticBlockConfig.ALLOW_RELOAD_WHILE_SPRINTING.get();
        }
        return BallisticBlockConfig.ALLOW_SHOOT_WHILE_SPRINTING.get();
    }
}
