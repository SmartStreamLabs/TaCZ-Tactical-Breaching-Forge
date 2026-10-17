package com.victoriomods.taczballisticbreaching.mixin;

import com.victoriomods.taczballisticbreaching.SuperbWarfareCompat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.atsuishio.superbwarfare.item.gun.GunItem", remap = false)
public abstract class SuperbWarfareRayHitMixin {
    @Inject(method = "onRayHitBlock", at = @At("HEAD"), remap = false)
    private void tacticalBreaching$onRayHit(Entity shooter, ServerLevel level, Entity target,
                                            @Coerce Object data, Vec3 direction, BlockHitResult hit,
                                            Vec3 position, CallbackInfo callback) {
        SuperbWarfareCompat.onRayHit(shooter, level, data, hit);
    }
}
