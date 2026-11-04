package com.onxon.profittracker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The P-key menu: Overview, Transactions, Gamblers and Settings. Every panel can pop out to the HUD. */
public class ProfitMenuScreen extends Screen {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d HH:mm").withZone(ZoneId.systemDefault());

    private enum Tab {
        OVERVIEW("Overview"), TRANSACTIONS("Transactions"), GAMBLERS("Gamblers"), SETTINGS("Settings");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private enum Sort {
        PROFIT("Their profit"), SPENT("Most spent"), LOSS("Their loss");

        final String label;

        Sort(String label) {
            this.label = label;
        }
    }

    private record Btn(int x, int y, int w, int h, Runnable action) {}

    private final Screen parent;
    private final List<Btn> buttons = new ArrayList<>();
    private Tab tab = Tab.OVERVIEW;
    private Sort sort = Sort.PROFIT;
    private int scroll = 0;
    private boolean confirmClear = false;

    private int mouseX;
    private int mouseY;
    private int accent;
    private int contentX;
    private int contentY;
    private int contentW;
    private int contentH;

    public ProfitMenuScreen(Screen parent) {
        super(Text.literal("Donut Profit Tracker"));
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        // "Clean background" skips the vanilla dim, so the panel floats over the game
        if (!Settings.get().cleanBackground) super.renderBackground(ctx, mx, my, delta);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        mouseX = mx;
        mouseY = my;
        renderBackground(ctx, mx, my, delta);
        buttons.clear();
        accent = Settings.get().accentRgb;
        TextRenderer tr = textRenderer;

        int pw = Math.min(width - 24, 440);
        int ph = Math.min(height - 24, 300);
        int px = (width - pw) / 2;
        int py = (height - ph) / 2;
        Draw.panel(ctx, px, py, pw, ph, accent, 235);
        Draw.rainbowText(ctx, tr, "« ONXON · PROFIT TRACKER »", px + 10, py + 9, accent);

        int tx = px + 10;
        int ty = py + 26;
        for (Tab t : Tab.values()) {
            int tw = tr.getWidth(t.label) + 16;
            button(ctx, tx, ty, tw, 16, t.label, tab == t, () -> {
                tab = t;
                scroll = 0;
                confirmClear = false;
            });
            tx += tw + 4;
        }

        contentX = px + 10;
        contentY = py + 50;
        contentW = pw - 20;
        contentH = ph - 74;

        switch (tab) {
            case OVERVIEW -> drawOverview(ctx, tr);
            case TRANSACTIONS -> drawTransactions(ctx, tr);
            case GAMBLERS -> drawGamblers(ctx, tr);
            case SETTINGS -> drawSettings(ctx, tr);
        }

        Draw.text(ctx, tr, "Esc closes  ·  Pop out sends a panel to your HUD", px + 10, py + ph - 14, Draw.DIM);
    }

    // ---------- Overview ----------

