package com.victoriomods.taczballisticbreaching;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class GasMaskItem extends ArmorItem {
    private static final String FILTER_ID_TAG = "GasMaskFilter";
    private static final String FILTER_DURABILITY_TAG = "GasMaskFilterDurability";

    public GasMaskItem(Properties properties) {
        super(GasMaskArmorMaterial.INSTANCE, Type.HELMET, properties.stacksTo(1));
    }

    static ItemStack installFilter(ItemStack maskStack, GasMaskFilterItem filterItem, int durability) {
        ItemStack oldFilter = createInstalledFilterStack(maskStack);
        CompoundTag tag = maskStack.getOrCreateTag();
        tag.putString(FILTER_ID_TAG, filterItem.filterType().id());
        tag.putInt(FILTER_DURABILITY_TAG, Math.max(0, Math.min(filterItem.filterType().maxDurability(), durability)));
        return oldFilter;
    }

    static boolean hasWorkingFilter(ItemStack maskStack) {
        return getFilterDurability(maskStack) > 0;
    }

    static GasMaskFilterType getFilterType(ItemStack maskStack) {
        CompoundTag tag = maskStack.getTag();
        if (tag == null || !tag.contains(FILTER_ID_TAG)) {
            return null;
        }
        return GasMaskFilterType.byId(tag.getString(FILTER_ID_TAG));
    }

    static int getFilterDurability(ItemStack maskStack) {
        CompoundTag tag = maskStack.getTag();
        if (tag == null || !tag.contains(FILTER_ID_TAG)) {
            return 0;
        }
        GasMaskFilterType type = GasMaskFilterType.byId(tag.getString(FILTER_ID_TAG));
        return Math.max(0, Math.min(type.maxDurability(), tag.getInt(FILTER_DURABILITY_TAG)));
    }

    static int getFilterPercent(ItemStack maskStack) {
        GasMaskFilterType type = getFilterType(maskStack);
        if (type == null) {
            return 0;
        }
        return Math.round(getFilterDurability(maskStack) * 100.0F / Math.max(1, type.maxDurability()));
    }

    static void drainFilter(ItemStack maskStack, int amount) {
        if (amount <= 0 || !BallisticBlockConfig.ENABLE_FILTER_DURABILITY.get()) {
            return;
        }
        GasMaskFilterType type = getFilterType(maskStack);
        if (type == null) {
            return;
        }
        maskStack.getOrCreateTag().putInt(FILTER_DURABILITY_TAG, Math.max(0, getFilterDurability(maskStack) - amount));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        GasMaskFilterType type = getFilterType(stack);
        if (type == null) {
            tooltip.add(Component.literal("Filter: None").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.literal("Filter: " + type.displayName()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Gas Mask Filter: " + getFilterPercent(stack) + "%").withStyle(hasWorkingFilter(stack) ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        tooltip.add(Component.literal("Helmet Slot").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> consumer.accept(ClientGasMaskExtensions.create()));
    }

    private static ItemStack createInstalledFilterStack(ItemStack maskStack) {
        GasMaskFilterType type = getFilterType(maskStack);
        if (type == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = switch (type) {
            case BASIC -> new ItemStack(ModItems.BASIC_FILTER.get());
            case MILITARY -> new ItemStack(ModItems.MILITARY_FILTER.get());
            case ADVANCED_NBC -> new ItemStack(ModItems.ADVANCED_NBC_FILTER.get());
        };
        if (stack.getItem() instanceof GasMaskFilterItem filterItem) {
            filterItem.setFilterDurability(stack, getFilterDurability(maskStack));
        }
        return stack;
    }
}
