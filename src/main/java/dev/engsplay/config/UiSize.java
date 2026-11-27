package dev.engsplay.config;

public enum UiSize {
    SMALL("Small", 0.85f),
    NORMAL("Normal", 1.0f),
    LARGE("Large", 1.15f),
    EXTRA_LARGE("Extra large", 1.30f);

    private final String label;
    private final float scale;

    UiSize(String label, float scale) {
        this.label = label;
        this.scale = scale;
    }

    public String getLabel() { return label; }
    public float getScale() { return scale; }
}
