package dev.rebase.viewgrid.config;

public final class ColorRgba {
    private int r;
    private int g;
    private int b;
    private int a;

    public ColorRgba(int r, int g, int b, int a) {
        this.r = clampByte(r);
        this.g = clampByte(g);
        this.b = clampByte(b);
        this.a = clampByte(a);
    }

    public ColorRgba copy() {
        return new ColorRgba(this.r, this.g, this.b, this.a);
    }

    public static int clampByte(int value) {
        return Math.max(0, Math.min(255, value));
    }

    public int getR() { return r; }
    public int getG() { return g; }
    public int getB() { return b; }
    public int getA() { return a; }

    public void setR(int r) { this.r = clampByte(r); }
    public void setG(int g) { this.g = clampByte(g); }
    public void setB(int b) { this.b = clampByte(b); }
    public void setA(int a) { this.a = clampByte(a); }

    public int toArgb() {
        return ((this.a & 0xFF) << 24)
             | ((this.r & 0xFF) << 16)
             | ((this.g & 0xFF) << 8)
             | (this.b & 0xFF);
    }

    public float[] toFloatRgba() {
        return new float[] {
            this.r / 255.0f,
            this.g / 255.0f,
            this.b / 255.0f,
            this.a / 255.0f
        };
    }

    public String toHex8() {
        return String.format("#%02X%02X%02X%02X", this.r, this.g, this.b, this.a);
    }

    public static ColorRgba fromHexOrFallback(String input, ColorRgba fallback) {
        if (input == null) {
            return fallback.copy();
        }
        String clean = input.trim();
        if (clean.startsWith("#")) {
            clean = clean.substring(1);
        }
        try {
            if (clean.length() == 6) {
                long rgb = Long.parseLong(clean, 16);
                return new ColorRgba(
                    (int) ((rgb >> 16) & 0xFF),
                    (int) ((rgb >> 8) & 0xFF),
                    (int) (rgb & 0xFF),
                    fallback.a
                );
            } else if (clean.length() == 8) {
                long rgba = Long.parseLong(clean, 16);
                return new ColorRgba(
                    (int) ((rgba >> 24) & 0xFF),
                    (int) ((rgba >> 16) & 0xFF),
                    (int) ((rgba >> 8) & 0xFF),
                    (int) (rgba & 0xFF)
                );
            }
        } catch (NumberFormatException ignored) {
            // Fall back safely on invalid hex text.
        }
        return fallback.copy();
    }
}
