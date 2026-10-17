package com.victoriomods.taczballisticbreaching;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
final class TacticalBreachingConfigScreen extends Screen {
    private static final int PANEL_WIDTH = 540;
    private static final int ROW_HEIGHT = 24;
    private static final int TOP = 62;
    private static final int BOTTOM_SPACE = 58;

    private final Screen parent;
    private final List<ConfigOption> options;
    private final List<String> categories;
    private final Map<String, String> pendingValues = new LinkedHashMap<>();
    private final List<VisibleRow> visibleRows = new ArrayList<>();

    private EditBox searchBox;
    private String searchText = "";
    private String selectedCategory = "all";
    private String statusText = "";
    private int statusColor = 0xA0A0A0;
    private int page;
    private int pageCount = 1;

    TacticalBreachingConfigScreen(Screen parent) {
        super(label("title", "TaCZ Tactical Breaching Config"));
        this.parent = parent;
        this.options = discoverOptions();
        Set<String> discoveredCategories = new LinkedHashSet<>();
        discoveredCategories.add("all");
        for (ConfigOption option : options) {
            discoveredCategories.add(option.category());
        }
        this.categories = List.copyOf(discoveredCategories);
    }

    @Override
    protected void init() {
        rebuildConfigWidgets();
    }

    private void rebuildConfigWidgets() {
        String currentSearch = searchBox == null ? searchText : searchBox.getValue();
        searchText = currentSearch == null ? "" : currentSearch;
        clearWidgets();
        visibleRows.clear();

        int panelWidth = Math.min(PANEL_WIDTH, width - 20);
        int left = (width - panelWidth) / 2;
        int searchWidth = Math.max(90, panelWidth - 246);

        searchBox = addRenderableWidget(new EditBox(
                font,
                left,
                30,
                searchWidth,
                20,
                label("search", "Search config")
        ));
        searchBox.setMaxLength(128);
        searchBox.setHint(label("search_hint", "Search settings..."));
        searchBox.setValue(searchText);

        addRenderableWidget(Button.builder(label("search", "Search"), button -> {
                    searchText = searchBox.getValue().trim();
                    page = 0;
                    rebuildConfigWidgets();
                })
                .bounds(left + searchWidth + 4, 30, 54, 20)
                .build());
        addRenderableWidget(Button.builder(label("clear", "Clear"), button -> {
                    searchText = "";
                    page = 0;
                    rebuildConfigWidgets();
                })
                .bounds(left + searchWidth + 62, 30, 48, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("<"), button -> cycleCategory(-1))
                .bounds(left + panelWidth - 128, 30, 20, 20)
                .build());
        Button categoryButton = addRenderableWidget(Button.builder(
                        categoryLabel(selectedCategory),
                        button -> cycleCategory(1)
                )
                .bounds(left + panelWidth - 104, 30, 80, 20)
                .build());
        categoryButton.setTooltip(Tooltip.create(Component.translatableWithFallback(key("category_tooltip"), "Category: %s", categoryLabel(selectedCategory))));
        addRenderableWidget(Button.builder(Component.literal(">"), button -> cycleCategory(1))
                .bounds(left + panelWidth - 20, 30, 20, 20)
                .build());

        List<ConfigOption> filtered = filteredOptions();
        int rowsPerPage = Math.max(3, (height - TOP - BOTTOM_SPACE) / ROW_HEIGHT);
        pageCount = Math.max(1, (filtered.size() + rowsPerPage - 1) / rowsPerPage);
        page = Math.max(0, Math.min(page, pageCount - 1));
        int start = page * rowsPerPage;
        int end = Math.min(filtered.size(), start + rowsPerPage);
        int labelWidth = Math.max(100, panelWidth / 2 - 18);
        int controlX = left + panelWidth / 2;
        int controlWidth = left + panelWidth - controlX;

        for (int index = start; index < end; index++) {
            ConfigOption option = filtered.get(index);
            int rowY = TOP + (index - start) * ROW_HEIGHT;
            String rawValue = pendingValues.getOrDefault(option.key(), option.currentValue());
            visibleRows.add(new VisibleRow(option, rowY, labelWidth));

            if (option.booleanValue()) {
                boolean enabled = Boolean.parseBoolean(rawValue);
                Button toggle = addRenderableWidget(Button.builder(toggleText(enabled), button -> {
                            boolean next = !Boolean.parseBoolean(
                                    pendingValues.getOrDefault(option.key(), option.currentValue())
                            );
                            pendingValues.put(option.key(), Boolean.toString(next));
                            button.setMessage(toggleText(next));
                            statusText = "Unsaved changes";
                            statusColor = 0xFFD166;
                        })
                        .bounds(controlX, rowY, controlWidth, 20)
                        .build());
                toggle.setTooltip(Tooltip.create(Component.literal(option.key())));
            } else {
                EditBox valueBox = addRenderableWidget(new EditBox(
                        font,
                        controlX,
                        rowY,
                        controlWidth,
                        20,
                        Component.literal(option.key())
                ));
                valueBox.setMaxLength(4096);
                valueBox.setValue(rawValue);
                valueBox.setResponder(value -> {
                    pendingValues.put(option.key(), value);
                    statusText = "Unsaved changes";
                    statusColor = 0xFFD166;
                });
                valueBox.setTooltip(Tooltip.create(Component.literal(
                        option.key() + "\nDefault: " + option.defaultValue()
                )));
            }
        }

        int bottomY = height - 28;
        Button previous = addRenderableWidget(Button.builder(label("previous", "< Prev"), button -> {
                    page--;
                    rebuildConfigWidgets();
                })
                .bounds(left, bottomY, 58, 20)
                .build());
        previous.active = page > 0;

        Button next = addRenderableWidget(Button.builder(label("next", "Next >"), button -> {
                    page++;
                    rebuildConfigWidgets();
                })
                .bounds(left + 62, bottomY, 58, 20)
                .build());
        next.active = page + 1 < pageCount;

        addRenderableWidget(Button.builder(label("reset_page", "Reset page"), button -> resetVisiblePage())
                .bounds(left + 128, bottomY, 82, 20)
                .build());
        addRenderableWidget(Button.builder(label("save", "Save"), button -> saveChanges(false))
                .bounds(left + panelWidth - 204, bottomY, 64, 20)
                .build());
        addRenderableWidget(Button.builder(label("save_close", "Save & close"), button -> saveChanges(true))
                .bounds(left + panelWidth - 136, bottomY, 88, 20)
                .build());
        addRenderableWidget(Button.builder(label("cancel", "Cancel"), button -> onClose())
                .bounds(left + panelWidth - 44, bottomY, 44, 20)
                .build());
    }

