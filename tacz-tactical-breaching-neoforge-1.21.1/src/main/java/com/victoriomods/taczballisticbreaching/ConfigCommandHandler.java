package com.victoriomods.taczballisticbreaching;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class ConfigCommandHandler {
    private static final int VALUES_PER_PAGE = 10;
    private static final Map<String, ConfigEntry> CONFIG_VALUES = new LinkedHashMap<>();
    private static final List<ConfigEntry> PRIMARY_ENTRIES = new ArrayList<>();
    private static boolean loaded;

    private ConfigCommandHandler() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("taczbreach")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("config")
                        .then(Commands.literal("list")
                                .executes(context -> list(context, 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(context -> list(context, IntegerArgumentType.getInteger(context, "page")))))
                        .then(Commands.literal("get")
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .suggests(ConfigCommandHandler::suggestKeys)
                                        .executes(ConfigCommandHandler::get)))
                        .then(Commands.literal("set")
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .suggests(ConfigCommandHandler::suggestKeys)
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .executes(ConfigCommandHandler::set))))
                        .then(Commands.literal("save")
                                .executes(ConfigCommandHandler::save))));
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncAllToPlayer(player);
        }
    }

    private static int list(CommandContext<CommandSourceStack> context, int page) {
        ensureLoaded();
        int totalPages = Math.max(1, (int) Math.ceil(PRIMARY_ENTRIES.size() / (double) VALUES_PER_PAGE));
        int safePage = Math.min(page, totalPages);
        int start = (safePage - 1) * VALUES_PER_PAGE;
        int end = Math.min(start + VALUES_PER_PAGE, PRIMARY_ENTRIES.size());
        context.getSource().sendSuccess(() -> Component.literal("TaCZ Tactical Breaching config page " + safePage + "/" + totalPages), false);
        for (int i = start; i < end; i++) {
            ConfigEntry entry = PRIMARY_ENTRIES.get(i);
            context.getSource().sendSuccess(() -> Component.literal(entry.key() + " = " + formatValue(entry.value().get())), false);
        }
        return end - start;
    }

    private static int get(CommandContext<CommandSourceStack> context) {
        ensureLoaded();
        String key = StringArgumentType.getString(context, "key");
        ConfigEntry entry = CONFIG_VALUES.get(normalizeKey(key));
        if (entry == null) {
            context.getSource().sendFailure(Component.literal("Unknown config key: " + key));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal(entry.key() + " = " + formatValue(entry.value().get())), false);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> context) {
        ensureLoaded();
        String key = StringArgumentType.getString(context, "key");
        String rawValue = StringArgumentType.getString(context, "value").trim();
        ConfigEntry entry = CONFIG_VALUES.get(normalizeKey(key));
        if (entry == null) {
            context.getSource().sendFailure(Component.literal("Unknown config key: " + key));
            return 0;
        }
        try {
            Object parsed = parseValue(entry.value().get(), rawValue);
            setConfigValue(entry.value(), parsed);
            BallisticBlockConfig.SPEC.save();
            syncToClients(context.getSource(), entry.key(), formatValue(entry.value().get()));
            context.getSource().sendSuccess(() -> Component.literal("Set " + entry.key() + " = " + formatValue(entry.value().get())), true);
            TaczTacticalBreaching.LOGGER.info("Config changed in-game: {} = {}", entry.key(), formatValue(entry.value().get()));
            return 1;
        } catch (IllegalArgumentException exception) {
            context.getSource().sendFailure(Component.literal("Invalid value for " + entry.key() + ": " + exception.getMessage()));
            return 0;
        } catch (RuntimeException exception) {
            context.getSource().sendFailure(Component.literal("Could not set " + entry.key() + ": " + exception.getMessage()));
            TaczTacticalBreaching.LOGGER.warn("Could not set config value {}", entry.key(), exception);
            return 0;
        }
    }

    private static int save(CommandContext<CommandSourceStack> context) {
        BallisticBlockConfig.SPEC.save();
        context.getSource().sendSuccess(() -> Component.literal("TaCZ Tactical Breaching config saved."), true);
        return 1;
    }

    static boolean applyRemoteValue(String key, String rawValue) {
        ensureLoaded();
        ConfigEntry entry = CONFIG_VALUES.get(normalizeKey(key));
        if (entry == null) {
            TaczTacticalBreaching.LOGGER.warn("Received unknown synced config key from server: {}", key);
            return false;
        }
        try {
            Object parsed = parseValue(entry.value().get(), rawValue);
            setConfigValue(entry.value(), parsed);
            TaczTacticalBreaching.LOGGER.debug("Applied synced config value: {} = {}", entry.key(), formatValue(entry.value().get()));
            return true;
        } catch (RuntimeException exception) {
            TaczTacticalBreaching.LOGGER.warn("Could not apply synced config value {}={}", key, rawValue, exception);
            return false;
        }
    }

    static void applyClientUpdate(ServerPlayer player, String key, String rawValue) {
        if (!canEditServerConfig(player)) {
            player.sendSystemMessage(Component.literal(
                    "You need operator permission to change TaCZ Tactical Breaching server settings."
            ));
            TaczTacticalBreaching.LOGGER.warn(
                    "Rejected config update from non-operator {} for {}",
                    player.getGameProfile().getName(),
                    key
            );
            syncAllToPlayer(player);
            return;
        }

        ensureLoaded();
        ConfigEntry entry = CONFIG_VALUES.get(normalizeKey(key));
        if (entry == null) {
            player.sendSystemMessage(Component.literal("Unknown config key: " + key));
            syncAllToPlayer(player);
            return;
        }

        try {
            Object parsed = parseValue(entry.value().get(), rawValue.trim());
            setConfigValue(entry.value(), parsed);
            BallisticBlockConfig.SPEC.save();
            String formatted = formatValue(entry.value().get());
            syncToClients(player.createCommandSourceStack(), entry.key(), formatted);
            player.sendSystemMessage(Component.literal("Config updated: " + entry.key() + " = " + formatted));
            TaczTacticalBreaching.LOGGER.info(
                    "Config changed from GUI by {}: {} = {}",
                    player.getGameProfile().getName(),
                    entry.key(),
                    formatted
            );
        } catch (RuntimeException exception) {
            player.sendSystemMessage(Component.literal(
                    "Invalid value for " + entry.key() + ": " + safeMessage(exception)
            ));
            TaczTacticalBreaching.LOGGER.warn(
                    "Could not apply GUI config update {}={} from {}",
                    entry.key(),
                    rawValue,
                    player.getGameProfile().getName(),
                    exception
            );
            syncAllToPlayer(player);
        }
    }

    private static boolean canEditServerConfig(ServerPlayer player) {
        return player.hasPermissions(2)
                || (player.getServer() != null
                && player.getServer().isSingleplayerOwner(player.getGameProfile()));
    }

    private static CompletableFuture<Suggestions> suggestKeys(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        ensureLoaded();
        String remaining = builder.getRemainingLowerCase();
        for (String key : CONFIG_VALUES.keySet()) {
            if (key.startsWith(remaining)) {
                builder.suggest(key);
            }
        }
        return builder.buildFuture();
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        List<ConfigEntry> entries = new ArrayList<>();
        for (Field field : BallisticBlockConfig.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !ModConfigSpec.ConfigValue.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                ModConfigSpec.ConfigValue<?> value = (ModConfigSpec.ConfigValue<?>) field.get(null);
                String key = pathOf(value, field);
                ConfigEntry entry = new ConfigEntry(key, field.getName(), value);
                entries.add(entry);
            } catch (IllegalAccessException exception) {
                TaczTacticalBreaching.LOGGER.warn("Could not expose config value {} to in-game commands", field.getName(), exception);
            }
        }
        entries.sort(Comparator.comparing(ConfigEntry::key));
        for (ConfigEntry entry : entries) {
            registerEntry(entry.key(), entry);
            registerEntry(toFieldAlias(entry.key()), entry);
            registerEntry(entry.key().replace("_", ""), entry);
            registerEntry(toFieldAlias(entry.key()).replace("_", ""), entry);
            registerEntry(entry.fieldName().toLowerCase(Locale.ROOT), entry);
            registerEntry(entry.fieldName().toLowerCase(Locale.ROOT).replace("_", ""), entry);
            registerEntry(toCamelAlias(entry.fieldName()), entry);
            PRIMARY_ENTRIES.add(entry);
        }
        loaded = true;
    }

    private static void registerEntry(String key, ConfigEntry entry) {
        if (key != null && !key.isBlank()) {
            CONFIG_VALUES.putIfAbsent(normalizeKey(key), entry);
        }
    }

    private static String pathOf(ModConfigSpec.ConfigValue<?> value, Field field) {
        try {
            Method method = value.getClass().getMethod("getPath");
            Object path = method.invoke(value);
            if (path instanceof Collection<?> collection && !collection.isEmpty()) {
                List<String> parts = new ArrayList<>();
                for (Object part : collection) {
                    parts.add(String.valueOf(part));
                }
                return String.join(".", parts);
            }
        } catch (ReflectiveOperationException ignored) {
            // Older loaders still work through the field-name fallback.
        }
        return field.getName().toLowerCase(Locale.ROOT);
    }

    private static String toFieldAlias(String key) {
        int dot = key.lastIndexOf('.');
        return dot >= 0 ? key.substring(dot + 1) : key;
    }

    private static String toCamelAlias(String fieldName) {
        String lower = fieldName.toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        boolean upperNext = false;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (c == '_') {
                upperNext = true;
            } else if (upperNext) {
                builder.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private static String normalizeKey(String key) {
        return key.toLowerCase(Locale.ROOT);
    }

    private static Object parseValue(Object current, String rawValue) {
        if (current instanceof Boolean) {
            return parseBoolean(rawValue);
        }
        if (current instanceof Integer) {
            return Integer.parseInt(rawValue);
        }
        if (current instanceof Long) {
            return Long.parseLong(rawValue);
        }
        if (current instanceof Double) {
            return Double.parseDouble(rawValue);
        }
        if (current instanceof Float) {
            return Float.parseFloat(rawValue);
        }
        if (current instanceof List<?>) {
            return parseList(rawValue);
        }
        return rawValue;
    }

    private static boolean parseBoolean(String rawValue) {
        return switch (rawValue.toLowerCase(Locale.ROOT)) {
            case "true", "on", "yes", "1", "enabled" -> true;
            case "false", "off", "no", "0", "disabled" -> false;
            default -> throw new IllegalArgumentException("expected true/false, on/off, yes/no, or 1/0");
        };
    }

    private static List<String> parseList(String rawValue) {
        List<String> values = new ArrayList<>();
        if (rawValue.equalsIgnoreCase("clear") || rawValue.equals("[]")) {
            return values;
        }
        for (String part : rawValue.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setConfigValue(ModConfigSpec.ConfigValue value, Object parsed) {
        value.set(parsed);
    }

    private static void syncToClients(CommandSourceStack source, String key, String value) {
        ConfigSyncPacket packet = new ConfigSyncPacket(key, value);
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            ModNetwork.sendToPlayer(player, packet);
        }
    }

    private static void syncAllToPlayer(ServerPlayer player) {
        ensureLoaded();
        for (ConfigEntry entry : PRIMARY_ENTRIES) {
            ModNetwork.sendToPlayer(
                    player,
                    new ConfigSyncPacket(entry.key(), formatValue(entry.value().get()))
            );
        }
        TaczTacticalBreaching.LOGGER.debug(
                "Synced {} config values to {}",
                PRIMARY_ENTRIES.size(),
                player.getGameProfile().getName()
        );
    }

    private static String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private static String formatValue(Object value) {
        if (value instanceof List<?> list) {
            return String.join(", ", list.stream().map(String::valueOf).toList());
        }
        return String.valueOf(value);
    }

    private record ConfigEntry(String key, String fieldName, ModConfigSpec.ConfigValue<?> value) {
    }
}
