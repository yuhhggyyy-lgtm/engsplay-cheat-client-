package dev.rebase.viewgrid.module;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
    private final String id;
    private final String name;
    private final String summary;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();

    private boolean enabled = false;
    private int keybind = GLFW.GLFW_KEY_UNKNOWN;
    private boolean keyLatchDown = false;

    protected Module(String id, String name, String summary, Category category) {
        this.id = id;
        this.name = name;
        this.summary = summary;
        this.category = category;
    }

    protected <T> Setting<T> registerSetting(Setting<T> setting) {
        this.settings.add(setting);
        return setting;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getSummary() { return summary; }
    public Category getCategory() { return category; }
    public List<Setting<?>> getSettings() { return Collections.unmodifiableList(settings); }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean nextState) {
        if (this.enabled == nextState) {
            return;
        }
        this.enabled = nextState;
        if (this.enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public void toggle() {
        setEnabled(!this.enabled);
    }

    public int getKeybind() { return keybind; }
    public void setKeybind(int keybind) { this.keybind = keybind; }

    public boolean isKeyLatchDown() { return keyLatchDown; }
    public void setKeyLatchDown(boolean keyLatchDown) { this.keyLatchDown = keyLatchDown; }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick(Minecraft client) {}
}
