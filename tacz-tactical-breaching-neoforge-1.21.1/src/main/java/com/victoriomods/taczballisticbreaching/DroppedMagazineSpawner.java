package com.victoriomods.taczballisticbreaching;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.FeedType;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;

public final class DroppedMagazineSpawner {
    private DroppedMagazineSpawner() {
    }

    @SubscribeEvent
    public static void onGunReload(GunReloadEvent event) {
        if (event.getLogicalSide() != LogicalSide.SERVER || !BallisticBlockConfig.DROP_MAGAZINES.get()) {
            return;
        }
        LivingEntity shooter = event.getEntity();
        if (!(shooter.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack gunStack = event.getGunItemStack();
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null || gun.useInventoryAmmo(gunStack)) {
            return;
        }

        ResourceLocation gunId = gun.getGunId(gunStack);
        CommonGunIndex gunIndex = TimelessAPI.getCommonGunIndex(gunId).orElse(null);
        if (gunIndex == null || gunIndex.getGunData().getReloadData().getType() != FeedType.MAGAZINE) {
            return;
        }

        ResourceLocation ammoId = gunIndex.getGunData().getAmmoId();
        int maxAmmo = Math.max(0, AttachmentDataUtils.getAmmoCountWithAttachment(gunStack, gunIndex.getGunData()));
        int remainingAmmo = BallisticBlockConfig.PRESERVE_DROPPED_MAGAZINE_AMMO.get() ? Math.max(0, gun.getCurrentAmmoCount(gunStack)) : 0;

        if (BallisticBlockConfig.ENABLE_TACTICAL_RELOAD.get() && shooter.isShiftKeyDown() && shooter instanceof Player player) {
            storeMagazine(player, gunId, ammoId, remainingAmmo, maxAmmo);
            return;
        }

        DroppedMagazineEntity magazine = createMagazine(level, shooter, gunId, ammoId, remainingAmmo, maxAmmo);
        level.addFreshEntity(magazine);
    }

    private static void storeMagazine(Player player, ResourceLocation gunId, ResourceLocation ammoId, int remainingAmmo, int maxAmmo) {
        ItemStack magazine = DroppedMagazineItem.create(gunId, ammoId, remainingAmmo, maxAmmo);
        if (!player.getInventory().add(magazine)) {
            player.drop(magazine, false);
        }
    }

    static void spawnSuperbMagazine(ServerLevel level, LivingEntity shooter, ResourceLocation gunId, ResourceLocation ammoId, int remainingAmmo, int maxAmmo) {
        level.addFreshEntity(createMagazine(level, shooter, gunId, ammoId, remainingAmmo, maxAmmo));
    }

    private static DroppedMagazineEntity createMagazine(ServerLevel level, LivingEntity shooter, ResourceLocation gunId, ResourceLocation ammoId, int remainingAmmo, int maxAmmo) {
        Vec3 look = shooter.getLookAngle();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 0.0001D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }

        Vec3 position = shooter.position()
                .add(0.0D, Math.max(0.55D, shooter.getBbHeight() * 0.55D), 0.0D)
                .add(right.scale(0.28D))
                .add(look.scale(0.08D));
        Vec3 random = new Vec3((level.random.nextDouble() - 0.5D) * 0.08D, level.random.nextDouble() * 0.035D, (level.random.nextDouble() - 0.5D) * 0.08D);
        Vec3 velocity = right.scale(0.11D)
                .add(0.0D, 0.10D + level.random.nextDouble() * 0.04D, 0.0D)
                .add(look.scale(-0.03D))
                .add(shooter.getDeltaMovement().scale(0.28D))
                .add(random);

        float spinX = 10.0F + level.random.nextFloat() * 20.0F;
        float spinY = (level.random.nextFloat() - 0.5F) * 18.0F;
        float spinZ = 12.0F + level.random.nextFloat() * 22.0F;
        return new DroppedMagazineEntity(level, gunId, ammoId, remainingAmmo, maxAmmo, position, velocity, spinX, spinY, spinZ);
    }
}
