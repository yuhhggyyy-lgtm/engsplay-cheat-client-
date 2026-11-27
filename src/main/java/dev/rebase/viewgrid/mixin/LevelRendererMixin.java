package dev.rebase.viewgrid.mixin;

import dev.rebase.viewgrid.ViewGridMod;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// UNVERIFIED: 1.21.11 LevelRenderer sky & world overlay render method names and parameters.
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void viewgrid$beforeSky(CallbackInfo ci) {
        ViewGridMod mod = ViewGridMod.get();
        if (mod == null || mod.getModuleManager() == null) return;
        var sky = mod.getModuleManager().getCustomSky();
        boolean shadersActive = false;
        if (sky.canApplySky(shadersActive)) {
            // Render Borealis/Nebula/Crimson/Frost sky and restore render/fog state cleanly once verified.
        }
    }
}
