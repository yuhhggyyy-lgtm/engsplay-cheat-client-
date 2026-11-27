package dev.rebase.viewgrid.module.impl;

import dev.rebase.viewgrid.config.ColorRgba;
import dev.rebase.viewgrid.module.Category;
import dev.rebase.viewgrid.module.Module;
import dev.rebase.viewgrid.module.Setting;

public final class CustomSkyModule extends Module {
    public enum SkyMode {
        BOREALIS("Borealis / Aurora", new ColorRgba(24, 230, 175, 255), new ColorRgba(12, 24, 48, 255)),
        NEBULA("Nebula / Deep Space", new ColorRgba(158, 72, 255, 255), new ColorRgba(10, 8, 26, 255)),
        CRIMSON("Crimson / Eclipse", new ColorRgba(235, 42, 68, 255), new ColorRgba(28, 8, 14, 255)),
        FROST("Frost / Polar", new ColorRgba(130, 224, 255, 255), new ColorRgba(24, 52, 78, 255));

        private final String displayName;
        private final ColorRgba defaultPrimary;
        private final ColorRgba defaultHorizon;

        SkyMode(String displayName, ColorRgba defaultPrimary, ColorRgba defaultHorizon) {
            this.displayName = displayName;
            this.defaultPrimary = defaultPrimary;
            this.defaultHorizon = defaultHorizon;
        }

        public String getDisplayName() { return displayName; }
        public ColorRgba getDefaultPrimary() { return defaultPrimary; }
        public ColorRgba getDefaultHorizon() { return defaultHorizon; }
    }

    private final Setting<SkyMode> skyMode = registerSetting(
        Setting.choice("sky_mode", "Sky Mode", SkyMode.BOREALIS, SkyMode.class)
    );
    private final Setting<ColorRgba> primaryColor = registerSetting(
        Setting.color("primary_color", "Primary Accent", SkyMode.BOREALIS.getDefaultPrimary())
    );
    private final Setting<ColorRgba> horizonColor = registerSetting(
        Setting.color("horizon_color", "Horizon / Fog Tint", SkyMode.BOREALIS.getDefaultHorizon())
    );
    private final Setting<Double> intensity = registerSetting(
        Setting.rangedDouble("intensity", "Effect Intensity", 0.75, 0.05, 1.0)
    );
    private final Setting<Boolean> respectShaders = registerSetting(
        Setting.bool("respect_shaders", "Yield When Shaders Active", true)
    );

    public CustomSkyModule() {
        super(
            "custom_sky",
            "Custom Sky",
            "Four sky themes (Borealis, Nebula, Crimson, Frost) with color controls and clean restore.",
            Category.VISUALS
        );
    }

    public boolean shouldRenderCustomSky(boolean shaderPackActive) {
        if (!isEnabled()) {
            return false;
        }
        return !(Boolean.TRUE.equals(this.respectShaders.getValue()) && shaderPackActive);
    }

    public SkyMode getSkyMode() { return skyMode.getValue(); }
    public ColorRgba getPrimaryColor() { return primaryColor.getValue().copy(); }
    public ColorRgba getHorizonColor() { return horizonColor.getValue().copy(); }
    public double getIntensity() { return intensity.getValue(); }
}