    private void cycleCategory(int direction) {
        int index = categories.indexOf(selectedCategory);
        selectedCategory = categories.get(Math.floorMod(index + direction, categories.size()));
        page = 0;
        rebuildConfigWidgets();
    }

    private void resetVisiblePage() {
        for (VisibleRow row : visibleRows) {
            pendingValues.put(row.option().key(), row.option().defaultValue());
        }
        statusText = "Page reset to defaults. Save to apply.";
        statusColor = 0xFFD166;
        rebuildConfigWidgets();
    }

    private List<ConfigOption> filteredOptions() {
        String query = searchText.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> selectedCategory.equals("all") || option.category().equals(selectedCategory))
                .filter(option -> query.isBlank()
                        || option.key().toLowerCase(Locale.ROOT).contains(query)
                        || optionLabel(option).getString().toLowerCase(Locale.ROOT).contains(query)
                        || prettyName(option.fieldName()).toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private void saveChanges(boolean closeAfterSave) {
        if (pendingValues.isEmpty()) {
            statusText = "No changes to save";
            statusColor = 0xA0A0A0;
            if (closeAfterSave) {
                onClose();
            }
            return;
        }

        int updateCount = pendingValues.size();
        if (!applyPendingValuesLocally()) {
            return;
        }

        if (isConnectedToServer()) {
            for (Map.Entry<String, String> entry : pendingValues.entrySet()) {
                ModNetwork.CHANNEL.sendToServer(new ConfigUpdatePacket(entry.getKey(), entry.getValue()));
            }
            pendingValues.clear();
            statusText = "Saved locally and sent " + updateCount + " setting(s) to server";
            statusColor = 0x55FF55;
            if (closeAfterSave) {
                onClose();
            } else {
                rebuildConfigWidgets();
            }
            return;
        }

        pendingValues.clear();
        statusText = "Configuration saved";
        statusColor = 0x55FF55;
        if (closeAfterSave) {
            onClose();
        } else {
            rebuildConfigWidgets();
        }
    }

    private boolean applyPendingValuesLocally() {
        Map<ConfigOption, Object> oldValues = new LinkedHashMap<>();
        try {
            for (ConfigOption option : options) {
                String rawValue = pendingValues.get(option.key());
                if (rawValue == null) {
                    continue;
                }
                oldValues.put(option, option.value().get());
                setValue(option.value(), parseValue(option.value().get(), rawValue));
            }
            BallisticBlockConfig.SPEC.save();
            refreshOptionValues();
            return true;
        } catch (RuntimeException exception) {
            for (Map.Entry<ConfigOption, Object> entry : oldValues.entrySet()) {
                try {
                    setValue(entry.getKey().value(), entry.getValue());
                } catch (RuntimeException ignored) {
                    // Keep the first validation error as the useful status message.
                }
            }
            statusText = "Invalid value: " + safeMessage(exception);
            statusColor = 0xFF5555;
            TaczTacticalBreaching.LOGGER.warn("Could not save the in-game config screen", exception);
            return false;
        }
    }

    private boolean isConnectedToServer() {
        return minecraft != null
                && minecraft.getConnection() != null;
    }

    private void refreshOptionValues() {
        for (int i = 0; i < options.size(); i++) {
            ConfigOption option = options.get(i);
            options.set(i, option.withCurrentValue(formatValue(option.value().get())));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        graphics.drawCenteredString(
                font,
                label("help", "All settings are saved to the common config file"),
                width / 2,
                18,
                0xA0A0A0
        );

        int panelWidth = Math.min(PANEL_WIDTH, width - 20);
        int left = (width - panelWidth) / 2;
        for (VisibleRow row : visibleRows) {
            String label = font.plainSubstrByWidth(optionLabel(row.option()).getString(), row.labelWidth());
            graphics.drawString(font, label, left + 4, row.y() + 6, 0xE0E0E0, false);
        }
        graphics.drawCenteredString(
                font,
                Component.translatableWithFallback(key("page"), "Page %s/%s", page + 1, pageCount),
                left + 270,
                height - 22,
                0xA0A0A0
        );
        if (!statusText.isBlank()) {
            graphics.drawCenteredString(font, Component.literal(statusText), width / 2, height - 40, statusColor);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private static List<ConfigOption> discoverOptions() {
        List<ConfigOption> discovered = new ArrayList<>();
        for (Field field : BallisticBlockConfig.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())
                    || !ForgeConfigSpec.ConfigValue.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                ForgeConfigSpec.ConfigValue<?> value = (ForgeConfigSpec.ConfigValue<?>) field.get(null);
                String key = pathOf(value, field);
                String category = categoryOf(key);
                Object current = value.get();
                discovered.add(new ConfigOption(
                        key,
                        field.getName(),
                        category,
                        value,
                        formatValue(current),
                        formatValue(value.getDefault()),
                        current instanceof Boolean
                ));
            } catch (IllegalAccessException exception) {
                TaczTacticalBreaching.LOGGER.warn(
                        "Could not expose config option {} in the config screen",
                        field.getName(),
                        exception
                );
            }
        }
        discovered.sort(Comparator.comparing(ConfigOption::category).thenComparing(ConfigOption::key));
        return discovered;
    }

    private static String pathOf(ForgeConfigSpec.ConfigValue<?> value, Field field) {
        try {
            Method method = value.getClass().getMethod("getPath");
            Object path = method.invoke(value);
            if (path instanceof Collection<?> collection && !collection.isEmpty()) {
                return String.join(".", collection.stream().map(String::valueOf).toList());
            }
        } catch (ReflectiveOperationException ignored) {
            // Forge versions without a public getPath still get a stable field-name fallback.
        }
        return field.getName().toLowerCase(Locale.ROOT);
    }

    private static String categoryOf(String key) {
        int separator = key.indexOf('.');
        return separator > 0 ? key.substring(0, separator) : "general";
    }

    private static Component toggleText(boolean enabled) {
        return label(enabled ? "on" : "off", enabled ? "ON" : "OFF")
                .withStyle(enabled ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED);
    }

    private static String key(String name) {
        return "config.tacz_tactical_breaching." + name;
    }

    private static MutableComponent label(String name, String fallback) {
        return Component.translatableWithFallback(key(name), fallback);
    }

    private static Component categoryLabel(String category) {
        return label("category." + category, prettyName(category));
    }

    private static Component optionLabel(ConfigOption option) {
        return label("option." + option.fieldName().toLowerCase(Locale.ROOT), prettyName(option.fieldName()));
    }

    private static String prettyName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String spaced = value
                .replace('.', ' ')
                .replace('_', ' ')
                .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(spaced.length());
        boolean capitalize = true;
        for (int i = 0; i < spaced.length(); i++) {
            char character = spaced.charAt(i);
            if (capitalize && Character.isLetter(character)) {
                result.append(Character.toUpperCase(character));
                capitalize = false;
            } else {
                result.append(character);
            }
            if (character == ' ') {
                capitalize = true;
            }
        }
        return result.toString();
    }

    private static Object parseValue(Object current, String rawValue) {
        String trimmed = rawValue.trim();
        if (current instanceof Boolean) {
            return switch (trimmed.toLowerCase(Locale.ROOT)) {
                case "true", "on", "yes", "1", "enabled" -> true;
                case "false", "off", "no", "0", "disabled" -> false;
                default -> throw new IllegalArgumentException("expected ON or OFF");
            };
        }
        if (current instanceof Integer) {
            return Integer.parseInt(trimmed);
        }
        if (current instanceof Long) {
            return Long.parseLong(trimmed);
        }
        if (current instanceof Double) {
            return Double.parseDouble(trimmed);
        }
        if (current instanceof Float) {
            return Float.parseFloat(trimmed);
        }
        if (current instanceof List<?>) {
            List<String> values = new ArrayList<>();
            if (trimmed.equalsIgnoreCase("clear") || trimmed.equals("[]") || trimmed.isBlank()) {
                return values;
            }
            for (String part : trimmed.split(",")) {
                String item = part.trim();
                if (!item.isEmpty()) {
                    values.add(item);
                }
            }
            return values;
        }
        return rawValue;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setValue(ForgeConfigSpec.ConfigValue value, Object newValue) {
        value.set(newValue);
    }

    private static String formatValue(Object value) {
        if (value instanceof List<?> list) {
            return String.join(", ", list.stream().map(String::valueOf).toList());
        }
        return String.valueOf(value);
    }

    private static String safeMessage(RuntimeException exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getMessage();
    }

    private record ConfigOption(
            String key,
            String fieldName,
            String category,
            ForgeConfigSpec.ConfigValue<?> value,
            String currentValue,
            String defaultValue,
            boolean booleanValue
    ) {
        ConfigOption withCurrentValue(String value) {
            return new ConfigOption(key, fieldName, category, this.value, value, defaultValue, booleanValue);
        }
    }

    private record VisibleRow(ConfigOption option, int y, int labelWidth) {
    }
}
