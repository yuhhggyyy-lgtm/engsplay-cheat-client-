package dev.rebase.viewgrid.mixin;

import dev.rebase.viewgrid.ViewGridMod;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// UNVERIFIED: Exact 1.21.11 official Mojang descriptor for Camera.setup and camera position/angle setters.
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "setup", at = @At("TAIL"))
    private void viewgrid$afterSetup(CallbackInfo ci) {
        ViewGridMod mod = ViewGridMod.get();
        if (mod == null || mod.getModuleManager() == null) return;
        var freeCam = mod.getModuleManager().getFreeCam();
        var freeLook = mod.getModuleManager().getFreeLook();
        if (freeCam.isReady()) {
            // Apply freeCam.getPos(), freeCam.getYaw(), freeCam.getPitch() once Camera setters are verified.
        } else if (freeLook.isEngaged()) {
            // Apply freeLook.getYaw(), freeLook.getPitch() once Camera rotation setters are verified.
        }
    }
}
