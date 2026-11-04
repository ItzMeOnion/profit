package com.onxon.profittracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Every payment you have seen, plus the session start time.
 * Saved to config/donut-profit-tracker/data.json.
 */
public final class Ledger {
    private static final int MAX_STORED = 2000;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Ledger INSTANCE = new Ledger();

    private final List<Transaction> all = new ArrayList<>();
    private long sessionStart = System.currentTimeMillis();

    private Ledger() {}

    public static Ledger get() {
        return INSTANCE;
    }

    /** Per-player numbers, from the other player's point of view where noted. */
    public static final class PlayerStat {
        public final String name;
        /** Money they sent you (what they spent with you). */
        public double fromThem;
        /** Money you sent them. */
        public double toThem;
        public int count;

        PlayerStat(String name) {
            this.name = name;
        }

        /** Their profit: what they got from you minus what they gave you. Positive = they won. */
        public double theirNet() {
            return toThem - fromThem;
        }
    }

    private static final class SaveFile {
        long sessionStart;
        List<Transaction> transactions;
    }

    public void record(Transaction t) {
        all.add(t);
        while (all.size() > MAX_STORED) all.remove(0);
        save();
    }

    public void resetSession() {
        sessionStart = System.currentTimeMillis();
        save();
    }

    public void clearAll() {
        all.clear();
        sessionStart = System.currentTimeMillis();
        save();
    }

    public long sessionStart() {
        return sessionStart;
    }

    /** Newest first. */
    public List<Transaction> view(boolean sessionOnly) {
        List<Transaction> out = new ArrayList<>();
        for (Transaction t : all) {
            if (!sessionOnly || t.time() >= sessionStart) out.add(t);
        }
        out.sort(Comparator.comparingLong(Transaction::time).reversed());
        return out;
    }

    /** Returns {incoming, outgoing}. */
    public double[] totals(boolean sessionOnly) {
        double in = 0;
        double out = 0;
        for (Transaction t : all) {
            if (sessionOnly && t.time() < sessionStart) continue;
            if (t.incoming()) in += t.amount();
            else out += t.amount();
        }
        return new double[]{in, out};
    }

    public Map<String, PlayerStat> playerStats(boolean sessionOnly) {
        Map<String, PlayerStat> map = new LinkedHashMap<>();
        for (Transaction t : view(sessionOnly)) {
            PlayerStat s = map.computeIfAbsent(t.player().toLowerCase(Locale.ROOT), k -> new PlayerStat(t.player()));
            if (t.incoming()) s.fromThem += t.amount();
            else s.toThem += t.amount();
            s.count++;
        }
        return map;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("donut-profit-tracker").resolve("data.json");
    }

    public static void load() {
        Path file = file();
        if (!Files.exists(file)) return;
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            SaveFile data = GSON.fromJson(reader, SaveFile.class);
            if (data == null) return;
            INSTANCE.all.clear();
            if (data.transactions != null) INSTANCE.all.addAll(data.transactions);
            if (data.sessionStart > 0) INSTANCE.sessionStart = data.sessionStart;
        } catch (Exception e) {
            ProfitTrackerClient.LOGGER.warn("Could not read payment history", e);
        }
    }

    private void save() {
        SaveFile f = new SaveFile();
        f.sessionStart = sessionStart;
        f.transactions = all;
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(f), StandardCharsets.UTF_8);
        } catch (IOException e) {
            ProfitTrackerClient.LOGGER.warn("Could not save payment history", e);
        }
    }
}
