package com.harrisfauntleroy.roomcull.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection")
public class ChunkRenderDispatcherMixin {

    /** Hook into render section creation for debug purposes. */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onRenderSectionInit(final CallbackInfo ci) {
        // Simple debug logging
        System.out.println("RenderSection created - occlusion system active");
    }
}
