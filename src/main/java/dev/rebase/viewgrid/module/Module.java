package dev.rebase.viewgrid.module;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
    private final String id;
    private final String title;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();

    private boolean enabled = false;
    private int keybind = GLFW.GLFW_KEY_UNKNOWN;
    private boolean pressLatch = false;

    protected Module(String id, String title, String description, Category category) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
    }

    protected <T> Setting<T> addSetting(Setting<T> setting) {
        this.settings.add(setting);
        return setting;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public List<Setting<?>> getSettings() { return Collections.unmodifiableList(this.settings); }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean next) {
        if (this.enabled == next) return;
        this.enabled = next;
        if (this.enabled) {
            onActivate();
        } else {
            onDeactivate();
        }
    }

    public void toggle() {
        setEnabled(!this.enabled);
    }

    public int getKeybind() { return keybind; }
    public void setKeybind(int keybind) { this.keybind = keybind; }

    public boolean consumePressLatch() {
        if (this.pressLatch) return true;
        this.pressLatch = true;
        return false;
    }

    public void resetPressLatch() {
        this.pressLatch = false;
    }

    protected void onActivate() {}
    protected void onDeactivate() {}
    public void onTick(Minecraft mc) {}
}
