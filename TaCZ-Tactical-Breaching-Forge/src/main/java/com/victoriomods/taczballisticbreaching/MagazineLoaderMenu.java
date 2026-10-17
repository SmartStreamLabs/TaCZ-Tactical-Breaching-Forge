package com.victoriomods.taczballisticbreaching;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MagazineLoaderMenu extends AbstractContainerMenu {
    private static final int LOAD_BUTTON = 0;

    private final Inventory playerInventory;
    private final InteractionHand hand;
    private final int magazineEntityId;
    private final Container ammoSlot;
    private final DataSlot currentAmmoData;
    private final DataSlot maxAmmoData;
    private boolean processing;

    public MagazineLoaderMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBoolean() ? buffer.readVarInt() : -1, buffer.readableBytes() > 0 ? buffer.readEnum(InteractionHand.class) : InteractionHand.MAIN_HAND);
    }

    public MagazineLoaderMenu(int containerId, Inventory inventory, InteractionHand hand) {
        this(containerId, inventory, -1, hand);
    }

    public MagazineLoaderMenu(int containerId, Inventory inventory, int magazineEntityId) {
        this(containerId, inventory, magazineEntityId, InteractionHand.MAIN_HAND);
    }

    private MagazineLoaderMenu(int containerId, Inventory inventory, int magazineEntityId, InteractionHand hand) {
        super(ModMenus.MAGAZINE_LOADER.get(), containerId);
        this.playerInventory = inventory;
        this.hand = hand;
        this.magazineEntityId = magazineEntityId;
        this.ammoSlot = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                MagazineLoaderMenu.this.refreshDataSlots();
            }
        };
        this.currentAmmoData = DataSlot.standalone();
        this.maxAmmoData = DataSlot.standalone();

        addSlot(new Slot(this.ammoSlot, 0, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return DroppedMagazineItem.isCompatibleAmmoItem(getMagazineStack(), stack) && getRemainingCapacity() > 0;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }

        addDataSlot(this.currentAmmoData);
        addDataSlot(this.maxAmmoData);
        refreshDataSlots();
    }

    @Override
    public void broadcastChanges() {
        refreshDataSlots();
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.magazineEntityId >= 0) {
            return getMagazineEntity() != null && player.distanceToSqr(getMagazineEntity()) < 64.0D;
        }
        return getMagazineStack().getItem() instanceof DroppedMagazineItem;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack source = slot.getItem();
            result = source.copy();
            if (index == 0) {
                if (!moveItemStackTo(source, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (DroppedMagazineItem.isCompatibleAmmoItem(getMagazineStack(), source) && getRemainingCapacity() > 0) {
                if (!moveItemStackTo(source, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 28) {
                if (!moveItemStackTo(source, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(source, 1, 28, false)) {
                return ItemStack.EMPTY;
            }

            if (source.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == LOAD_BUTTON) {
            loadInsertedAmmo();
            return true;
        }
        return super.clickMenuButton(player, buttonId);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        ItemStack leftover = this.ammoSlot.removeItemNoUpdate(0);
        if (!leftover.isEmpty() && !player.level().isClientSide) {
            player.getInventory().placeItemBackInInventory(leftover);
        }
    }

    public ItemStack getMagazineStack() {
        DroppedMagazineEntity entity = getMagazineEntity();
        if (entity != null) {
            return entity.getRenderStack();
        }
        return this.playerInventory.player.getItemInHand(this.hand);
    }

    public String getMagazineName() {
        return DroppedMagazineItem.getMagazineDisplayName(getMagazineStack());
    }

    public int getCurrentAmmo() {
        return this.currentAmmoData.get();
    }

    public int getMaxAmmo() {
        return this.maxAmmoData.get();
    }

    public String getAmmoTypeName() {
        ItemStack magazine = getMagazineStack();
        if (magazine.getItem() instanceof DroppedMagazineItem item) {
            ResourceLocation ammoId = item.getAmmoId(magazine);
            return DroppedMagazineItem.formatAmmoName(ammoId);
        }
        return "Unknown";
    }

    private int getRemainingCapacity() {
        return Math.max(0, getMaxAmmo() - getCurrentAmmo());
    }

    private void refreshDataSlots() {
        ItemStack magazine = getMagazineStack();
        if (magazine.getItem() instanceof DroppedMagazineItem item) {
            this.currentAmmoData.set(item.getAmmoCount(magazine));
            this.maxAmmoData.set(item.getMaxAmmo(magazine));
        } else {
            this.currentAmmoData.set(0);
            this.maxAmmoData.set(0);
        }
    }

    private void loadInsertedAmmo() {
        if (this.processing) {
            return;
        }
        this.processing = true;
        ItemStack magazine = getMagazineStack();
        ItemStack ammo = this.ammoSlot.getItem(0);
        if (!magazine.isEmpty()
                && !ammo.isEmpty()
                && DroppedMagazineItem.isCompatibleAmmoItem(magazine, ammo)
                && getRemainingCapacity() > 0) {
            DroppedMagazineItem.fillFromAmmoStack(magazine, ammo);
            applyMagazineStack(magazine);
            this.ammoSlot.setItem(0, ammo.isEmpty() ? ItemStack.EMPTY : ammo);
        }
        refreshDataSlots();
        broadcastChanges();
        this.processing = false;
    }

    private DroppedMagazineEntity getMagazineEntity() {
        if (this.magazineEntityId < 0) {
            return null;
        }
        if (this.playerInventory.player.level().getEntity(this.magazineEntityId) instanceof DroppedMagazineEntity magazine) {
            return magazine;
        }
        return null;
    }

    private void applyMagazineStack(ItemStack stack) {
        DroppedMagazineEntity entity = getMagazineEntity();
        if (entity != null) {
            entity.loadFromMagazineStack(stack);
        }
    }
}
