package com.onxon.profittracker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The three pop-out panels. The same drawing code runs in-game and in the layout editor. */
public final class HudWidgets {
    public static final String SUMMARY = "summary";
    public static final String FEED = "feed";
    public static final String GAMBLERS = "gamblers";
    public static final int WIDTH = 176;

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private HudWidgets() {}

    public static int width(String id) {
        return WIDTH;
    }

    public static int height(String id) {
        return switch (id) {
            case FEED -> 22 + Settings.get().feedRows * 11 + 6;
            case GAMBLERS -> 22 + 5 * 11 + 8;
            default -> 76;
        };
    }

    public static void renderAll(DrawContext ctx) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        Settings s = Settings.get();
        int alpha = Math.round(255 * s.opacity);
        for (String id : List.of(SUMMARY, FEED, GAMBLERS)) {
            Settings.WidgetPos p = s.widget(id);
            if (p.visible) draw(ctx, tr, id, p.x, p.y, alpha);
        }
    }

    public static void draw(DrawContext ctx, TextRenderer tr, String id, int x, int y, int alpha) {
        int accent = Settings.get().accentRgb;
        switch (id) {
            case FEED -> drawFeed(ctx, tr, x, y, alpha, accent);
            case GAMBLERS -> drawGamblers(ctx, tr, x, y, alpha, accent);
            default -> drawSummary(ctx, tr, x, y, alpha, accent);
        }
    }

    private static void drawSummary(DrawContext ctx, TextRenderer tr, int x, int y, int alpha, int accent) {
        Settings s = Settings.get();
        Ledger ledger = Ledger.get();
        double[] t = ledger.totals(s.sessionOnly);
        double in = t[0];
        double out = t[1];
        double net = in - out;

        Draw.panel(ctx, x, y, WIDTH, height(SUMMARY), accent, alpha);
        Draw.rainbowText(ctx, tr, "PROFIT TRACKER", x + 8, y + 7, accent);

        Draw.text(ctx, tr, "IN", x + 8, y + 22, Draw.GRAY);
        Draw.textRight(ctx, tr, "+" + Money.compact(in), x + WIDTH - 8, y + 22, Draw.GREEN);
        Draw.text(ctx, tr, "OUT", x + 8, y + 33, Draw.GRAY);
        Draw.textRight(ctx, tr, "-" + Money.compact(out), x + WIDTH - 8, y + 33, Draw.RED);

        // The net figure gently pulses toward white
        double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 350.0);
        int netColor = Draw.mix(Money.signColor(net), Draw.WHITE, (float) (pulse * 0.25));
        Draw.text(ctx, tr, "NET", x + 8, y + 44, Draw.GRAY);
        Draw.textRight(ctx, tr, Money.signed(net), x + WIDTH - 8, y + 44, netColor);

        String status = net > 0.005 ? "PROFIT" : net < -0.005 ? "LOSS" : "EVEN";
        int sc = Money.signColor(net);
        int pillW = tr.getWidth(status) + 12;
        ctx.fill(x + 8, y + 58, x + 8 + pillW, y + 70, Draw.argb(70, sc));
        Draw.text(ctx, tr, status, x + 14, y + 60, sc);

        String right = s.sessionOnly ? Draw.hms(System.currentTimeMillis() - ledger.sessionStart()) : "ALL TIME";
        Draw.textRight(ctx, tr, right, x + WIDTH - 8, y + 60, Draw.DIM);
    }

    private static void drawFeed(DrawContext ctx, TextRenderer tr, int x, int y, int alpha, int accent) {
        Settings s = Settings.get();
        Draw.panel(ctx, x, y, WIDTH, height(FEED), accent, alpha);
        Draw.rainbowText(ctx, tr, "RECENT", x + 8, y + 7, accent);

        List<Transaction> list = Ledger.get().view(s.sessionOnly);
        if (list.isEmpty()) {
            Draw.text(ctx, tr, "No payments yet", x + 8, y + 22, Draw.GRAY);
            return;
        }

        int rows = Math.min(list.size(), s.feedRows);
        for (int i = 0; i < rows; i++) {
            Transaction tx = list.get(i);
            int ry = y + 22 + i * 11;
            Draw.text(ctx, tr, TIME.format(Instant.ofEpochMilli(tx.time())), x + 8, ry, Draw.DIM);
            Draw.text(ctx, tr, Draw.fit(tr, tx.player(), 74), x + 38, ry, Draw.WHITE);
            Draw.textRight(ctx, tr, (tx.incoming() ? "+" : "-") + Money.compact(tx.amount()),
                    x + WIDTH - 8, ry, tx.incoming() ? Draw.GREEN : Draw.RED);
        }
    }

    private static void drawGamblers(DrawContext ctx, TextRenderer tr, int x, int y, int alpha, int accent) {
        Settings s = Settings.get();
        Draw.panel(ctx, x, y, WIDTH, height(GAMBLERS), accent, alpha);
        Draw.rainbowText(ctx, tr, "GAMBLERS", x + 8, y + 7, accent);
        Draw.textRight(ctx, tr, "their net", x + WIDTH - 8, y + 7, Draw.DIM);

        List<Ledger.PlayerStat> list = new ArrayList<>(Ledger.get().playerStats(s.sessionOnly).values());
        list.sort(Comparator.comparingDouble(Ledger.PlayerStat::theirNet).reversed());
        if (list.isEmpty()) {
            Draw.text(ctx, tr, "No gamblers yet", x + 8, y + 22, Draw.GRAY);
            return;
        }

        int rows = Math.min(5, list.size());
        for (int i = 0; i < rows; i++) {
            Ledger.PlayerStat p = list.get(i);
            int ry = y + 22 + i * 11;
            Draw.text(ctx, tr, "#" + (i + 1), x + 8, ry, i == 0 ? Draw.GOLD : Draw.GRAY);
            Draw.text(ctx, tr, Draw.fit(tr, p.name, 96), x + 28, ry, Draw.WHITE);
            Draw.textRight(ctx, tr, Money.signed(p.theirNet()), x + WIDTH - 8, ry, Draw.gamblerColor(p.theirNet()));
        }
    }
}
