package dev.rebase.viewgrid.module.impl;

import dev.rebase.viewgrid.module.Category;
import dev.rebase.viewgrid.module.Module;
import dev.rebase.viewgrid.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class FreeLookModule extends Module {
    private final Setting<Double> yawLimitDeg = registerSetting(
        Setting.rangedDouble("yaw_limit_deg", "Max Yaw Offset (deg)", 140.0, 30.0, 180.0)
    );
    private final Setting<Double> pitchLimitDeg = registerSetting(
        Setting.rangedDouble("pitch_limit_deg", "Max Pitch Clamp (deg)", 89.0, 20.0, 90.0)
    );
    private final Setting<Double> smoothing = registerSetting(
        Setting.rangedDouble("smoothing", "Camera Smoothing", 0.35, 0.0, 0.9)
    );

    private boolean active = false;
    private float anchorYaw = 0.0f;
    private float anchorPitch = 0.0f;
    private float cameraYaw = 0.0f;
    private float cameraPitch = 0.0f;

    public FreeLookModule() {
        super(
            "free_look",
            "Free Look",
            "Decouples camera look direction from player movement orientation while enabled.",
            Category.CAMERA
        );
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            this.anchorYaw = mc.player.getYRot();
            this.anchorPitch = mc.player.getXRot();
            this.cameraYaw = this.anchorYaw;
            this.cameraPitch = this.anchorPitch;
            this.active = true;
        }
    }

    @Override
    protected void onDisable() {
        this.active = false;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null) {
            setEnabled(false);
            return;
        }
        if (!this.active) {
            onEnable();
        }
    }

    public boolean interceptLookDelta(double deltaYaw, double deltaPitch, float baseYaw, float basePitch) {
        if (!isEnabled() || !this.active) {
            return false;
        }
        float maxYaw = this.yawLimitDeg.getValue().floatValue();
        float maxPitch = this.pitchLimitDeg.getValue().floatValue();
        float alpha = 1.0f - this.smoothing.getValue().floatValue();

        float targetYaw = this.cameraYaw + (float) deltaYaw;
        float deltaFromAnchor = Mth.wrapDegrees(targetYaw - baseYaw);
        deltaFromAnchor = Mth.clamp(deltaFromAnchor, -maxYaw, maxYaw);
        float clampedTargetYaw = baseYaw + deltaFromAnchor;

        float targetPitch = Mth.clamp(this.cameraPitch + (float) deltaPitch, -maxPitch, maxPitch);

        this.cameraYaw = Mth.lerp(alpha, this.cameraYaw, clampedTargetYaw);
        this.cameraPitch = Mth.lerp(alpha, this.cameraPitch, targetPitch);
        return true;
    }

    public boolean isOverridingCamera() {
        return isEnabled() && this.active;
    }

    public float getCameraYaw() { return cameraYaw; }
    public float getCameraPitch() { return cameraPitch; }
}
