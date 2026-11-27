package dev.rebase.viewgrid.module;

import dev.rebase.viewgrid.config.ColorRgba;

public final class Setting<T> {
    public enum Type {
        BOOLEAN,
        DOUBLE,
        INT,
        ENUM,
        COLOR,
        BLOCK_LIST
    }

    private final String key;
    private final String label;
    private final Type type;
    private T value;
    private final T defaultValue;
    private final double minDouble;
    private final double maxDouble;
    private final int minInt;
    private final int maxInt;
    private final Enum<?>[] enumConstants;

    private Setting(
        String key,
        String label,
        Type type,
        T defaultValue,
        double minDouble,
        double maxDouble,
        int minInt,
        int maxInt,
        Enum<?>[] enumConstants
    ) {
        this.key = key;
        this.label = label;
        this.type = type;
        this.defaultValue = defaultValue;
        this.value = copyValue(defaultValue);
        this.minDouble = minDouble;
        this.maxDouble = maxDouble;
        this.minInt = minInt;
        this.maxInt = maxInt;
        this.enumConstants = enumConstants;
    }

    @SuppressWarnings("unchecked")
    private T copyValue(T v) {
        if (v instanceof ColorRgba c) {
            return (T) c.copy();
        }
        return v;
    }

    public static Setting<Boolean> bool(String key, String label, boolean def) {
        return new Setting<>(key, label, Type.BOOLEAN, def, 0, 1, 0, 1, null);
    }

    public static Setting<Double> rangedDouble(String key, String label, double def, double min, double max) {
        return new Setting<>(key, label, Type.DOUBLE, def, min, max, 0, 0, null);
    }

    public static Setting<Integer> rangedInt(String key, String label, int def, int min, int max) {
        return new Setting<>(key, label, Type.INT, def, 0, 0, min, max, null);
    }

    public static <E extends Enum<E>> Setting<E> choice(String key, String label, E def, Class<E> enumClass) {
        return new Setting<>(key, label, Type.ENUM, def, 0, 0, 0, 0, enumClass.getEnumConstants());
    }

    public static Setting<ColorRgba> color(String key, String label, ColorRgba def) {
        return new Setting<>(key, label, Type.COLOR, def, 0, 0, 0, 0, null);
    }

    public static Setting<String> blockList(String key, String label, String defCsv) {
        return new Setting<>(key, label, Type.BLOCK_LIST, defCsv, 0, 0, 0, 0, null);
    }

    public String getKey() { return key; }
    public String getLabel() { return label; }
    public Type getType() { return type; }
    public T getValue() { return value; }
    public T getDefaultValue() { return defaultValue; }
    public double getMinDouble() { return minDouble; }
    public double getMaxDouble() { return maxDouble; }
    public int getMinInt() { return minInt; }
    public int getMaxInt() { return maxInt; }
    public Enum<?>[] getEnumConstants() { return enumConstants; }

    public void setValue(T newValue) {
        if (newValue == null) {
            return;
        }
        switch (this.type) {
            case DOUBLE -> {
                double d = ((Number) newValue).doubleValue();
                if (!Double.isFinite(d)) return;
                d = Math.max(this.minDouble, Math.min(this.maxDouble, d));
                this.value = (T) Double.valueOf(d);
            }
            case INT -> {
                int i = ((Number) newValue).intValue();
                i = Math.max(this.minInt, Math.min(this.maxInt, i));
                this.value = (T) Integer.valueOf(i);
            }
            case COLOR -> {
                if (newValue instanceof ColorRgba c) {
                    this.value = (T) c.copy();
                }
            }
            default -> this.value = newValue;
        }
    }
}
