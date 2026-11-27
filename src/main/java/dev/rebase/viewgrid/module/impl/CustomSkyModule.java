package dev.rebase.viewgrid.module.impl;

import dev.rebase.viewgrid.config.ColorRgba;
import dev.rebase.viewgrid.module.Category;
import dev.rebase.viewgrid.module.Module;
import dev.rebase.viewgrid.module.Setting;

public final class CustomSkyModule extends Module {
    public enum Mode {
        BOREALIS("Borealis / Aurora", new ColorRgba(24, 230, 175, 255), new ColorRgba(12, 24, 48, 255)),
        NEBULA("Nebula / Deep Space", new ColorRgba(158, 72, 255, 255), new ColorRgba(10, 8, 26, 255)),
        CRIMSON("Crimson / Eclipse", new ColorRgba(235, 42, 68, 255), new ColorRgba(28, 8, 14, 255)),
        FROST("Frost / Polar", new ColorRgba(130, 224, 255, 255), new ColorRgba(24, 52, 78, 255));

        private final String label;
        private final ColorRgba primary;
        private final ColorRgba horizon;

        Mode(String label, ColorRgba primary, ColorRgba horizon) {
            this.label = label;
            this.primary = primary;
            this.horizon = horizon;
        }

        public String getLabel() { return label; }
        public ColorRgba getPrimary() { return primary; }
        public ColorRgba getHorizon() { return horizon; }
    }

    private final Setting<Mode> mode = addSetting(
        Setting.dropdown("mode", "Sky Theme", Mode.BOREALIS, Mode.class)
    );
    private final Setting<ColorRgba> accent = addSetting(
        Setting.colorPicker("accent", "Primary Color", Mode.BOREALIS.getPrimary())
    );
    private final Setting<ColorRgba> fogTint = addSetting(
        Setting.colorPicker("fog_tint", "Horizon Color", Mode.BOREALIS.getHorizon())
    );
    private final Setting<Double> strength = addSetting(
        Setting.sliderDouble("strength", "Intensity", 0.75, 0.05, 1.0)
    );
    private final Setting<Boolean> yieldToShaders = addSetting(
        Setting.toggle("yield_to_shaders", "Yield When Shaders Active", true)
    );

    public CustomSkyModule() {
        super(
            "custom_sky",
            "Custom Sky",
            "Borealis, Nebula, Crimson, and Frost sky modes with color controls and clean restore.",
            Category.VISUALS
        );
    }

    public boolean canApplySky(boolean shaderPackEnabled) {
        if (!isEnabled()) return false;
        return !(Boolean.TRUE.equals(this.yieldToShaders.get()) && shaderPackEnabled);
    }

    public Mode getMode() { return mode.get(); }
    public ColorRgba getAccent() { return accent.get().copy(); }
    public ColorRgba getFogTint() { return fogTint.get().copy(); }
    public double getStrength() { return strength.get(); }
}
