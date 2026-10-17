package com.victoriomods.taczballisticbreaching;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

public final class HandcuffEventHandler {
    private static final String RESTRAINED_KEY = TaczTacticalBreaching.MOD_ID + ":handcuffed";
    private static final String PREVIOUS_NO_AI_KEY = TaczTacticalBreaching.MOD_ID + ":handcuff_previous_no_ai";
    private static final String ANCHOR_X_KEY = TaczTacticalBreaching.MOD_ID + ":handcuff_anchor_x";
    private static final String ANCHOR_Y_KEY = TaczTacticalBreaching.MOD_ID + ":handcuff_anchor_y";
    private static final String ANCHOR_Z_KEY = TaczTacticalBreaching.MOD_ID + ":handcuff_anchor_z";
    private static final UUID SPEED_MODIFIER_ID = UUID.fromString("9b67e3bf-1388-4df6-9453-1a9d60f42f0e");
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
        handleEntityInteraction(event, event.getTarget(), event.getItemStack());
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        handleEntityInteraction(event, event.getTarget(), event.getItemStack());
    }

    private static void handleEntityInteraction(PlayerInteractEvent event, Entity target, ItemStack stack) {
        if (!enabled() || event.getLevel().isClientSide()) {
            return;
        }
        Player player = event.getEntity();
        if (isRestrained(player)) {
            block(event);
            return;
        }
        if (!(target instanceof LivingEntity livingTarget) || !canTarget(livingTarget)) {
            return;
        }

        boolean key = stack.is(ModItems.HANDCUFF_KEY.get());
        boolean cuffs = stack.is(ModItems.HANDCUFFS.get());
        boolean restrained = isRestrained(livingTarget);
        boolean forceUnlock = !BallisticBlockConfig.REQUIRE_KEY_TO_UNLOCK.get()
                || (BallisticBlockConfig.OPERATORS_CAN_FORCE_UNLOCK.get() && isOperator(player));

        if (restrained && (key || forceUnlock)) {
            setRestrained(livingTarget, false);
            player.level().playSound(null, livingTarget.blockPosition(), SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 0.65F, 1.15F);
            block(event);
            return;
        }
        if (!restrained && cuffs && !isSelf(player, livingTarget)) {
            setRestrained(livingTarget, true);
            player.level().playSound(null, livingTarget.blockPosition(), SoundEvents.ARMOR_EQUIP_CHAIN, SoundSource.PLAYERS, 0.65F, 0.9F);
            block(event);
        } else if (restrained) {
            block(event);
        }
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
    public static void onLivingAttack(LivingAttackEvent event) {
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
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (isRestrained(event.player)) {
            event.player.setSprinting(false);
            applySpeedModifier(event.player);
            if (event.player.tickCount % 40 == 0) {
                event.player.displayClientMessage(HANDCUFFED_MESSAGE, true);
            }
        } else {
            removeSpeedModifier(event.player);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
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
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new HandcuffSyncPacket(living.getId(), true));
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
                && serverPlayer.server.getPlayerList().isOp(serverPlayer.getGameProfile());
    }

    private static void block(PlayerInteractEvent event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    private static void applySpeedModifier(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null || speed.getModifier(SPEED_MODIFIER_ID) != null) {
            return;
        }
        double multiplier = BallisticBlockConfig.RESTRAINED_SPEED_MULTIPLIER.get();
        speed.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_ID, "Handcuffed speed", multiplier - 1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void removeSpeedModifier(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(SPEED_MODIFIER_ID) != null) {
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
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }
}
