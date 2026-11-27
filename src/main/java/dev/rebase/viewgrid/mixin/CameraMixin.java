package dev.rebase.viewgrid.mixin;

import dev.rebase.viewgrid.ViewGridMod;
import dev.rebase.viewgrid.module.impl.FreeCamModule;
import dev.rebase.viewgrid.module.impl.FreeLookModule;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// UNVERIFIED: Exact parameter list and method descriptor of Camera.setup in 1.21.11 official Mojang mappings.
// Confirm Camera's setup(...) method signature in the decompiled 1.21.11 jar before compiling.
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "setup", at = @At("TAIL"))
    private void viewgrid$onCameraSetup(CallbackInfo ci) {
        ViewGridMod mod = ViewGridMod.getInstance();
        if (mod == null || mod.getModuleManager() == null) {
            return;
        }
        FreeCamModule freeCam = mod.getModuleManager().getFreeCam();
        FreeLookModule freeLook = mod.getModuleManager().getFreeLook();

        if (freeCam.isControllingCamera()) {
            // Apply detached camera position/rotation on Camera instance after verifying setter/field names.
            var pos = freeCam.getCamPos();
            if (pos != null) {
                // TODO (UNVERIFIED): Set camera position & rotation via verified Camera methods/accessors.
            }
        } else if (freeLook.isOverridingCamera()) {
            // TODO (UNVERIFIED): Apply freeLook.getCameraYaw() / getCameraPitch() on Camera instance.
        }
    }
}
