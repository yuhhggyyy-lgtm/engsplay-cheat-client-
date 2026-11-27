package dev.engsplay.ui.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class GlassRenderer {
    public static final int PANEL_BG = 0xD8141214;
    public static final int SIDEBAR_BG = 0xE2100E10;
    public static final int CARD_BG = 0xB81C191C;
    public static final int CARD_HOVER_BG = 0xCC242024;
    public static final int PILL_BG = 0xC8181518;
    public static final int BORDER_SUBTLE = 0x26FFFFFF;
    public static final int BORDER_STRONG = 0x3DFFFFFF;
    public static final int TEXT_PRIMARY = 0xFFF5F5F7;
    public static final int TEXT_SECONDARY = 0xFFA1A1AA;
    public static final int TEXT_MUTED = 0xFF71717A;

    private GlassRenderer() {}

    public static void fillRoundedRect(DrawContext ctx, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(w / 2, h / 2)));
        if (r == 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        ctx.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int dx = (int) Math.round(r - Math.sqrt(Math.max(0.0, r * r - dy * dy)));
            ctx.fill(x + dx, y + i, x + w - dx, y + i + 1, color);
            ctx.fill(x + dx, y + h - 1 - i, x + w - dx, y + h - i, color);
        }
    }

    public static void drawRoundedOutline(DrawContext ctx, int x, int y, int w, int h, int radius, int borderColor) {
        if (w <= 2 || h <= 2) return;
        int r = Math.max(0, Math.min(radius, Math.min(w / 2, h / 2)));
        ctx.fill(x + r, y, x + w - r, y + 1, borderColor);
        ctx.fill(x + r, y + h - 1, x + w - r, y + h, borderColor);
        ctx.fill(x, y + r, x + 1, y + h - r, borderColor);
        ctx.fill(x + w - 1, y + r, x + w, y + h - r, borderColor);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int dx = (int) Math.round(r - Math.sqrt(Math.max(0.0, r * r - dy * dy)));
            ctx.fill(x + dx, y + i, x + dx + 1, y + i + 1, borderColor);
            ctx.fill(x + w - dx - 1, y + i, x + w - dx, y + i + 1, borderColor);
            ctx.fill(x + dx, y + h - 1 - i, x + dx + 1, y + h - 1 - i + 1, borderColor);
            ctx.fill(x + w - dx - 1, y + h - 1 - i, x + w - dx, y + h - 1 - i + 1, borderColor);
        }
    }

    public static void drawGlassCard(DrawContext ctx, int x, int y, int w, int h, int radius, int bgColor, int borderColor) {
        fillRoundedRect(ctx, x, y, w, h, radius, bgColor);
        drawRoundedOutline(ctx, x, y, w, h, radius, borderColor);
    }

    public static void drawPillSwitch(DrawContext ctx, int x, int y, float animProgress, int accentArgb) {
        int w = 32;
        int h = 16;
        int trackColor = lerpColor(0xFF2E2A2E, accentArgb, animProgress);
        fillRoundedRect(ctx, x, y, w, h, 8, trackColor);
        drawRoundedOutline(ctx, x, y, w, h, 8, 0x30FFFFFF);

        int knobSize = 12;
        int knobX = Math.round(x + 2 + animProgress * (w - knobSize - 4));
        int knobY = y + 2;
        fillRoundedRect(ctx, knobX, knobY, knobSize, knobSize, 6, 0xFFFFFFFF);
    }

    public static void drawPlayerHead(DrawContext ctx, Identifier skinTexture, int x, int y, int size) {
        ctx.drawTexture(skinTexture, x, y, size, size, 8f, 8f, 8, 8, 64, 64);
        ctx.drawTexture(skinTexture, x - 1, y - 1, size + 2, size + 2, 40f, 8f, 8, 8, 64, 64);
    }

    public static void drawEmblem(DrawContext ctx, TextRenderer tr, int x, int y, int size, int accentArgb) {
        fillRoundedRect(ctx, x, y, size, size, size / 3, accentArgb);
        drawRoundedOutline(ctx, x, y, size, size, size / 3, 0x55FFFFFF);
        String letter = "E";
        int lw = tr.getWidth(letter);
        ctx.drawTextWithShadow(tr, letter, x + (size - lw) / 2, y + (size - tr.fontHeight) / 2 + 1, 0xFFFFFFFF);
    }

    public static int lerpColor(int c1, int c2, float t) {
        float cl = Math.max(0f, Math.min(1f, t));
        int a1 = (c1 >>> 24) & 0xFF, r1 = (c1 >>> 16) & 0xFF, g1 = (c1 >>> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF, r2 = (c2 >>> 16) & 0xFF, g2 = (c2 >>> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = Math.round(a1 + (a2 - a1) * cl);
        int r = Math.round(r1 + (r2 - r1) * cl);
        int g = Math.round(g1 + (g2 - g1) * cl);
        int b = Math.round(b1 + (b2 - b1) * cl);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static boolean isHovered(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public static Identifier getLocalPlayerSkin() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            return mc.player.getSkinTextures().texture();
        }
        return Identifier.ofVanilla("textures/entity/player/wide/steve.png");
    }
}
