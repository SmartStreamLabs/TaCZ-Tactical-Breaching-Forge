package com.victoriomods.taczballisticbreaching.mixin;

import com.victoriomods.taczballisticbreaching.SuperbWarfareCompat;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

@Mixin(targets = "com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity", remap = false)
public abstract class SuperbWarfareProjectileMixin {
    @Shadow(remap = false) @Final private static Predicate<BlockState> IGNORE_LIST;

    @Redirect(method = "tick", at = @At(value = "FIELD",
            target = "Lcom/atsuishio/superbwarfare/entity/projectile/ProjectileEntity;IGNORE_LIST:Ljava/util/function/Predicate;",
            opcode = 178, remap = false), remap = false, require = 1)
    private Predicate<BlockState> tacticalBreaching$includeBreachableBlocks() {
        return state -> !SuperbWarfareCompat.shouldCollideWith(state) && IGNORE_LIST.test(state);
    }
}
