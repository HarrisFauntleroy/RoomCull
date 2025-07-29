package com.harrisfauntleroy.roomcull;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Matrix4f;

public class RoomBlockEntityRenderer implements BlockEntityRenderer<RoomBlockEntity> {
    
    public RoomBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        // Constructor required by the renderer system
    }
    
    @Override
    public void render(RoomBlockEntity blockEntity, float partialTick, PoseStack poseStack, 
                      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        
        // Get the vertex consumer for translucent rendering
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.translucent());
        
        poseStack.pushPose();
        
        // Move to block center
        poseStack.translate(0.5, 0.5, 0.5);
        
        Matrix4f matrix = poseStack.last().pose();
        
        // Render a simple glowing cube in the center (0.4x0.4x0.4)
        float size = 0.2f;
        
        // Purple color with transparency
        float red = 0.6f;
        float green = 0.2f; 
        float blue = 0.8f;
        float alpha = 0.8f;
        
        // Render all 6 faces of the cube
        renderCubeFace(vertexConsumer, matrix, -size, -size, -size, size, size, -size, red, green, blue, alpha, packedLight); // Front
        renderCubeFace(vertexConsumer, matrix, size, -size, size, -size, size, size, red, green, blue, alpha, packedLight); // Back
        renderCubeFace(vertexConsumer, matrix, -size, -size, size, -size, size, -size, red, green, blue, alpha, packedLight); // Left
        renderCubeFace(vertexConsumer, matrix, size, -size, -size, size, size, size, red, green, blue, alpha, packedLight); // Right
        renderCubeFace(vertexConsumer, matrix, -size, size, -size, size, size, size, red, green, blue, alpha, packedLight); // Top
        renderCubeFace(vertexConsumer, matrix, -size, -size, size, size, -size, -size, red, green, blue, alpha, packedLight); // Bottom
        
        poseStack.popPose();
    }
    
    /**
     * Render a single face of a cube
     */
    private void renderCubeFace(VertexConsumer consumer, Matrix4f matrix, 
                               float x1, float y1, float z1, float x2, float y2, float z2,
                               float red, float green, float blue, float alpha, int packedLight) {
        
        // Calculate normal vector (simplified - just use face direction)
        float normalX = 0;
        float normalY = 0; 
        float normalZ = 1;
        
        // Add vertices for the quad (two triangles)
        consumer.addVertex(matrix, x1, y1, z1).setColor(red, green, blue, alpha).setUv(0, 0).setOverlay(0).setLight(packedLight).setNormal(normalX, normalY, normalZ);
        consumer.addVertex(matrix, x2, y1, z1).setColor(red, green, blue, alpha).setUv(1, 0).setOverlay(0).setLight(packedLight).setNormal(normalX, normalY, normalZ);
        consumer.addVertex(matrix, x2, y2, z2).setColor(red, green, blue, alpha).setUv(1, 1).setOverlay(0).setLight(packedLight).setNormal(normalX, normalY, normalZ);
        consumer.addVertex(matrix, x1, y2, z2).setColor(red, green, blue, alpha).setUv(0, 1).setOverlay(0).setLight(packedLight).setNormal(normalX, normalY, normalZ);
    }
}