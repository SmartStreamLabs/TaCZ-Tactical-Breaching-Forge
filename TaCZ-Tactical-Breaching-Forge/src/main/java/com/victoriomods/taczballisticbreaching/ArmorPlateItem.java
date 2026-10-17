package com.victoriomods.taczballisticbreaching;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ArmorPlateItem extends Item {
    static final String DURABILITY_TAG = "PlateDurability";

    private final ArmorPlateType plateType;

    public ArmorPlateItem(ArmorPlateType plateType, Properties properties) {
        super(properties.stacksTo(1));
        this.plateType = plateType;
    }

    ArmorPlateType plateType() {
        return plateType;
    }

    int getPlateDurability(ItemStack stack) {
        if (!stack.hasTag() || !stack.getOrCreateTag().contains(DURABILITY_TAG)) {
            return plateType.maxDurability();
        }
        return Math.max(0, Math.min(plateType.maxDurability(), stack.getOrCreateTag().getInt(DURABILITY_TAG)));
    }

    void setPlateDurability(ItemStack stack, int durability) {
        stack.getOrCreateTag().putInt(DURABILITY_TAG, Math.max(0, Math.min(plateType.maxDurability(), durability)));
    }

    boolean damagePlate(ItemStack stack, int amount) {
        if (!BallisticBlockConfig.ENABLE_PLATE_DURABILITY.get()) {
            return false;
        }
        int remaining = getPlateDurability(stack) - Math.max(0, amount);
        setPlateDurability(stack, remaining);
        return remaining <= 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!BallisticBlockConfig.ENABLE_ARMOR_PLATES.get()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide()) {
            ArmorPlateHandler.equipPlate(player, hand, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Durability: " + getPlateDurability(stack) + "/" + plateType.maxDurability()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Protection: " + plateType.protectionLabel()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Weight: " + plateType.weightLabel()).withStyle(ChatFormatting.GRAY));
        if (plateType.movementPenalty() > 0.0D) {
            tooltip.add(Component.literal("Movement: -" + Math.round(plateType.movementPenalty() * 100.0D) + "%").withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.literal("Right Click: Equip plate").withStyle(ChatFormatting.DARK_GREEN));
    }
}
