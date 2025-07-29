package com.harrisfauntleroy.roomcull.mixins;

import com.harrisfauntleroy.roomcull.RoomManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    
    /**
     * Hook into level rendering to add debug output and test mixin functionality
     */
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void onRenderLevel(net.minecraft.client.DeltaTracker deltaTracker, boolean renderBlockOutline,
                              Camera camera, net.minecraft.client.renderer.GameRenderer gameRenderer,
                              net.minecraft.client.renderer.LightTexture lightTexture,
                              org.joml.Matrix4f projectionMatrix, org.joml.Matrix4f frustrumMatrix,
                              CallbackInfo ci) {
        
        // Simple debug to confirm mixin is working
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getGameTime() % 100 == 0) {
            System.out.println("LevelRenderer mixin: renderLevel called, rooms active: " + 
                             RoomManager.getActiveRoomCount());
        }
    }
}