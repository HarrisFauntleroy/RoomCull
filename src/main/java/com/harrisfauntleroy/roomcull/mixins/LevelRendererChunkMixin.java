package com.harrisfauntleroy.roomcull.mixins;

import com.harrisfauntleroy.roomcull.RoomManager;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class LevelRendererChunkMixin {
    
    /**
     * Hook into the method that determines if a render section should be rendered
     * This targets the core visibility check in chunk rendering
     */
    @Inject(method = "lambda$renderLevel$6", at = @At("HEAD"), cancellable = true)
    private void onRenderSectionVisibilityCheck(SectionRenderDispatcher.RenderSection renderSection, 
                                               CallbackInfoReturnable<Boolean> cir) {
        
        // Get the section position
        BlockPos sectionPos = renderSection.getOrigin();
        
        // Check if this section should be occluded
        if (RoomManager.isChunkOccluded(sectionPos)) {
            System.out.println("Occlusion system: Hiding chunk at " + sectionPos);
            cir.setReturnValue(false); // Don't render this section
        }
    }
}