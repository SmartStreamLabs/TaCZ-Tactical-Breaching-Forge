package com.victoriomods.taczballisticbreaching.mixin;

import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.victoriomods.taczballisticbreaching.BallisticBlockConfig;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.tacz.guns.entity.shooter.LivingEntityShoot", remap = false)
public abstract class LivingEntityShootMixin {
    @Redirect(
            method = "shoot(Ljava/util/function/Supplier;Ljava/util/function/Supplier;JFZ)Lcom/tacz/guns/api/entity/ShootResult;",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/tacz/guns/entity/shooter/ShooterDataHolder;sprintTimeS:F",
                    opcode = Opcodes.GETFIELD
            ),
            require = 0
    )
    private float taczTacticalBreaching$allowShootWhileSprinting(ShooterDataHolder data) {
        if (BallisticBlockConfig.ALLOW_SHOOT_WHILE_SPRINTING.get()) {
            return 0.0F;
        }
        return data.sprintTimeS;
    }
}
