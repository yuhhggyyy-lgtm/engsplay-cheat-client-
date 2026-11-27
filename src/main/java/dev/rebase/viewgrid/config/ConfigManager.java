package dev.rebase.viewgrid.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.rebase.viewgrid.ViewGridMod;
import dev.rebase.viewgrid.module.Module;
import dev.rebase.viewgrid.module.ModuleManager;
import dev.rebase.viewgrid.module.Setting;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ConfigManager {
    private static final int SCHEMA_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final ModuleManager moduleManager;
    private final Path configPath;

    private boolean audioFeedback = true;
    private boolean compactCards = false;
    private String lastCategory = "VISUALS";

    public ConfigManager(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
        this.configPath = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("viewgrid")
            .resolve("config.json");
    }

    public void load() {
        if (!Files.exists(this.configPath)) {
            save();
            return;
        }
        try {
            String raw = Files.readString(this.configPath, StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(raw);
            if (!parsed.isJsonObject()) {
                ViewGridMod.LOGGER.warn("Config root not a JSON object; writing defaults");
                save();
                return;
            }
            JsonObject root = parsed.getAsJsonObject();
            int version = root.has("schemaVersion") && root.get("schemaVersion").isJsonPrimitive()
                ? root.get("schemaVersion").getAsInt()
                : SCHEMA_VERSION;
            if (version != SCHEMA_VERSION) {
                ViewGridMod.LOGGER.warn("Config schemaVersion {} differs from expected {}; clamping fields", version, SCHEMA_VERSION);
            }

            if (root.has("audioFeedback") && root.get("audioFeedback").isJsonPrimitive()) {
                this.audioFeedback = root.get("audioFeedback").getAsBoolean();
            }
            if (root.has("compactCards") && root.get("compactCards").isJsonPrimitive()) {
                this.compactCards = root.get("compactCards").getAsBoolean();
            }
            if (root.has("lastCategory") && root.get("lastCategory").isJsonPrimitive()) {
                this.lastCategory = root.get("lastCategory").getAsString();
            }

            JsonObject modulesObj = root.has("modules") && root.get("modules").isJsonObject()
                ? root.getAsJsonObject("modules")
                : new JsonObject();

            for (Module module : this.moduleManager.getModules()) {
                if (!modulesObj.has(module.getId()) || !modulesObj.get(module.getId()).isJsonObject()) {
                    continue;
                }
                JsonObject mObj = modulesObj.getAsJsonObject(module.getId());
                if (mObj.has("enabled") && mObj.get("enabled").isJsonPrimitive()) {
                    module.setEnabled(mObj.get("enabled").getAsBoolean());
                }
                if (mObj.has("keybind") && mObj.get("keybind").isJsonPrimitive()) {
                    int kc = mObj.get("keybind").getAsInt();
                    if (kc >= GLFW.GLFW_KEY_UNKNOWN && kc <= GLFW.GLFW_KEY_LAST) {
                        module.setKeybind(kc);
                    }
                }
                if (mObj.has("settings") && mObj.get("settings").isJsonObject()) {
                    JsonObject sObj = mObj.getAsJsonObject("settings");
                    for (Setting<?> setting : module.getSettings()) {
                        if (!sObj.has(setting.getKey())) {
                            continue;
                        }
                        applySettingJson(setting, sObj.get(setting.getKey()));
                    }
                }
            }
        } catch (IOException | RuntimeException ex) {
            ViewGridMod.LOGGER.error("Failed reading config {}, keeping safe defaults", this.configPath, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private void applySettingJson(Setting<?> setting, JsonElement el) {
        switch (setting.getType()) {
            case BOOLEAN -> {
                if (el.isJsonPrimitive()) {
                    ((Setting<Boolean>) setting).setValue(el.getAsBoolean());
                }
            }
            case DOUBLE -> {
                if (el.isJsonPrimitive()) {
                    double v = el.getAsDouble();
                    if (Double.isFinite(v)) {
                        ((Setting<Double>) setting).setValue(v);
                    }
                }
            }
            case INT -> {
                if (el.isJsonPrimitive()) {
                    ((Setting<Integer>) setting).setValue(el.getAsInt());
                }
            }
            case ENUM -> {
                if (el.isJsonPrimitive()) {
                    Setting<Enum<?>> enumSetting = (Setting<Enum<?>>) setting;
                    String targetName = el.getAsString();
                    for (Enum<?> candidate : enumSetting.getEnumConstants()) {
                        if (candidate.name().equalsIgnoreCase(targetName)) {
                            enumSetting.setValue(candidate);
                            break;
                        }
                    }
                }
            }
            case COLOR -> {
                if (el.isJsonObject()) {
                    JsonObject co = el.getAsJsonObject();
                    Setting<ColorRgba> colSetting = (Setting<ColorRgba>) setting;
                    ColorRgba cur = colSetting.getValue();
                    int r = co.has("r") ? co.get("r").getAsInt() : cur.getR();
                    int g = co.has("g") ? co.get("g").getAsInt() : cur.getG();
                    int b = co.has("b") ? co.get("b").getAsInt() : cur.getB();
                    int a = co.has("a") ? co.get("a").getAsInt() : cur.getA();
                    colSetting.setValue(new ColorRgba(r, g, b, a));
                }
            }
            case BLOCK_LIST -> {
                if (el.isJsonPrimitive()) {
                    String csv = el.getAsString();
                    List<String> cleaned = Arrays.stream(csv.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .limit(64)
                        .toList();
                    ((Setting<String>) setting).setValue(String.join(", ", cleaned));
                }
            }
        }
    }

    public void save() {
        try {
            Files.createDirectories(this.configPath.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("schemaVersion", SCHEMA_VERSION);
            root.addProperty("audioFeedback", this.audioFeedback);
            root.addProperty("compactCards", this.compactCards);
            root.addProperty("lastCategory", this.lastCategory);

            JsonObject modulesObj = new JsonObject();
            for (Module module : this.moduleManager.getModules()) {
                JsonObject mObj = new JsonObject();
                mObj.addProperty("enabled", module.isEnabled());
                mObj.addProperty("keybind", module.getKeybind());

                JsonObject sObj = new JsonObject();
                for (Setting<?> setting : module.getSettings()) {
                    switch (setting.getType()) {
                        case BOOLEAN -> sObj.addProperty(setting.getKey(), (Boolean) setting.getValue());
                        case DOUBLE -> sObj.addProperty(setting.getKey(), (Double) setting.getValue());
                        case INT -> sObj.addProperty(setting.getKey(), (Integer) setting.getValue());
                        case ENUM -> sObj.addProperty(setting.getKey(), ((Enum<?>) setting.getValue()).name());
                        case BLOCK_LIST -> sObj.addProperty(setting.getKey(), (String) setting.getValue());
                        case COLOR -> {
                            ColorRgba c = (ColorRgba) setting.getValue();
                            JsonObject co = new JsonObject();
                            co.addProperty("r", c.getR());
                            co.addProperty("g", c.getG());
                            co.addProperty("b", c.getB());
                            co.addProperty("a", c.getA());
                            sObj.add(setting.getKey(), co);
                        }
                    }
                }
                mObj.add("settings", sObj);
                modulesObj.add(module.getId(), mObj);
            }
            root.add("modules", modulesObj);

            Path tmp = this.configPath.resolveSibling("config.json.tmp");
            Files.writeString(
                tmp,
                GSON.toJson(root),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
            Files.move(tmp, this.configPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            ViewGridMod.LOGGER.error("Failed writing config {}", this.configPath, ex);
        }
    }

    public boolean isAudioFeedback() { return audioFeedback; }
    public void setAudioFeedback(boolean audioFeedback) { this.audioFeedback = audioFeedback; }

    public boolean isCompactCards() { return compactCards; }
    public void setCompactCards(boolean compactCards) { this.compactCards = compactCards; }

    public String getLastCategory() { return lastCategory; }
    public void setLastCategory(String lastCategory) { this.lastCategory = lastCategory; }

    public static Set<String> parseBlockFilterSet(String csv) {
        Set<String> out = new HashSet<>();
        if (csv == null || csv.isBlank()) {
            return out;
        }
        for (String part : csv.split(",")) {
            String normalized = part.trim().toLowerCase();
            if (!normalized.isEmpty()) {
                if (!normalized.contains(":")) {
                    normalized = "minecraft:" + normalized;
                }
                out.add(normalized);
            }
        }
        return out;
    }
}
