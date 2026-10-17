package com.victoriomods.taczballisticbreaching;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class MagazineReloadHandler {
    private static final String DUMMY_AMMO_TAG = "DummyAmmo";
    private static final String MAX_DUMMY_AMMO_TAG = "MaxDummyAmmo";
    private static final int START_TIMEOUT_TICKS = 10;
    private static final int HARD_TIMEOUT_TICKS = 20 * 30;
    private static final Map<UUID, ReloadSession> ACTIVE_RELOADS = new HashMap<>();

    private MagazineReloadHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGunReload(GunReloadEvent event) {
        if (event.getLogicalSide() != LogicalSide.SERVER || !shouldInterceptReloads()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack gunStack = event.getGunItemStack();
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null || gun.useInventoryAmmo(gunStack) || gun.useDummyAmmo(gunStack)) {
            return;
        }
        if (hasCreativeAmmoBox(player, gunStack)) {
            return;
        }
        if (!BallisticBlockConfig.ALLOW_MAGAZINE_RELOADING.get()) {
            cancelNoMagazine(event, player);
            return;
        }

        MagazineSelection selection = findLoadedMagazine(player, gunStack);
        if (selection == null) {
            cancelNoMagazine(event, player);
            return;
        }

        ReloadSession previous = ACTIVE_RELOADS.remove(player.getUUID());
        if (previous != null) {
            finishSession(player, previous);
        }

        ReloadSession session = ReloadSession.capture(gunStack, selection.slot(), selection.ammoCount());
        ACTIVE_RELOADS.put(player.getUUID(), session);
        gun.setDummyAmmoAmount(gunStack, selection.ammoCount());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        ReloadSession session = ACTIVE_RELOADS.get(player.getUUID());
        if (session == null) {
            return;
        }

        session.age++;
        boolean reloading = IGunOperator.fromLivingEntity(player).getDataHolder().reloadStateType.isReloading();
        if (reloading) {
            session.sawReloading = true;
        }
        if ((session.sawReloading && !reloading) || (!session.sawReloading && session.age > START_TIMEOUT_TICKS) || session.age > HARD_TIMEOUT_TICKS) {
            ACTIVE_RELOADS.remove(player.getUUID());
            finishSession(player, session);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ReloadSession session = ACTIVE_RELOADS.remove(player.getUUID());
            if (session != null) {
                finishSession(player, session);
            }
        }
    }

    private static boolean shouldInterceptReloads() {
        return BallisticBlockConfig.FORCE_MAGAZINE_RELOADING.get() && !BallisticBlockConfig.ALLOW_LOOSE_AMMO_RELOADING.get();
    }

    private static MagazineSelection findLoadedMagazine(ServerPlayer player, ItemStack gunStack) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof DroppedMagazineItem magazine && magazine.isAmmoBoxOfGun(gunStack, stack)) {
                int ammo = magazine.getAmmoCount(stack);
                if (ammo > 0) {
                    return new MagazineSelection(slot, ammo);
                }
            }
        }
        return null;
    }

    private static boolean hasCreativeAmmoBox(ServerPlayer player, ItemStack gunStack) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof IAmmoBox ammoBox
                    && (ammoBox.isAllTypeCreative(stack) || (ammoBox.isCreative(stack) && ammoBox.isAmmoBoxOfGun(gunStack, stack)))) {
                return true;
            }
        }
        return false;
    }

    private static void cancelNoMagazine(GunReloadEvent event, ServerPlayer player) {
        event.setCanceled(true);
        player.displayClientMessage(Component.literal("No loaded magazine available"), true);
    }

    private static void finishSession(ServerPlayer player, ReloadSession session) {
        int remainingDummy = getDummyAmmo(session.gunStack);
        int consumed = Math.max(0, Math.min(session.initialMagazineAmmo, session.loadedDummyAmmo - remainingDummy));
        if (consumed > 0) {
            ItemStack magazineStack = findSessionMagazine(player, session);
            if (magazineStack.getItem() instanceof DroppedMagazineItem magazine) {
                magazine.setAmmoCount(magazineStack, Math.max(0, magazine.getAmmoCount(magazineStack) - consumed));
                player.getInventory().setChanged();
            }
        }
        restoreDummyAmmo(session);
    }

    private static ItemStack findSessionMagazine(ServerPlayer player, ReloadSession session) {
        Inventory inventory = player.getInventory();
        if (session.magazineSlot >= 0 && session.magazineSlot < inventory.getContainerSize()) {
            ItemStack stack = inventory.getItem(session.magazineSlot);
            if (stack.getItem() instanceof DroppedMagazineItem magazine && magazine.isAmmoBoxOfGun(session.gunStack, stack)) {
                return stack;
            }
        }
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof DroppedMagazineItem magazine
                    && magazine.isAmmoBoxOfGun(session.gunStack, stack)
                    && magazine.getAmmoCount(stack) >= session.initialMagazineAmmo) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static int getDummyAmmo(ItemStack stack) {
        CompoundTag tag = dataTag(stack);
        return tag.contains(DUMMY_AMMO_TAG, Tag.TAG_INT) ? Math.max(0, tag.getInt(DUMMY_AMMO_TAG)) : 0;
    }

    private static void restoreDummyAmmo(ReloadSession session) {
        updateTag(session.gunStack, tag -> {
            if (session.hadDummyAmmo) {
                tag.putInt(DUMMY_AMMO_TAG, session.previousDummyAmmo);
            } else {
                tag.remove(DUMMY_AMMO_TAG);
            }
            if (session.hadMaxDummyAmmo) {
                tag.putInt(MAX_DUMMY_AMMO_TAG, session.previousMaxDummyAmmo);
            } else {
                tag.remove(MAX_DUMMY_AMMO_TAG);
            }
        });
    }

    private static CompoundTag dataTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void updateTag(ItemStack stack, Consumer<CompoundTag> updater) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(updater));
    }

    private record MagazineSelection(int slot, int ammoCount) {
    }

    private static final class ReloadSession {
        private final ItemStack gunStack;
        private final int magazineSlot;
        private final int initialMagazineAmmo;
        private final int loadedDummyAmmo;
        private final boolean hadDummyAmmo;
        private final int previousDummyAmmo;
        private final boolean hadMaxDummyAmmo;
        private final int previousMaxDummyAmmo;
        private int age;
        private boolean sawReloading;

        private ReloadSession(ItemStack gunStack, int magazineSlot, int initialMagazineAmmo, int loadedDummyAmmo, boolean hadDummyAmmo, int previousDummyAmmo, boolean hadMaxDummyAmmo, int previousMaxDummyAmmo) {
            this.gunStack = gunStack;
            this.magazineSlot = magazineSlot;
            this.initialMagazineAmmo = initialMagazineAmmo;
            this.loadedDummyAmmo = loadedDummyAmmo;
            this.hadDummyAmmo = hadDummyAmmo;
            this.previousDummyAmmo = previousDummyAmmo;
            this.hadMaxDummyAmmo = hadMaxDummyAmmo;
            this.previousMaxDummyAmmo = previousMaxDummyAmmo;
        }

        private static ReloadSession capture(ItemStack gunStack, int magazineSlot, int magazineAmmo) {
            CompoundTag tag = dataTag(gunStack);
            boolean hadDummy = tag.contains(DUMMY_AMMO_TAG, Tag.TAG_INT);
            boolean hadMaxDummy = tag.contains(MAX_DUMMY_AMMO_TAG, Tag.TAG_INT);
            return new ReloadSession(
                    gunStack,
                    magazineSlot,
                    magazineAmmo,
                    magazineAmmo,
                    hadDummy,
                    hadDummy ? tag.getInt(DUMMY_AMMO_TAG) : 0,
                    hadMaxDummy,
                    hadMaxDummy ? tag.getInt(MAX_DUMMY_AMMO_TAG) : 0
            );
        }
    }
}
