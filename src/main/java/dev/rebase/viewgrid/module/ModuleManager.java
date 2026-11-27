package dev.rebase.viewgrid.module;

import dev.rebase.viewgrid.module.impl.BlockEspModule;
import dev.rebase.viewgrid.module.impl.ChunkFinderModule;
import dev.rebase.viewgrid.module.impl.CustomSkyModule;
import dev.rebase.viewgrid.module.impl.FreeCamModule;
import dev.rebase.viewgrid.module.impl.FreeLookModule;
import dev.rebase.viewgrid.module.impl.StorageEspModule;
import dev.rebase.viewgrid.module.impl.SusChunkFinderModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    private final StorageEspModule storageEsp = new StorageEspModule();
    private final BlockEspModule blockEsp = new BlockEspModule();
    private final ChunkFinderModule chunkFinder = new ChunkFinderModule();
    private final SusChunkFinderModule susChunkFinder = new SusChunkFinderModule();
    private final FreeLookModule freeLook = new FreeLookModule();
    private final FreeCamModule freeCam = new FreeCamModule();
    private final CustomSkyModule customSky = new CustomSkyModule();

    public ModuleManager() {
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

    public List<Module> getAll() {
        return Collections.unmodifiableList(this.modules);
    }

    public List<Module> filter(Category category, String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Module> result = new ArrayList<>();
        for (Module m : this.modules) {
            if (category != null && m.getCategory() != category) continue;
            if (!needle.isEmpty()) {
                boolean hitTitle = m.getTitle().toLowerCase(Locale.ROOT).contains(needle);
                boolean hitDesc = m.getDescription().toLowerCase(Locale.ROOT).contains(needle);
                if (!hitTitle && !hitDesc) continue;
            }
            result.add(m);
        }
        return result;
    }

    public long countActive() {
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
