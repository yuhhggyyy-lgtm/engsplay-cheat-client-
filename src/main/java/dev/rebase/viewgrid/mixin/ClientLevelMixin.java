package dev.rebase.viewgrid.mixin;

import dev.rebase.viewgrid.ViewGridMod;
import dev.rebase.viewgrid.module.impl.BlockEspModule;
import dev.rebase.viewgrid.module.impl.ChunkFinderModule;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// UNVERIFIED: Exact method signatures for block state updates and chunk load/unload on ClientLevel in 1.21.11.
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "setBlock", at = @At("RETURN"))
    private void viewgrid$onSetBlock(BlockPos pos, BlockState state, int flags, int recursionLeft, CallbackInfo ci) {
        ViewGridMod mod = ViewGridMod.getInstance();
        if (mod == null || mod.getModuleManager() == null) {
            return;
        }
        BlockEspModule blockEsp = mod.getModuleManager().getBlockEsp();
        if (blockEsp.isEnabled()) {
            blockEsp.onBlockChanged(pos, state);
        }
    }

    @Inject(method = "unload", at = @At("HEAD"))
    private void viewgrid$onChunkUnload(CallbackInfo ci) {
        ViewGridMod mod = ViewGridMod.getInstance();
        if (mod == null || mod.getModuleManager() == null) {
            return;
        }
        ChunkFinderModule chunkFinder = mod.getModuleManager().getChunkFinder();
        BlockEspModule blockEsp = mod.getModuleManager().getBlockEsp();
        // Wire verified chunk position parameter once confirmed on ClientLevel in 1.21.11.
        long dummyKey = 0L;
        chunkFinder.onChunkLoaded(dummyKey);
        blockEsp.onChunkUnloaded(dummyKey);
    }
}
