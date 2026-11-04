package com.onxon.profittracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Saved to config/donut-profit-tracker/config.json.
 * The chat regexes live here too, so you can tune them without recompiling.
 */
public final class Settings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Settings instance = new Settings();

    public static final int[] ACCENTS = {0x9B6BFF, 0x22D3EE, 0xFF6BD6, 0x4ADE80, 0xFBBF24};
    public static final float[] OPACITIES = {1.0f, 0.85f, 0.7f, 0.5f};

    public static final class WidgetPos {
        public int x;
        public int y;
        public boolean visible;

        public WidgetPos() {}

        public WidgetPos(int x, int y, boolean visible) {
            this.x = x;
            this.y = y;
            this.visible = visible;
        }
    }

    /** Regexes with named groups: amount, suffix (optional K/M/B/T) and player. */
    public List<String> incomingPatterns = new ArrayList<>(List.of(
            "(?i)you (?:received|got) \\$?(?<amount>[\\d.,]+)(?<suffix>[kmbt])? from (?<player>\\w{1,16})",
            "(?i)(?<player>\\w{1,16}) (?:paid|sent|gave) you \\$?(?<amount>[\\d.,]+)(?<suffix>[kmbt])?"
    ));

    public List<String> outgoingPatterns = new ArrayList<>(List.of(
            "(?i)you (?:paid|sent|gave) \\$?(?<amount>[\\d.,]+)(?<suffix>[kmbt])? to (?<player>\\w{1,16})"
    ));

    public boolean cleanBackground = true;
    public boolean sessionOnly = true;
    public int feedRows = 8;
    public float opacity = 0.85f;
    public int accentRgb = 0x9B6BFF;
    public Map<String, WidgetPos> widgets = new LinkedHashMap<>();

    private Settings() {}

    public static Settings get() {
        return instance;
    }

    public WidgetPos widget(String id) {
        return widgets.computeIfAbsent(id, Settings::defaultPos);
    }

    private static WidgetPos defaultPos(String id) {
        return switch (id) {
            case HudWidgets.FEED -> new WidgetPos(6, 88, false);
            case HudWidgets.GAMBLERS -> new WidgetPos(6, 212, false);
            default -> new WidgetPos(6, 6, true);
        };
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("donut-profit-tracker").resolve("config.json");
    }

    public static void load() {
        Path file = file();
        if (Files.exists(file)) {
            try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Settings loaded = GSON.fromJson(reader, Settings.class);
                if (loaded != null) instance = loaded;
            } catch (Exception e) {
                ProfitTrackerClient.LOGGER.warn("Could not read config, using defaults", e);
            }
        }
        if (instance.widgets == null) instance.widgets = new LinkedHashMap<>();
        if (instance.incomingPatterns == null) instance.incomingPatterns = new ArrayList<>();
        if (instance.outgoingPatterns == null) instance.outgoingPatterns = new ArrayList<>();
        save();
    }

    public static void save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (IOException e) {
            ProfitTrackerClient.LOGGER.warn("Could not save config", e);
        }
    }
}
