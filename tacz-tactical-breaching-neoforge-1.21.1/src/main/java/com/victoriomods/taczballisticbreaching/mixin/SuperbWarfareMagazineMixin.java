package com.victoriomods.taczballisticbreaching.mixin;

import com.victoriomods.taczballisticbreaching.SuperbWarfareCompat;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.atsuishio.superbwarfare.data.gun.GunData", remap = false)
public abstract class SuperbWarfareMagazineMixin {
    @Inject(method = "hasBackupAmmo", at = @At("RETURN"), cancellable = true, remap = false)
    private void tacticalBreaching$magazineCountsAsBackup(Entity shooter, CallbackInfoReturnable<Boolean> result) {
        if (!result.getReturnValue() && SuperbWarfareCompat.hasStoredMagazineAmmo(this, shooter)) {
            result.setReturnValue(true);
        }
    }

    @Inject(method = "reloadAmmo(Lnet/minecraft/world/entity/Entity;Z)V", at = @At("HEAD"), remap = false)
    private void tacticalBreaching$loadFromMagazine(Entity shooter, boolean extraOne, CallbackInfo callback) {
        SuperbWarfareCompat.transferStoredMagazineAmmo(this, shooter, extraOne);
    }
}
