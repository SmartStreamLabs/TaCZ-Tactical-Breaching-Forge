package com.victoriomods.taczballisticbreaching;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

/** Optional bridge for the stable SBW block-hit event shared by 0.8.9 and 0.8.9.1. */
public final class SuperbWarfareCompat {
    private static final String HIT_EVENT = "com.atsuishio.superbwarfare.api.event.ProjectileHitEvent$HitBlock";
    private static final String SHOOT_EVENT = "com.atsuishio.superbwarfare.api.event.ShootEvent$Post";
    private static final String RELOAD_EVENT = "com.atsuishio.superbwarfare.api.event.ReloadEvent$Pre";
    private static Method position;
    private static Method state;
    private static Method face;
    private static Method owner;
    private static Method projectile;
    private static Method hitVec;
    private static Class<?> gunPropertyType;
    private static Method sbwEntityHit;
    private static java.lang.reflect.Constructor<?> sbwEntityResult;
    private static java.lang.reflect.Constructor<?> sbwHitResult;
    private static int loggedImpacts;

    private SuperbWarfareCompat() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static void register() {
        if (!ModList.get().isLoaded("superbwarfare")) {
            return;
        }
        String version = ModList.get().getModContainerById("superbwarfare")
                .map(container -> container.getModInfo().getVersion().toString()).orElse("unknown");
        String apiVersion = version.split("[-+]", 2)[0];
        if (!apiVersion.equals("0.8.9") && !apiVersion.equals("0.8.9.1")) {
            TaczTacticalBreaching.LOGGER.warn("Unsupported Superb Warfare version {}; compatibility disabled.", version);
            return;
        }
        try {
            Class<?> eventType = Class.forName(HIT_EVENT);
            position = eventType.getMethod("getPos");
            state = eventType.getMethod("getState");
            face = eventType.getMethod("getFace");
            owner = eventType.getMethod("getOwner");
            projectile = eventType.getMethod("getProjectile");
            hitVec = eventType.getMethod("getHitVec");
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, (Class) eventType,
                    (Consumer) (event -> onBlockHit((Event) event)));
            Class<?> shootType = Class.forName(SHOOT_EVENT);
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, (Class) shootType,
                    (Consumer) (event -> onShoot((Event) event)));
            try {
                Class<?> reloadType = Class.forName(RELOAD_EVENT);
                MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, (Class) reloadType,
                        (Consumer) (event -> onReload((Event) event)));
            } catch (ReflectiveOperationException | LinkageError exception) {
                TaczTacticalBreaching.LOGGER.warn("Superb Warfare magazine reload event unavailable.", exception);
            }
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, ExplosionEvent.Detonate.class,
                    SuperbWarfareCompat::onExplosion);
            TaczTacticalBreaching.LOGGER.info("Superb Warfare {} shot and block-hit bridge active.", version);
        } catch (ReflectiveOperationException | LinkageError exception) {
            TaczTacticalBreaching.LOGGER.warn("Superb Warfare {} block-hit API unavailable; bridge disabled.", version, exception);
        }
    }

    private static void onShoot(Event event) {
        if (!BallisticBlockConfig.SUPERB_WARFARE_SUPPORT.get()) {
            return;
        }
        try {
            Entity shooter = (Entity) event.getClass().getMethod("getShooter").invoke(event);
            if (!(shooter instanceof LivingEntity living) || !(shooter.level() instanceof ServerLevel level)) {
                return;
            }
            Object gunData = event.getClass().getMethod("getData").invoke(event);
            ItemStack stack = (ItemStack) gunData.getClass().getMethod("stack").invoke(gunData);
            if (numberProperty(gunData, "EXPLOSION_DAMAGE") > 0.0D) {
                return;
            }
            boolean suppressed = isSuppressed(gunData);
            ShellCasingSpawner.spawnForSuperb(level, living, stack);
            MuzzleSmokeNetwork.sendSuperb(level, living, stack, suppressed);
            DistantGunshotHandler.onSuperbShot(living, stack, suppressed);
        } catch (ReflectiveOperationException | LinkageError exception) {
            TaczTacticalBreaching.LOGGER.debug("Skipping incompatible Superb Warfare shot event.", exception);
        }
    }

    private static void onReload(Event event) {
        if (!BallisticBlockConfig.SUPERB_WARFARE_SUPPORT.get() || !BallisticBlockConfig.DROP_MAGAZINES.get()) {
            return;
        }
        try {
            Entity shooter = (Entity) event.getClass().getMethod("getEntity").invoke(event);
            if (!(shooter instanceof LivingEntity living) || !(shooter.level() instanceof ServerLevel level)) {
                return;
            }
            Object data = event.getClass().getMethod("getData").invoke(event);
            Class<?> propertyType = Class.forName("com.atsuishio.superbwarfare.data.gun.GunProp");
            Object reloadTypes = data.getClass().getMethod("get", propertyType)
                    .invoke(data, propertyType.getField("RELOAD_TYPES").get(null));
            if (!(reloadTypes instanceof Set<?> types)
                    || types.stream().noneMatch(type -> type.toString().equals("MAGAZINE"))
                    || types.stream().anyMatch(type -> type.toString().equals("CLIP"))) {
                return;
            }
            int capacity = (int) numberProperty(data, "MAGAZINE");
            ResourceLocation ammoId = superbAmmoId(data);
            if (capacity <= 0 || ammoId == null || (!hasStoredMagazineAmmo(data, shooter)
                    && (int) data.getClass().getMethod("countBackupAmmo", Entity.class).invoke(data, shooter) <= 0)) {
                return;
            }
            ItemStack stack = (ItemStack) data.getClass().getMethod("stack").invoke(data);
            ResourceLocation gunId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!gunId.getNamespace().equals("superbwarfare")) {
                return;
            }
            int remaining = 0;
            if (BallisticBlockConfig.PRESERVE_DROPPED_MAGAZINE_AMMO.get()) {
                Object ammo = data.getClass().getField("ammo").get(data);
                remaining = Math.max(0, (int) ammo.getClass().getMethod("get").invoke(ammo));
                if (remaining > 0) {
                    ammo.getClass().getMethod("set", int.class).invoke(ammo, 0);
                    data.getClass().getMethod("save").invoke(data);
                }
            }
            DroppedMagazineSpawner.spawnSuperbMagazine(level, living, gunId, ammoId, remaining, capacity);
        } catch (ReflectiveOperationException | LinkageError | ClassCastException exception) {
            TaczTacticalBreaching.LOGGER.debug("Skipping incompatible Superb Warfare reload event.", exception);
        }
    }

    private static ResourceLocation superbAmmoId(Object data) throws ReflectiveOperationException {
        Object consumer = data.getClass().getMethod("selectedAmmoConsumer").invoke(data);
        if ((int) consumer.getClass().getMethod("getLoadAmount").invoke(consumer) != 1) {
            return null;
        }
        ItemStack ammo = (ItemStack) consumer.getClass().getMethod("stack").invoke(consumer);
        return ammo.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(ammo.getItem());
    }

    public static boolean hasStoredMagazineAmmo(Object data, Entity shooter) {
        if (!BallisticBlockConfig.SUPERB_WARFARE_SUPPORT.get() || !BallisticBlockConfig.DROP_MAGAZINES.get()
                || !(shooter instanceof Player player)) {
            return false;
        }
        try {
            ResourceLocation gunId = BuiltInRegistries.ITEM.getKey(((ItemStack) data.getClass().getMethod("stack").invoke(data)).getItem());
            ResourceLocation ammoId = superbAmmoId(data);
            if (ammoId == null) {
                return false;
            }
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.getItem() instanceof DroppedMagazineItem magazine
                        && DroppedMagazineItem.getGunId(stack).equals(gunId)
                        && magazine.getAmmoId(stack).equals(ammoId)
                        && magazine.getAmmoCount(stack) > 0) {
                    return true;
                }
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            TaczTacticalBreaching.LOGGER.debug("Superb Warfare magazine lookup unavailable.", exception);
        }
        return false;
    }

    public static void transferStoredMagazineAmmo(Object data, Entity shooter, boolean extraOne) {
        if (!(shooter instanceof Player player) || !(shooter.level() instanceof ServerLevel)
                || !hasStoredMagazineAmmo(data, shooter)) {
            return;
        }
        try {
            ResourceLocation gunId = BuiltInRegistries.ITEM.getKey(((ItemStack) data.getClass().getMethod("stack").invoke(data)).getItem());
            ResourceLocation ammoId = superbAmmoId(data);
            Object ammo = data.getClass().getField("ammo").get(data);
            Object virtual = data.getClass().getField("virtualAmmo").get(data);
            int loaded = (int) ammo.getClass().getMethod("get").invoke(ammo);
            int virtualRounds = (int) virtual.getClass().getMethod("get").invoke(virtual);
            int needed = Math.max(0, (int) numberProperty(data, "MAGAZINE") - loaded
                    + (extraOne ? 1 : 0) - virtualRounds);
            if (needed == 0) {
                return;
            }
            for (int slot = 0; slot < player.getInventory().getContainerSize() && needed > 0; slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (!(stack.getItem() instanceof DroppedMagazineItem magazine)
                        || !DroppedMagazineItem.getGunId(stack).equals(gunId)
                        || !magazine.getAmmoId(stack).equals(ammoId)) {
                    continue;
                }
                int moved = Math.min(needed, magazine.getAmmoCount(stack));
                if (moved > 0) {
                    magazine.setAmmoCount(stack, magazine.getAmmoCount(stack) - moved);
                    virtual.getClass().getMethod("set", int.class).invoke(virtual, virtualRounds + moved);
                    virtualRounds += moved;
                    needed -= moved;
                    player.getInventory().setChanged();
                }
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            TaczTacticalBreaching.LOGGER.debug("Superb Warfare magazine transfer unavailable.", exception);
        }
    }

    private static boolean isSuppressed(Object gunData) {
        try {
            Class<?> type = Class.forName("com.atsuishio.superbwarfare.data.gun.value.AttachmentType");
            Object barrel = type.getField("BARREL").get(null);
            Object attachments = gunData.getClass().getField("attachment").get(gunData);
            return ((Number) attachments.getClass().getMethod("get", type).invoke(attachments, barrel)).intValue() == 2;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return false;
        }
    }

    private static double numberProperty(Object gunData, String name) throws ReflectiveOperationException {
        if (gunPropertyType == null) {
            gunPropertyType = Class.forName("com.atsuishio.superbwarfare.data.gun.GunProp");
        }
        Object property = gunPropertyType.getField(name).get(null);
        Object value = gunData.getClass().getMethod("get", gunPropertyType).invoke(gunData, property);
        return value instanceof Number number ? number.doubleValue() : 0.0D;
    }

    private static void onBlockHit(Event event) {
        if (!BallisticBlockConfig.SUPERB_WARFARE_SUPPORT.get()) {
            return;
        }
        try {
            Projectile bullet = (Projectile) projectile.invoke(event);
            if (!(bullet.level() instanceof ServerLevel level)) {
                return;
            }
            double explosiveDamage = number(bullet, "getExplosionDamage");
            if (explosiveDamage > 0.0D) {
                return; // Explosives are not ordinary bullet hits.
            }
            BlockPos pos = (BlockPos) position.invoke(event);
            BlockState hitState = (BlockState) state.invoke(event);
            if (level.getBlockState(pos).getBlock() != hitState.getBlock()) {
                return;
            }
            Vec3 location = (Vec3) hitVec.invoke(event);
            Direction direction = (Direction) face.invoke(event);
            Entity shooter = (Entity) owner.invoke(event);
            if (shooter == null) {
                shooter = bullet.getOwner();
            }
            applyImpact(level, new BlockHitResult(location, direction, pos, false), shooter, number(bullet, "getDamage"));
            if (BallisticBlockConfig.GLASS_PASSTHROUGH_ENABLED.get()
                    && BallisticBlockRules.isGlassTarget(hitState)
                    && hitEntityBehindGlass(level, bullet, shooter, pos, location)) {
                event.setCanceled(true);
            }
        } catch (ReflectiveOperationException | ClassCastException exception) {
            TaczTacticalBreaching.LOGGER.debug("Skipping incompatible Superb Warfare projectile hit.", exception);
        }
    }

    private static boolean hitEntityBehindGlass(ServerLevel level, Projectile bullet, Entity shooter, BlockPos glassPos, Vec3 hit) {
        Vec3 motion = bullet.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6D) {
            return false;
        }
        Vec3 direction = motion.normalize();
        double exitDistance = Double.POSITIVE_INFINITY;
        if (Math.abs(direction.x) > 1.0E-6D) {
            exitDistance = Math.min(exitDistance, ((direction.x > 0 ? glassPos.getX() + 1 : glassPos.getX()) - hit.x) / direction.x);
        }
        if (Math.abs(direction.y) > 1.0E-6D) {
            exitDistance = Math.min(exitDistance, ((direction.y > 0 ? glassPos.getY() + 1 : glassPos.getY()) - hit.y) / direction.y);
        }
        if (Math.abs(direction.z) > 1.0E-6D) {
            exitDistance = Math.min(exitDistance, ((direction.z > 0 ? glassPos.getZ() + 1 : glassPos.getZ()) - hit.z) / direction.z);
        }
        Vec3 start = hit.add(direction.scale(Math.max(0.0D, exitDistance) + 0.02D));
        Vec3 end = bullet.position().add(motion);
        if (start.distanceToSqr(end) < 0.01D || end.subtract(start).dot(direction) <= 0.0D) {
            return false;
        }
        BlockHitResult wall = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bullet));
        Vec3 visibleEnd = wall.getType() == HitResult.Type.BLOCK ? wall.getLocation() : end;
        EntityHitResult target = ProjectileUtil.getEntityHitResult(level, bullet, start, visibleEnd,
                new AABB(start, visibleEnd).inflate(1.0D), entity -> entity instanceof LivingEntity
                        && entity.isAlive() && !entity.isSpectator() && entity.isPickable()
                        && entity != bullet && entity != shooter
                        && !(shooter instanceof Player player && entity instanceof Player other && !player.canHarmPlayer(other)));
        try {
            if (target != null && sbwHitResult == null) {
                Class<?> entityResultType = Class.forName("com.atsuishio.superbwarfare.world.phys.EntityResult");
                sbwEntityResult = entityResultType.getConstructor(Entity.class, Vec3.class, boolean.class, boolean.class);
                Class<?> resultType = Class.forName("com.atsuishio.superbwarfare.world.phys.ExtendedEntityRayTraceResult");
                sbwHitResult = resultType.getConstructor(entityResultType);
                Class<?> projectileType = Class.forName("com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity");
                try {
                    sbwEntityHit = projectileType.getDeclaredMethod("onHitEntity", EntityHitResult.class);
                } catch (NoSuchMethodException remapped) {
                    sbwEntityHit = projectileType.getDeclaredMethod("m_5790_", EntityHitResult.class);
                }
                sbwEntityHit.setAccessible(true);
            }
            if (target == null && wall.getType() == HitResult.Type.BLOCK) {
                return false;
            }
            float damage = (float) number(bullet, "getDamage");
            bullet.getClass().getMethod("setDamage", float.class).invoke(bullet,
                    damage * BallisticBlockConfig.GLASS_PASSTHROUGH_DAMAGE_MULTIPLIER.get().floatValue());
            if (target == null) {
                return true;
            }
            Entity entity = target.getEntity();
            double height = target.getLocation().y - entity.position().y;
            Object hitData = sbwEntityResult.newInstance(entity, target.getLocation(),
                    entity instanceof LivingEntity living && height > living.getEyeHeight() - 0.25D && height < living.getEyeHeight() + 0.3D,
                    entity instanceof LivingEntity && height < entity.getBbHeight() * 0.33D);
            sbwEntityHit.invoke(bullet, sbwHitResult.newInstance(hitData));
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            TaczTacticalBreaching.LOGGER.debug("Superb Warfare glass entity hit unavailable.", exception);
            return false;
        }
    }

    public static void onRayHit(Entity shooter, ServerLevel level, Object gunData, BlockHitResult hit) {
        if (!BallisticBlockConfig.SUPERB_WARFARE_SUPPORT.get()) {
            return;
        }
        try {
            applyImpact(level, hit, shooter, numberProperty(gunData, "DAMAGE"));
        } catch (ReflectiveOperationException | LinkageError exception) {
            TaczTacticalBreaching.LOGGER.debug("Skipping incompatible Superb Warfare ray hit.", exception);
        }
    }

    private static void applyImpact(ServerLevel level, BlockHitResult hit, Entity shooter, double rawDamage) {
        applyImpact(level, hit, shooter, rawDamage, true);
    }

    private static void applyImpact(ServerLevel level, BlockHitResult hit, Entity shooter, double rawDamage, boolean showEffects) {
        BallisticBlockTarget target = BallisticBlockTarget.fromHit(level, hit.getBlockPos(), level.getBlockState(hit.getBlockPos()));
        BlockState damageState = target.damageState();
        if (!BallisticBlockRules.isDamageable(damageState) || !enabledFor(damageState)) {
            return;
        }
        double damage = Math.min(Math.max(rawDamage, 0.0D) * BallisticBlockConfig.SUPERB_WARFARE_DAMAGE_MULTIPLIER.get(), 500.0D);
        if (!Double.isFinite(damage) || damage <= 0.0D) {
            return;
        }
        boolean glass = BallisticBlockRules.isGlassTarget(damageState);
        double maxHp = BallisticBlockRules.getMaxHp(damageState);
        double remaining = BallisticDamageStore.applyDamage(level, target.damagePos(), damageState.getBlock(), maxHp, damage, level.getGameTime());
        if (loggedImpacts++ < 4) {
            TaczTacticalBreaching.LOGGER.info("SBW breach hit block={} damage={} remainingHp={}", damageState.getBlock(), damage, remaining);
        }
        if (showEffects && BallisticBlockConfig.SUPERB_WARFARE_IMPACTS.get()) {
            // SBW already sends its own bullet mark; only reuse breach debris here.
            BallisticBlockHitHandler.playImpactEffects(level, target.hitPos(), target.hitState(), hit.getLocation(), glass);
        }
        if (remaining <= 0.0D) {
            BallisticBlockHitHandler.breakBlock(level, target.damagePos(), damageState, shooter, glass);
        } else {
            BallisticDamageStore.updateProgress(level, target.damagePos(), remaining, maxHp);
        }
    }

    private static void onExplosion(ExplosionEvent.Detonate event) {
        if (!BallisticBlockConfig.SUPERB_WARFARE_SUPPORT.get()
                || !BallisticBlockConfig.SUPERB_WARFARE_EXPLOSIVES.get()
                || !(event.getLevel() instanceof ServerLevel level)
                || !event.getExplosion().getClass().getName().equals("com.atsuishio.superbwarfare.tools.CustomExplosion")) {
            return;
        }
        var explosion = event.getExplosion();
        double radius = number(explosion, "getRadius");
        double strength = number(explosion, "getDamage");
        if (!Double.isFinite(radius) || !Double.isFinite(strength) || radius <= 0.0D || strength <= 0.0D) {
            return;
        }
        Vec3 center = explosion.getPosition();
        Entity source = explosion.getExploder();
        Entity shooter = source instanceof Projectile projectileSource ? projectileSource.getOwner() : source;
        int processed = 0;
        Iterator<BlockPos> blocks = event.getAffectedBlocks().iterator();
        while (blocks.hasNext() && processed < 128) {
            BlockPos pos = blocks.next();
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!BallisticBlockRules.isDamageable(state) || !enabledFor(state)) {
                continue;
            }
            double distance = Vec3.atCenterOf(pos).distanceTo(center);
            double damage = strength * Math.max(0.15D, 1.0D - distance / Math.max(radius, 0.1D));
            if (damage <= 0.0D) {
                continue;
            }
            blocks.remove();
            applyImpact(level, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false), shooter, damage, false);
            processed++;
        }
    }

    private static boolean enabledFor(BlockState state) {
        if (BallisticBlockRules.isGlassTarget(state)) {
            return BallisticBlockConfig.SUPERB_WARFARE_GLASS.get();
        }
        if (state.getBlock() instanceof TrapDoorBlock || state.is(net.minecraft.tags.BlockTags.TRAPDOORS)) {
            return BallisticBlockConfig.SUPERB_WARFARE_TRAPDOORS.get();
        }
        if (state.getBlock() instanceof DoorBlock || state.is(net.minecraft.tags.BlockTags.DOORS)) {
            return BallisticBlockConfig.SUPERB_WARFARE_DOORS.get();
        }
        return true;
    }

    public static boolean shouldCollideWith(BlockState state) {
        return BallisticBlockConfig.SUPERB_WARFARE_SUPPORT.get()
                && BallisticBlockRules.isDamageable(state) && enabledFor(state);
    }

    private static double number(Object source, String getter) {
        try {
            Object value = source.getClass().getMethod(getter).invoke(source);
            return value instanceof Number number ? number.doubleValue() : 0.0D;
        } catch (ReflectiveOperationException exception) {
            if (!getter.startsWith("get") || getter.length() < 4) {
                return 0.0D;
            }
            // 0.8.9 stores explosion values privately; 0.8.9.1 exposes getters.
            String fieldName = Character.toLowerCase(getter.charAt(3)) + getter.substring(4);
            for (Class<?> type = source.getClass(); type != null; type = type.getSuperclass()) {
                try {
                    var field = type.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(source);
                    return value instanceof Number number ? number.doubleValue() : 0.0D;
                } catch (NoSuchFieldException ignored) {
                    // Continue through projectile base classes.
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    return 0.0D;
                }
            }
            return 0.0D;
        }
    }
}