    private void drawOverview(DrawContext ctx, TextRenderer tr) {
        Settings s = Settings.get();
        List<Transaction> list = Ledger.get().view(s.sessionOnly);
        double[] t = Ledger.get().totals(s.sessionOnly);
        double in = t[0];
        double out = t[1];
        double net = in - out;

        double biggestIn = 0;
        double biggestOut = 0;
        for (Transaction tx : list) {
            if (tx.incoming()) biggestIn = Math.max(biggestIn, tx.amount());
            else biggestOut = Math.max(biggestOut, tx.amount());
        }

        int x = contentX;
        int y = contentY;
        scopeButton(ctx, x + contentW - 100, y);

        int ly = y + 22;
        row(ctx, tr, "Incoming", "+" + Money.compact(in), Draw.GREEN, x, ly); ly += 14;
        row(ctx, tr, "Outgoing", "-" + Money.compact(out), Draw.RED, x, ly); ly += 14;
        row(ctx, tr, "Net", Money.signed(net), Money.signColor(net), x, ly); ly += 14;
        String status = net > 0.005 ? "PROFIT" : net < -0.005 ? "LOSS" : "EVEN";
        row(ctx, tr, "Status", status, Money.signColor(net), x, ly); ly += 14;
        row(ctx, tr, "Payments", String.valueOf(list.size()), Draw.WHITE, x, ly); ly += 14;
        row(ctx, tr, "Biggest in", Money.compact(biggestIn), Draw.GREEN, x, ly); ly += 14;
        row(ctx, tr, "Biggest out", Money.compact(biggestOut), Draw.RED, x, ly);

        int bx = x + contentW / 2 + 10;
        int bw = contentW / 2 - 10;
        int by = y + 22;
        Draw.text(ctx, tr, "POP OUT PANELS", bx, by, Draw.GRAY);
        popButton(ctx, bx, by + 14, bw, 18, HudWidgets.SUMMARY, "Summary");
        popButton(ctx, bx, by + 36, bw, 18, HudWidgets.FEED, "Feed");
        popButton(ctx, bx, by + 58, bw, 18, HudWidgets.GAMBLERS, "Gamblers");
        button(ctx, bx, by + 80, bw, 18, "Edit HUD layout", false,
                () -> MinecraftClient.getInstance().setScreen(new HudLayoutScreen(this)));
    }

    private void row(DrawContext ctx, TextRenderer tr, String label, String value, int color, int x, int y) {
        Draw.text(ctx, tr, label, x, y, Draw.GRAY);
        Draw.textRight(ctx, tr, value, x + contentW / 2 - 20, y, color);
    }

    // ---------- Transactions ----------

