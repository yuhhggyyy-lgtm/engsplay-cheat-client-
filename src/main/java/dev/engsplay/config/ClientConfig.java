package dev.engsplay.config;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class ClientConfig {
    public String activeProfile = "Standaard";
    public AccentColor accentColor = AccentColor.PERIWINKLE;
    public UiSize uiSize = UiSize.NORMAL;
    public boolean backgroundBlur = true;
    public boolean uiSounds = true;
    public float uiSoundVolume = 0.70f;
    public String language = "EN";
    public int guiKeybind = GLFW.GLFW_KEY_RIGHT_SHIFT;
    public int hudDragKeybind = GLFW.GLFW_KEY_T;

    public Map<String, float[]> hudPositions = new HashMap<>();

    public long totalPlaytimeSeconds = 2 * 3600 + 34 * 60;
    public int totalSessions = 128;
    public int streakDays = 1;
    public int[] weeklyMinutes = new int[]{25, 42, 18, 55, 30, 48, 32};

    public ClientConfig() {
        hudPositions.put("fps", new float[]{0.012f, 0.015f});
        hudPositions.put("ping", new float[]{0.075f, 0.015f});
        hudPositions.put("cps", new float[]{0.135f, 0.015f});
        hudPositions.put("coords", new float[]{0.215f, 0.015f});
        hudPositions.put("clock", new float[]{0.345f, 0.015f});
        hudPositions.put("keystrokes", new float[]{0.012f, 0.060f});
    }
}
