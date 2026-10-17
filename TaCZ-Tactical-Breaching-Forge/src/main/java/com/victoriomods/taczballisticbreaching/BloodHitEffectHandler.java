package com.victoriomods.taczballisticbreaching;

import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public final class BloodHitEffectHandler {
    private static final long DUPLICATE_WINDOW_TICKS = 3L;
    private static final Map<HitKey, Long> RECENT_HITS = new HashMap<>();

    private BloodHitEffectHandler() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!effectsEnabled() || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        if (event.getAmount() <= 0.0F) {
            return;
        }

        LivingEntity target = event.getEntity();
        sendFromDamageSource(level, event.getSource(), target, event.getAmount());
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!effectsEnabled() || !(event.getEntity().level() instanceof ServerLevel level) || event.getAmount() <= 0.0F) {
            return;
        }
        sendFromDamageSource(level, event.getSource(), event.getEntity(), event.getAmount());
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!effectsEnabled() || !(event.getProjectile() instanceof EntityKineticBullet bullet) || !(bullet.level() instanceof ServerLevel level)) {
            return;
        }
        HitResult result = event.getRayTraceResult();
        if (!(result instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof LivingEntity target)) {
            return;
        }
        sendOnce(level, bullet, target, entityHit.getLocation(), 0.0F);
    }

    private static void sendFromDamageSource(ServerLevel level, DamageSource source, LivingEntity target, float damageAmount) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof EntityKineticBullet bullet) {
            sendOnce(level, bullet, target, impactLocation(bullet, target), damageAmount);
            return;
        }
        if (!looksLikeTaCZBulletDamage(source)) {
            return;
        }

        Entity attacker = source.getEntity();
        Vec3 location = target.getEyePosition().add(0.0D, -target.getBbHeight() * 0.25D, 0.0D);
        Vec3 direction = attacker == null ? target.getLookAngle().scale(-1.0D) : target.position().subtract(attacker.position());
        sendGenericOnce(level, target, source.getMsgId().hashCode(), location, direction, damageAmount);
    }

    private static void sendOnce(ServerLevel level, EntityKineticBullet bullet, LivingEntity target, Vec3 location, float damageAmount) {
        long now = level.getGameTime();
        cleanup(now);
        HitKey key = new HitKey(bullet.getId(), target.getId());
        Long previous = RECENT_HITS.get(key);
        if (previous != null && now - previous <= DUPLICATE_WINDOW_TICKS) {
            return;
        }
        RECENT_HITS.put(key, now);

        Vec3 direction = bullet.getDeltaMovement();
        if (direction.lengthSqr() < 0.001D) {
            direction = target.position().subtract(location);
        }

        ArmorProfile armor = armorProfile(target);
        boolean highCaliber = BulletDamageCalculator.isHighCaliber(bullet);
        boolean shotgun = BulletDamageCalculator.isShotgunImpact(bullet, location);
        boolean armorPiercing = BulletDamageCalculator.isArmorPiercing(bullet);
        boolean helmetHit = armor.helmet && location.y >= target.getEyeY() - 0.35D;
        float armorReduction = armorReduction(armor, armorPiercing, highCaliber, shotgun);

        BloodHitFxNetwork.send(level, target, location, direction, highCaliber, shotgun, armorPiercing, armor.armored, helmetHit, armorReduction, damageAmount);
    }

    private static void sendGenericOnce(ServerLevel level, LivingEntity target, int sourceHash, Vec3 location, Vec3 direction, float damageAmount) {
        long now = level.getGameTime();
        cleanup(now);
        HitKey key = new HitKey(sourceHash, target.getId());
        Long previous = RECENT_HITS.get(key);
        if (previous != null && now - previous <= DUPLICATE_WINDOW_TICKS) {
            return;
        }
        RECENT_HITS.put(key, now);

        ArmorProfile armor = armorProfile(target);
        boolean helmetHit = armor.helmet && location.y >= target.getEyeY() - 0.35D;
        float armorReduction = armor.armored ? 0.42F : 1.0F;
        BloodHitFxNetwork.send(level, target, location, direction, false, false, false, armor.armored, helmetHit, armorReduction, damageAmount);
    }

    private static boolean effectsEnabled() {
        return BallisticBlockConfig.BLOOD_EFFECTS_ENABLED.get()
                && (BallisticBlockConfig.ENABLE_BLOOD_MIST.get() || BallisticBlockConfig.ENABLE_WALL_SPLATTER.get() || BallisticBlockConfig.BLOOD_PHYSICS_ENABLED.get());
    }

    private static boolean looksLikeTaCZBulletDamage(DamageSource source) {
        String messageId = source.getMsgId().toLowerCase(Locale.ROOT);
        if (containsAny(messageId, "tacz", "bullet", "gun", "kinetic", "projectile")) {
            return true;
        }
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        return entityIdContains(attacker, "tacz", "bullet", "gun") || entityIdContains(direct, "tacz", "bullet", "kinetic");
    }

    private static boolean entityIdContains(Entity entity, String... needles) {
        if (entity == null) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        String value = id == null ? "" : id.toString().toLowerCase(Locale.ROOT);
        return containsAny(value, needles);
    }

    private static ArmorProfile armorProfile(LivingEntity target) {
        boolean armored = false;
        boolean helmet = false;
        for (ItemStack stack : target.getArmorSlots()) {
            if (!stack.isEmpty()) {
                armored = true;
                if (stack.getItem() instanceof ArmorItem armorItem && armorItem.getEquipmentSlot() == EquipmentSlot.HEAD) {
                    helmet = true;
                }
            }
        }
        boolean shield = isShield(target.getUseItem()) || isShield(target.getMainHandItem()) || isShield(target.getOffhandItem());
        if (target.isBlocking() && shield) {
            armored = true;
        }
        if (isArmoredEntity(target)) {
            armored = true;
        }
        return new ArmorProfile(armored, helmet, shield);
    }

    private static boolean isShield(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.SHIELD);
    }

    private static boolean isArmoredEntity(LivingEntity target) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        String path = id == null ? "" : id.getPath().toLowerCase(Locale.ROOT);
        return containsAny(path, "armored", "armoured", "armor", "armour", "tank", "apc", "ifv", "vehicle", "turret", "mech", "drone");
    }

    private static float armorReduction(ArmorProfile armor, boolean armorPiercing, boolean highCaliber, boolean shotgun) {
        if (!armor.armored) {
            return 1.0F;
        }
        double reduction = armorPiercing ? 0.56D : 0.34D;
        if (highCaliber) {
            reduction += 0.16D;
        }
        if (shotgun) {
            reduction -= 0.10D;
        }
        if (armor.shield) {
            reduction -= 0.12D;
        }
        return (float) Math.max(0.12D, Math.min(0.82D, reduction));
    }

    private static Vec3 impactLocation(EntityKineticBullet bullet, LivingEntity target) {
        Vec3 bulletPos = bullet.position();
        if (bulletPos.distanceToSqr(target.position()) <= Math.max(4.0D, target.getBbWidth() * target.getBbWidth() + target.getBbHeight() * target.getBbHeight())) {
            return bulletPos.add(0.0D, target.getBbHeight() * 0.12D, 0.0D);
        }
        return target.getEyePosition().add(0.0D, -target.getBbHeight() * 0.25D, 0.0D);
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
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
