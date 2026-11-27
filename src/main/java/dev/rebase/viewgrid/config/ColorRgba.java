package dev.rebase.viewgrid.config;

public final class ColorRgba {
    private int r;
    private int g;
    private int b;
    private int a;

    public ColorRgba(int r, int g, int b, int a) {
        this.r = clampChannel(r);
        this.g = clampChannel(g);
        this.b = clampChannel(b);
        this.a = clampChannel(a);
    }

    public static int clampChannel(int val) {
        return Math.max(0, Math.min(255, val));
    }

    public ColorRgba copy() {
        return new ColorRgba(this.r, this.g, this.b, this.a);
    }

    public int getR() { return r; }
    public int getG() { return g; }
    public int getB() { return b; }
    public int getA() { return a; }

    public int toArgb() {
        return ((this.a & 0xFF) << 24)
             | ((this.r & 0xFF) << 16)
             | ((this.g & 0xFF) << 8)
             | (this.b & 0xFF);
    }

    public String toHexRgba() {
        return String.format("#%02X%02X%02X%02X", this.r, this.g, this.b, this.a);
    }

    public static ColorRgba parseHex(String text, ColorRgba fallback) {
        if (text == null) {
            return fallback.copy();
        }
        String raw = text.trim();
        if (raw.startsWith("#")) {
            raw = raw.substring(1);
        }
        try {
            if (raw.length() == 6) {
                long rgb = Long.parseLong(raw, 16);
                return new ColorRgba(
                    (int) ((rgb >> 16) & 0xFF),
                    (int) ((rgb >> 8) & 0xFF),
                    (int) (rgb & 0xFF),
                    fallback.a
                );
            }
            if (raw.length() == 8) {
                long rgba = Long.parseLong(raw, 16);
                return new ColorRgba(
                    (int) ((rgba >> 24) & 0xFF),
                    (int) ((rgba >> 16) & 0xFF),
                    (int) ((rgba >> 8) & 0xFF),
                    (int) (rgba & 0xFF)
                );
            }
        } catch (NumberFormatException ignored) {
            // Keep fallback on malformed hex input.
        }
        return fallback.copy();
    }
}
