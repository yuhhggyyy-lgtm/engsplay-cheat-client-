package dev.engsplay.module;

import dev.engsplay.module.impl.CoreModules;
import dev.engsplay.module.impl.HudModules;
import dev.engsplay.module.impl.VisualsModules;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        VisualsModules.register(this);
        HudModules.register(this);
        CoreModules.register(this);
    }

    public void register(Module module) {
        modules.add(module);
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public List<Module> getByCategory(Category category) {
        return modules.stream()
                .filter(m -> m.getCategory() == category)
                .collect(Collectors.toList());
    }

    public List<Module> search(String query) {
        if (query == null || query.isBlank()) return getModules();
        String q = query.toLowerCase(Locale.ROOT).trim();
        return modules.stream()
                .filter(m -> m.getName().toLowerCase(Locale.ROOT).contains(q)
                        || m.getDescription().toLowerCase(Locale.ROOT).contains(q)
                        || m.getCategory().getTitle().toLowerCase(Locale.ROOT).contains(q))
                .collect(Collectors.toList());
    }

    public Module getById(String id) {
        for (Module m : modules) {
            if (m.getId().equalsIgnoreCase(id)) return m;
        }
        return null;
    }

    public boolean isEnabled(String id) {
        Module m = getById(id);
        return m != null && m.isEnabled();
    }

    public int getActiveCount() {
        int c = 0;
        for (Module m : modules) if (m.isEnabled()) c++;
        return c;
    }

    public int getTotalCount() {
        return modules.size();
    }

    public int getActiveCount(Category category) {
        int c = 0;
        for (Module m : modules) if (m.getCategory() == category && m.isEnabled()) c++;
        return c;
    }

    public int getTotalCount(Category category) {
        int c = 0;
        for (Module m : modules) if (m.getCategory() == category) c++;
        return c;
    }

    public void onTick(MinecraftClient client) {
        for (Module m : modules) {
            if (m.isEnabled()) {
                m.onTick(client);
            }
        }
    }

    public void onKeyPress(int key) {
        if (key <= 0) return;
        for (Module m : modules) {
            if (m.getKeybind() == key) {
                m.toggle();
            }
        }
    }
}
