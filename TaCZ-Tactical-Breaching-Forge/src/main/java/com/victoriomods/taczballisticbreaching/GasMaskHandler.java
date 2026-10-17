package com.victoriomods.taczballisticbreaching;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class GasMaskHandler {
    private GasMaskHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || !BallisticBlockConfig.ENABLE_GAS_MASKS.get()) {
            return;
        }
        ItemStack mask = event.player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(mask.getItem() instanceof GasMaskItem) || !GasMaskItem.hasWorkingFilter(mask)) {
            return;
        }
        if (event.player.tickCount % 20 == 0) {
            GasMaskItem.drainFilter(mask, 1);
            event.player.getInventory().setChanged();
            event.player.inventoryMenu.broadcastChanges();
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
