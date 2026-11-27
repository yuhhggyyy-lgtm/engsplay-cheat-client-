package dev.engsplay.module.impl;

import dev.engsplay.module.Category;
import dev.engsplay.module.Module;
import dev.engsplay.module.ModuleManager;
import dev.engsplay.module.Setting;

public final class HudModules {
    private HudModules() {}

    public static void register(ModuleManager manager) {
        manager.register(new HudModule("fps", "FPS", "Displays current client frame rate", true,
                new Setting.BoolSetting("background", "Glass Pill", true)));

        manager.register(new HudModule("ping", "Ping", "Displays server network latency in ms", true,
                new Setting.BoolSetting("background", "Glass Pill", true)));

        manager.register(new HudModule("cps", "CPS", "Tracks left and right clicks per second", true,
                new Setting.BoolSetting("show_rmb", "Show RMB", true)));

        manager.register(new HudModule("coordinates", "Coordinates", "Shows X, Y, Z and facing direction", true,
                new Setting.BoolSetting("direction", "Facing Letter", true)));

        manager.register(new HudModule("clock", "Clock", "Real-time 24h system clock pill", true));
        manager.register(new HudModule("keystrokes", "Keystrokes", "Live WASD and mouse click visualizer", true,
                new Setting.BoolSetting("mouse_buttons", "LMB / RMB", true)));

        manager.register(new HudModule("armor_hud", "Armor HUD", "Displays equipped armor durability", true));
        manager.register(new HudModule("potions", "Potion Effects", "Active status effects and timers", true));
        manager.register(new HudModule("reach_display", "Reach Display", "Shows last hit distance in blocks", true));
        manager.register(new HudModule("combo_counter", "Combo Counter", "Tracks consecutive hits without taking damage", true));
        manager.register(new HudModule("speedometer", "Speedometer", "Displays movement speed in m/s", true));
        manager.register(new HudModule("server_ip", "Server IP", "Shows current multiplayer host address", true));
        manager.register(new HudModule("pack_display", "Pack Display", "Shows active resource pack icon", true));
        manager.register(new HudModule("scoreboard", "Scoreboard", "Clean translucent sidebar scoreboard", true));
        manager.register(new HudModule("bossbar", "Bossbar", "Minimalist translucent boss health bar", true));
    }

    private static class HudModule extends Module {
        HudModule(String id, String name, String desc, boolean def, Setting<?>... settings) {
            super(id, name, desc, Category.HUD, def);
            for (Setting<?> s : settings) addSetting(s);
        }
    }
}
