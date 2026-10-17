package com.victoriomods.taczballisticbreaching;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.event.common.GunShootEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class WeaponJamHandler {
    private static final long RAPID_FIRE_WINDOW_MILLIS = 5000L;
    private static final int CLEAR_SYNC_INTERVAL_TICKS = 5;
    private static final Map<UUID, ShotWindow> SHOT_WINDOWS = new HashMap<>();
    private static final Map<UUID, ClearState> CLEAR_STATES = new HashMap<>();
    private static final Map<UUID, Long> MESSAGE_COOLDOWNS = new HashMap<>();
    private static final ResourceLocation TACZ_EMPTY_GUN_SOUND_ID = new ResourceLocation("tacz", "gun");

    private WeaponJamHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGunShoot(GunShootEvent event) {
        if (!BallisticBlockConfig.ENABLE_WEAPON_JAMMING.get()) {
            return;
        }
        ItemStack gunStack = event.getGunItemStack();
        if (!WeaponJamData.isGun(gunStack) || !WeaponJamData.isJammed(gunStack)) {
            return;
        }
        event.setCanceled(true);
        if (event.getLogicalSide() != LogicalSide.SERVER || !(event.getShooter() instanceof ServerPlayer player)) {
            return;
        }
        warnJammed(player, gunStack, true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGunFire(GunFireEvent event) {
        ItemStack gunStack = event.getGunItemStack();
        if (!WeaponJamData.isGun(gunStack)) {
            return;
        }
        if (event.getLogicalSide() != LogicalSide.SERVER) {
            if (BallisticBlockConfig.ENABLE_WEAPON_JAMMING.get() && WeaponJamData.isJammed(gunStack)) {
                event.setCanceled(true);
            }
            return;
        }
        if (!(event.getShooter() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        if (BallisticBlockConfig.ENABLE_WEAPON_JAMMING.get()) {
            if (WeaponJamData.isJammed(gunStack)) {
                event.setCanceled(true);
                warnJammed(player, gunStack, true);
                return;
            }

            double chance = jamChance(level, player, gunStack);
            if (chance > 0.0D && level.random.nextDouble() < chance) {
                WeaponJamData.setJammed(gunStack, true);
                event.setCanceled(true);
                player.getInventory().setChanged();
                player.containerMenu.broadcastChanges();
                warnJammed(player, gunStack, false);
                syncState(player, gunStack, 0);
                return;
            }
        }

        int rapidShots = recordShot(player);
        if (BallisticBlockConfig.ENABLE_WEAPON_CONDITION.get()) {
            double wear = conditionWear(level, player, rapidShots);
            WeaponJamData.damageCondition(gunStack, wear);
            player.getInventory().setChanged();
            syncState(player, gunStack, 0);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGunReload(GunReloadEvent event) {
        if (event.getLogicalSide() != LogicalSide.SERVER || !BallisticBlockConfig.ENABLE_WEAPON_JAMMING.get()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack gunStack = event.getGunItemStack();
        if (WeaponJamData.isGun(gunStack) && WeaponJamData.isJammed(gunStack)) {
            event.setCanceled(true);
            warnJammed(player, gunStack, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        ClearState state = CLEAR_STATES.get(player.getUUID());
        if (state == null) {
            return;
        }

        ItemStack gunStack = currentGun(player);
        if (!state.pressed || gunStack.isEmpty() || !WeaponJamData.isJammed(gunStack)) {
            if (state.progressTicks != 0) {
                state.progressTicks = 0;
                syncState(player, gunStack, 0);
            }
            return;
        }

        int clearTicks = clearTimeTicks();
        state.progressTicks++;
        if (state.progressTicks >= clearTicks) {
            WeaponJamData.setJammed(gunStack, false);
            state.progressTicks = 0;
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            player.displayClientMessage(Component.literal("Malfunction Cleared"), true);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 0.75F, 0.92F + player.getRandom().nextFloat() * 0.12F);
            syncState(player, gunStack, 0);
            return;
        }

        if (state.progressTicks == 1 || state.progressTicks % CLEAR_SYNC_INTERVAL_TICKS == 0) {
            syncState(player, gunStack, state.progressTicks);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        CLEAR_STATES.remove(id);
        SHOT_WINDOWS.remove(id);
        MESSAGE_COOLDOWNS.remove(id);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!WeaponJamData.isGun(stack)) {
            return;
        }
        if (BallisticBlockConfig.ENABLE_WEAPON_CONDITION.get()) {
            event.getToolTip().add(Component.literal("Condition: " + Math.round(WeaponJamData.getCondition(stack)) + "%"));
        }
        if (WeaponJamData.isJammed(stack)) {
            event.getToolTip().add(Component.literal("JAMMED - hold J to clear"));
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("weaponjam")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("force")
                        .executes(context -> forceJam(context.getSource())))
                .then(Commands.literal("clear")
                        .executes(context -> clearJam(context.getSource()))));
        dispatcher.register(Commands.literal("weaponcondition")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                .executes(context -> setCondition(context.getSource(), IntegerArgumentType.getInteger(context, "value"))))));
    }

    static void setClearInput(ServerPlayer player, boolean pressed) {
        ClearState state = CLEAR_STATES.computeIfAbsent(player.getUUID(), ignored -> new ClearState());
        if (state.pressed != pressed) {
            state.pressed = pressed;
            if (!pressed) {
                state.progressTicks = 0;
                syncState(player, currentGun(player), 0);
            }
        }
    }

    private static int forceJam(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack gunStack = currentGun(player);
        if (gunStack.isEmpty()) {
            source.sendFailure(Component.literal("Hold a TaCZ firearm first."));
            return 0;
        }
        WeaponJamData.setJammed(gunStack, true);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        warnJammed(player, gunStack, false);
        syncState(player, gunStack, 0);
        source.sendSuccess(() -> Component.literal("Forced current weapon to jam."), true);
        return 1;
    }

    private static int clearJam(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack gunStack = currentGun(player);
        if (gunStack.isEmpty()) {
            source.sendFailure(Component.literal("Hold a TaCZ firearm first."));
            return 0;
        }
        WeaponJamData.setJammed(gunStack, false);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        syncState(player, gunStack, 0);
        source.sendSuccess(() -> Component.literal("Cleared current weapon jam."), true);
        return 1;
    }

    private static int setCondition(CommandSourceStack source, int value) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack gunStack = currentGun(player);
        if (gunStack.isEmpty()) {
            source.sendFailure(Component.literal("Hold a TaCZ firearm first."));
            return 0;
        }
        WeaponJamData.setCondition(gunStack, value);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        syncState(player, gunStack, 0);
        source.sendSuccess(() -> Component.literal("Set current weapon condition to " + value + "%."), true);
        return 1;
    }

    private static void warnJammed(ServerPlayer player, ItemStack gunStack, boolean throttled) {
        long now = System.currentTimeMillis();
        Long previous = MESSAGE_COOLDOWNS.get(player.getUUID());
        if (!throttled || previous == null || now - previous > 850L) {
            MESSAGE_COOLDOWNS.put(player.getUUID(), now);
            player.displayClientMessage(Component.literal("Weapon Jammed! Press J to clear malfunction."), true);
            playEmptyGunSound(player);
        }
        syncState(player, gunStack, 0);
    }

    private static void playEmptyGunSound(ServerPlayer player) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(TACZ_EMPTY_GUN_SOUND_ID);
        if (sound == null) {
            sound = SoundEvents.STONE_BUTTON_CLICK_OFF;
        }
        player.serverLevel().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.9F, 0.94F + player.getRandom().nextFloat() * 0.12F);
    }

    private static double jamChance(ServerLevel level, ServerPlayer player, ItemStack gunStack) {
        double condition = BallisticBlockConfig.ENABLE_WEAPON_CONDITION.get() ? WeaponJamData.getCondition(gunStack) : 100.0D;
        double chance = baseChance(condition);
        if (chance <= 0.0D) {
            return 0.0D;
        }
        if (BallisticBlockConfig.ENABLE_RAIN_JAMS.get() && level.isRainingAt(player.blockPosition())) {
            chance *= 1.65D;
        }
        if (BallisticBlockConfig.ENABLE_RAPID_FIRE_JAMS.get()) {
            int recentShots = recentShotCount(player);
            if (recentShots > 8) {
                chance += Math.min(0.04D, (recentShots - 8) * 0.0025D);
            }
        }
        return Mth.clamp(chance, 0.0D, 0.35D);
    }

    private static double baseChance(double condition) {
        if (condition >= 80.0D) {
            return 0.0D;
        }
        if (condition >= 60.0D) {
            return 0.0015D;
        }
        if (condition >= 40.0D) {
            return 0.006D;
        }
        if (condition >= 20.0D) {
            return 0.018D;
        }
        return 0.045D;
    }

    private static double conditionWear(ServerLevel level, ServerPlayer player, int rapidShots) {
        double wear = 0.055D;
        if (BallisticBlockConfig.ENABLE_RAPID_FIRE_JAMS.get() && rapidShots > 8) {
            wear += Math.min(0.08D, (rapidShots - 8) * 0.006D);
        }
        if (BallisticBlockConfig.ENABLE_RAIN_JAMS.get() && level.isRainingAt(player.blockPosition())) {
            wear *= 1.15D;
        }
        return wear;
    }

    private static int recordShot(ServerPlayer player) {
        long now = System.currentTimeMillis();
        ShotWindow window = SHOT_WINDOWS.computeIfAbsent(player.getUUID(), ignored -> new ShotWindow());
        cleanupWindow(window, now);
        window.shots.addLast(now);
        window.lastSeenMillis = now;
        cleanupShotWindows(now);
        return window.shots.size();
    }

    private static int recentShotCount(ServerPlayer player) {
        long now = System.currentTimeMillis();
        ShotWindow window = SHOT_WINDOWS.get(player.getUUID());
        if (window == null) {
            return 0;
        }
        cleanupWindow(window, now);
        return window.shots.size();
    }

    private static void cleanupWindow(ShotWindow window, long now) {
        long cutoff = now - RAPID_FIRE_WINDOW_MILLIS;
        while (!window.shots.isEmpty() && window.shots.peekFirst() < cutoff) {
            window.shots.removeFirst();
        }
    }

    private static void cleanupShotWindows(long now) {
        Iterator<Map.Entry<UUID, ShotWindow>> iterator = SHOT_WINDOWS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ShotWindow> entry = iterator.next();
            if (now - entry.getValue().lastSeenMillis > RAPID_FIRE_WINDOW_MILLIS * 3L) {
                iterator.remove();
            }
        }
    }

    private static ItemStack currentGun(Player player) {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (WeaponJamData.isGun(mainHand)) {
            return mainHand;
        }
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        return WeaponJamData.isGun(offHand) ? offHand : ItemStack.EMPTY;
    }

    private static void syncState(ServerPlayer player, ItemStack gunStack, int clearProgressTicks) {
        boolean jammed = !gunStack.isEmpty() && WeaponJamData.isJammed(gunStack);
        float condition = gunStack.isEmpty() ? 100.0F : (float) WeaponJamData.getCondition(gunStack);
        WeaponJamStatePacket packet = new WeaponJamStatePacket(jammed, condition, clearProgressTicks, clearTimeTicks());
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private static int clearTimeTicks() {
        return Math.max(1, (int) Math.ceil(BallisticBlockConfig.JAM_CLEAR_TIME_SECONDS.get() * 20.0D));
    }

    private static final class ShotWindow {
        private final ArrayDeque<Long> shots = new ArrayDeque<>();
        private long lastSeenMillis;
    }

    private static final class ClearState {
        private boolean pressed;
        private int progressTicks;
    }
}