    private void drawTransactions(DrawContext ctx, TextRenderer tr) {
        int x = contentX;
        int y = contentY;
        Draw.text(ctx, tr, "Recent payments", x, y + 4, Draw.GRAY);
        scopeButton(ctx, x + contentW - 100, y);
        popButton(ctx, x + contentW - 206, y, 100, 16, HudWidgets.FEED, "Feed");

        List<Transaction> list = Ledger.get().view(Settings.get().sessionOnly);
        int top = y + 22;
        int rowH = 14;
        int visible = Math.max(1, (contentH - 40) / rowH);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, list.size() - visible)));

        if (list.isEmpty()) {
            Draw.text(ctx, tr, "No payments yet. Send or receive money on Donut SMP.", x, top, Draw.GRAY);
            return;
        }

        for (int i = 0; i < visible; i++) {
            int idx = i + scroll;
            if (idx >= list.size()) break;
            Transaction tx = list.get(idx);
            int ry = top + i * rowH;
            int c = tx.incoming() ? Draw.GREEN : Draw.RED;
            Draw.text(ctx, tr, DATE.format(java.time.Instant.ofEpochMilli(tx.time())), x, ry, Draw.DIM);
            Draw.text(ctx, tr, tx.incoming() ? "IN" : "OUT", x + 96, ry, c);
            Draw.text(ctx, tr, Draw.fit(tr, tx.player(), 140), x + 128, ry, Draw.WHITE);
            Draw.textRight(ctx, tr, (tx.incoming() ? "+" : "-") + Money.compact(tx.amount()), x + contentW, ry, c);
        }

        String hint = (scroll + 1) + "-" + Math.min(list.size(), scroll + visible) + " of " + list.size() + "  scroll for more";
        Draw.textRight(ctx, tr, hint, x + contentW, y + contentH - 8, Draw.DIM);
    }

    // ---------- Gamblers ----------

    private void drawGamblers(DrawContext ctx, TextRenderer tr) {
        int x = contentX;
        int y = contentY;

        int bx = x;
        for (Sort s : Sort.values()) {
            int w = tr.getWidth(s.label) + 14;
            button(ctx, bx, y, w, 16, s.label, sort == s, () -> {
                sort = s;
                scroll = 0;
            });
            bx += w + 4;
        }
        scopeButton(ctx, x + contentW - 100, y + 20);
        popButton(ctx, x, y + 20, 110, 16, HudWidgets.GAMBLERS, "Gamblers");

        List<Ledger.PlayerStat> list = new ArrayList<>(Ledger.get().playerStats(Settings.get().sessionOnly).values());
        Comparator<Ledger.PlayerStat> cmp = switch (sort) {
            case PROFIT -> Comparator.comparingDouble(Ledger.PlayerStat::theirNet).reversed();
            case SPENT -> Comparator.comparingDouble((Ledger.PlayerStat p) -> p.fromThem).reversed();
            case LOSS -> Comparator.comparingDouble(Ledger.PlayerStat::theirNet);
        };
        list.sort(cmp);

        int headY = y + 42;
        Draw.text(ctx, tr, "#", x, headY, Draw.DIM);
        Draw.text(ctx, tr, "Player", x + 18, headY, Draw.DIM);
        Draw.textRight(ctx, tr, "Paid you", x + contentW - 130, headY, Draw.DIM);
        Draw.textRight(ctx, tr, "You paid", x + contentW - 66, headY, Draw.DIM);
        Draw.textRight(ctx, tr, "Their net", x + contentW, headY, Draw.DIM);

        int top = headY + 12;
        int rowH = 13;
        int visible = Math.max(1, (contentH - 70) / rowH);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, list.size() - visible)));

        if (list.isEmpty()) {
            Draw.text(ctx, tr, "No gamblers yet. Payments show up here as they happen.", x, top, Draw.GRAY);
            return;
        }

        for (int i = 0; i < visible; i++) {
            int idx = i + scroll;
            if (idx >= list.size()) break;
            Ledger.PlayerStat p = list.get(idx);
            int ry = top + i * rowH;
            Draw.text(ctx, tr, "#" + (idx + 1), x, ry, idx == 0 ? Draw.GOLD : Draw.GRAY);
            Draw.text(ctx, tr, Draw.fit(tr, p.name, 130), x + 18, ry, Draw.WHITE);
            Draw.textRight(ctx, tr, Money.compact(p.fromThem), x + contentW - 130, ry, Draw.GREEN);
            Draw.textRight(ctx, tr, Money.compact(p.toThem), x + contentW - 66, ry, Draw.RED);
            Draw.textRight(ctx, tr, Money.signed(p.theirNet()), x + contentW, ry, Draw.gamblerColor(p.theirNet()));
        }

        String legend = "Gold = they won  ·  Green = you won";
        Draw.text(ctx, tr, legend, x, y + contentH - 8, Draw.DIM);
        String hint = (scroll + 1) + "-" + Math.min(list.size(), scroll + visible) + " of " + list.size();
        Draw.textRight(ctx, tr, hint, x + contentW, y + contentH - 8, Draw.DIM);
    }

    // ---------- Settings ----------

    private void drawSettings(DrawContext ctx, TextRenderer tr) {
        Settings s = Settings.get();
        int x = contentX;
        int y = contentY;
        int w = contentW;
        int r = 0;
        int rowH = 22;

        settingRow(ctx, tr, x, y + r++ * rowH, w, "Clean background (no dim)",
                s.cleanBackground ? "ON" : "OFF", () -> {
                    s.cleanBackground = !s.cleanBackground;
                    Settings.save();
                });

        settingRow(ctx, tr, x, y + r++ * rowH, w, "Session-only stats",
                s.sessionOnly ? "ON" : "OFF", () -> {
                    s.sessionOnly = !s.sessionOnly;
                    Settings.save();
                });

        int ry = y + r++ * rowH;
        Draw.text(ctx, tr, "Feed rows", x, ry + 4, Draw.WHITE);
        button(ctx, x + w - 150, ry, 20, 16, "-", false, () -> {
            s.feedRows = Math.max(3, s.feedRows - 1);
            Settings.save();
        });
        Draw.textRight(ctx, tr, String.valueOf(s.feedRows), x + w - 122, ry + 4, Draw.WHITE);
        button(ctx, x + w - 20, ry, 20, 16, "+", false, () -> {
            s.feedRows = Math.min(14, s.feedRows + 1);
            Settings.save();
        });

        settingRow(ctx, tr, x, y + r++ * rowH, w, "HUD opacity",
                Math.round(s.opacity * 100) + "%", () -> {
                    int idx = 0;
                    for (int i = 0; i < Settings.OPACITIES.length; i++) {
                        if (Math.abs(Settings.OPACITIES[i] - s.opacity) < 0.01f) idx = i;
                    }
                    s.opacity = Settings.OPACITIES[(idx + 1) % Settings.OPACITIES.length];
                    Settings.save();
                });

        settingRow(ctx, tr, x, y + r++ * rowH, w, "Accent colour",
                String.format("#%06X", s.accentRgb), () -> {
                    int idx = 0;
                    for (int i = 0; i < Settings.ACCENTS.length; i++) {
                        if (Settings.ACCENTS[i] == s.accentRgb) idx = i;
                    }
                    s.accentRgb = Settings.ACCENTS[(idx + 1) % Settings.ACCENTS.length];
                    Settings.save();
                });

        settingRow(ctx, tr, x, y + r++ * rowH, w, "Move the HUD panels", "Open",
                () -> MinecraftClient.getInstance().setScreen(new HudLayoutScreen(this)));

        settingRow(ctx, tr, x, y + r++ * rowH, w, "Reset session totals", "Reset",
                () -> {
                    Ledger.get().resetSession();
                    scroll = 0;
                });

        settingRow(ctx, tr, x, y + r++ * rowH, w, "Erase ALL payment history",
                confirmClear ? "Click again" : "Erase", () -> {
                    if (!confirmClear) {
                        confirmClear = true;
                    } else {
                        Ledger.get().clearAll();
                        confirmClear = false;
                    }
                });

        Draw.text(ctx, tr, "Chat patterns: config/donut-profit-tracker/config.json", x, y + r * rowH + 4, Draw.DIM);
    }

    private void settingRow(DrawContext ctx, TextRenderer tr, int x, int y, int w, String label, String buttonLabel, Runnable action) {
        Draw.text(ctx, tr, label, x, y + 4, Draw.WHITE);
        button(ctx, x + w - 120, y, 120, 16, buttonLabel, false, action);
    }

    // ---------- Shared pieces ----------

    private void scopeButton(DrawContext ctx, int x, int y) {
        Settings s = Settings.get();
        button(ctx, x, y, 100, 16, s.sessionOnly ? "Scope: session" : "Scope: all time", false, () -> {
            s.sessionOnly = !s.sessionOnly;
            scroll = 0;
            Settings.save();
        });
    }

    private void popButton(DrawContext ctx, int x, int y, int w, int h, String id, String name) {
        boolean on = Settings.get().widget(id).visible;
        button(ctx, x, y, w, h, (on ? "Close " : "Pop out ") + name, on, () -> toggleWidget(id));
    }

    private void toggleWidget(String id) {
        Settings.WidgetPos p = Settings.get().widget(id);
        p.visible = !p.visible;
        Settings.save();
    }

    private void button(DrawContext ctx, int x, int y, int w, int h, String label, boolean active, Runnable action) {
        boolean hover = inside(mouseX, mouseY, x, y, w, h);
        Draw.button(ctx, textRenderer, x, y, w, h, label, accent, active, hover);
        buttons.add(new Btn(x, y, w, h, action));
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        for (Btn b : buttons) {
            if (inside(click.x(), click.y(), b.x(), b.y(), b.w(), b.h())) {
                b.action().run();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (tab == Tab.TRANSACTIONS || tab == Tab.GAMBLERS) {
            scroll = Math.max(0, scroll - (int) Math.signum(vertical));
            return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }

    @Override
    public void close() {
        Settings.save();
        MinecraftClient.getInstance().setScreen(parent);
    }
}
