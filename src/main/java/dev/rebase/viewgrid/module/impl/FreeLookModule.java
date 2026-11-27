package dev.rebase.viewgrid.module.impl;

import dev.rebase.viewgrid.module.Category;
import dev.rebase.viewgrid.module.Module;
import dev.rebase.viewgrid.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class FreeLookModule extends Module {
    private final Setting<Double> maxYaw = addSetting(
        Setting.sliderDouble("max_yaw", "Yaw Clamp (deg)", 140.0, 30.0, 180.0)
    );
    private final Setting<Double> maxPitch = addSetting(
        Setting.sliderDouble("max_pitch", "Pitch Clamp (deg)", 89.0, 20.0, 90.0)
    );
    private final Setting<Double> smooth = addSetting(
        Setting.sliderDouble("smooth", "Smooth Factor", 0.35, 0.0, 0.9)
    );

    private boolean engaged = false;
    private float yaw = 0.0f;
    private float pitch = 0.0f;

    public FreeLookModule() {
        super(
            "free_look",
            "Free Look",
            "Decouples camera orientation from player look/movement direction while active.",
            Category.CAMERA
        );
    }

    @Override
    protected void onActivate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            this.yaw = mc.player.getYRot();
            this.pitch = mc.player.getXRot();
            this.engaged = true;
        }
    }

    @Override
    protected void onDeactivate() {
        this.engaged = false;
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null) {
            setEnabled(false);
            return;
        }
        if (!this.engaged) onActivate();
    }

    public boolean stepLook(double dyaw, double dpitch, float anchorYaw) {
        if (!isEnabled() || !this.engaged) return false;
        float yawLim = this.maxYaw.get().floatValue();
        float pitchLim = this.maxPitch.get().floatValue();
        float alpha = 1.0f - this.smooth.get().floatValue();

        float rawYaw = this.yaw + (float) dyaw;
        float relYaw = Mth.clamp(Mth.wrapDegrees(rawYaw - anchorYaw), -yawLim, yawLim);
        float nextPitch = Mth.clamp(this.pitch + (float) dpitch, -pitchLim, pitchLim);

        this.yaw = Mth.lerp(alpha, this.yaw, anchorYaw + relYaw);
        this.pitch = Mth.lerp(alpha, this.pitch, nextPitch);
        return true;
    }

    public boolean isEngaged() { return isEnabled() && this.engaged; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
}
