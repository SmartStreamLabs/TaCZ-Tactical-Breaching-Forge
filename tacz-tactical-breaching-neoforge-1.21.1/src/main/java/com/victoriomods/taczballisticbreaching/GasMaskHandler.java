package com.victoriomods.taczballisticbreaching;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class GasMaskHandler {
    private GasMaskHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !BallisticBlockConfig.ENABLE_GAS_MASKS.get()) {
            return;
        }
        ItemStack mask = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(mask.getItem() instanceof GasMaskItem) || !GasMaskItem.hasWorkingFilter(mask)) {
            return;
        }
        if (player.tickCount % 20 == 0) {
            GasMaskItem.drainFilter(mask, 1);
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }
    }

    static boolean hasWorkingGasMask(Player player) {
        if (!BallisticBlockConfig.ENABLE_GAS_MASKS.get()) {
            return false;
        }
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        return helmet.getItem() instanceof GasMaskItem && GasMaskItem.hasWorkingFilter(helmet);
    }

    static int filterPercent(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        return helmet.getItem() instanceof GasMaskItem ? GasMaskItem.getFilterPercent(helmet) : 0;
    }
}
