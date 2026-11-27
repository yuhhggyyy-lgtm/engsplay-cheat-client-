package dev.engsplay.module.impl;

import dev.engsplay.module.Category;
import dev.engsplay.module.Module;
import dev.engsplay.module.ModuleManager;
import dev.engsplay.module.Setting;
import org.lwjgl.glfw.GLFW;

public final class CoreModules {
    private CoreModules() {}

    public static void register(ModuleManager manager) {
        manager.register(new SimpleModule("clean_movement", "Clean Movement", "Responsive input smoothing and sprint state", Category.MOVEMENT, true));
        manager.register(new SimpleModule("toggle_sprint", "Toggle Sprint", "Automatically maintain sprint while moving", Category.MOVEMENT, true));
        manager.register(new SimpleModule("freelook", "Freelook", "Orbit camera 360 degrees without turning", Category.MOVEMENT, false));

        manager.register(new SimpleModule("hit_color", "Hit Color", "Customize damage tint overlay on entities", Category.COMBAT, false,
                new Setting.ColorSetting("tint", "Damage Tint", 0xFFF87171, 0xFFF87171, 0xFF7B8CDE, 0xFF38BDF8)));
        manager.register(new SimpleModule("block_hit", "1.7 Visuals", "Classic block-hitting and item swing animations", Category.COMBAT, false));
        manager.register(new SimpleModule("target_hud", "Target HUD", "Compact glass card for current combat target", Category.COMBAT, false));

        Module zoom = new SimpleModule("zoom", "Zoom", "Smooth cinematic camera zoom", Category.PLAYER, false,
                new Setting.SliderSetting("fov", "Zoom FOV", 24f, 10f, 50f, 1f, "°"),
                new Setting.BoolSetting("smooth_camera", "Smooth Camera", true));
        zoom.setKeybind(GLFW.GLFW_KEY_C);
        manager.register(zoom);

        manager.register(new SimpleModule("auto_gg", "Auto GG", "Automatically send GG at match end", Category.PLAYER, false));
        manager.register(new SimpleModule("no_hurt_cam", "No Hurt Cam", "Remove screen shake when taking damage", Category.PLAYER, false));

        manager.register(new SimpleModule("time_changer", "Time Changer", "Override client-side world time of day", Category.WORLD, false,
                new Setting.SliderSetting("time", "World Time", 6000f, 0f, 24000f, 500f, "")));
        manager.register(new SimpleModule("fullbright", "Fullbright", "Maximum gamma illumination in dark areas", Category.WORLD, false));

        manager.register(new SimpleModule("cape", "Engsplay Cape", "Render custom client cape on your player", Category.SKIN, false));
        manager.register(new SimpleModule("skin_layers", "3D Skin Layers", "Enhanced depth on outer jacket and hat layers", Category.SKIN, false));
    }

    private static class SimpleModule extends Module {
        SimpleModule(String id, String name, String desc, Category cat, boolean def, Setting<?>... settings) {
            super(id, name, desc, cat, def);
            for (Setting<?> s : settings) addSetting(s);
        }
    }
}
