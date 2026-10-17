package com.victoriomods.taczballisticbreaching;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

final class NightstickAttachmentUtil {
    private NightstickAttachmentUtil() {
    }

    static boolean hasNightstick(LivingEntity entity) {
        return entity != null && (stackHasNightstick(entity.getMainHandItem()) || stackHasNightstick(entity.getOffhandItem()));
    }

    static boolean stackHasNightstick(ItemStack stack) {
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) {
            return false;
        }
        ResourceLocation laserId = gun.getAttachmentId(stack, AttachmentType.LASER);
        if (laserId == null) {
            laserId = gun.getBuiltInAttachmentId(stack, AttachmentType.LASER);
        }
        if (laserId == null) {
            return false;
        }
        String path = laserId.getPath().toLowerCase(Locale.ROOT);
        return path.contains("nightstick") || path.contains("laser_nightstick");
    }
}
