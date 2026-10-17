package com.victoriomods.taczballisticbreaching;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.Locale;

public final class ShellCasingSpawner {
    private static final ResourceLocation DEFAULT_RIFLE_AMMO = ResourceLocation.fromNamespaceAndPath("tacz", "556x45");
    private static final ResourceLocation DEFAULT_PISTOL_AMMO = ResourceLocation.fromNamespaceAndPath("tacz", "9mm");
    private static final ResourceLocation DEFAULT_SHOTGUN_AMMO = ResourceLocation.fromNamespaceAndPath("tacz", "12g");

    private ShellCasingSpawner() {
    }

    @SubscribeEvent
    public static void onGunFire(GunFireEvent event) {
        if (event.getLogicalSide() != LogicalSide.SERVER) {
            return;
        }
        if (!(event.getShooter().level() instanceof ServerLevel level)) {
            return;
        }
        MuzzleSmokeNetwork.send(level, event.getShooter(), event.getGunItemStack());
        if (!BallisticBlockConfig.ENABLE_SHELL_CASINGS.get()) {
            return;
        }
        int maxNearby = BallisticBlockConfig.MAX_NEARBY_CASINGS.get();
        if (maxNearby <= 0 || tooManyNearbyCasings(level, event.getShooter(), maxNearby)) {
            return;
        }

        CasingInfo casingInfo = determineCasingInfo(event.getGunItemStack());
        ShellCasingEntity casing = createCasing(level, event.getShooter(), casingInfo);
        level.addFreshEntity(casing);
    }

