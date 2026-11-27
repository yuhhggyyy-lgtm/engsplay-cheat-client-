package dev.rebase.viewgrid.module.impl;

import dev.rebase.viewgrid.config.ColorRgba;
import dev.rebase.viewgrid.module.Category;
import dev.rebase.viewgrid.module.Module;
import dev.rebase.viewgrid.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.TrappedChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StorageEspModule extends Module {
    public enum RenderStyle {
        OUTLINE,
        BOX,
        BOTH
    }

    public record TargetEntry(BlockPos pos, String label, ColorRgba color, double distance) {}

    private final Setting<RenderStyle> renderStyle = registerSetting(
        Setting.choice("render_style", "Render Style", RenderStyle.BOTH, RenderStyle.class)
    );
    private final Setting<Boolean> perTypeColors = registerSetting(
        Setting.bool("per_type_colors", "Per-Type Colors", true)
    );
    private final Setting<ColorRgba> globalColor = registerSetting(
        Setting.color("global_color", "Global Color", new ColorRgba(255, 196, 48, 210))
    );
    private final Setting<ColorRgba> chestColor = registerSetting(
        Setting.color("chest_color", "Chest / Barrel Color", new ColorRgba(245, 175, 40, 210))
    );
    private final Setting<ColorRgba> shulkerColor = registerSetting(
        Setting.color("shulker_color", "Shulker Color", new ColorRgba(210, 75, 255, 220))
    );
    private final Setting<ColorRgba> enderColor = registerSetting(
        Setting.color("ender_color", "Ender Chest Color", new ColorRgba(45, 220, 210, 210))
    );
    private final Setting<ColorRgba> utilityColor = registerSetting(
        Setting.color("utility_color", "Hopper / Disp / Drop Color", new ColorRgba(120, 175, 255, 200))
    );
    private final Setting<Boolean> tracers = registerSetting(
        Setting.bool("tracers", "Tracers", false)
    );
    private final Setting<Boolean> showLabels = registerSetting(
        Setting.bool("show_labels", "Count & Type Labels", true)
    );
    private final Setting<Integer> maxDistance = registerSetting(
        Setting.rangedInt("max_distance", "Max Distance", 96, 8, 256)
    );

    private volatile List<TargetEntry> snapshot = Collections.emptyList();

    public StorageEspModule() {
        super(
            "storage_esp",
            "Storage ESP",
            "Highlights chests, barrels, shulkers, hoppers, and dispensers within range.",
            Category.VISUALS
        );
    }

    @Override
    protected void onDisable() {
        this.snapshot = Collections.emptyList();
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.level == null || client.player == null) {
            this.snapshot = Collections.emptyList();
            return;
        }
        Level level = client.level;
        Vec3 eyePos = client.player.getEyePosition(1.0f);
        double maxDistSq = (double) this.maxDistance.getValue() * this.maxDistance.getValue();

        // UNVERIFIED: ClientLevel block entity iteration helper in 1.21.11 official Mojang mappings.
        // Inspect net.minecraft.client.multiplayer.ClientLevel to confirm loaded block entity map access.
        List<BlockEntity> loadedEntities = collectLoadedBlockEntities(level);
        List<TargetEntry> next = new ArrayList<>();

        for (BlockEntity be : loadedEntities) {
            if (be == null || be.isRemoved()) {
                continue;
            }
            BlockPos pos = be.getBlockPos();
            double distSq = pos.distToCenterSqr(eyePos.x, eyePos.y, eyePos.z);
            if (distSq > maxDistSq) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            String label = classifyContainer(state);
            if (label == null) {
                continue;
            }
            ColorRgba color = resolveColor(state);
            next.add(new TargetEntry(pos, label, color, Math.sqrt(distSq)));
        }

        this.snapshot = Collections.unmodifiableList(next);
    }

    private String classifyContainer(BlockState state) {
        if (state.getBlock() instanceof TrappedChestBlock) return "Trapped Chest";
        if (state.getBlock() instanceof ChestBlock) return "Chest";
        if (state.getBlock() instanceof EnderChestBlock) return "Ender Chest";
        if (state.getBlock() instanceof BarrelBlock) return "Barrel";
        if (state.getBlock() instanceof ShulkerBoxBlock) return "Shulker Box";
        if (state.getBlock() instanceof HopperBlock) return "Hopper";
        if (state.getBlock() instanceof DispenserBlock) return "Dispenser";
        if (state.getBlock() instanceof DropperBlock) return "Dropper";
        return null;
    }

    private ColorRgba resolveColor(BlockState state) {
        if (!Boolean.TRUE.equals(this.perTypeColors.getValue())) {
            return this.globalColor.getValue().copy();
        }
        if (state.getBlock() instanceof ShulkerBoxBlock) {
            return this.shulkerColor.getValue().copy();
        }
        if (state.getBlock() instanceof EnderChestBlock) {
            return this.enderColor.getValue().copy();
        }
        if (state.getBlock() instanceof HopperBlock
            || state.getBlock() instanceof DispenserBlock
            || state.getBlock() instanceof DropperBlock) {
            return this.utilityColor.getValue().copy();
        }
        return this.chestColor.getValue().copy();
    }

    private List<BlockEntity> collectLoadedBlockEntities(Level level) {
        // UNVERIFIED: Direct access to loaded block entity tickers/entries in 1.21.11.
        // Replace with verified ClientLevel block entity collection after inspecting ClientLevel in decompiled jar.
        return Collections.emptyList();
    }

    public List<TargetEntry> getSnapshot() { return snapshot; }
    public RenderStyle getRenderStyle() { return renderStyle.getValue(); }
    public boolean isTracersEnabled() { return Boolean.TRUE.equals(tracers.getValue()); }
    public boolean isLabelsEnabled() { return Boolean.TRUE.equals(showLabels.getValue()); }
}
