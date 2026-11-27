package dev.rebase.viewgrid.module;

import dev.rebase.viewgrid.config.ColorRgba;

public final class Setting<T> {
    public enum Kind {
        TOGGLE,
        SLIDER_DOUBLE,
        SLIDER_INT,
        DROPDOWN,
        COLOR_PICKER,
        BLOCK_FILTER
    }

    private final String id;
    private final String label;
    private final Kind kind;
    private T current;
    private final double minD;
    private final double maxD;
    private final int minI;
    private final int maxI;
    private final Enum<?>[] choices;

    private Setting(
        String id,
        String label,
        Kind kind,
        T initial,
        double minD,
        double maxD,
        int minI,
        int maxI,
        Enum<?>[] choices
    ) {
        this.id = id;
        this.label = label;
        this.kind = kind;
        this.current = initial instanceof ColorRgba c ? (T) c.copy() : initial;
        this.minD = minD;
        this.maxD = maxD;
        this.minI = minI;
        this.maxI = maxI;
        this.choices = choices;
    }

    public static Setting<Boolean> toggle(String id, String label, boolean def) {
        return new Setting<>(id, label, Kind.TOGGLE, def, 0, 1, 0, 1, null);
    }

    public static Setting<Double> sliderDouble(String id, String label, double def, double min, double max) {
        return new Setting<>(id, label, Kind.SLIDER_DOUBLE, def, min, max, 0, 0, null);
    }

    public static Setting<Integer> sliderInt(String id, String label, int def, int min, int max) {
        return new Setting<>(id, label, Kind.SLIDER_INT, def, 0, 0, min, max, null);
    }

    public static <E extends Enum<E>> Setting<E> dropdown(String id, String label, E def, Class<E> enumType) {
        return new Setting<>(id, label, Kind.DROPDOWN, def, 0, 0, 0, 0, enumType.getEnumConstants());
    }

    public static Setting<ColorRgba> colorPicker(String id, String label, ColorRgba def) {
        return new Setting<>(id, label, Kind.COLOR_PICKER, def, 0, 0, 0, 0, null);
    }

    public static Setting<String> blockFilter(String id, String label, String defCsv) {
        return new Setting<>(id, label, Kind.BLOCK_FILTER, defCsv, 0, 0, 0, 0, null);
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public Kind getKind() { return kind; }
    public T get() { return current; }
    public double getMinD() { return minD; }
    public double getMaxD() { return maxD; }
    public int getMinI() { return minI; }
    public int getMaxI() { return maxI; }
    public Enum<?>[] getChoices() { return choices; }

    @SuppressWarnings("unchecked")
    public void set(T value) {
        if (value == null) return;
        switch (this.kind) {
            case SLIDER_DOUBLE -> {
                double d = ((Number) value).doubleValue();
                if (Double.isFinite(d)) {
                    this.current = (T) Double.valueOf(Math.max(this.minD, Math.min(this.maxD, d)));
                }
            }
            case SLIDER_INT -> {
                int i = ((Number) value).intValue();
                this.current = (T) Integer.valueOf(Math.max(this.minI, Math.min(this.maxI, i)));
            }
            case COLOR_PICKER -> {
                if (value instanceof ColorRgba c) {
                    this.current = (T) c.copy();
                }
            }
            default -> this.current = value;
        }
    }
}
