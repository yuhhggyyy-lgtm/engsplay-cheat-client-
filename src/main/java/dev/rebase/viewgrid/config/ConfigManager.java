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
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ConfigManager {
    private static final int SCHEMA_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final ModuleManager moduleManager;
    private final Path filePath;

    private boolean soundFeedback = true;
    private boolean compactLayout = false;
    private String selectedCategory = "VISUALS";

    public ConfigManager(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
        this.filePath = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("viewgrid")
            .resolve("settings.json");
    }

    public void load() {
        if (!Files.exists(this.filePath)) {
            save();
            return;
        }
        try {
            String content = Files.readString(this.filePath, StandardCharsets.UTF_8);
            JsonElement rootEl = JsonParser.parseString(content);
            if (!rootEl.isJsonObject()) {
                save();
                return;
            }
            JsonObject root = rootEl.getAsJsonObject();
            if (root.has("soundFeedback") && root.get("soundFeedback").isJsonPrimitive()) {
                this.soundFeedback = root.get("soundFeedback").getAsBoolean();
            }
            if (root.has("compactLayout") && root.get("compactLayout").isJsonPrimitive()) {
                this.compactLayout = root.get("compactLayout").getAsBoolean();
            }
            if (root.has("selectedCategory") && root.get("selectedCategory").isJsonPrimitive()) {
                this.selectedCategory = root.get("selectedCategory").getAsString();
            }

            JsonObject modsJson = root.has("modules") && root.get("modules").isJsonObject()
                ? root.getAsJsonObject("modules")
                : new JsonObject();

            for (Module mod : this.moduleManager.getAll()) {
                if (!modsJson.has(mod.getId()) || !modsJson.get(mod.getId()).isJsonObject()) {
                    continue;
                }
                JsonObject mJson = modsJson.getAsJsonObject(mod.getId());
                if (mJson.has("enabled") && mJson.get("enabled").isJsonPrimitive()) {
                    mod.setEnabled(mJson.get("enabled").getAsBoolean());
                }
                if (mJson.has("keybind") && mJson.get("keybind").isJsonPrimitive()) {
                    int kc = mJson.get("keybind").getAsInt();
                    if (kc >= GLFW.GLFW_KEY_UNKNOWN && kc <= GLFW.GLFW_KEY_LAST) {
                        mod.setKeybind(kc);
                    }
                }
                if (mJson.has("options") && mJson.get("options").isJsonObject()) {
                    JsonObject opts = mJson.getAsJsonObject("options");
                    for (Setting<?> s : mod.getSettings()) {
                        if (opts.has(s.getId())) {
                            readSetting(s, opts.get(s.getId()));
                        }
                    }
                }
            }
        } catch (IOException | RuntimeException ex) {
            ViewGridMod.LOGGER.error("Failed loading settings from {}, using defaults", this.filePath, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private void readSetting(Setting<?> setting, JsonElement el) {
        switch (setting.getKind()) {
            case TOGGLE -> {
                if (el.isJsonPrimitive()) ((Setting<Boolean>) setting).set(el.getAsBoolean());
            }
            case SLIDER_DOUBLE -> {
                if (el.isJsonPrimitive() && Double.isFinite(el.getAsDouble())) {
                    ((Setting<Double>) setting).set(el.getAsDouble());
                }
            }
            case SLIDER_INT -> {
                if (el.isJsonPrimitive()) ((Setting<Integer>) setting).set(el.getAsInt());
            }
            case DROPDOWN -> {
                if (el.isJsonPrimitive()) {
                    Setting<Enum<?>> es = (Setting<Enum<?>>) setting;
                    String target = el.getAsString();
                    for (Enum<?> c : es.getChoices()) {
                        if (c.name().equalsIgnoreCase(target)) {
                            es.set(c);
                            break;
                        }
                    }
                }
            }
            case COLOR_PICKER -> {
                if (el.isJsonObject()) {
                    JsonObject co = el.getAsJsonObject();
                    Setting<ColorRgba> cs = (Setting<ColorRgba>) setting;
                    ColorRgba cur = cs.get();
                    cs.set(new ColorRgba(
                        co.has("r") ? co.get("r").getAsInt() : cur.getR(),
                        co.has("g") ? co.get("g").getAsInt() : cur.getG(),
                        co.has("b") ? co.get("b").getAsInt() : cur.getB(),
                        co.has("a") ? co.get("a").getAsInt() : cur.getA()
                    ));
                }
            }
            case BLOCK_FILTER -> {
                if (el.isJsonPrimitive()) {
                    List<String> items = Arrays.stream(el.getAsString().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .limit(64)
                        .toList();
                    ((Setting<String>) setting).set(String.join(", ", items));
                }
            }
        }
    }

    public void save() {
        try {
            Files.createDirectories(this.filePath.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("schemaVersion", SCHEMA_VERSION);
            root.addProperty("soundFeedback", this.soundFeedback);
            root.addProperty("compactLayout", this.compactLayout);
            root.addProperty("selectedCategory", this.selectedCategory);

            JsonObject modsJson = new JsonObject();
            for (Module mod : this.moduleManager.getAll()) {
                JsonObject mJson = new JsonObject();
                mJson.addProperty("enabled", mod.isEnabled());
                mJson.addProperty("keybind", mod.getKeybind());

                JsonObject opts = new JsonObject();
                for (Setting<?> s : mod.getSettings()) {
                    switch (s.getKind()) {
                        case TOGGLE -> opts.addProperty(s.getId(), (Boolean) s.get());
                        case SLIDER_DOUBLE -> opts.addProperty(s.getId(), (Double) s.get());
                        case SLIDER_INT -> opts.addProperty(s.getId(), (Integer) s.get());
                        case DROPDOWN -> opts.addProperty(s.getId(), ((Enum<?>) s.get()).name());
                        case BLOCK_FILTER -> opts.addProperty(s.getId(), (String) s.get());
                        case COLOR_PICKER -> {
                            ColorRgba c = (ColorRgba) s.get();
                            JsonObject co = new JsonObject();
                            co.addProperty("r", c.getR());
                            co.addProperty("g", c.getG());
                            co.addProperty("b", c.getB());
                            co.addProperty("a", c.getA());
                            opts.add(s.getId(), co);
                        }
                    }
                }
                mJson.add("options", opts);
                modsJson.add(mod.getId(), mJson);
            }
            root.add("modules", modsJson);

            Path staged = this.filePath.resolveSibling("settings.json.tmp");
            Files.writeString(
                staged,
                GSON.toJson(root),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
            Files.move(staged, this.filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            ViewGridMod.LOGGER.error("Failed saving settings to {}", this.filePath, ex);
        }
    }

    public boolean isSoundFeedback() { return soundFeedback; }
    public void setSoundFeedback(boolean soundFeedback) { this.soundFeedback = soundFeedback; }

    public boolean isCompactLayout() { return compactLayout; }
    public void setCompactLayout(boolean compactLayout) { this.compactLayout = compactLayout; }

    public String getSelectedCategory() { return selectedCategory; }
    public void setSelectedCategory(String selectedCategory) { this.selectedCategory = selectedCategory; }

    public static Set<String> normalizeBlockIds(String csv) {
        Set<String> set = new HashSet<>();
        if (csv == null || csv.isBlank()) {
            return set;
        }
        for (String token : csv.split(",")) {
            String norm = token.trim().toLowerCase(Locale.ROOT);
            if (!norm.isEmpty()) {
                set.add(norm.contains(":") ? norm : "minecraft:" + norm);
            }
        }
        return set;
    }
}
