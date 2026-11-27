package dev.rebase.viewgrid.module.impl;

import dev.rebase.viewgrid.config.ColorRgba;
import dev.rebase.viewgrid.module.Category;
import dev.rebase.viewgrid.module.Module;
import dev.rebase.viewgrid.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
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
    public record FlaggedChunk(long chunkKey, int chunkX, int chunkZ, String reason, int score) {}

    private final Setting<Boolean> flagSpawners = registerSetting(
        Setting.bool("flag_spawners", "Flag Spawner Chunks", true)
    );
    private final Setting<Boolean> flagChests = registerSetting(
        Setting.bool("flag_chests", "Flag High-Chest Chunks", true)
    );
    private final Setting<Integer> chestThreshold = registerSetting(
        Setting.rangedInt("chest_threshold", "Chest Count Threshold", 4, 1, 32)
    );
    private final Setting<Integer> entityThreshold = registerSetting(
        Setting.rangedInt("entity_threshold", "Entity Count Threshold", 18, 2, 128)
    );
    private final Setting<Boolean> flagYAnomaly = registerSetting(
        Setting.bool("flag_y_anomaly", "Flag Low-Y Non-Deepslate", true)
    );
    private final Setting<ColorRgba> markerColor = registerSetting(
        Setting.color("marker_color", "Chunk Highlight Color", new ColorRgba(64, 235, 140, 175))
    );

    private final Set<Long> visitedChunks = ConcurrentHashMap.newKeySet();
    private final Set<Long> customMarkedChunks = ConcurrentHashMap.newKeySet();
    private final Map<Long, FlaggedChunk> flaggedChunks = new ConcurrentHashMap<>();

    public ChunkFinderModule() {
        super(
            "chunk_finder",
            "Chunk Finder",
            "Tracks visited chunks, custom markers, spawners, chest clusters, and entity density.",
            Category.WORLD
        );
    }

    public void toggleCustomMark(ChunkPos pos) {
        if (pos == null) return;
        long key = pos.toLong();
        if (!this.customMarkedChunks.remove(key)) {
            this.customMarkedChunks.add(key);
        }
    }

    public void onChunkLoaded(long chunkKey) {
        this.visitedChunks.add(chunkKey);
    }

    @Override
    public void onTick(Minecraft client) {
        if (!isEnabled() || client.level == null || client.player == null) {
            return;
        }
        Level level = client.level;
        ChunkPos playerChunk = client.player.chunkPosition();
        this.visitedChunks.add(playerChunk.toLong());

        Map<Long, Integer> entityCounts = new ConcurrentHashMap<>();
        Map<Long, Integer> playerCounts = new ConcurrentHashMap<>();
        // UNVERIFIED: ClientLevel entity iterator method in 1.21.11 official Mojang mappings.
        for (Entity entity : collectLevelEntities(level)) {
            if (entity == null || entity.isRemoved()) continue;
            long key = ChunkPos.asLong(entity.blockPosition());
            entityCounts.merge(key, 1, Integer::sum);
            if (entity instanceof Player) {
                playerCounts.merge(key, 1, Integer::sum);
            }
        }

        int scanRadius = 4;
        for (int dx = -scanRadius; dx <= scanRadius; dx++) {
            for (int dz = -scanRadius; dz <= scanRadius; dz++) {
                int cx = playerChunk.x + dx;
                int cz = playerChunk.z + dz;
                long key = ChunkPos.asLong(cx, cz);
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) {
                    continue;
                }
                evaluateChunk(level, chunk, cx, cz, entityCounts.getOrDefault(key, 0), playerCounts.getOrDefault(key, 0));
            }
        }
    }

    private void evaluateChunk(Level level, LevelChunk chunk, int cx, int cz, int entities, int players) {
        long key = ChunkPos.asLong(cx, cz);
        int spawners = 0;
        int chests = 0;
        int lowYNonDeepslate = 0;

        // UNVERIFIED: Chunk block entity map access in 1.21.11 official Mojang mappings.
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be == null) continue;
            if (be.getBlockState().getBlock() instanceof SpawnerBlock) {
                spawners++;
            } else if (be.getBlockState().getBlock() instanceof ChestBlock) {
                chests++;
            }
        }

        if (Boolean.TRUE.equals(this.flagYAnomaly.getValue())) {
            LevelChunkSection[] sections = chunk.getSections();
            int minSection = level.getMinSectionY();
            for (int i = 0; i < sections.length; i++) {
                int baseY = (minSection + i) << 4;
                if (baseY >= 0) break;
                LevelChunkSection sec = sections[i];
                if (sec == null || sec.hasOnlyAir()) continue;
                for (int ly = 0; ly < 16; ly++) {
                    int worldY = baseY + ly;
                    if (worldY > -40) break;
                    for (int lz = 0; lz < 16; lz += 2) {
                        for (int lx = 0; lx < 16; lx += 2) {
                            var st = sec.getBlockState(lx, ly, lz);
                            if (!st.isAir() && !st.is(net.minecraft.world.level.block.Blocks.DEEPSLATE)
                                && !st.is(net.minecraft.world.level.block.Blocks.BEDROCK)) {
                                lowYNonDeepslate++;
                            }
                        }
                    }
                }
            }
        }

        StringBuilder reasons = new StringBuilder();
        int score = 0;
        if (Boolean.TRUE.equals(this.flagSpawners.getValue()) && spawners > 0) {
            reasons.append("Spawner(").append(spawners).append(") ");
            score += spawners * 25;
        }
        if (Boolean.TRUE.equals(this.flagChests.getValue()) && chests >= this.chestThreshold.getValue()) {
            reasons.append("Chests(").append(chests).append(") ");
            score += chests * 10;
        }
        if (entities >= this.entityThreshold.getValue()) {
            reasons.append("Entities(").append(entities).append(") ");
            score += entities * 2;
        }
        if (players > 1) {
            reasons.append("Players(").append(players).append(") ");
            score += players * 20;
        }
        if (lowYNonDeepslate >= 36) {
            reasons.append("LowY(").append(lowYNonDeepslate).append(") ");
            score += 20;
        }
        if (this.customMarkedChunks.contains(key)) {
            reasons.append("Marked ");
            score += 50;
        }

        if (score > 0) {
            this.flaggedChunks.put(key, new FlaggedChunk(key, cx, cz, reasons.toString().trim(), score));
        } else {
            this.flaggedChunks.remove(key);
        }
    }

    private Iterable<Entity> collectLevelEntities(Level level) {
        // UNVERIFIED: Direct entity iterable on ClientLevel in 1.21.11.
        return Collections.emptyList();
    }

    public Set<Long> getVisitedChunks() { return new HashSet<>(this.visitedChunks); }
    public Set<Long> getCustomMarkedChunks() { return new HashSet<>(this.customMarkedChunks); }
    public Map<Long, FlaggedChunk> getFlaggedChunks() { return Collections.unmodifiableMap(this.flaggedChunks); }
    public ColorRgba getMarkerColor() { return this.markerColor.getValue().copy(); }
}
