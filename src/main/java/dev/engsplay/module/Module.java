package dev.engsplay.module;

import dev.engsplay.EngsplayClient;
import dev.engsplay.ui.anim.SpringValue;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class Module {
    private final String id;
    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;
    private int keybind = GLFW.GLFW_KEY_UNKNOWN;
    private boolean expanded = false;

    private final List<Setting<?>> settings = new ArrayList<>();
    private final SpringValue toggleAnim = new SpringValue(0f, 260f, 22f);
    private final SpringValue expandAnim = new SpringValue(0f, 240f, 24f);

    public Module(String id, String name, String description, Category category, boolean defaultEnabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = defaultEnabled;
        this.toggleAnim.snap(defaultEnabled ? 1f : 0f);
    }

    protected <T extends Setting<?>> T addSetting(T setting) {
        settings.add(setting);
        return setting;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        this.toggleAnim.setTarget(enabled ? 1f : 0f);
        if (enabled) onEnable();
        else onDisable();
        if (EngsplayClient.getInstance() != null) {
            EngsplayClient.getInstance().playUiClick(enabled ? 1.15f : 0.90f);
            EngsplayClient.getInstance().saveConfig();
        }
    }

    public void setEnabledSilent(boolean enabled) {
        this.enabled = enabled;
        this.toggleAnim.snap(enabled ? 1f : 0f);
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick(MinecraftClient client) {}

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public int getKeybind() { return keybind; }
    public void setKeybind(int keybind) { this.keybind = keybind; }

    public boolean isExpanded() { return expanded; }
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        this.expandAnim.setTarget(expanded ? 1f : 0f);
    }

    public List<Setting<?>> getSettings() { return settings; }
    public SpringValue getToggleAnim() { return toggleAnim; }
    public SpringValue getExpandAnim() { return expandAnim; }
}
