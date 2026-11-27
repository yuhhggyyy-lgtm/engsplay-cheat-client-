package dev.engsplay.mixin;

import dev.engsplay.EngsplayClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void engsplay$renderHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (EngsplayClient.getInstance() != null) {
            EngsplayClient.getInstance().getHudRenderer().render(context, tickCounter.getTickDelta(true), false);
        }
    }
}
