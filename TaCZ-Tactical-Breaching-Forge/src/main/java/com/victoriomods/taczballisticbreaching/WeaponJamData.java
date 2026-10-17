package com.victoriomods.taczballisticbreaching;

import com.tacz.guns.api.item.IGun;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

final class WeaponJamData {
    private static final String CONDITION_TAG = TaczTacticalBreaching.MOD_ID + ":weapon_condition";
    private static final String JAMMED_TAG = TaczTacticalBreaching.MOD_ID + ":weapon_jammed";
    private static final String JAM_TYPE_TAG = TaczTacticalBreaching.MOD_ID + ":weapon_jam_type";

    private WeaponJamData() {
    }

    static boolean isGun(ItemStack stack) {
        return !stack.isEmpty() && IGun.getIGunOrNull(stack) != null;
    }

    static double getCondition(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(CONDITION_TAG)) {
            return 100.0D;
        }
        return clampCondition(tag.getDouble(CONDITION_TAG));
    }

    static void setCondition(ItemStack stack, double condition) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putDouble(CONDITION_TAG, clampCondition(condition));
    }

    static void damageCondition(ItemStack stack, double amount) {
        if (amount <= 0.0D || !BallisticBlockConfig.ENABLE_WEAPON_CONDITION.get()) {
            return;
        }
        setCondition(stack, getCondition(stack) - amount);
    }

    static boolean isJammed(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(JAMMED_TAG);
    }

    static void setJammed(ItemStack stack, boolean jammed) {
        CompoundTag tag = stack.getOrCreateTag();
        if (jammed) {
            tag.putBoolean(JAMMED_TAG, true);
            tag.putString(JAM_TYPE_TAG, JamType.SIMPLE_MALFUNCTION.serializedName());
        } else {
            tag.remove(JAMMED_TAG);
            tag.remove(JAM_TYPE_TAG);
        }
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    private static double clampCondition(double value) {
        return Math.max(0.0D, Math.min(100.0D, value));
    }

    enum JamType {
        SIMPLE_MALFUNCTION("simple_malfunction");

        private final String serializedName;

        JamType(String serializedName) {
            this.serializedName = serializedName;
        }

        String serializedName() {
            return serializedName;
        }
    }
}
