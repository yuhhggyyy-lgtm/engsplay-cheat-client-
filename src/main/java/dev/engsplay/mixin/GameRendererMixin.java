package dev.engsplay.mixin;

import dev.engsplay.EngsplayClient;
import dev.engsplay.module.Module;
import dev.engsplay.module.Setting;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void engsplay$zoomFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        if (EngsplayClient.getInstance() == null) return;
        Module zoom = EngsplayClient.getInstance().getModuleManager().getById("zoom");
        if (zoom != null && zoom.isEnabled()) {
            for (Setting<?> s : zoom.getSettings()) {
                if ("fov".equals(s.getId()) && s instanceof Setting.SliderSetting ss) {
                    cir.setReturnValue((double) ss.get());
                    break;
                }
            }
        }
    }
}
