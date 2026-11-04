package com.onxon.profittracker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Drag the pop-out panels anywhere on screen. Positions save as you let go. */
public class HudLayoutScreen extends Screen {
    private static final String[] ORDER = {HudWidgets.GAMBLERS, HudWidgets.FEED, HudWidgets.SUMMARY};
    private static final String HINT = "DRAG TO MOVE  ·  ESC TO SAVE";

    private final Screen parent;
    private String dragging;
    private int grabX;
    private int grabY;

    public HudLayoutScreen(Screen parent) {
        super(Text.literal("HUD layout"));
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        if (!Settings.get().cleanBackground) super.renderBackground(ctx, mx, my, delta);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);
        TextRenderer tr = textRenderer;
        Settings s = Settings.get();
        int alpha = Math.round(255 * s.opacity);

        Draw.rainbowText(ctx, tr, HINT, (width - tr.getWidth(HINT)) / 2, 10, s.accentRgb);

        boolean anyVisible = false;
        for (String id : ORDER) {
            Settings.WidgetPos p = s.widget(id);
            if (!p.visible) continue;
            anyVisible = true;
            int w = HudWidgets.width(id);
            int h = HudWidgets.height(id);
            HudWidgets.draw(ctx, tr, id, p.x, p.y, alpha);
            boolean hot = id.equals(dragging) || inside(mx, my, p.x, p.y, w, h);
            Draw.frame(ctx, p.x - 1, p.y - 1, w + 2, h + 2, Draw.argb(hot ? 255 : 90, hot ? Draw.PINK : s.accentRgb));
        }

        if (!anyVisible) {
            String msg = "No panels are popped out. Turn them on from the P menu first.";
            Draw.text(ctx, tr, msg, (width - tr.getWidth(msg)) / 2, height / 2, Draw.GRAY);
        }

        int dx = doneX();
        int dy = doneY();
        Draw.button(ctx, tr, dx, dy, 80, 18, "Done", s.accentRgb, true, inside(mx, my, dx, dy, 80, 18));
    }

    private int doneX() {
        return width / 2 - 40;
    }

    private int doneY() {
        return height - 30;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double x = click.x();
        double y = click.y();
        if (inside(x, y, doneX(), doneY(), 80, 18)) {
            close();
            return true;
        }
        for (String id : ORDER) {
            Settings.WidgetPos p = Settings.get().widget(id);
            if (p.visible && inside(x, y, p.x, p.y, HudWidgets.width(id), HudWidgets.height(id))) {
                dragging = id;
                grabX = (int) x - p.x;
                grabY = (int) y - p.y;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging == null) return super.mouseDragged(click, offsetX, offsetY);
        Settings.WidgetPos p = Settings.get().widget(dragging);
        int maxX = Math.max(0, width - HudWidgets.width(dragging));
        int maxY = Math.max(0, height - HudWidgets.height(dragging));
        p.x = Math.max(0, Math.min(maxX, (int) click.x() - grabX));
        p.y = Math.max(0, Math.min(maxY, (int) click.y() - grabY));
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null) {
            dragging = null;
            Settings.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void close() {
        Settings.save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
