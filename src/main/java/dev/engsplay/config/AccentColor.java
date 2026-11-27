package dev.engsplay.config;

public enum AccentColor {
    PERIWINKLE("Periwinkle", 0xFF7B8CDE, 0xFF9AA8F8),
    SKY_BLUE("Sky Blue", 0xFF38BDF8, 0xFF7DD3FC),
    CYAN_TEAL("Cyan", 0xFF2DD4BF, 0xFF5EEAD4),
    EMERALD("Emerald", 0xFF34D399, 0xFF6EE7B7),
    WARM_GOLD("Gold", 0xFFFBBF24, 0xFFFDE68A),
    AMBER_ORANGE("Amber", 0xFFFB923C, 0xFFFDBA74),
    CORAL_RED("Coral", 0xFFF87171, 0xFFFCA5A5),
    ORCHID_PINK("Orchid", 0xFFE879F9, 0xFFF0ABFC);

    private final String label;
    private final int argb;
    private final int lightArgb;

    AccentColor(String label, int argb, int lightArgb) {
        this.label = label;
        this.argb = argb;
        this.lightArgb = lightArgb;
    }

    public String getLabel() { return label; }
    public int getArgb() { return argb; }
    public int getLightArgb() { return lightArgb; }

    public int withAlpha(int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    public AccentColor next() {
        AccentColor[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
