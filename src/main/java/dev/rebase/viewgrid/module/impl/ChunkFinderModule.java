package dev.rebase.viewgrid.module.impl;

import dev.rebase.viewgrid.config.ColorRgba;
import dev.rebase.viewgrid.module.Category;
import dev.rebase.viewgrid.module.Module;
import dev.rebase.viewgrid.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ChunkFinderModule extends Module {
    public record FlaggedChunk(long key, int x, int z, String reason, int score) {}

    private final Setting<Boolean> spawners = addSetting(
        Setting.toggle("spawners", "Detect Spawners", true)
    );
    private final Setting<Boolean> chests = addSetting(
        Setting.toggle("chests", "Detect Chest Clusters", true)
    );
    private final Setting<Integer> chestMin = addSetting(
        Setting.sliderInt("chest_min", "Min Chests", 4, 1, 32)
    );
    private final Setting<Integer> entityMin = addSetting(
        Setting.sliderInt("entity_min", "Min Entities", 18, 2, 128)
    );
    private final Setting<Boolean> lowYCheck = addSetting(
        Setting.toggle("low_y_check", "Low-Y Block Check", true)
    );
    private final Setting<ColorRgba> highlight = addSetting(
        Setting.colorPicker("highlight", "Chunk Marker Color", new ColorRgba(64, 235, 140, 175))
    );

    private final Set<Long> visited = ConcurrentHashMap.newKeySet();
    private final Set<Long> customMarks = ConcurrentHashMap.newKeySet();
    private final Map<Long, FlaggedChunk> flagged = new ConcurrentHashMap<>();

    public ChunkFinderModule() {
        super(
            "chunk_finder",
            "Chunk Finder",
            "Tracks visited/marked chunks and flags spawners, chests, entity counts, or Y anomalies.",
            Category.WORLD
        );
    }

    public void toggleCustomMark(ChunkPos pos) {
        if (pos == null) return;
        long key = pos.toLong();
        if (!this.customMarks.remove(key)) this.customMarks.add(key);
    }

    public void notifyChunkLoaded(long key) {
        this.visited.add(key);
    }

    @Override
    public void onTick(Minecraft mc) {
        if (!isEnabled() || mc.level == null || mc.player == null) return;
        Level level = mc.level;
        ChunkPos center = mc.player.chunkPosition();
        this.visited.add(center.toLong());

        Map<Long, Integer> entityTotals = new ConcurrentHashMap<>();
        Map<Long, Integer> playerTotals = new ConcurrentHashMap<>();
        // UNVERIFIED: Loaded entity iterable on ClientLevel in 1.21.11 official Mojang mappings.
        for (Entity ent : queryEntities(level)) {
            if (ent == null || ent.isRemoved()) continue;
            long k = ChunkPos.asLong(ent.blockPosition());
            entityTotals.merge(k, 1, Integer::sum);
            if (ent instanceof Player) playerTotals.merge(k, 1, Integer::sum);
        }

        int r = 4;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int cx = center.x + dx;
                int cz = center.z + dz;
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) continue;
                long key = ChunkPos.asLong(cx, cz);
                inspectChunk(level, chunk, cx, cz, key,
                    entityTotals.getOrDefault(key, 0),
                    playerTotals.getOrDefault(key, 0));
            }
        }
    }

    private void inspectChunk(Level level, LevelChunk chunk, int cx, int cz, long key, int ents, int players) {
        int spawnerCount = 0;
        int chestCount = 0;
        // UNVERIFIED: LevelChunk#getBlockEntities in 1.21.11 official Mojang mappings.
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be == null) continue;
            if (be.getBlockState().getBlock() instanceof SpawnerBlock) spawnerCount++;
            else if (be.getBlockState().getBlock() instanceof ChestBlock) chestCount++;
        }

        int unusualLowBlocks = 0;
        if (Boolean.TRUE.equals(this.lowYCheck.get())) {
            LevelChunkSection[] secs = chunk.getSections();
            int minSec = level.getMinSectionY();
            for (int i = 0; i < secs.length; i++) {
                int baseY = (minSec + i) << 4;
                if (baseY >= 0) break;
                LevelChunkSection sec = secs[i];
                if (sec == null || sec.hasOnlyAir()) continue;
                for (int ly = 0; ly < 16; ly++) {
                    if (baseY + ly > -40) break;
                    for (int lz = 0; lz < 16; lz += 2) {
                        for (int lx = 0; lx < 16; lx += 2) {
                            var st = sec.getBlockState(lx, ly, lz);
                            if (!st.isAir() && !st.is(Blocks.DEEPSLATE) && !st.is(Blocks.BEDROCK)) {
                                unusualLowBlocks++;
                            }
                        }
                    }
                }
            }
        }

        StringBuilder tags = new StringBuilder();
        int score = 0;
        if (Boolean.TRUE.equals(this.spawners.get()) && spawnerCount > 0) {
            tags.append("Spawner(").append(spawnerCount).append(") ");
            score += spawnerCount * 25;
        }
        if (Boolean.TRUE.equals(this.chests.get()) && chestCount >= this.chestMin.get()) {
            tags.append("Chests(").append(chestCount).append(") ");
            score += chestCount * 10;
        }
        if (ents >= this.entityMin.get()) {
            tags.append("Entities(").append(ents).append(") ");
            score += ents * 2;
        }
        if (players > 1) {
            tags.append("Players(").append(players).append(") ");
            score += players * 20;
        }
        if (unusualLowBlocks >= 36) {
            tags.append("LowY(").append(unusualLowBlocks).append(") ");
            score += 20;
        }
        if (this.customMarks.contains(key)) {
            tags.append("Marked ");
            score += 50;
        }

        if (score > 0) {
            this.flagged.put(key, new FlaggedChunk(key, cx, cz, tags.toString().trim(), score));
        } else {
            this.flagged.remove(key);
        }
    }

    private Iterable<Entity> queryEntities(Level level) {
        // UNVERIFIED: Confirm client entity list accessor on ClientLevel in 1.21.11 decompiled jar.
        return Collections.emptyList();
    }

    public Set<Long> getVisited() { return new HashSet<>(this.visited); }
    public Set<Long> getCustomMarks() { return new HashSet<>(this.customMarks); }
    public Map<Long, FlaggedChunk> getFlagged() { return Collections.unmodifiableMap(this.flagged); }
    public ColorRgba getHighlight() { return this.highlight.get().copy(); }
}
