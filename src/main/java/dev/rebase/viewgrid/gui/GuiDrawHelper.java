package dev.rebase.viewgrid.gui;

import dev.rebase.viewgrid.config.ColorRgba;
import net.minecraft.client.gui.GuiGraphics;

public final class GuiDrawHelper {
    private GuiDrawHelper() {}

    public static void drawPanel(GuiGraphics graphics, int x, int y, int w, int h, int fillArgb, int borderArgb) {
        graphics.fill(x, y, x + w, y + h, fillArgb);
        // Top, bottom, left, right 1px hairline border
        graphics.fill(x, y, x + w, y + 1, borderArgb);
        graphics.fill(x, y + h - 1, x + w, y + h, borderArgb);
        graphics.fill(x, y + 1, x + 1, y + h - 1, borderArgb);
        graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, borderArgb);
    }

    public static void drawPill(GuiGraphics graphics, int x, int y, int w, int h, int fillArgb, int borderArgb) {
        int cap = Math.min(2, h / 2);
        graphics.fill(x + cap, y, x + w - cap, y + h, fillArgb);
        graphics.fill(x, y + cap, x + cap, y + h - cap, fillArgb);
        graphics.fill(x + w - cap, y + cap, x + w, y + h - cap, fillArgb);
        graphics.fill(x + cap, y, x + w - cap, y + 1, borderArgb);
        graphics.fill(x + cap, y + h - 1, x + w - cap, y + h, borderArgb);
    }

    public static void drawSwatch(GuiGraphics graphics, int x, int y, int size, ColorRgba color) {
        int argb = color.toArgb();
        drawPanel(graphics, x, y, size, size, argb, 0xCCFFFFFF);
    }

    public static boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
