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
    public enum Style { OUTLINE, BOX, BOTH }
    public record Entry(BlockPos pos, String label, ColorRgba color, double distance) {}

    private final Setting<Style> style = addSetting(
        Setting.dropdown("style", "Render Mode", Style.BOTH, Style.class)
    );
    private final Setting<Boolean> perType = addSetting(
        Setting.toggle("per_type", "Per-Type Colors", true)
    );
    private final Setting<ColorRgba> sharedColor = addSetting(
        Setting.colorPicker("shared_color", "Global Color", new ColorRgba(250, 190, 45, 210))
    );
    private final Setting<ColorRgba> chestTint = addSetting(
        Setting.colorPicker("chest_tint", "Chest / Barrel", new ColorRgba(240, 165, 35, 210))
    );
    private final Setting<ColorRgba> shulkerTint = addSetting(
        Setting.colorPicker("shulker_tint", "Shulker Box", new ColorRgba(205, 80, 255, 215))
    );
    private final Setting<ColorRgba> enderTint = addSetting(
        Setting.colorPicker("ender_tint", "Ender Chest", new ColorRgba(40, 220, 205, 210))
    );
    private final Setting<ColorRgba> mechTint = addSetting(
        Setting.colorPicker("mech_tint", "Hopper / Disp / Drop", new ColorRgba(110, 175, 255, 200))
    );
    private final Setting<Boolean> tracers = addSetting(
        Setting.toggle("tracers", "Tracers", false)
    );
    private final Setting<Boolean> labels = addSetting(
        Setting.toggle("labels", "Labels & Count", true)
    );
    private final Setting<Integer> range = addSetting(
        Setting.sliderInt("range", "Max Distance", 96, 8, 256)
    );

    private volatile List<Entry> entries = Collections.emptyList();

    public StorageEspModule() {
        super(
            "storage_esp",
            "Storage ESP",
            "Highlights containers and hoppers/dispensers/droppers in range.",
            Category.VISUALS
        );
    }

    @Override
    protected void onDeactivate() {
        this.entries = Collections.emptyList();
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            this.entries = Collections.emptyList();
            return;
        }
        Level level = mc.level;
        Vec3 eye = mc.player.getEyePosition(1.0f);
        double maxDistSq = (double) this.range.get() * this.range.get();

        // UNVERIFIED: Loaded block-entity accessor on ClientLevel in 1.21.11 official Mojang mappings.
        List<BlockEntity> blockEntities = queryLoadedBlockEntities(level);
        List<Entry> next = new ArrayList<>();

        for (BlockEntity be : blockEntities) {
            if (be == null || be.isRemoved()) continue;
            BlockPos pos = be.getBlockPos();
            double dSq = pos.distToCenterSqr(eye.x, eye.y, eye.z);
            if (dSq > maxDistSq) continue;
            BlockState state = level.getBlockState(pos);
            String kind = identifyStorage(state);
            if (kind == null) continue;
            next.add(new Entry(pos, kind, pickColor(state), Math.sqrt(dSq)));
        }

        this.entries = Collections.unmodifiableList(next);
    }

    private String identifyStorage(BlockState st) {
        if (st.getBlock() instanceof TrappedChestBlock) return "Trapped Chest";
        if (st.getBlock() instanceof ChestBlock) return "Chest";
        if (st.getBlock() instanceof EnderChestBlock) return "Ender Chest";
        if (st.getBlock() instanceof BarrelBlock) return "Barrel";
        if (st.getBlock() instanceof ShulkerBoxBlock) return "Shulker Box";
        if (st.getBlock() instanceof HopperBlock) return "Hopper";
        if (st.getBlock() instanceof DispenserBlock) return "Dispenser";
        if (st.getBlock() instanceof DropperBlock) return "Dropper";
        return null;
    }

    private ColorRgba pickColor(BlockState st) {
        if (!Boolean.TRUE.equals(this.perType.get())) return this.sharedColor.get().copy();
        if (st.getBlock() instanceof ShulkerBoxBlock) return this.shulkerTint.get().copy();
        if (st.getBlock() instanceof EnderChestBlock) return this.enderTint.get().copy();
        if (st.getBlock() instanceof HopperBlock || st.getBlock() instanceof DispenserBlock || st.getBlock() instanceof DropperBlock) {
            return this.mechTint.get().copy();
        }
        return this.chestTint.get().copy();
    }

    private List<BlockEntity> queryLoadedBlockEntities(Level level) {
        // UNVERIFIED: Confirm loaded block-entity list/map on ClientLevel in 1.21.11 decompiled jar.
        return Collections.emptyList();
    }

    public List<Entry> getEntries() { return entries; }
    public Style getStyle() { return style.get(); }
    public boolean hasTracers() { return Boolean.TRUE.equals(tracers.get()); }
    public boolean hasLabels() { return Boolean.TRUE.equals(labels.get()); }
}
