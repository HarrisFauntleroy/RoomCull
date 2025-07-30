package com.harrisfauntleroy.roomcull;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A specialized block that detects and manages room boundaries for occlusion culling.
 *
 * <p>The Room Block is the core component of the room culling system. When placed in a world, it
 * automatically detects the boundaries of the room it's placed in by scanning for walls, floor, and
 * ceiling. It then enables occlusion culling, hiding objects outside the room when a player is
 * inside.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Automatic room boundary detection
 *   <li>Small collision box for easy placement and interaction
 *   <li>Glowing appearance for easy identification
 *   <li>Non-occluding to avoid blocking light or visibility
 *   <li>Custom rendering via {@link RoomBlockEntity}
 * </ul>
 *
 * <p>The block is designed to be minimally intrusive - it has a small collision box, doesn't block
 * light, and allows skylight transmission. It's primarily a functional block rather than a
 * decorative one.
 *
 * @see RoomBlockEntity
 * @see RoomManager
 * @author Harris Fauntleroy
 * @since 1.0.0
 */
public final class RoomBlock extends BaseEntityBlock {

    /** Codec for serialization/deserialization of this block. */
    public static final MapCodec<RoomBlock> CODEC = simpleCodec(properties -> new RoomBlock());

    /** Full block shape for collision and selection. */
    private static final VoxelShape SHAPE = Shapes.block();

    /** Block strength values matching lodestone. */
    private static final float BLOCK_STRENGTH = 3.5F;

    /**
     * Creates a new Room Block with lodestone-like properties.
     *
     * <p>The block is configured with:
     *
     * <ul>
     *   <li>Stone gray color like lodestone
     *   <li>Strong strength matching lodestone (3.5F hardness, 3.5F resistance)
     *   <li>No occlusion to avoid blocking visibility
     *   <li>Lodestone appearance without light emission
     * </ul>
     */
    public RoomBlock() {
        super(
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.STONE)
                        .strength(BLOCK_STRENGTH, BLOCK_STRENGTH)
                        .noOcclusion()
                        .requiresCorrectToolForDrops());
    }

    /**
     * Specifies that this block uses custom entity-based rendering.
     *
     * @param state the block state
     * @return {@link RenderShape#ENTITYBLOCK_ANIMATED} to enable custom rendering
     */
    @Override
    public RenderShape getRenderShape(final BlockState state) {
        return RenderShape
                .ENTITYBLOCK_ANIMATED; // Custom rendering for bounding box + lodestone model
    }

    /**
     * Gets the selection box shape for this block.
     *
     * @param state the block state
     * @param level the block getter (world)
     * @param pos the position
     * @param context the collision context
     * @return a full block voxel shape for normal interaction
     */
    @Override
    public VoxelShape getShape(
            final BlockState state,
            final BlockGetter level,
            final BlockPos pos,
            final CollisionContext context) {
        return SHAPE; // Full block selection box
    }

    /**
     * Gets the collision shape for this block.
     *
     * @param state the block state
     * @param level the block getter (world)
     * @param pos the position
     * @param context the collision context
     * @return a full block voxel shape for normal collision
     */
    @Override
    public VoxelShape getCollisionShape(
            final BlockState state,
            final BlockGetter level,
            final BlockPos pos,
            final CollisionContext context) {
        return SHAPE; // Full block collision
    }

    @Override
    public boolean skipRendering(
            final BlockState state,
            final BlockState adjacentBlockState,
            final net.minecraft.core.Direction direction) {
        return false; // Always render the room block
    }

    @Override
    public float getShadeBrightness(
            final BlockState state, final BlockGetter level, final BlockPos pos) {
        return 1.0F; // Don't affect lighting
    }

    @Override
    public boolean propagatesSkylightDown(
            final BlockState state, final BlockGetter level, final BlockPos pos) {
        return true; // Allow skylight through
    }

    @Override
    public int getLightBlock(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return 0; // Don't block light transmission
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /**
     * Creates a new {@link RoomBlockEntity} for this block.
     *
     * @param pos the position where the block entity should be created
     * @param state the block state
     * @return a new room block entity instance
     */
    @Nullable @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new RoomBlockEntity(pos, state);
    }

    /**
     * Provides a ticker for the room block entity on the client side.
     *
     * <p>The ticker handles periodic room scanning and particle effects. Only operates on the
     * client side to avoid unnecessary server processing.
     *
     * @param level the world level
     * @param state the block state
     * @param blockEntityType the block entity type
     * @return a ticker for client-side operations, null on server side
     */
    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> blockEntityType) {
        return level.isClientSide
                ? createTickerHelper(
                        blockEntityType, RoomCullMod.ROOM_BLOCK_ENTITY.get(), RoomBlockEntity::tick)
                : null;
    }
}
