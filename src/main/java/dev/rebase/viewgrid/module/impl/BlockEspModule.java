package dev.rebase.viewgrid.module.impl;

import dev.rebase.viewgrid.config.ColorRgba;
import dev.rebase.viewgrid.config.ConfigManager;
import dev.rebase.viewgrid.module.Category;
import dev.rebase.viewgrid.module.Module;
import dev.rebase.viewgrid.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockEspModule extends Module {
    public enum Mode {
        OUTLINE,
        FILLED_BOX,
        BOTH
    }

    private final Setting<String> blockFilter = registerSetting(
        Setting.blockList(
            "block_filter",
            "Target Blocks (CSV)",
            "minecraft:ancient_debris, minecraft:diamond_ore, minecraft:deepslate_diamond_ore, minecraft:spawner"
        )
    );
    private final Setting<ColorRgba> espColor = registerSetting(
        Setting.color("esp_color", "Highlight Color", new ColorRgba(56, 212, 255, 190))
    );
    private final Setting<Mode> renderMode = registerSetting(
        Setting.choice("render_mode", "Render Mode", Mode.BOTH, Mode.class)
    );
    private final Setting<Integer> maxRadius = registerSetting(
        Setting.rangedInt("max_radius", "Max Scan Radius", 48, 8, 128)
    );

    private final Map<Long, Set<BlockPos>> chunkMatches = new ConcurrentHashMap<>();
    private final Set<Long> dirtyChunks = ConcurrentHashMap.newKeySet();
    private String lastAppliedFilter = "";
    private volatile List<BlockPos> activePositions = Collections.emptyList();

    public BlockEspModule() {
        super(
            "block_esp",
            "Block ESP",
            "Caches matching blocks per chunk and updates on load, unload, or block change.",
            Category.VISUALS
        );
    }

    @Override
    protected void onEnable() {
        this.dirtyChunks.clear();
        this.chunkMatches.clear();
        this.lastAppliedFilter = "";
        this.activePositions = Collections.emptyList();
    }

    @Override
    protected void onDisable() {
        this.dirtyChunks.clear();
        this.chunkMatches.clear();
        this.activePositions = Collections.emptyList();
    }

    public void onChunkLoaded(long chunkKey) {
        if (isEnabled()) {
            this.dirtyChunks.add(chunkKey);
        }
    }

    public void onChunkUnloaded(long chunkKey) {
        this.dirtyChunks.remove(chunkKey);
        if (this.chunkMatches.remove(chunkKey) != null && isEnabled()) {
            rebuildFlatSnapshot();
        }
    }

    public void onBlockChanged(BlockPos pos, BlockState newState) {
        if (!isEnabled() || pos == null) {
            return;
        }
        Set<String> targets = ConfigManager.parseBlockFilterSet(this.blockFilter.getValue());
        long key = ChunkPos.asLong(pos);
        Set<BlockPos> set = this.chunkMatches.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
        BlockPos immutable = pos.immutable();
        if (matchesFilter(newState, targets)) {
            set.add(immutable);
        } else {
            set.remove(immutable);
        }
        rebuildFlatSnapshot();
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.level == null || client.player == null) {
            return;
        }
        String currentFilter = this.blockFilter.getValue();
        if (!currentFilter.equals(this.lastAppliedFilter)) {
            this.lastAppliedFilter = currentFilter;
            this.chunkMatches.clear();
            queueLoadedChunksInRadius(client.level, client.player.blockPosition(), this.maxRadius.getValue());
        }

        queueLoadedChunksInRadius(client.level, client.player.blockPosition(), this.maxRadius.getValue());

        if (!this.dirtyChunks.isEmpty()) {
            Set<String> targets = ConfigManager.parseBlockFilterSet(currentFilter);
            int processed = 0;
            for (Long key : new ArrayList<>(this.dirtyChunks)) {
                this.dirtyChunks.remove(key);
                scanChunk(client.level, key, targets);
                processed++;
                if (processed >= 4) {
                    break;
                }
            }
            rebuildFlatSnapshot();
        }
    }

    private void queueLoadedChunksInRadius(Level level, BlockPos origin, int radius) {
        int chunkRadius = Math.max(1, (radius + 15) / 16);
        int centerX = origin.getX() >> 4;
        int centerZ = origin.getZ() >> 4;
        for (int cx = centerX - chunkRadius; cx <= centerX + chunkRadius; cx++) {
            for (int cz = centerZ - chunkRadius; cz <= centerZ + chunkRadius; cz++) {
                long key = ChunkPos.asLong(cx, cz);
                if (!this.chunkMatches.containsKey(key) && level.hasChunk(cx, cz)) {
                    this.dirtyChunks.add(key);
                }
            }
        }
    }

    private void scanChunk(Level level, long chunkKey, Set<String> targets) {
        int cx = ChunkPos.getX(chunkKey);
        int cz = ChunkPos.getZ(chunkKey);
        LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
        if (chunk == null || targets.isEmpty()) {
            this.chunkMatches.put(chunkKey, new HashSet<>());
            return;
        }

        Set<BlockPos> found = new HashSet<>();
        LevelChunkSection[] sections = chunk.getSections();
        int minSection = level.getMinSectionY();

        for (int idx = 0; idx < sections.length; idx++) {
            LevelChunkSection section = sections[idx];
            if (section == null || section.hasOnlyAir()) {
                continue;
            }
            int baseY = (minSection + idx) << 4;
            for (int ly = 0; ly < 16; ly++) {
                for (int lz = 0; lz < 16; lz++) {
                    for (int lx = 0; lx < 16; lx++) {
                        BlockState state = section.getBlockState(lx, ly, lz);
                        if (matchesFilter(state, targets)) {
                            found.add(new BlockPos((cx << 4) + lx, baseY + ly, (cz << 4) + lz));
                        }
                    }
                }
            }
        }
        this.chunkMatches.put(chunkKey, found);
    }

    private boolean matchesFilter(BlockState state, Set<String> targets) {
        if (state == null || state.isAir() || targets.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null && targets.contains(id.toString().toLowerCase());
    }

    private void rebuildFlatSnapshot() {
        List<BlockPos> out = new ArrayList<>();
        for (Set<BlockPos> s : this.chunkMatches.values()) {
            if (s != null && !s.isEmpty()) {
                out.addAll(s);
            }
        }
        this.activePositions = Collections.unmodifiableList(out);
    }

    public List<BlockPos> getActivePositions() { return activePositions; }
    public ColorRgba getEspColor() { return espColor.getValue().copy(); }
    public Mode getRenderMode() { return renderMode.getValue(); }
    public int getMaxRadius() { return maxRadius.getValue(); }
}
