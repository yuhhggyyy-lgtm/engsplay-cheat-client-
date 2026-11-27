package dev.engsplay.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Arrays;
import java.util.List;

public abstract class Setting<T> {
    private final String id;
    private final String name;
    protected T value;

    protected Setting(String id, String name, T defaultValue) {
        this.id = id;
        this.name = name;
        this.value = defaultValue;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public T get() { return value; }
    public void set(T value) { this.value = value; }

    public abstract JsonElement serialize();
    public abstract void deserialize(JsonElement element);

    public static class BoolSetting extends Setting<Boolean> {
        public BoolSetting(String id, String name, boolean def) {
            super(id, name, def);
        }

        @Override
        public JsonElement serialize() {
            return new JsonPrimitive(value);
        }

        @Override
        public void deserialize(JsonElement element) {
            if (element != null && element.isJsonPrimitive()) {
                this.value = element.getAsBoolean();
            }
        }
    }

    public static class SliderSetting extends Setting<Float> {
        private final float min;
        private final float max;
        private final float step;
        private final String suffix;

        public SliderSetting(String id, String name, float def, float min, float max, float step, String suffix) {
            super(id, name, def);
            this.min = min;
            this.max = max;
            this.step = step;
            this.suffix = suffix;
        }

        public float getMin() { return min; }
        public float getMax() { return max; }
        public float getStep() { return step; }
        public String getSuffix() { return suffix; }

        public void setNormalized(float norm) {
            float clamped = Math.max(0f, Math.min(1f, norm));
            float raw = min + (max - min) * clamped;
            float snapped = Math.round(raw / step) * step;
            this.value = Math.max(min, Math.min(max, snapped));
        }

        public float getNormalized() {
            return (value - min) / (max - min);
        }

        public String formatValue() {
            if (step >= 1.0f) {
                return Math.round(value) + suffix;
            }
            return String.format("%.1f%s", value, suffix);
        }

        @Override
        public JsonElement serialize() {
            return new JsonPrimitive(value);
        }

        @Override
        public void deserialize(JsonElement element) {
            if (element != null && element.isJsonPrimitive()) {
                this.value = Math.max(min, Math.min(max, element.getAsFloat()));
            }
        }
    }

    public static class ModeSetting extends Setting<String> {
        private final List<String> modes;

        public ModeSetting(String id, String name, String def, String... modes) {
            super(id, name, def);
            this.modes = Arrays.asList(modes);
        }

        public List<String> getModes() { return modes; }

        public void cycle(int dir) {
            int idx = modes.indexOf(value);
            if (idx < 0) idx = 0;
            int next = (idx + dir + modes.size()) % modes.size();
            this.value = modes.get(next);
        }

        @Override
        public JsonElement serialize() {
            return new JsonPrimitive(value);
        }

        @Override
        public void deserialize(JsonElement element) {
            if (element != null && element.isJsonPrimitive()) {
                String v = element.getAsString();
                if (modes.contains(v)) {
                    this.value = v;
                }
            }
        }
    }

    public static class ColorSetting extends Setting<Integer> {
        private final int[] presets;

        public ColorSetting(String id, String name, int def, int... presets) {
            super(id, name, def);
            this.presets = presets;
        }

        public int[] getPresets() { return presets; }

        @Override
        public JsonElement serialize() {
            return new JsonPrimitive(value);
        }

        @Override
        public void deserialize(JsonElement element) {
            if (element != null && element.isJsonPrimitive()) {
                this.value = element.getAsInt();
            }
        }
    }
}
