package dev.engsplay.module.impl;

import dev.engsplay.module.Category;
import dev.engsplay.module.Module;
import dev.engsplay.module.ModuleManager;
import dev.engsplay.module.Setting;

public final class VisualsModules {
    private VisualsModules() {}

    public static void register(ModuleManager manager) {
        manager.register(new VisualModule("drift", "Drift", "Smooth camera inertia and sway motion", false,
                new Setting.SliderSetting("strength", "Strength", 45f, 0f, 100f, 5f, "%")));

        manager.register(new VisualModule("glow", "Glow", "Highlight entities and world outlines", false,
                new Setting.ColorSetting("color", "Color", 0xFF7B8CDE,
                        0xFF7B8CDE, 0xFF38BDF8, 0xFF2DD4BF, 0xFF34D399, 0xFFFBBF24, 0xFFF87171)));

        manager.register(new VisualModule("hit_effects", "Hit Effects", "Custom particle bursts on melee hits", false,
                new Setting.ModeSetting("style", "Style", "Sparks", "Sparks", "Hearts", "Criticals")));

        manager.register(new VisualModule("kill_effect", "Kill Effect", "Play visual FX when eliminating targets", false,
                new Setting.ModeSetting("effect", "Effect", "Lightning", "Lightning", "Soul", "Explosion")));

        manager.register(new VisualModule("jump_circles", "Jump Circles", "Render expanding rings beneath your feet", false,
                new Setting.SliderSetting("radius", "Radius", 1.2f, 0.5f, 2.5f, 0.1f, "m"),
                new Setting.ColorSetting("color", "Color", 0xFF7B8CDE,
                        0xFF7B8CDE, 0xFF38BDF8, 0xFF34D399, 0xFFE879F9)));

        manager.register(new VisualModule("bloom", "Bloom", "Soft HDR glow on bright emissive surfaces", false,
                new Setting.SliderSetting("intensity", "Intensity", 60f, 10f, 100f, 5f, "%")));

        manager.register(new VisualModule("sky", "Sky", "Customize skybox tint and horizon gradient", false,
                new Setting.ColorSetting("tint", "Sky Tint", 0xFF38BDF8,
                        0xFF38BDF8, 0xFF7B8CDE, 0xFFFB923C, 0xFFF87171)));

        manager.register(new VisualModule("clouds", "Clouds", "Volumetric and stylized cloud layer", true,
                new Setting.ModeSetting("style", "Style", "Cumulus", "Cumulus", "Stratus", "Cirrus", "Vanilla"),
                new Setting.SliderSetting("coverage", "Coverage", 90f, 0f, 100f, 5f, "%"),
                new Setting.SliderSetting("height", "Height", 192f, 64f, 320f, 4f, ""),
                new Setting.SliderSetting("wind", "Wind", 100f, 0f, 200f, 10f, "%"),
                new Setting.ColorSetting("color", "Color", 0xFFFFFFFF,
                        0xFFFFFFFF, 0xFF7B8CDE, 0xFF38BDF8, 0xFFFBBF24, 0xFFF87171, 0xFFE879F9)));

        manager.register(new VisualModule("rain", "Rain", "Override weather precipitation density", false,
                new Setting.SliderSetting("density", "Density", 50f, 0f, 100f, 5f, "%")));

        manager.register(new VisualModule("sun_rays", "Sun Rays", "Volumetric crepuscular light shafts", false,
                new Setting.SliderSetting("opacity", "Opacity", 75f, 10f, 100f, 5f, "%")));

        manager.register(new VisualModule("blades", "Blades", "Animated grass and foliage wind sway", false,
                new Setting.SliderSetting("sway", "Sway", 65f, 0f, 100f, 5f, "%")));

        manager.register(new VisualModule("mist", "Mist", "Atmospheric distance fog blending", false,
                new Setting.SliderSetting("distance", "Distance", 80f, 10f, 100f, 5f, "%")));
    }

    private static class VisualModule extends Module {
        VisualModule(String id, String name, String desc, boolean def, Setting<?>... settings) {
            super(id, name, desc, Category.VISUALS, def);
            for (Setting<?> s : settings) addSetting(s);
        }
    }
}
