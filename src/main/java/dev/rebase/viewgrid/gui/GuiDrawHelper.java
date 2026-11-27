package dev.rebase.viewgrid.gui;

import dev.rebase.viewgrid.config.ColorRgba;
import net.minecraft.client.gui.GuiGraphics;

public final class GuiDrawHelper {
    private GuiDrawHelper() {}

    public static void drawBox(GuiGraphics g, int x, int y, int w, int h, int fill, int outline) {
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w, y + 1, outline);
        g.fill(x, y + h - 1, x + w, y + h, outline);
        g.fill(x, y + 1, x + 1, y + h - 1, outline);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, outline);
    }

    public static void drawCapsule(GuiGraphics g, int x, int y, int w, int h, int fill, int outline) {
        int step = Math.min(2, h / 2);
        g.fill(x + step, y, x + w - step, y + h, fill);
        g.fill(x, y + step, x + step, y + h - step, fill);
        g.fill(x + w - step, y + step, x + w, y + h - step, fill);
        g.fill(x + step, y, x + w - step, y + 1, outline);
        g.fill(x + step, y + h - 1, x + w - step, y + h, outline);
    }

    public static void drawSwatch(GuiGraphics g, int x, int y, int size, ColorRgba c) {
        drawBox(g, x, y, size, size, c.toArgb(), 0xCCFFFFFF);
    }

    public static boolean contains(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
