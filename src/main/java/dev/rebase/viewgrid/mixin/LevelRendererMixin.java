package dev.rebase.viewgrid.mixin;

import dev.rebase.viewgrid.ViewGridMod;
import dev.rebase.viewgrid.module.impl.CustomSkyModule;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// UNVERIFIED: 1.21.11 LevelRenderer sky & world-overlay render method names and parameters.
// Note on shaders (e.g., Iris): when a shader pack is active and CustomSkyModule.respectShaders is true,
// skip custom sky override so shader sky/fog state is not clobbered.
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void viewgrid$onRenderSky(CallbackInfo ci) {
        ViewGridMod mod = ViewGridMod.getInstance();
        if (mod == null || mod.getModuleManager() == null) {
            return;
        }
        CustomSkyModule customSky = mod.getModuleManager().getCustomSky();
        boolean shaderActive = false; // Replace with shader-active check if Iris/shader API is present.
        if (customSky.shouldRenderCustomSky(shaderActive)) {
            // Render selected SkyMode (BOREALIS, NEBULA, CRIMSON, FROST) and restore render/fog state cleanly.
            // UNVERIFIED: Exact 1.21.11 render command / vertex buffer submission path for sky & world ESP boxes.
        }
    }
}
