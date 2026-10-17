package com.victoriomods.taczballisticbreaching;

import com.mojang.brigadier.CommandDispatcher;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.Locale;
import java.util.UUID;

public final class ArmorPlateHandler {
    private static final String PLATE_TAG = TaczTacticalBreaching.MOD_ID + ":equipped_armor_plate";
    private static final UUID PLATE_SPEED_MODIFIER_ID = UUID.fromString("6a8ed179-e01c-46ec-9a9f-6888322fb4d8");

    private ArmorPlateHandler() {
    }

    static void equipPlate(Player player, InteractionHand hand, ItemStack handStack) {
        if (!(handStack.getItem() instanceof ArmorPlateItem plateItem)) {
            return;
        }
        ItemStack newPlate = handStack.copy();
        newPlate.setCount(1);
        ItemStack oldPlate = getEquippedPlate(player);
        setEquippedPlate(player, newPlate);

        handStack.shrink(1);
        if (!oldPlate.isEmpty()) {
            if (handStack.isEmpty()) {
                player.setItemInHand(hand, oldPlate);
            } else if (!player.getInventory().add(oldPlate)) {
                player.drop(oldPlate, false);
            }
        }

        player.displayClientMessage(Component.literal("Equipped " + plateItem.plateType().displayName()), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.75F, 0.95F);
        applyMovementPenalty(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGunHurt(EntityHurtByGunEvent.Pre event) {
        if (event.getLogicalSide() != LogicalSide.SERVER || !BallisticBlockConfig.ENABLE_ARMOR_PLATES.get()) {
            return;
        }
        if (!(event.getHurtEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack plateStack = getEquippedPlate(player);
        if (!(plateStack.getItem() instanceof ArmorPlateItem plateItem)) {
            removeMovementPenalty(player);
            return;
        }

        ShotInfo shotInfo = ShotInfo.from(event);
        double reduction = plateItem.plateType().reductionFor(shotInfo.threat(), shotInfo.armorPiercing());
        if (reduction <= 0.0D) {
            return;
        }

        float originalBase = Math.max(0.0F, event.getBaseAmount());
        float reducedBase = (float) (originalBase * (1.0D - reduction));
        event.setBaseAmount(Math.max(0.0F, reducedBase));

        int durabilityLoss = durabilityLoss(originalBase - reducedBase, shotInfo);
        boolean destroyed = plateItem.damagePlate(plateStack, durabilityLoss);
        if (destroyed) {
            clearEquippedPlate(player);
            player.displayClientMessage(Component.literal("Plate Destroyed"), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.9F, 0.8F);
            removeMovementPenalty(player);
            return;
        }

        setEquippedPlate(player, plateStack);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.32F, 1.25F);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (BallisticBlockConfig.ENABLE_ARMOR_PLATES.get() && hasPlate(event.player)) {
            applyMovementPenalty(event.player);
        } else {
            removeMovementPenalty(event.player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            return;
        }
        ItemStack plate = getEquippedPlate(event.getOriginal());
        if (!plate.isEmpty()) {
            setEquippedPlate(event.getEntity(), plate);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        applyMovementPenalty(event.getEntity());
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("armorplate")
                .then(Commands.literal("status")
                        .executes(context -> status(context.getSource())))
                .then(Commands.literal("remove")
                        .executes(context -> remove(context.getSource()))));
    }

    static boolean hasPlate(Player player) {
        return !getEquippedPlate(player).isEmpty();
    }

    private static int status(CommandSourceStack source) {
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }
        ItemStack plate = getEquippedPlate(player);
        if (!(plate.getItem() instanceof ArmorPlateItem plateItem)) {
            source.sendSuccess(() -> Component.literal("No armor plate equipped."), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal(plateItem.plateType().displayName() + " durability: " + plateItem.getPlateDurability(plate) + "/" + plateItem.plateType().maxDurability()), false);
        return 1;
    }

    private static int remove(CommandSourceStack source) {
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }
        ItemStack plate = getEquippedPlate(player);
        if (plate.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No armor plate equipped."), false);
            return 1;
        }
        clearEquippedPlate(player);
        if (!player.getInventory().add(plate)) {
            player.drop(plate, false);
        }
        removeMovementPenalty(player);
        source.sendSuccess(() -> Component.literal("Removed armor plate."), false);
        return 1;
    }

    private static int durabilityLoss(float absorbedDamage, ShotInfo shotInfo) {
        double multiplier = switch (shotInfo.threat()) {
            case PISTOL -> 16.0D;
            case RIFLE -> 22.0D;
            case HIGH_POWER -> 30.0D;
            case SHOTGUN -> 24.0D;
        };
        if (shotInfo.armorPiercing()) {
            multiplier *= 1.35D;
        }
        return Math.max(1, Mth.ceil(absorbedDamage * multiplier));
    }

    private static ItemStack getEquippedPlate(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(PLATE_TAG)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = ItemStack.of(data.getCompound(PLATE_TAG));
        return stack.getItem() instanceof ArmorPlateItem ? stack : ItemStack.EMPTY;
    }

    private static void setEquippedPlate(Player player, ItemStack plate) {
        if (plate.isEmpty()) {
            clearEquippedPlate(player);
            return;
        }
        ItemStack copy = plate.copy();
        copy.setCount(1);
        player.getPersistentData().put(PLATE_TAG, copy.save(new CompoundTag()));
    }

    private static void clearEquippedPlate(Player player) {
        player.getPersistentData().remove(PLATE_TAG);
    }

    private static void applyMovementPenalty(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        if (speed.getModifier(PLATE_SPEED_MODIFIER_ID) != null) {
            speed.removeModifier(PLATE_SPEED_MODIFIER_ID);
        }
        if (!BallisticBlockConfig.ENABLE_MOVEMENT_PENALTY.get()) {
            return;
        }
        ItemStack plate = getEquippedPlate(player);
        if (!(plate.getItem() instanceof ArmorPlateItem plateItem)) {
            return;
        }
        double penalty = plateItem.plateType().movementPenalty();
        if (penalty > 0.0D) {
            speed.addTransientModifier(new AttributeModifier(PLATE_SPEED_MODIFIER_ID, "Armor plate weight", -penalty, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void removeMovementPenalty(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(PLATE_SPEED_MODIFIER_ID) != null) {
            speed.removeModifier(PLATE_SPEED_MODIFIER_ID);
        }
    }

    private record ShotInfo(ArmorPlateType.Threat threat, boolean armorPiercing) {
        private static ShotInfo from(EntityHurtByGunEvent.Pre event) {
            Entity bullet = event.getBullet();
            if (bullet instanceof EntityKineticBullet kineticBullet) {
                if (BulletDamageCalculator.isShotgunImpact(kineticBullet, event.getHurtEntity().position())) {
                    return new ShotInfo(ArmorPlateType.Threat.SHOTGUN, BulletDamageCalculator.isArmorPiercing(kineticBullet));
                }
                if (BulletDamageCalculator.isHighCaliber(kineticBullet)) {
                    return new ShotInfo(ArmorPlateType.Threat.HIGH_POWER, BulletDamageCalculator.isArmorPiercing(kineticBullet));
                }
                return new ShotInfo(isPistolLike(kineticBullet.getGunId(), kineticBullet.getAmmoId()) ? ArmorPlateType.Threat.PISTOL : ArmorPlateType.Threat.RIFLE, BulletDamageCalculator.isArmorPiercing(kineticBullet));
            }
            ResourceLocation gunId = event.getGunId();
            String path = gunId == null ? "" : gunId.getPath().toLowerCase(Locale.ROOT);
            if (containsAny(path, "shotgun", "m870", "m1014", "spas", "aa12")) {
                return new ShotInfo(ArmorPlateType.Threat.SHOTGUN, containsAny(path, "ap", "armor_piercing", "fmj"));
            }
            if (containsAny(path, "50", "bmg", "127", "338", "m107", "m95")) {
                return new ShotInfo(ArmorPlateType.Threat.HIGH_POWER, containsAny(path, "ap", "armor_piercing", "fmj"));
            }
            return new ShotInfo(containsAny(path, "pistol", "glock", "m1911", "m9", "9mm", "45acp", "smg", "vector") ? ArmorPlateType.Threat.PISTOL : ArmorPlateType.Threat.RIFLE, containsAny(path, "ap", "armor_piercing", "fmj"));
        }

        private static boolean isPistolLike(ResourceLocation gunId, ResourceLocation ammoId) {
            return containsAny(path(gunId), "pistol", "glock", "m1911", "m9", "9mm", "smg", "vector")
                    || containsAny(path(ammoId), "9mm", "45acp", "380", "22lr", "57", "5_7", "pistol");
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
    }
}
