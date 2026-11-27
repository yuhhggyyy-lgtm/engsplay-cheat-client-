package dev.engsplay.mixin;

import dev.engsplay.EngsplayClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void engsplay$onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (action == GLFW.GLFW_PRESS && EngsplayClient.getInstance() != null) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                EngsplayClient.getInstance().getStatsTracker().recordLeftClick();
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                EngsplayClient.getInstance().getStatsTracker().recordRightClick();
            }
        }
    }
}
