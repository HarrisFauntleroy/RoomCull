package com.harrisfauntleroy.roomcull;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Renderer for the Room Block Entity that displays the room boundaries.
 *
 * <p>This renderer displays:
 *
 * <ul>
 *   <li>The lodestone block model at the room block position
 *   <li>A wireframe outline showing the detected room boundaries
 *   <li>Visual indicators for the room detection beams
 * </ul>
 *
 * The rendering is designed to be visible from any angle and distance to help players understand
 * the room boundaries that have been detected.
 *
 * @author Harris Fauntleroy
 * @since 1.0.0
 */
public class RoomBlockEntityRenderer implements BlockEntityRenderer<RoomBlockEntity> {

    /** Maximum view distance for room boundary rendering. */
    private static final int MAX_VIEW_DISTANCE = 256;

    /**
     * Creates a new room block entity renderer.
     *
     * @param context the renderer context
     */
    public RoomBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
        // Constructor for renderer
    }

    /**
     * Renders the room block entity including the lodestone model and room boundaries.
     *
     * @param blockEntity the room block entity to render
     * @param partialTick partial tick time for smooth animation
     * @param poseStack the pose stack for transformations
     * @param bufferSource the buffer source for rendering
     * @param packedLight the packed light value
     * @param packedOverlay the packed overlay value
     */
    @Override
    public void render(
            final RoomBlockEntity blockEntity,
            final float partialTick,
            final PoseStack poseStack,
            final MultiBufferSource bufferSource,
            final int packedLight,
            final int packedOverlay) {

        // First render the lodestone block model
        poseStack.pushPose();

        // Get the minecraft instance and block render dispatcher
        Minecraft minecraft = Minecraft.getInstance();
        BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();

        // Render lodestone block state
        BlockState lodestoneState = Blocks.LODESTONE.defaultBlockState();
        blockRenderer.renderSingleBlock(
                lodestoneState,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                ModelData.EMPTY,
                RenderType.solid());

        BlockPos pos = blockEntity.getBlockPos();

        // Render the room boundaries if they exist
        AABB roomBounds = blockEntity.getOcclusionBounds();
        if (roomBounds != null) {
            renderRoomBoundaries(poseStack, bufferSource, roomBounds, pos, packedLight);
        }

        poseStack.popPose();
    }

    /**
     * Renders the room boundary wireframe outline.
     *
     * @param poseStack the pose stack for transformations
     * @param bufferSource the buffer source for rendering
     * @param roomBounds the room boundaries to render
     * @param blockPos the position of the room block
     * @param packedLight the packed light value
     */
    private void renderRoomBoundaries(
            final PoseStack poseStack,
            final MultiBufferSource bufferSource,
            final AABB roomBounds,
            final BlockPos blockPos,
            final int packedLight) {

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());

        poseStack.pushPose();

        // Translate to the room bounds relative to the block position
        double offsetX = roomBounds.minX - blockPos.getX();
        double offsetY = roomBounds.minY - blockPos.getY();
        double offsetZ = roomBounds.minZ - blockPos.getZ();

        poseStack.translate(offsetX, offsetY, offsetZ);

        // Calculate the size of the room bounds
        double sizeX = roomBounds.maxX - roomBounds.minX;
        double sizeY = roomBounds.maxY - roomBounds.minY;
        double sizeZ = roomBounds.maxZ - roomBounds.minZ;

        // Create AABB for the room boundaries
        AABB renderBounds = new AABB(0, 0, 0, sizeX, sizeY, sizeZ);

        // Render the wireframe outline in bright red color
        LevelRenderer.renderLineBox(
                poseStack, vertexConsumer, renderBounds, 1.0f, 0.0f, 0.0f, 1.0f); // Bright red

        poseStack.popPose();
    }

    /**
     * Gets the render distance for this renderer. Room boundaries should be visible from far away.
     *
     * @return the render distance in blocks
     */
    @Override
    public int getViewDistance() {
        return MAX_VIEW_DISTANCE; // Visible from very far away
    }

    /**
     * Determines if this renderer should render even when the block is not directly visible. We
     * want the room boundaries to always be visible when the room block is loaded.
     *
     * @param blockEntity the block entity
     * @return true to always render when in range
     */
    @Override
    public boolean shouldRenderOffScreen(final RoomBlockEntity blockEntity) {
        return true; // Always render room boundaries when in range
    }
}
