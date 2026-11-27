package dev.rebase.viewgrid.module.impl;

import dev.rebase.viewgrid.module.Category;
import dev.rebase.viewgrid.module.Module;
import dev.rebase.viewgrid.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class FreeCamModule extends Module {
    private final Setting<Double> moveSpeed = addSetting(
        Setting.sliderDouble("move_speed", "Horizontal Speed", 1.1, 0.1, 5.0)
    );
    private final Setting<Double> climbSpeed = addSetting(
        Setting.sliderDouble("climb_speed", "Vertical Speed", 0.75, 0.1, 4.0)
    );
    private final Setting<Boolean> noclip = addSetting(
        Setting.toggle("noclip", "Noclip Camera", true)
    );

    private Vec3 pos = Vec3.ZERO;
    private float yaw = 0.0f;
    private float pitch = 0.0f;
    private boolean ready = false;

    public FreeCamModule() {
        super(
            "free_cam",
            "Free Cam",
            "Moves a detached client camera without moving the player or sending movement packets.",
            Category.CAMERA
        );
    }

    @Override
    protected void onActivate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            this.pos = mc.player.getEyePosition(1.0f);
            this.yaw = mc.player.getYRot();
            this.pitch = mc.player.getXRot();
            this.ready = true;
        }
    }

    @Override
    protected void onDeactivate() {
        this.ready = false;
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            setEnabled(false);
            return;
        }
        if (!this.ready) {
            onActivate();
            return;
        }
        if (mc.screen != null) return;

        double fwd = 0.0;
        double right = 0.0;
        double vert = 0.0;

        if (mc.options.keyUp.isDown()) fwd += 1.0;
        if (mc.options.keyDown.isDown()) fwd -= 1.0;
        if (mc.options.keyRight.isDown()) right += 1.0;
        if (mc.options.keyLeft.isDown()) right -= 1.0;
        if (mc.options.keyJump.isDown()) vert += 1.0;
        if (mc.options.keyShift.isDown()) vert -= 1.0;

        double hSpd = this.moveSpeed.get();
        double vSpd = this.climbSpeed.get();
        if (mc.options.keySprint.isDown()) {
            hSpd *= 1.8;
            vSpd *= 1.6;
        }

        double rad = Math.toRadians(this.yaw);
        double fx = -Math.sin(rad);
        double fz = Math.cos(rad);
        double rx = Math.cos(rad);
        double rz = Math.sin(rad);

        Vec3 candidate = this.pos.add(
            (fx * fwd + rx * right) * hSpd,
            vert * vSpd,
            (fz * fwd + rz * right) * hSpd
        );

        if (!Boolean.TRUE.equals(this.noclip.get())) {
            BlockPos bp = BlockPos.containing(candidate.x, candidate.y, candidate.z);
            var st = mc.level.getBlockState(bp);
            if (!st.isAir() && st.blocksMotion()) {
                return;
            }
        }
        this.pos = candidate;
    }

    public void turn(double dyaw, double dpitch) {
        if (!isEnabled() || !this.ready) return;
        this.yaw += (float) dyaw;
        this.pitch = Mth.clamp(this.pitch + (float) dpitch, -90.0f, 90.0f);
    }

    public boolean isReady() { return isEnabled() && this.ready; }
    public Vec3 getPos() { return pos; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
}
