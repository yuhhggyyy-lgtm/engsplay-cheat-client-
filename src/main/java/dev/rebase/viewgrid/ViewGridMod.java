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
    private KeyMapping openGuiKeybind;
    private boolean prevRightShiftState = false;
    private ModuleManager moduleManager;
    private ConfigManager configManager;

    public static ViewGridMod get() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        this.moduleManager = new ModuleManager();
        this.configManager = new ConfigManager(this.moduleManager);
        this.configManager.load();

        // UNVERIFIED: KeyMapping.Category.MISC identifier in 1.21.11 official Mojang mappings.
        this.openGuiKeybind = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.viewgrid.toggle_gui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::tickClient);
        LOGGER.info("ViewGrid initialized for 1.21.11");
    }

    private void tickClient(Minecraft mc) {
        if (mc == null) {
            return;
        }
        long win = mc.getWindow().getWindow();
        boolean rShiftHeld = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean bindTriggered = this.openGuiKeybind.consumeClick();

        if ((rShiftHeld && !this.prevRightShiftState) || bindTriggered) {
            if (mc.screen instanceof ClickGuiScreen) {
                mc.setScreen(null);
            } else if (mc.screen == null && mc.level != null) {
                mc.setScreen(new ClickGuiScreen(this.moduleManager, this.configManager));
            }
        }
        this.prevRightShiftState = rShiftHeld;

        if (mc.screen == null && mc.level != null) {
            for (Module mod : this.moduleManager.getAll()) {
                int key = mod.getKeybind();
                if (key != GLFW.GLFW_KEY_UNKNOWN && GLFW.glfwGetKey(win, key) == GLFW.GLFW_PRESS) {
                    if (!mod.consumePressLatch()) {
                        mod.toggle();
                        this.configManager.save();
                    }
                } else {
                    mod.resetPressLatch();
                }
            }
        }

        if (mc.level != null && mc.player != null) {
            for (Module mod : this.moduleManager.getAll()) {
                if (!mod.isEnabled()) {
                    continue;
                }
                try {
                    mod.onTick(mc);
                } catch (RuntimeException ex) {
                    LOGGER.error("Disabling module {} after tick exception", mod.getId(), ex);
                    mod.setEnabled(false);
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
