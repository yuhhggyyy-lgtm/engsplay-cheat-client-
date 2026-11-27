package dev.rebase.viewgrid;

import com.mojang.blaze3d.platform.InputConstants;
import dev.rebase.viewgrid.config.ConfigManager;
import dev.rebase.viewgrid.gui.ClickGuiScreen;
import dev.rebase.viewgrid.module.Module;
import dev.rebase.viewgrid.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ViewGridMod implements ClientModInitializer {
    public static final String MOD_ID = "viewgrid";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ViewGridMod instance;
    private KeyMapping openGuiKey;
    private boolean rightShiftWasDown = false;
    private ModuleManager moduleManager;
    private ConfigManager configManager;

    public static ViewGridMod getInstance() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        this.moduleManager = new ModuleManager();
        this.configManager = new ConfigManager(this.moduleManager);
        this.configManager.load();

        // KeyMapping registered for rebinding support in controls menu, plus direct Right Shift handling.
        // UNVERIFIED: KeyMapping.Category constructor signature in 1.21.11 official Mojang mappings.
        this.openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.viewgrid.open_gui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        LOGGER.info("ViewGrid initialized for Minecraft 1.21.11");
    }

    private void onClientTick(Minecraft client) {
        if (client == null) {
            return;
        }

        long windowHandle = client.getWindow().getWindow();
        boolean rightShiftDown = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean keyMappingPressed = this.openGuiKey.consumeClick();

        if ((rightShiftDown && !this.rightShiftWasDown) || keyMappingPressed) {
            if (client.screen instanceof ClickGuiScreen) {
                client.setScreen(null);
            } else if (client.screen == null && client.level != null) {
                client.setScreen(new ClickGuiScreen(this.moduleManager, this.configManager));
            }
        }
        this.rightShiftWasDown = rightShiftDown;

        // Handle per-module keybinds when no screen is blocking gameplay.
        if (client.screen == null && client.level != null) {
            for (Module module : this.moduleManager.getModules()) {
                int bind = module.getKeybind();
                if (bind != GLFW.GLFW_KEY_UNKNOWN && GLFW.glfwGetKey(windowHandle, bind) == GLFW.GLFW_PRESS) {
                    if (!module.isKeyLatchDown()) {
                        module.setKeyLatchDown(true);
                        module.toggle();
                        this.configManager.save();
                    }
                } else {
                    module.setKeyLatchDown(false);
                }
            }
        }

        if (client.level != null && client.player != null) {
            for (Module module : this.moduleManager.getModules()) {
                if (module.isEnabled()) {
                    try {
                        module.onTick(client);
                    } catch (RuntimeException ex) {
                        LOGGER.error("Module {} failed during tick; disabling safely", module.getId(), ex);
                        module.setEnabled(false);
                    }
                }
            }
        }
    }

    public ModuleManager getModuleManager() {
        return this.moduleManager;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }
}
