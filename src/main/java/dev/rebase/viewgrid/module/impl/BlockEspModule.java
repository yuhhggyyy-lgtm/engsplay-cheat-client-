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
    public enum RenderMode { OUTLINE, FILLED_BOX, BOTH }

    private final Setting<String> targetsCsv = addSetting(
        Setting.blockFilter(
            "targets_csv",
            "Target Blocks",
            "minecraft:ancient_debris, minecraft:diamond_ore, minecraft:deepslate_diamond_ore, minecraft:spawner"
        )
    );
    private final Setting<ColorRgba> color = addSetting(
        Setting.colorPicker("color", "ESP Color", new ColorRgba(56, 212, 255, 190))
    );
    private final Setting<RenderMode> renderMode = addSetting(
        Setting.dropdown("render_mode", "Render Mode", RenderMode.BOTH, RenderMode.class)
    );
    private final Setting<Integer> radius = addSetting(
        Setting.sliderInt("radius", "Max Radius", 48, 8, 128)
    );

    private final Map<Long, Set<BlockPos>> cacheByChunk = new ConcurrentHashMap<>();
    private final Set<Long> queuedChunks = ConcurrentHashMap.newKeySet();
    private String lastFilter = "";
    private volatile List<BlockPos> flatMatches = Collections.emptyList();

    public BlockEspModule() {
        super(
            "block_esp",
            "Block ESP",
            "Caches matching blocks by chunk and updates on chunk load/unload or block edits.",
            Category.VISUALS
        );
    }

    @Override
    protected void onActivate() {
        this.cacheByChunk.clear();
        this.queuedChunks.clear();
        this.lastFilter = "";
        this.flatMatches = Collections.emptyList();
    }

    @Override
    protected void onDeactivate() {
        this.cacheByChunk.clear();
        this.queuedChunks.clear();
        this.flatMatches = Collections.emptyList();
    }

    public void notifyChunkLoaded(long chunkKey) {
        if (isEnabled()) this.queuedChunks.add(chunkKey);
    }

    public void notifyChunkUnloaded(long chunkKey) {
        this.queuedChunks.remove(chunkKey);
        if (this.cacheByChunk.remove(chunkKey) != null && isEnabled()) {
            rebuildSnapshot();
        }
    }

    public void notifyBlockChange(BlockPos pos, BlockState state) {
        if (!isEnabled() || pos == null) return;
        Set<String> filter = ConfigManager.normalizeBlockIds(this.targetsCsv.get());
        long key = ChunkPos.asLong(pos);
        Set<BlockPos> set = this.cacheByChunk.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
        BlockPos copyPos = pos.immutable();
        if (matches(state, filter)) {
            set.add(copyPos);
        } else {
            set.remove(copyPos);
        }
        rebuildSnapshot();
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.level == null || mc.player == null) return;
        String filterStr = this.targetsCsv.get();
        if (!filterStr.equals(this.lastFilter)) {
            this.lastFilter = filterStr;
            this.cacheByChunk.clear();
        }

        enqueueAround(mc.level, mc.player.blockPosition(), this.radius.get());
        if (!this.queuedChunks.isEmpty()) {
            Set<String> filter = ConfigManager.normalizeBlockIds(filterStr);
            int budget = 4;
            for (Long key : new ArrayList<>(this.queuedChunks)) {
                this.queuedChunks.remove(key);
                scanSingleChunk(mc.level, key, filter);
                if (--budget <= 0) break;
            }
            rebuildSnapshot();
        }
    }

    private void enqueueAround(Level level, BlockPos center, int r) {
        int chunkR = Math.max(1, (r + 15) / 16);
        int cx0 = center.getX() >> 4;
        int cz0 = center.getZ() >> 4;
        for (int cx = cx0 - chunkR; cx <= cx0 + chunkR; cx++) {
            for (int cz = cz0 - chunkR; cz <= cz0 + chunkR; cz++) {
                long key = ChunkPos.asLong(cx, cz);
                if (!this.cacheByChunk.containsKey(key) && level.hasChunk(cx, cz)) {
                    this.queuedChunks.add(key);
                }
            }
        }
    }

    private void scanSingleChunk(Level level, long key, Set<String> filter) {
        int cx = ChunkPos.getX(key);
        int cz = ChunkPos.getZ(key);
        LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
        if (chunk == null || filter.isEmpty()) {
            this.cacheByChunk.put(key, new HashSet<>());
            return;
        }
        Set<BlockPos> found = new HashSet<>();
        LevelChunkSection[] sections = chunk.getSections();
        int minSec = level.getMinSectionY();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection sec = sections[i];
            if (sec == null || sec.hasOnlyAir()) continue;
            int baseY = (minSec + i) << 4;
            for (int ly = 0; ly < 16; ly++) {
                for (int lz = 0; lz < 16; lz++) {
                    for (int lx = 0; lx < 16; lx++) {
                        BlockState st = sec.getBlockState(lx, ly, lz);
                        if (matches(st, filter)) {
                            found.add(new BlockPos((cx << 4) + lx, baseY + ly, (cz << 4) + lz));
                        }
                    }
                }
            }
        }
        this.cacheByChunk.put(key, found);
    }

    private boolean matches(BlockState st, Set<String> filter) {
        if (st == null || st.isAir() || filter.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(st.getBlock());
        return id != null && filter.contains(id.toString().toLowerCase());
    }

    private void rebuildSnapshot() {
        List<BlockPos> all = new ArrayList<>();
        for (Set<BlockPos> s : this.cacheByChunk.values()) {
            if (s != null && !s.isEmpty()) all.addAll(s);
        }
        this.flatMatches = Collections.unmodifiableList(all);
    }

    public List<BlockPos> getFlatMatches() { return flatMatches; }
    public ColorRgba getColor() { return color.get().copy(); }
    public RenderMode getRenderMode() { return renderMode.get(); }
    public int getRadius() { return radius.get(); }
}
