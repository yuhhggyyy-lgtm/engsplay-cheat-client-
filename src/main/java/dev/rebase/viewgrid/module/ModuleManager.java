package dev.rebase.viewgrid.module;

import dev.rebase.viewgrid.module.impl.BlockEspModule;
import dev.rebase.viewgrid.module.impl.ChunkFinderModule;
import dev.rebase.viewgrid.module.impl.CustomSkyModule;
import dev.rebase.viewgrid.module.impl.FreeCamModule;
import dev.rebase.viewgrid.module.impl.FreeLookModule;
import dev.rebase.viewgrid.module.impl.StorageEspModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    private final StorageEspModule storageEsp;
    private final BlockEspModule blockEsp;
    private final ChunkFinderModule chunkFinder;
    private final SusChunkFinderModule susChunkFinder;
    private final FreeLookModule freeLook;
    private final FreeCamModule freeCam;
    private final CustomSkyModule customSky;

    public ModuleManager() {
        this.storageEsp = new StorageEspModule();
        this.blockEsp = new BlockEspModule();
        this.chunkFinder = new ChunkFinderModule();
        this.susChunkFinder = new SusChunkFinderModule();
        this.freeLook = new FreeLookModule();
        this.freeCam = new FreeCamModule();
        this.customSky = new CustomSkyModule();

        Collections.addAll(
            this.modules,
            this.storageEsp,
            this.blockEsp,
            this.chunkFinder,
            this.susChunkFinder,
            this.freeLook,
            this.freeCam,
            this.customSky
        );
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(this.modules);
    }

    public List<Module> getByCategoryAndFilter(Category category, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Module> out = new ArrayList<>();
        for (Module m : this.modules) {
            if (category != null && m.getCategory() != category) {
                continue;
            }
            if (!q.isEmpty()) {
                boolean matchesName = m.getName().toLowerCase(Locale.ROOT).contains(q);
                boolean matchesSummary = m.getSummary().toLowerCase(Locale.ROOT).contains(q);
                if (!matchesName && !matchesSummary) {
                    continue;
                }
            }
            out.add(m);
        }
        return out;
    }

    public long getActiveCount() {
        return this.modules.stream().filter(Module::isEnabled).count();
    }

    public StorageEspModule getStorageEsp() { return storageEsp; }
    public BlockEspModule getBlockEsp() { return blockEsp; }
    public ChunkFinderModule getChunkFinder() { return chunkFinder; }
    public SusChunkFinderModule getSusChunkFinder() { return susChunkFinder; }
    public FreeLookModule getFreeLook() { return freeLook; }
    public FreeCamModule getFreeCam() { return freeCam; }
    public CustomSkyModule getCustomSky() { return customSky; }
}