    static void spawnForSuperb(ServerLevel level, LivingEntity shooter, ItemStack gunStack) {
        if (!BallisticBlockConfig.ENABLE_SHELL_CASINGS.get()) {
            return;
        }
        int maxNearby = BallisticBlockConfig.MAX_NEARBY_CASINGS.get();
        if (maxNearby <= 0 || tooManyNearbyCasings(level, shooter, maxNearby)) {
            return;
        }
        ResourceLocation gunId = BuiltInRegistries.ITEM.getKey(gunStack.getItem());
        CasingInfo info = fromGunId(gunId);
        level.addFreshEntity(createCasing(level, shooter, info));
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("taczbreach")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("casings")
                        .then(Commands.literal("delete")
                                .executes(context -> deleteCasings(context.getSource(), 128))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 1024))
                                        .executes(context -> deleteCasings(context.getSource(), IntegerArgumentType.getInteger(context, "radius")))))
                        .then(Commands.literal("clear")
                                .executes(context -> deleteCasings(context.getSource(), 128))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 1024))
                                        .executes(context -> deleteCasings(context.getSource(), IntegerArgumentType.getInteger(context, "radius")))))));
    }

    private static int deleteCasings(CommandSourceStack source, int radius) {
        ServerLevel level = source.getLevel();
        Vec3 center = source.getPosition();
        List<ShellCasingEntity> casings = level.getEntitiesOfClass(ShellCasingEntity.class, new AABB(center, center).inflate(radius), casing -> true);
        int count = casings.size();
        for (ShellCasingEntity casing : casings) {
            casing.discard();
        }
        source.sendSuccess(() -> Component.literal("Deleted " + count + " shell casings within " + radius + " blocks."), true);
        return count;
    }

    private static boolean tooManyNearbyCasings(ServerLevel level, LivingEntity shooter, int maxNearby) {
        AABB area = shooter.getBoundingBox().inflate(24.0D);
        return level.getEntitiesOfClass(ShellCasingEntity.class, area, Entity -> true).size() >= maxNearby;
    }

    private static ShellCasingEntity createCasing(ServerLevel level, LivingEntity shooter, CasingInfo casingInfo) {
        CasingType casingType = casingInfo.type();
        Vec3 look = shooter.getLookAngle();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 0.0001D) {
            float yaw = shooter.getYRot() * Mth.DEG_TO_RAD;
            right = new Vec3(Mth.cos(yaw), 0.0D, Mth.sin(yaw));
        } else {
            right = right.normalize();
        }
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 back = look.scale(-0.035D);

        Vec3 position = shooter.getEyePosition()
                .add(look.scale(0.22D))
                .add(right.scale(0.34D))
                .add(0.0D, -0.16D, 0.0D);

        double sideSpeed = casingType == CasingType.SHOTGUN ? 0.145D : 0.205D;
        double upwardSpeed = casingType == CasingType.SHOTGUN ? 0.105D : 0.145D;
        Vec3 randomDrift = new Vec3((level.random.nextDouble() - 0.5D) * 0.035D, level.random.nextDouble() * 0.025D, (level.random.nextDouble() - 0.5D) * 0.035D);
        Vec3 velocity = right.scale(sideSpeed)
                .add(up.scale(upwardSpeed))
                .add(back)
                .add(shooter.getDeltaMovement().scale(0.35D))
                .add(randomDrift);

        float spinBase = casingType == CasingType.SHOTGUN ? 13.0F : 22.0F;
        float spinX = spinBase + level.random.nextFloat() * spinBase;
        float spinY = (level.random.nextFloat() - 0.5F) * spinBase;
        float spinZ = spinBase * 0.7F + level.random.nextFloat() * spinBase;
        return new ShellCasingEntity(level, casingType, casingInfo.ammoId(), position, velocity, spinX, spinY, spinZ);
    }

    private static CasingInfo determineCasingInfo(ItemStack gunStack) {
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null) {
            return new CasingInfo(CasingType.RIFLE, DEFAULT_RIFLE_AMMO);
        }

        ResourceLocation gunId = gun.getGunId(gunStack);
        try {
            return TimelessAPI.getCommonGunIndex(gunId)
                    .map(ShellCasingSpawner::fromGunIndex)
                    .orElseGet(() -> fromGunId(gunId));
        } catch (RuntimeException exception) {
            TaczTacticalBreaching.LOGGER.debug("Could not inspect TaCZ gun index {}", gunId, exception);
            return fromGunId(gunId);
        }
    }

    private static CasingInfo fromGunIndex(CommonGunIndex index) {
        String type = index.getType() == null ? "" : index.getType().toLowerCase(Locale.ROOT);
        ResourceLocation ammoId = index.getGunData().getAmmoId();
        String ammoPath = ammoId == null ? "" : ammoId.getPath().toLowerCase(Locale.ROOT);

        if (type.contains("shotgun") || index.getBulletData().getBulletAmount() > 1 || ammoPath.contains("12g")) {
            return new CasingInfo(CasingType.SHOTGUN, ammoId == null ? DEFAULT_SHOTGUN_AMMO : ammoId);
        }
        if (type.contains("pistol") || ammoPath.contains("9mm") || ammoPath.contains("45acp") || ammoPath.contains("357") || ammoPath.contains("50ae")) {
            return new CasingInfo(CasingType.PISTOL, ammoId == null ? DEFAULT_PISTOL_AMMO : ammoId);
        }
        return new CasingInfo(CasingType.RIFLE, ammoId == null ? DEFAULT_RIFLE_AMMO : ammoId);
    }

    private static CasingInfo fromGunId(ResourceLocation gunId) {
        if (gunId == null) {
            return new CasingInfo(CasingType.RIFLE, DEFAULT_RIFLE_AMMO);
        }
        String path = gunId.getPath().toLowerCase(Locale.ROOT);
        if (path.contains("shotgun") || path.contains("m870") || path.contains("m1014") || path.contains("spas") || path.contains("aa12") || path.contains("db_")) {
            return new CasingInfo(CasingType.SHOTGUN, DEFAULT_SHOTGUN_AMMO);
        }
        if (path.contains("glock") || path.contains("pistol") || path.contains("m1911") || path.contains("m9") || path.contains("p320") || path.contains("cz75") || path.contains("b93r") || path.contains("deagle") || path.contains("mk23") || path.contains("rhino") || path.contains("taurus")) {
            return new CasingInfo(CasingType.PISTOL, DEFAULT_PISTOL_AMMO);
        }
        return new CasingInfo(CasingType.RIFLE, DEFAULT_RIFLE_AMMO);
    }

    private record CasingInfo(CasingType type, ResourceLocation ammoId) {
    }
}
