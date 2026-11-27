package dev.rebase.viewgrid.module.impl;

import dev.rebase.viewgrid.module.Category;
import dev.rebase.viewgrid.module.Module;
import dev.rebase.viewgrid.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class FreeCamModule extends Module {
    private final Setting<Double> horizontalSpeed = registerSetting(
        Setting.rangedDouble("horizontal_speed", "Move Speed", 1.1, 0.1, 5.0)
    );
    private final Setting<Double> verticalSpeed = registerSetting(
        Setting.rangedDouble("vertical_speed", "Vertical Speed", 0.75, 0.1, 4.0)
    );
    private final Setting<Boolean> noclipCamera = registerSetting(
        Setting.bool("noclip_camera", "Noclip Camera", true)
    );

    private Vec3 camPos = Vec3.ZERO;
    private float camYaw = 0.0f;
    private float camPitch = 0.0f;
    private boolean initialized = false;

    public FreeCamModule() {
        super(
            "free_cam",
            "Free Cam",
            "Detaches a free-moving client camera without moving the player or sending movement packets.",
            Category.CAMERA
        );
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            this.camPos = mc.player.getEyePosition(1.0f);
            this.camYaw = mc.player.getYRot();
            this.camPitch = mc.player.getXRot();
            this.initialized = true;
        }
    }

    @Override
    protected void onDisable() {
        this.initialized = false;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            setEnabled(false);
            return;
        }
        if (!this.initialized) {
            onEnable();
            return;
        }
        if (client.screen != null) {
            return;
        }

        double forwardInput = 0.0;
        double strafeInput = 0.0;
        double verticalInput = 0.0;

        if (client.options.keyUp.isDown()) forwardInput += 1.0;
        if (client.options.keyDown.isDown()) forwardInput -= 1.0;
        if (client.options.keyRight.isDown()) strafeInput += 1.0;
        if (client.options.keyLeft.isDown()) strafeInput -= 1.0;
        if (client.options.keyJump.isDown()) verticalInput += 1.0;
        if (client.options.keyShift.isDown()) verticalInput -= 1.0;

        double hSpeed = this.horizontalSpeed.getValue();
        double vSpeed = this.verticalSpeed.getValue();
        if (client.options.keySprint.isDown()) {
            hSpeed *= 1.8;
            vSpeed *= 1.6;
        }

        double radYaw = Math.toRadians(this.camYaw);
        double lookX = -Math.sin(radYaw);
        double lookZ = Math.cos(radYaw);
        double rightX = Math.cos(radYaw);
        double rightZ = Math.sin(radYaw);

        double dx = (lookX * forwardInput + rightX * strafeInput) * hSpeed;
        double dz = (lookZ * forwardInput + rightZ * strafeInput) * hSpeed;
        double dy = verticalInput * vSpeed;

        Vec3 next = this.camPos.add(dx, dy, dz);
        if (!Boolean.TRUE.equals(this.noclipCamera.getValue())) {
            next = clampToNonSolid(client, this.camPos, next);
        }
        this.camPos = next;
    }

    private Vec3 clampToNonSolid(Minecraft client, Vec3 current, Vec3 desired) {
        if (client.level == null) return current;
        net.minecraft.core.BlockPos targetBlock = net.minecraft.core.BlockPos.containing(desired.x, desired.y, desired.z);
        var state = client.level.getBlockState(targetBlock);
        if (state.isAir() || !state.blocksMotion()) {
            return desired;
        }
        return current;
    }

    public void adjustLook(double deltaYaw, double deltaPitch) {
        if (!isEnabled() || !this.initialized) return;
        this.camYaw += (float) deltaYaw;
        this.camPitch = net.minecraft.util.Mth.clamp(this.camPitch + (float) deltaPitch, -90.0f, 90.0f);
    }

    public boolean isControllingCamera() {
        return isEnabled() && this.initialized;
    }

    public Vec3 getCamPos() { return camPos; }
    public float getCamYaw() { return camYaw; }
    public float getCamPitch() { return camPitch; }
}
