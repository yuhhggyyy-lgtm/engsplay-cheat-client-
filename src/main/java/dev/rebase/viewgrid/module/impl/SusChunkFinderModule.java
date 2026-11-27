package dev.rebase.viewgrid.module.impl;

import dev.rebase.viewgrid.config.ColorRgba;
import dev.rebase.viewgrid.module.Category;
import dev.rebase.viewgrid.module.Module;
import dev.rebase.viewgrid.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.PistonBaseBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SusChunkFinderModule extends Module {
    public enum HighlightStyle { OUTLINE, TINT, BOTH }
    public record SusEntry(long key, int x, int z, double score, String summary) {}

    public static final String HEURISTIC_DISCLAIMER =
        "Client-side heuristic indicator only; not proof of server activity.";

    private final Setting<Double> threshold = addSetting(
        Setting.sliderDouble("threshold", "Sensitivity Threshold", 0.65, 0.10, 1.00)
    );
    private final Setting<ColorRgba> tint = addSetting(
        Setting.colorPicker("tint", "Flag Color", new ColorRgba(255, 68, 92, 185))
    );
    private final Setting<HighlightStyle> highlightStyle = addSetting(
        Setting.dropdown("highlight_style", "Highlight Style", HighlightStyle.BOTH, HighlightStyle.class)
    );

    private final Map<Long, SusEntry> results = new ConcurrentHashMap<>();

    public SusChunkFinderModule() {
        super(
            "sus_chunk_finder",
            "Sus Chunk Finder",
            "Client-side heuristic for unusual container, redstone, spawner, or bedrock density.",
            Category.WORLD
        );
    }

    @Override
    protected void onDeactivate() {
        this.results.clear();
    }

    @Override
    public void onTick(Minecraft mc) {
        if (!isEnabled() || mc.level == null || mc.player == null) return;
        Level level = mc.level;
        ChunkPos center = mc.player.chunkPosition();
        double minScore = this.threshold.get();

        int r = 4;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int cx = center.x + dx;
                int cz = center.z + dz;
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) continue;
                long key = ChunkPos.asLong(cx, cz);
                SusEntry entry = evaluate(level, chunk, cx, cz, key);
                if (entry.score() >= minScore) {
                    this.results.put(key, entry);
                } else {
                    this.results.remove(key);
                }
            }
        }
    }

    private SusEntry evaluate(Level level, LevelChunk chunk, int cx, int cz, long key) {
        int storageTiles = 0;
        int spawnerTiles = 0;
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be == null) continue;
            var block = be.getBlockState().getBlock();
            if (block instanceof SpawnerBlock) spawnerTiles++;
            else if (block instanceof ChestBlock || block instanceof HopperBlock) storageTiles++;
        }

        int redstoneParts = 0;
        int highBedrock = 0;
        LevelChunkSection[] secs = chunk.getSections();
        int minSec = level.getMinSectionY();
        int baseFloor = level.getMinBuildHeight() + 8;

        for (int i = 0; i < secs.length; i++) {
            LevelChunkSection sec = secs[i];
            if (sec == null || sec.hasOnlyAir()) continue;
            int baseY = (minSec + i) << 4;
            for (int ly = 0; ly < 16; ly++) {
                int y = baseY + ly;
                for (int lz = 0; lz < 16; lz++) {
                    for (int lx = 0; lx < 16; lx++) {
                        BlockState st = sec.getBlockState(lx, ly, lz);
                        if (st.isAir()) continue;
                        var blk = st.getBlock();
                        if (blk instanceof RedStoneWireBlock || blk instanceof PistonBaseBlock
                            || blk instanceof ObserverBlock || blk instanceof HopperBlock) {
                            redstoneParts++;
                        }
                        if (st.is(Blocks.BEDROCK) && y > baseFloor) {
                            highBedrock++;
                        }
                    }
                }
            }
        }

        double score = 0.0;
        StringBuilder sb = new StringBuilder();
        if (storageTiles >= 8) {
            score += Math.min(0.35, storageTiles / 40.0);
            sb.append("storage=").append(storageTiles).append(" ");
        }
        if (redstoneParts >= 24) {
            score += Math.min(0.40, redstoneParts / 120.0);
            sb.append("redstone=").append(redstoneParts).append(" ");
        }
        if (spawnerTiles >= 2) {
            score += Math.min(0.30, spawnerTiles * 0.15);
            sb.append("spawners=").append(spawnerTiles).append(" ");
        }
        if (highBedrock > 0) {
            score += Math.min(0.45, highBedrock * 0.15);
            sb.append("high_bedrock=").append(highBedrock).append(" ");
        }

        double clamped = Math.max(0.0, Math.min(1.0, score));
        String info = sb.length() == 0 ? "none" : sb.toString().trim();
        return new SusEntry(key, cx, cz, clamped, info);
    }

    public Map<Long, SusEntry> getResults() { return Collections.unmodifiableMap(this.results); }
    public ColorRgba getTint() { return this.tint.get().copy(); }
    public HighlightStyle getHighlightStyle() { return this.highlightStyle.get(); }
    public double getThreshold() { return this.threshold.get(); }
}
