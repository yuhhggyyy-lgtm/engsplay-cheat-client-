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
    public enum HighlightMode {
        BORDER_OUTLINE,
        VOLUME_TINT,
        BOTH
    }

    public record SusResult(long chunkKey, int chunkX, int chunkZ, double heuristicScore, String signals) {}

    // Client-side heuristic indicator only; does not prove server-side activity or player intent.
    public static final String CLIENT_HEURISTIC_NOTICE =
        "Client-side heuristic signal only; not proof of server activity.";

    private final Setting<Double> sensitivityThreshold = registerSetting(
        Setting.rangedDouble("sensitivity_threshold", "Anomaly Threshold", 0.65, 0.10, 1.00)
    );
    private final Setting<ColorRgba> susColor = registerSetting(
        Setting.color("sus_color", "Sus Highlight Color", new ColorRgba(255, 68, 92, 185))
    );
    private final Setting<HighlightMode> highlightMode = registerSetting(
        Setting.choice("highlight_mode", "Highlight Mode", HighlightMode.BOTH, HighlightMode.class)
    );

    private final Map<Long, SusResult> flagged = new ConcurrentHashMap<>();

    public SusChunkFinderModule() {
        super(
            "sus_chunk_finder",
            "Sus Chunk Finder",
            "Client-side heuristic analyzer for unusual container, redstone, spawner, or bedrock ratios.",
            Category.WORLD
        );
    }

    @Override
    protected void onDisable() {
        this.flagged.clear();
    }

    @Override
    public void onTick(Minecraft client) {
        if (!isEnabled() || client.level == null || client.player == null) {
            return;
        }
        Level level = client.level;
        ChunkPos center = client.player.chunkPosition();
        double threshold = this.sensitivityThreshold.getValue();

        int radius = 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = center.x + dx;
                int cz = center.z + dz;
                long key = ChunkPos.asLong(cx, cz);
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) {
                    continue;
                }
                SusResult res = scoreChunk(level, chunk, cx, cz);
                if (res.heuristicScore() >= threshold) {
                    this.flagged.put(key, res);
                } else {
                    this.flagged.remove(key);
                }
            }
        }
    }

    private SusResult scoreChunk(Level level, LevelChunk chunk, int cx, int cz) {
        long key = ChunkPos.asLong(cx, cz);
        int containers = 0;
        int spawners = 0;
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be == null) continue;
            BlockState st = be.getBlockState();
            if (st.getBlock() instanceof SpawnerBlock) {
                spawners++;
            } else if (st.getBlock() instanceof ChestBlock || st.getBlock() instanceof HopperBlock) {
                containers++;
            }
        }

        int redstoneCount = 0;
        int elevatedBedrock = 0;
        LevelChunkSection[] sections = chunk.getSections();
        int minSection = level.getMinSectionY();
        int surfaceFloor = level.getMinBuildHeight() + 8;

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection sec = sections[i];
            if (sec == null || sec.hasOnlyAir()) continue;
            int baseY = (minSection + i) << 4;
            for (int ly = 0; ly < 16; ly++) {
                int worldY = baseY + ly;
                for (int lz = 0; lz < 16; lz++) {
                    for (int lx = 0; lx < 16; lx++) {
                        BlockState st = sec.getBlockState(lx, ly, lz);
                        if (st.isAir()) continue;
                        if (st.getBlock() instanceof RedStoneWireBlock
                            || st.getBlock() instanceof PistonBaseBlock
                            || st.getBlock() instanceof ObserverBlock
                            || st.getBlock() instanceof HopperBlock) {
                            redstoneCount++;
                        }
                        if (st.is(Blocks.BEDROCK) && worldY > surfaceFloor) {
                            elevatedBedrock++;
                        }
                    }
                }
            }
        }

        double raw = 0.0;
        StringBuilder sb = new StringBuilder();
        if (containers >= 8) {
            raw += Math.min(0.35, containers / 40.0);
            sb.append("containers=").append(containers).append(" ");
        }
        if (redstoneCount >= 24) {
            raw += Math.min(0.40, redstoneCount / 120.0);
            sb.append("redstone=").append(redstoneCount).append(" ");
        }
        if (spawners >= 2) {
            raw += Math.min(0.30, spawners * 0.15);
            sb.append("spawners=").append(spawners).append(" ");
        }
        if (elevatedBedrock > 0) {
            raw += Math.min(0.45, elevatedBedrock * 0.15);
            sb.append("high_bedrock=").append(elevatedBedrock).append(" ");
        }

        double clampedScore = Math.max(0.0, Math.min(1.0, raw));
        String signals = sb.length() == 0 ? "none" : sb.toString().trim();
        return new SusResult(key, cx, cz, clampedScore, signals);
    }

    public Map<Long, SusResult> getFlaggedChunks() { return Collections.unmodifiableMap(this.flagged); }
    public ColorRgba getSusColor() { return this.susColor.getValue().copy(); }
    public HighlightMode getHighlightMode() { return this.highlightMode.getValue(); }
    public double getSensitivityThreshold() { return this.sensitivityThreshold.getValue(); }
}
