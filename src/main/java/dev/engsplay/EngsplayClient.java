package dev.engsplay;

import dev.engsplay.config.ClientConfig;
import dev.engsplay.config.ConfigManager;
import dev.engsplay.module.ModuleManager;
import dev.engsplay.stats.SessionStatsTracker;
import dev.engsplay.ui.hud.EngsplayHudRenderer;
import dev.engsplay.ui.screen.EngsplayClickGuiScreen;
import dev.engsplay.ui.screen.HudEditorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.glfw.GLFW;

public class EngsplayClient implements ClientModInitializer {
    public static final String MOD_ID = "engsplay";
    public static final String VERSION = "1.21.11";
    public static final String TAGLINE = "a Minecraft 1.21.11 Fabric client";

    private static EngsplayClient instance;

    private final ModuleManager moduleManager = new ModuleManager();
    private final ConfigManager configManager = new ConfigManager();
    private final SessionStatsTracker statsTracker = new SessionStatsTracker();
    private final EngsplayHudRenderer hudRenderer = new EngsplayHudRenderer();

    private boolean prevGuiKeyDown = false;
    private boolean prevHudDragKeyDown = false;

    public static EngsplayClient getInstance() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        configManager.load(moduleManager);
        configManager.getConfig().totalSessions += 1;
        configManager.save(moduleManager);

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient mc) {
        statsTracker.tick(mc);
        moduleManager.onTick(mc);

        long handle = mc.getWindow().getHandle();
        int guiKey = configManager.getConfig().guiKeybind;
        boolean guiKeyDown = guiKey > 0 && GLFW.glfwGetKey(handle, guiKey) == GLFW.GLFW_PRESS;

        if (guiKeyDown && !prevGuiKeyDown && mc.currentScreen == null) {
            mc.setScreen(new EngsplayClickGuiScreen());
        }
        prevGuiKeyDown = guiKeyDown;

        int hudKey = configManager.getConfig().hudDragKeybind;
        boolean hudKeyDown = hudKey > 0 && GLFW.glfwGetKey(handle, hudKey) == GLFW.GLFW_PRESS;
        if (hudKeyDown && !prevHudDragKeyDown && mc.currentScreen == null && mc.player != null) {
            mc.setScreen(new HudEditorScreen(null));
        }
        prevHudDragKeyDown = hudKeyDown;
    }

    public void playUiClick(float pitch) {
        ClientConfig cfg = configManager.getConfig();
        if (!cfg.uiSounds) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, pitch, cfg.uiSoundVolume));
    }

    public void saveConfig() {
        configManager.save(moduleManager);
    }

    public ClientConfig getConfig() {
        return configManager.getConfig();
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public SessionStatsTracker getStatsTracker() {
        return statsTracker;
    }

    public EngsplayHudRenderer getHudRenderer() {
        return hudRenderer;
    }
}
