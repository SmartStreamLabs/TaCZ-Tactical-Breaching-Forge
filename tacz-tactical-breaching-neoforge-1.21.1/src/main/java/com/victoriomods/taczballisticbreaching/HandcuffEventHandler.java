package com.victoriomods.taczballisticbreaching;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class HandcuffEventHandler {
    private static final String RESTRAINED_KEY = TaczTacticalBreaching.MOD_ID + ":handcuffed";
    private static final String PREVIOUS_NO_AI_KEY = TaczTacticalBreaching.MOD_ID + ":handcuff_previous_no_ai";
    private static final String ANCHOR_X_KEY = TaczTacticalBreaching.MOD_ID + ":handcuff_anchor_x";
    private static final String ANCHOR_Y_KEY = TaczTacticalBreaching.MOD_ID + ":handcuff_anchor_y";
    private static final String ANCHOR_Z_KEY = TaczTacticalBreaching.MOD_ID + ":handcuff_anchor_z";
    private static final ResourceLocation SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(TaczTacticalBreaching.MOD_ID, "handcuffed_speed");
    private static final Component HANDCUFFED_MESSAGE = Component.literal("You are handcuffed.");

    private HandcuffEventHandler() {
    }

    static boolean isRestrained(Entity entity) {
        return entity instanceof LivingEntity && entity.getPersistentData().getBoolean(RESTRAINED_KEY);
    }

    static void setRestrained(LivingEntity entity, boolean restrained) {
        CompoundTag data = entity.getPersistentData();
        if (restrained) {
            data.putBoolean(RESTRAINED_KEY, true);
            if (entity instanceof Villager villager) {
                data.putBoolean(PREVIOUS_NO_AI_KEY, villager.isNoAi());
                data.putDouble(ANCHOR_X_KEY, villager.getX());
                data.putDouble(ANCHOR_Y_KEY, villager.getY());
                data.putDouble(ANCHOR_Z_KEY, villager.getZ());
                villager.getNavigation().stop();
                villager.setNoAi(true);
            }
            if (entity instanceof Player player) {
                applySpeedModifier(player);
                player.setSprinting(false);
            }
        } else {
            data.remove(RESTRAINED_KEY);
            if (entity instanceof Villager villager) {
                boolean previousNoAi = data.getBoolean(PREVIOUS_NO_AI_KEY);
                villager.setNoAi(previousNoAi);
                data.remove(PREVIOUS_NO_AI_KEY);
                data.remove(ANCHOR_X_KEY);
                data.remove(ANCHOR_Y_KEY);
                data.remove(ANCHOR_Z_KEY);
            }
            if (entity instanceof Player player) {
                removeSpeedModifier(player);
            }
        }
        sync(entity);
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("taczbreach")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("handcuff")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    setRestrained(target, true);
                                    context.getSource().sendSuccess(() -> Component.literal("Handcuffed " + target.getGameProfile().getName() + "."), true);
                                    return 1;
                                })))
                .then(Commands.literal("unhandcuff")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    setRestrained(target, false);
                                    context.getSource().sendSuccess(() -> Component.literal("Released " + target.getGameProfile().getName() + "."), true);
                                    return 1;
                                }))));
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (handleEntityInteraction(event.getEntity(), event.getLevel(), event.getTarget(), event.getItemStack())) {
            block(event);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (handleEntityInteraction(event.getEntity(), event.getLevel(), event.getTarget(), event.getItemStack())) {
            block(event);
        }
    }

    private static boolean handleEntityInteraction(Player player, Level level, Entity target, ItemStack stack) {
        if (!enabled() || level.isClientSide()) {
            return false;
        }
        if (isRestrained(player)) {
            return true;
        }
        if (!(target instanceof LivingEntity livingTarget) || !canTarget(livingTarget)) {
            return false;
        }

        boolean key = stack.is(ModItems.HANDCUFF_KEY.get());
        boolean cuffs = stack.is(ModItems.HANDCUFFS.get());
        boolean restrained = isRestrained(livingTarget);
        boolean forceUnlock = !BallisticBlockConfig.REQUIRE_KEY_TO_UNLOCK.get()
                || (BallisticBlockConfig.OPERATORS_CAN_FORCE_UNLOCK.get() && isOperator(player));

        if (restrained && (key || forceUnlock)) {
            setRestrained(livingTarget, false);
            player.level().playSound(null, livingTarget.blockPosition(), SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 0.65F, 1.15F);
            return true;
        }
        if (!restrained && cuffs && !isSelf(player, livingTarget)) {
            setRestrained(livingTarget, true);
            player.level().playSound(null, livingTarget.blockPosition(), SoundEvents.ARMOR_EQUIP_CHAIN.value(), SoundSource.PLAYERS, 0.65F, 0.9F);
            return true;
        }
        return restrained;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isRestrained(event.getEntity())) {
            block(event);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isRestrained(event.getEntity())) {
            block(event);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isRestrained(event.getEntity())) {
            block(event);
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (isRestrained(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (isRestrained(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMultiPlaceBlock(BlockEvent.EntityMultiPlaceEvent event) {
        if (isRestrained(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (isRestrained(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        if (isRestrained(attacker) || isRestrained(direct)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (isRestrained(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (BallisticBlockConfig.BLOCK_INVENTORY_INTERACTION.get() && isRestrained(event.getEntity())) {
            event.getEntity().closeContainer();
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (isRestrained(player)) {
            player.setSprinting(false);
            applySpeedModifier(player);
            if (player.tickCount % 40 == 0) {
                player.displayClientMessage(HANDCUFFED_MESSAGE, true);
            }
        } else {
            removeSpeedModifier(player);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Villager villager) || villager.level().isClientSide() || !isRestrained(villager)) {
            return;
        }
        villager.setNoAi(true);
        villager.setDeltaMovement(0.0D, Math.min(0.0D, villager.getDeltaMovement().y), 0.0D);
        villager.getNavigation().stop();
        CompoundTag data = villager.getPersistentData();
        if (data.contains(ANCHOR_X_KEY)) {
            double dx = villager.getX() - data.getDouble(ANCHOR_X_KEY);
            double dy = villager.getY() - data.getDouble(ANCHOR_Y_KEY);
            double dz = villager.getZ() - data.getDouble(ANCHOR_Z_KEY);
            if (dx * dx + dy * dy + dz * dz > 0.16D) {
                villager.teleportTo(data.getDouble(ANCHOR_X_KEY), data.getDouble(ANCHOR_Y_KEY), data.getDouble(ANCHOR_Z_KEY));
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living && isRestrained(living)) {
            if (living instanceof Player player) {
                applySpeedModifier(player);
            }
            if (living instanceof Villager villager) {
                villager.setNoAi(true);
            }
            sync(living);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (isRestrained(player)) {
                applySpeedModifier(player);
            }
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof LivingEntity living && isRestrained(living)) {
            ModNetwork.sendToPlayer(player, new HandcuffSyncPacket(living.getId(), true));
        }
    }

    private static boolean enabled() {
        return BallisticBlockConfig.HANDCUFFS_ENABLED.get();
    }

    private static boolean canTarget(LivingEntity target) {
        if (target instanceof Player) {
            return BallisticBlockConfig.ALLOW_HANDCUFF_PLAYERS.get();
        }
        if (target instanceof Villager) {
            return BallisticBlockConfig.ALLOW_HANDCUFF_VILLAGERS.get();
        }
        return false;
    }

    private static boolean isSelf(Player player, LivingEntity target) {
        return player.getId() == target.getId();
    }

    private static boolean isOperator(Player player) {
        return player instanceof ServerPlayer serverPlayer
                && serverPlayer.getServer().getPlayerList().isOp(serverPlayer.getGameProfile());
    }

    private static void block(PlayerInteractEvent.EntityInteract event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    private static void block(PlayerInteractEvent.EntityInteractSpecific event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    private static void block(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    private static void block(PlayerInteractEvent.RightClickItem event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    private static void block(PlayerInteractEvent.LeftClickBlock event) {
        event.setCanceled(true);
    }

    private static void applySpeedModifier(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null || speed.hasModifier(SPEED_MODIFIER_ID)) {
            return;
        }
        double multiplier = BallisticBlockConfig.RESTRAINED_SPEED_MULTIPLIER.get();
        speed.addOrUpdateTransientModifier(new AttributeModifier(SPEED_MODIFIER_ID, multiplier - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void removeSpeedModifier(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.hasModifier(SPEED_MODIFIER_ID)) {
            speed.removeModifier(SPEED_MODIFIER_ID);
        }
    }

    private static void sync(LivingEntity entity) {
        Level level = entity.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        HandcuffSyncPacket packet = new HandcuffSyncPacket(entity.getId(), isRestrained(entity));
        for (ServerPlayer player : serverLevel.players()) {
            if (player == entity || player.distanceToSqr(entity) <= 128.0D * 128.0D) {
                ModNetwork.sendToPlayer(player, packet);
            }
        }
    }
}
