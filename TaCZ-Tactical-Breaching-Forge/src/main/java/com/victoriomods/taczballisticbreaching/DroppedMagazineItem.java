package com.victoriomods.taczballisticbreaching;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

public class DroppedMagazineItem extends Item implements IAmmoBox {
    private static final String AMMO_ID_TAG = "AmmoId";
    private static final String AMMO_COUNT_TAG = "AmmoCount";
    private static final String GUN_ID_TAG = "GunId";
    private static final String MAX_AMMO_TAG = "MaxAmmo";

    public DroppedMagazineItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof DroppedMagazineItem)) {
            return InteractionResultHolder.pass(stack);
        }
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (containerId, inventory, menuPlayer) -> new MagazineLoaderMenu(containerId, inventory, hand),
                    Component.literal(getMagazineDisplayName(stack))
            ), buffer -> {
                buffer.writeBoolean(false);
                buffer.writeEnum(hand);
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static ItemStack create(ResourceLocation gunId, ResourceLocation ammoId, int remainingAmmo, int maxAmmo) {
        ItemStack stack = new ItemStack(ModItems.DROPPED_MAGAZINE.get());
        setGunId(stack, gunId);
        if (stack.getItem() instanceof DroppedMagazineItem magazine) {
            magazine.setAmmoId(stack, ammoId);
            magazine.setAmmoCount(stack, remainingAmmo);
            magazine.setMaxAmmo(stack, maxAmmo);
        }
        return stack;
    }

    public static boolean isCompatibleAmmoItem(ItemStack magazineStack, ItemStack ammoStack) {
        if (isSuperbMagazine(magazineStack)) {
            return magazineStack.getItem() instanceof DroppedMagazineItem magazine
                    && magazine.getAmmoId(magazineStack).equals(BuiltInRegistries.ITEM.getKey(ammoStack.getItem()))
                    && magazine.getAmmoCount(magazineStack) < magazine.getMaxAmmo(magazineStack);
        }
        if (!(magazineStack.getItem() instanceof DroppedMagazineItem magazine) || !(ammoStack.getItem() instanceof IAmmo ammo)) {
            return false;
        }
        ResourceLocation ammoId = ammo.getAmmoId(ammoStack);
        if (ammoId == null || ammoId.equals(DefaultAssets.EMPTY_AMMO_ID)) {
            return false;
        }
        if (magazine.getAmmoCount(magazineStack) <= 0) {
            return true;
        }
        ResourceLocation magazineAmmo = magazine.getAmmoId(magazineStack);
        return ammoId.equals(magazineAmmo);
    }

    public static int fillFromAmmoStack(ItemStack magazineStack, ItemStack ammoStack) {
        if (!(magazineStack.getItem() instanceof DroppedMagazineItem magazine) || !isCompatibleAmmoItem(magazineStack, ammoStack)) {
            return 0;
        }
        int space = Math.max(0, magazine.getMaxAmmo(magazineStack) - magazine.getAmmoCount(magazineStack));
        if (space <= 0) {
            return 0;
        }
        if (isSuperbMagazine(magazineStack)) {
            int moved = Math.min(space, ammoStack.getCount());
            magazine.setAmmoCount(magazineStack, magazine.getAmmoCount(magazineStack) + moved);
            ammoStack.shrink(moved);
            return moved;
        }
        IAmmo ammo = (IAmmo) ammoStack.getItem();
        if (magazine.getAmmoCount(magazineStack) <= 0) {
            ResourceLocation ammoId = ammo.getAmmoId(ammoStack);
            if (ammoId == null || ammoId.equals(DefaultAssets.EMPTY_AMMO_ID)) {
                return 0;
            }
            magazine.setAmmoId(magazineStack, ammoId);
        }
        int moved = Math.min(space, ammoStack.getCount());
        magazine.setAmmoCount(magazineStack, magazine.getAmmoCount(magazineStack) + moved);
        ammoStack.shrink(moved);
        return moved;
    }

    public static int fillFromInventory(ItemStack magazineStack, Inventory inventory) {
        int loaded = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack ammoStack = inventory.getItem(slot);
            if (ammoStack == magazineStack || ammoStack.isEmpty()) {
                continue;
            }
            int moved = fillFromAmmoStack(magazineStack, ammoStack);
            if (moved > 0) {
                loaded += moved;
                inventory.setChanged();
            }
            if (magazineStack.getItem() instanceof DroppedMagazineItem magazine && magazine.getAmmoCount(magazineStack) >= magazine.getMaxAmmo(magazineStack)) {
                break;
            }
        }
        return loaded;
    }

    public static String getMagazineDisplayName(ItemStack stack) {
        ResourceLocation gunId = getGunId(stack);
        String path = gunId.getPath().toLowerCase(Locale.ROOT);
        if (path.contains("m4") || path.contains("m16") || path.contains("mk18") || path.contains("hk416") || path.contains("scar") || path.contains("spr")) {
            return "STANAG Magazine";
        }
        if (path.contains("ak") || path.contains("type_81")) {
            return "AK Magazine";
        }
        if (path.contains("glock") || path.contains("m1911") || path.contains("pistol") || path.contains("deagle") || path.contains("b93r") || path.contains("taurus")) {
            return "Pistol Magazine";
        }
        if (path.contains("ump") || path.contains("mp5") || path.contains("uzi") || path.contains("vector")) {
            return "SMG Magazine";
        }
        if (path.contains("m107") || path.contains("m95") || path.contains("timeless50")) {
            return "Heavy Rifle Magazine";
        }
        return "Tactical Magazine";
    }

    public static String formatAmmoName(ResourceLocation ammoId) {
        String path = ammoId == null ? "unknown" : ammoId.getPath().toLowerCase(Locale.ROOT);
        return switch (path) {
            case "556x45", "5_56x45", "5.56x45" -> "5.56x45 NATO";
            case "762x39", "7_62x39", "7.62x39" -> "7.62x39";
            case "762x51", "7_62x51", "7.62x51" -> "7.62x51 NATO";
            case "9mm", "9x19" -> "9mm";
            case "45acp", "45_acp" -> ".45 ACP";
            case "12g", "12_gauge" -> "12 Gauge";
            case "50bmg", "50_bmg" -> ".50 BMG";
            default -> path.replace('_', ' ').replace('-', ' ');
        };
    }

    public static ResourceLocation getGunId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(GUN_ID_TAG, Tag.TAG_STRING)) {
            ResourceLocation parsed = ResourceLocation.tryParse(tag.getString(GUN_ID_TAG));
            if (parsed != null) {
                return parsed;
            }
        }
        return new ResourceLocation("tacz", "unknown");
    }

    public static boolean isSuperbMagazine(ItemStack stack) {
        return getGunId(stack).getNamespace().equals("superbwarfare");
    }

    public static void setGunId(ItemStack stack, ResourceLocation gunId) {
        stack.getOrCreateTag().putString(GUN_ID_TAG, safeId(gunId, "unknown").toString());
    }

    public int getMaxAmmo(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(MAX_AMMO_TAG, Tag.TAG_INT) ? Math.max(0, tag.getInt(MAX_AMMO_TAG)) : 0;
    }

    public void setMaxAmmo(ItemStack stack, int maxAmmo) {
        stack.getOrCreateTag().putInt(MAX_AMMO_TAG, Math.max(0, maxAmmo));
    }

    @Override
    public ResourceLocation getAmmoId(ItemStack ammoBox) {
        CompoundTag tag = ammoBox.getTag();
        if (tag != null && tag.contains(AMMO_ID_TAG, Tag.TAG_STRING)) {
            ResourceLocation parsed = ResourceLocation.tryParse(tag.getString(AMMO_ID_TAG));
            if (parsed != null) {
                return parsed;
            }
        }
        return DefaultAssets.EMPTY_AMMO_ID;
    }

    @Override
    public int getAmmoCount(ItemStack ammoBox) {
        CompoundTag tag = ammoBox.getTag();
        return tag != null && tag.contains(AMMO_COUNT_TAG, Tag.TAG_INT) ? Math.max(0, tag.getInt(AMMO_COUNT_TAG)) : 0;
    }

    @Override
    public void setAmmoId(ItemStack ammoBox, ResourceLocation ammoId) {
        if ((ammoId == null || ammoId.equals(DefaultAssets.EMPTY_AMMO_ID)) && ammoBox.getOrCreateTag().contains(AMMO_ID_TAG, Tag.TAG_STRING)) {
            return;
        }
        ammoBox.getOrCreateTag().putString(AMMO_ID_TAG, safeId(ammoId, "empty").toString());
    }

    @Override
    public void setAmmoCount(ItemStack ammoBox, int count) {
        ammoBox.getOrCreateTag().putInt(AMMO_COUNT_TAG, Math.max(0, count));
    }

    @Override
    public boolean isAmmoBoxOfGun(ItemStack gun, ItemStack ammoBox) {
        if (isSuperbMagazine(ammoBox) || !(gun.getItem() instanceof IGun iGun)) {
            return false;
        }
        ResourceLocation ammoId = getAmmoId(ammoBox);
        if (ammoId.equals(DefaultAssets.EMPTY_AMMO_ID) || getAmmoCount(ammoBox) <= 0) {
            return false;
        }
        return TimelessAPI.getCommonGunIndex(iGun.getGunId(gun))
                .map(index -> ammoId.equals(index.getGunData().getAmmoId()))
                .orElse(false);
    }

    @Override
    public ItemStack setAmmoLevel(ItemStack ammoBox, int ammoLevel) {
        return ammoBox;
    }

    @Override
    public int getAmmoLevel(ItemStack ammoBox) {
        return 0;
    }

    @Override
    public boolean isCreative(ItemStack ammoBox) {
        return false;
    }

    @Override
    public boolean isAllTypeCreative(ItemStack ammoBox) {
        return false;
    }

    @Override
    public ItemStack setCreative(ItemStack ammoBox, boolean isAllType) {
        return ammoBox;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int rounds = getAmmoCount(stack);
        int max = getMaxAmmo(stack);
        tooltip.add(Component.literal(rounds + "/" + Math.max(max, rounds) + " Rounds").withStyle(ChatFormatting.GRAY));
        if (isSuperbMagazine(stack)) {
            tooltip.add(Component.literal("Superb Warfare ammo: " + formatAmmoName(getAmmoId(stack))).withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal("Shift + Right Click: Open loader").withStyle(ChatFormatting.YELLOW));
            return;
        }
        tooltip.add(Component.literal("Ammo Type: " + formatAmmoName(getAmmoId(stack))).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Shift + Right Click: Open loader").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("Empty mags accept any TaCZ ammo.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("First loaded round sets the ammo type.").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(getMagazineDisplayName(stack));
    }

    private static ResourceLocation safeId(ResourceLocation id, String fallbackPath) {
        return id == null ? new ResourceLocation(TaczTacticalBreaching.MOD_ID, fallbackPath) : id;
    }
}
