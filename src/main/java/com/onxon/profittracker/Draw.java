package com.onxon.profittracker;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Small drawing helpers shared by the HUD and the menus. */
public final class Draw {
    public static final int GREEN = 0x4ADE80;
    public static final int RED = 0xF87171;
    public static final int GOLD = 0xFBBF24;
    public static final int GRAY = 0x9CA3AF;
    public static final int DIM = 0x6B7280;
    public static final int WHITE = 0xF5F5F5;
    public static final int PINK = 0xFF6BD6;
    public static final int INK = 0x0B0B12;

    private Draw() {}

    public static int opaque(int rgb) {
        return 0xFF000000 | rgb;
    }

    public static int argb(int alpha, int rgb) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** Gold = they won (their profit), green = you won, grey = even. */
    public static int gamblerColor(double theirNet) {
        if (theirNet > 0.005) return GOLD;
        if (theirNet < -0.005) return GREEN;
        return GRAY;
    }

    public static void panel(DrawContext ctx, int x, int y, int w, int h, int accent, int alpha) {
        ctx.fill(x, y, x + w, y + h, argb(alpha, INK));
        frame(ctx, x, y, w, h, argb(alpha / 2, accent));
        for (int i = 0; i < w; i++) {
            ctx.fill(x + i, y, x + i + 1, y + 2, opaque(mix(accent, PINK, i / (float) w)));
        }
    }

    public static void frame(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void button(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h,
                              String label, int accent, boolean active, boolean hover) {
        int bg = active ? argb(200, accent) : hover ? argb(160, 0x2A2A3C) : argb(150, 0x15151F);
        ctx.fill(x, y, x + w, y + h, bg);
        frame(ctx, x, y, w, h, argb(active ? 255 : 110, accent));
        int tw = tr.getWidth(label);
        text(ctx, tr, label, x + (w - tw) / 2, y + (h - 8) / 2 + 1, active || hover ? WHITE : GRAY);
    }

    /** Letters cycle between the accent colour and pink, so the title shimmers. */
    public static int rainbowText(DrawContext ctx, TextRenderer tr, String s, int x, int y, int accent) {
        long t = System.currentTimeMillis();
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            float phase = (float) (0.5 + 0.5 * Math.sin(t / 400.0 + i * 0.5));
            ctx.drawTextWithShadow(tr, Text.literal(ch), cx, y, opaque(mix(accent, PINK, phase)));
            cx += tr.getWidth(ch);
        }
        return cx;
    }

    public static void text(DrawContext ctx, TextRenderer tr, String s, int x, int y, int rgb) {
        ctx.drawTextWithShadow(tr, Text.literal(s), x, y, opaque(rgb));
    }

    public static void textRight(DrawContext ctx, TextRenderer tr, String s, int rightX, int y, int rgb) {
        text(ctx, tr, s, rightX - tr.getWidth(s), y, rgb);
    }

    public static String fit(TextRenderer tr, String s, int maxWidth) {
        return tr.trimToWidth(s, maxWidth);
    }

    public static String hms(long ms) {
        long s = Math.max(0, ms / 1000);
        return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }
}
