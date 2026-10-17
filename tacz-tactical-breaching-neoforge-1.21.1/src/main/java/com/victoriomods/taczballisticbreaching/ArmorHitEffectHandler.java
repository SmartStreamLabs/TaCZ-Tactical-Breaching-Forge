package com.victoriomods.taczballisticbreaching;

import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public final class ArmorHitEffectHandler {
    private static final long DUPLICATE_WINDOW_TICKS = 4L;
    private static final Map<HitKey, Long> RECENT_HITS = new HashMap<>();

    private ArmorHitEffectHandler() {
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!BallisticBlockConfig.ENABLE_ARMOR_HIT_EFFECTS.get() || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        Entity direct = event.getSource().getDirectEntity();
        if (!(direct instanceof EntityKineticBullet bullet)) {
            return;
        }
        LivingEntity target = event.getEntity();
        ArmorProfile armor = armorProfile(target);
        if (!armor.armored) {
            return;
        }

        Vec3 location = impactLocation(bullet, target);
        boolean highCaliber = BulletDamageCalculator.isHighCaliber(bullet);
        boolean armorPiercing = BulletDamageCalculator.isArmorPiercing(bullet);
        boolean weakCaliber = isWeakCaliber(bullet);
        boolean helmetHit = armor.helmet && location.y >= target.getEyeY() - 0.35D;
        boolean deflected = shouldDeflect(level, bullet, highCaliber, armorPiercing, weakCaliber, helmetHit, false, armor.shield);
        if (deflected && BallisticBlockConfig.ENABLE_ARMOR_RICOCHET.get()) {
            event.setNewDamage(event.getNewDamage() * (armor.shield ? 0.18F : 0.38F));
        }
        sendOnce(level, bullet, target, location, highCaliber, armorPiercing, weakCaliber, helmetHit, false, deflected);
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!BallisticBlockConfig.ENABLE_ARMOR_HIT_EFFECTS.get() || !(event.getProjectile() instanceof EntityKineticBullet bullet) || !(bullet.level() instanceof ServerLevel level)) {
            return;
        }
        HitResult result = event.getRayTraceResult();
        if (!(result instanceof EntityHitResult entityHit) || entityHit.getEntity() instanceof LivingEntity) {
            return;
        }
        Entity target = entityHit.getEntity();
        if (!isArmoredVehicle(target)) {
            return;
        }

        Vec3 location = entityHit.getLocation();
        boolean highCaliber = BulletDamageCalculator.isHighCaliber(bullet);
        boolean armorPiercing = BulletDamageCalculator.isArmorPiercing(bullet);
        boolean weakCaliber = isWeakCaliber(bullet);
        boolean deflected = shouldDeflect(level, bullet, highCaliber, armorPiercing, weakCaliber, false, true, false);
        sendOnce(level, bullet, target, location, highCaliber, armorPiercing, weakCaliber, false, true, deflected);
    }

    private static void sendOnce(ServerLevel level, EntityKineticBullet bullet, Entity target, Vec3 location, boolean highCaliber, boolean armorPiercing, boolean weakCaliber, boolean helmetHit, boolean vehicleHit, boolean deflected) {
        long now = level.getGameTime();
        cleanup(now);
        HitKey key = new HitKey(bullet.getId(), target.getId());
        Long previous = RECENT_HITS.get(key);
        if (previous != null && now - previous <= DUPLICATE_WINDOW_TICKS) {
            return;
        }
        RECENT_HITS.put(key, now);
        Vec3 normal = bullet.getDeltaMovement().lengthSqr() < 0.001D ? target.position().subtract(location) : bullet.getDeltaMovement().scale(-1.0D);
        ArmorHitFxNetwork.send(level, target, location, normal, highCaliber, armorPiercing, weakCaliber, helmetHit, vehicleHit, deflected);
    }

    private static ArmorProfile armorProfile(LivingEntity target) {
        boolean armored = false;
        boolean helmet = false;
        for (ItemStack stack : target.getArmorSlots()) {
            if (!stack.isEmpty()) {
                armored = true;
                if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem armorItem && armorItem.getEquipmentSlot() == net.minecraft.world.entity.EquipmentSlot.HEAD) {
                    helmet = true;
                }
            }
        }
        boolean shield = isShield(target.getUseItem()) || isShield(target.getMainHandItem()) || isShield(target.getOffhandItem());
        if (target.isBlocking() && shield) {
            armored = true;
        }
        if (isArmoredVehicle(target)) {
            armored = true;
        }
        return new ArmorProfile(armored, helmet, shield);
    }

    private static boolean isShield(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.SHIELD);
    }

    private static boolean isArmoredVehicle(Entity target) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        String path = id == null ? "" : id.getPath().toLowerCase(Locale.ROOT);
        return containsAny(path, "armored", "armoured", "armor", "armour", "tank", "apc", "ifv", "vehicle", "turret", "mech", "drone");
    }

    private static Vec3 impactLocation(EntityKineticBullet bullet, LivingEntity target) {
        Vec3 bulletPos = bullet.position();
        if (bulletPos.distanceToSqr(target.position()) <= Math.max(4.0D, target.getBbWidth() * target.getBbWidth() + target.getBbHeight() * target.getBbHeight())) {
            return bulletPos.add(0.0D, target.getBbHeight() * 0.12D, 0.0D);
        }
        return target.getEyePosition().add(0.0D, -target.getBbHeight() * 0.25D, 0.0D);
    }

    private static boolean shouldDeflect(ServerLevel level, EntityKineticBullet bullet, boolean highCaliber, boolean armorPiercing, boolean weakCaliber, boolean helmetHit, boolean vehicleHit, boolean shieldHit) {
        if (!BallisticBlockConfig.ENABLE_ARMOR_RICOCHET.get()) {
            return false;
        }
        double chance = BallisticBlockConfig.ARMOR_RICOCHET_CHANCE.get();
        if (weakCaliber) {
            chance += 0.18D;
        }
        if (helmetHit) {
            chance += 0.08D;
        }
        if (shieldHit) {
            chance += 0.16D;
        }
        if (vehicleHit) {
            chance += 0.12D;
        }
        if (highCaliber) {
            chance -= 0.22D;
        }
        if (armorPiercing) {
            chance -= 0.28D;
        }
        if (BulletDamageCalculator.isShotgunImpact(bullet, bullet.position())) {
            chance += 0.12D;
        }
        return level.random.nextDouble() < MthClamp(chance, 0.02D, 0.92D);
    }

    private static boolean isWeakCaliber(EntityKineticBullet bullet) {
        ResourceLocation ammoId = bullet.getAmmoId();
        ResourceLocation gunId = bullet.getGunId();
        return containsAny(path(ammoId), "9mm", "45acp", "380", "22lr", "57", "5_7", "pistol")
                || containsAny(path(gunId), "pistol", "glock", "m1911", "m9", "smg", "vector");
    }

    private static String path(ResourceLocation id) {
        return id == null ? "" : id.getPath().toLowerCase(Locale.ROOT);
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static double MthClamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void cleanup(long now) {
        Iterator<Map.Entry<HitKey, Long>> iterator = RECENT_HITS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue() > 80L) {
                iterator.remove();
            }
        }
    }

    private record HitKey(int bulletId, int targetId) {
    }

    private record ArmorProfile(boolean armored, boolean helmet, boolean shield) {
    }
}
