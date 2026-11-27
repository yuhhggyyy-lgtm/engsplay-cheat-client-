package dev.rebase.viewgrid.mixin;

import dev.rebase.viewgrid.ViewGridMod;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// UNVERIFIED: Exact 1.21.11 ClientLevel block-change and chunk load/unload method signatures.
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "setBlock", at = @At("RETURN"))
    private void viewgrid$afterSetBlock(BlockPos pos, BlockState state, int flags, int recursionLeft, CallbackInfo ci) {
        ViewGridMod mod = ViewGridMod.get();
        if (mod == null || mod.getModuleManager() == null) return;
        var blockEsp = mod.getModuleManager().getBlockEsp();
        if (blockEsp.isEnabled()) {
            blockEsp.notifyBlockChange(pos, state);
        }
    }

    @Inject(method = "unload", at = @At("HEAD"))
    private void viewgrid$onUnload(CallbackInfo ci) {
        ViewGridMod mod = ViewGridMod.get();
        if (mod == null || mod.getModuleManager() == null) return;
        // Wire verified chunk position parameter once confirmed on ClientLevel in 1.21.11.
    }
}
