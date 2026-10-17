package com.victoriomods.taczballisticbreaching;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GasMaskFilterItem extends Item {
    static final String DURABILITY_TAG = "FilterDurability";

    private final GasMaskFilterType filterType;

    public GasMaskFilterItem(GasMaskFilterType filterType, Properties properties) {
        super(properties.stacksTo(1));
        this.filterType = filterType;
    }

    GasMaskFilterType filterType() {
        return filterType;
    }

    int getFilterDurability(ItemStack stack) {
        if (!stack.hasTag() || !stack.getOrCreateTag().contains(DURABILITY_TAG)) {
            return filterType.maxDurability();
        }
        return Math.max(0, Math.min(filterType.maxDurability(), stack.getOrCreateTag().getInt(DURABILITY_TAG)));
    }

    void setFilterDurability(ItemStack stack, int durability) {
        stack.getOrCreateTag().putInt(DURABILITY_TAG, Math.max(0, Math.min(filterType.maxDurability(), durability)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack filterStack = player.getItemInHand(hand);
        if (!BallisticBlockConfig.ENABLE_GAS_MASKS.get()) {
            return InteractionResultHolder.pass(filterStack);
        }
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof GasMaskItem)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.literal("Wear a Gas Mask to install a filter."), true);
            }
            return InteractionResultHolder.fail(filterStack);
        }
        if (!level.isClientSide()) {
            ItemStack oldFilter = GasMaskItem.installFilter(helmet, this, getFilterDurability(filterStack));
            filterStack.shrink(1);
            if (!oldFilter.isEmpty() && !player.getInventory().add(oldFilter)) {
                player.drop(oldFilter, false);
            }
            player.displayClientMessage(Component.literal("Installed " + filterType.displayName()), true);
        }
        return InteractionResultHolder.sidedSuccess(filterStack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int durability = getFilterDurability(stack);
        int percent = Math.round(durability * 100.0F / Math.max(1, filterType.maxDurability()));
        tooltip.add(Component.literal("Durability: " + durability + "/" + filterType.maxDurability() + " (" + percent + "%)").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Right Click while wearing Gas Mask: Install").withStyle(ChatFormatting.DARK_GREEN));
    }
}
