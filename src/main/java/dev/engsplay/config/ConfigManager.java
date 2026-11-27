package dev.engsplay.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.engsplay.module.Module;
import dev.engsplay.module.ModuleManager;
import dev.engsplay.module.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path configDir;
    private final Path clientFile;
    private final Path profileFile;
    private ClientConfig config;

    public ConfigManager() {
        this.configDir = FabricLoader.getInstance().getConfigDir().resolve("engsplay");
        this.clientFile = configDir.resolve("client.json");
        this.profileFile = configDir.resolve("Standaard.json");
        this.config = new ClientConfig();
    }

    public ClientConfig getConfig() {
        return config;
    }

    public void load(ModuleManager moduleManager) {
        try {
            Files.createDirectories(configDir);
            if (Files.exists(clientFile)) {
                String raw = Files.readString(clientFile, StandardCharsets.UTF_8);
                ClientConfig loaded = GSON.fromJson(raw, ClientConfig.class);
                if (loaded != null) {
                    if (loaded.accentColor == null) loaded.accentColor = AccentColor.PERIWINKLE;
                    if (loaded.uiSize == null) loaded.uiSize = UiSize.NORMAL;
                    if (loaded.hudPositions == null || loaded.hudPositions.isEmpty()) {
                        loaded.hudPositions = new ClientConfig().hudPositions;
                    }
                    if (loaded.weeklyMinutes == null || loaded.weeklyMinutes.length != 7) {
                        loaded.weeklyMinutes = new ClientConfig().weeklyMinutes;
                    }
                    this.config = loaded;
                }
            }

            if (Files.exists(profileFile)) {
                String raw = Files.readString(profileFile, StandardCharsets.UTF_8);
                JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
                for (Module m : moduleManager.getModules()) {
                    if (!root.has(m.getId())) continue;
                    JsonObject modObj = root.getAsJsonObject(m.getId());
                    if (modObj.has("enabled")) {
                        m.setEnabledSilent(modObj.get("enabled").getAsBoolean());
                    }
                    if (modObj.has("keybind")) {
                        m.setKeybind(modObj.get("keybind").getAsInt());
                    }
                    if (modObj.has("settings")) {
                        JsonObject settingsObj = modObj.getAsJsonObject("settings");
                        for (Setting<?> s : m.getSettings()) {
                            if (!settingsObj.has(s.getId())) continue;
                            JsonElement el = settingsObj.get(s.getId());
                            s.deserialize(el);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save(ModuleManager moduleManager) {
        try {
            Files.createDirectories(configDir);
            Files.writeString(clientFile, GSON.toJson(config), StandardCharsets.UTF_8);

            JsonObject root = new JsonObject();
            for (Module m : moduleManager.getModules()) {
                JsonObject modObj = new JsonObject();
                modObj.addProperty("enabled", m.isEnabled());
                modObj.addProperty("keybind", m.getKeybind());
                JsonObject settingsObj = new JsonObject();
                for (Setting<?> s : m.getSettings()) {
                    settingsObj.add(s.getId(), s.serialize());
                }
                modObj.add("settings", settingsObj);
                root.add(m.getId(), modObj);
            }
            Files.writeString(profileFile, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
