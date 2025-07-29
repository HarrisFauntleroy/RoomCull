package com.harrisfauntleroy.roomcull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

/**
 * Block entity that manages room boundary detection and occlusion culling.
 *
 * <p>This block entity automatically scans for solid blocks in all six cardinal directions to
 * determine the boundaries of a room. It then creates an occlusion bounding box that can be used to
 * determine what should be rendered when a viewer is inside the room.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Automatic wall detection in all six directions (N/S/E/W/Up/Down)
 *   <li>Periodic rescanning to handle room modifications
 *   <li>Visual particle effects showing room boundaries
 *   <li>Integration with {@link RoomManager} for global occlusion management
 * </ul>
 *
 * <p>The room boundaries are detected by scanning outward from the room block until a solid, opaque
 * block is found. The occlusion bounds are then calculated with a small inward offset to ensure
 * culling occurs inside walls rather than outside.
 *
 * @see RoomManager
 * @see RoomBlock
 * @author Harris Fauntleroy
 * @since 1.0.0
 */
public class RoomBlockEntity extends BlockEntity {

    /** Distance from room block to north wall (negative Z direction). */
    private int northWall = 0;

    /** Distance from room block to south wall (positive Z direction). */
    private int southWall = 0;

    /** Distance from room block to east wall (positive X direction). */
    private int eastWall = 0;

    /** Distance from room block to west wall (negative X direction). */
    private int westWall = 0;

    /** Distance from room block to ceiling (positive Y direction). */
    private int upWall = 0;

    /** Distance from room block to floor (negative Y direction). */
    private int downWall = 0;

    /** The calculated occlusion bounding box for this room. */
    private AABB occlusionBounds;

    /** Timer for periodic room rescanning. */
    private int rescanTimer = 0;

    /** How often to rescan room boundaries (in ticks, ~8 seconds). */
    private static final int RESCAN_INTERVAL = 160;

    /** Maximum distance for room boundary scanning. */
    private static final int MAX_SCAN_DISTANCE = 64;

    /** Inward offset for occlusion bounds to keep culling inside walls. */
    private static final double WALL_OFFSET = 0.1;

    /** Interval for particle effects in ticks. */
    private static final int PARTICLE_INTERVAL = 40;

    /** Particle spacing for beam effects. */
    private static final int PARTICLE_SPACING = 2;

    /** Block center offset for particle positioning. */
    private static final double BLOCK_CENTER_OFFSET = 0.5;

    /**
     * Creates a new room block entity at the specified position.
     *
     * @param pos the world position of this block entity
     * @param blockState the block state of the associated room block
     */
    public RoomBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(RoomCullMod.ROOM_BLOCK_ENTITY.get(), pos, blockState);
    }

    /**
     * Called when this block entity is loaded into the world.
     *
     * <p>On the client side, this triggers an initial room scan and registers this room with the
     * {@link RoomManager} for occlusion calculations.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            scanRoom();
            RoomManager.addRoom(this);
        }
    }

    /**
     * Called when this block entity is being removed from the world.
     *
     * <p>On the client side, this unregisters the room from the {@link RoomManager} to prevent
     * memory leaks and stale occlusion calculations.
     */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            RoomManager.removeRoom(this);
        }
    }

    /**
     * Scans in all six cardinal directions to detect room boundaries.
     *
     * <p>This method performs a comprehensive scan from the room block's position, looking for
     * solid, opaque blocks that define the room's walls, floor, and ceiling. The scan extends up to
     * 64 blocks in each direction to handle large rooms. After scanning, it updates the occlusion
     * bounds for this room.
     */
    private void scanRoom() {
        if (level == null) {
            return;
        }

        BlockPos center = getBlockPos();

        // Scan North (negative Z)
        northWall = scanDirection(center, Direction.NORTH, MAX_SCAN_DISTANCE);

        // Scan South (positive Z)
        southWall = scanDirection(center, Direction.SOUTH, MAX_SCAN_DISTANCE);

        // Scan East (positive X)
        eastWall = scanDirection(center, Direction.EAST, MAX_SCAN_DISTANCE);

        // Scan West (negative X)
        westWall = scanDirection(center, Direction.WEST, MAX_SCAN_DISTANCE);

        // Scan Up (positive Y)
        upWall = scanDirection(center, Direction.UP, MAX_SCAN_DISTANCE);

        // Scan Down (negative Y)
        downWall = scanDirection(center, Direction.DOWN, MAX_SCAN_DISTANCE);

        updateOcclusionBounds();
    }

    /**
     * Scans in a specific direction from the center position until a solid block is found.
     *
     * <p>This method moves block by block in the specified direction, checking each block to see if
     * it's solid and opaque (suitable as a room boundary). The scan stops when a suitable wall
     * block is found or the maximum distance is reached.
     *
     * @param center the starting position for the scan
     * @param direction the direction to scan in
     * @param maxDistance the maximum number of blocks to scan
     * @return the distance to the nearest wall block, or maxDistance if no wall found
     */
    private int scanDirection(
            final BlockPos center, final Direction direction, final int maxDistance) {
        BlockPos.MutableBlockPos pos = center.mutable();

        for (int i = 1; i <= maxDistance; i++) {
            pos.move(direction);
            BlockState blockState = level.getBlockState(pos);

            // If we hit a solid block that can act as a wall, this is our boundary
            if (!blockState.isAir() && isValidWallBlock(blockState, pos)) {
                return i;
            }
        }

        // If we didn't find a wall within max distance, use max distance
        return maxDistance;
    }

    /**
     * Determines if a block can act as a valid wall for room boundary detection.
     *
     * <p>A block is considered a valid wall if it:
     *
     * <ul>
     *   <li>Has collision (blocks player movement)
     *   <li>Is solid render (opaque blocks) OR transparent but solid (glass, etc.)
     *   <li>Is not a liquid or replaceable block
     * </ul>
     *
     * @param blockState the block state to check
     * @param pos the position of the block
     * @return true if the block can serve as a room wall
     */
    private boolean isValidWallBlock(final BlockState blockState, final BlockPos pos) {
        if (level == null) {
            return false;
        }

        // Check if block has collision shape (blocks movement)
        if (blockState.getCollisionShape(level, pos).isEmpty()) {
            return false;
        }

        // Accept solid render blocks (stone, wood, etc.)
        if (blockState.isSolidRender(level, pos)) {
            return true;
        }

        // Accept transparent but solid blocks (glass, iron bars, etc.)
        // These have collision but aren't solid render - exclude liquids and replaceable blocks
        return blockState.getFluidState().isEmpty() && !blockState.canBeReplaced();
    }

    /**
     * Updates the occlusion bounding box based on the detected room boundaries.
     *
     * <p>This method calculates the precise room bounds using the wall distances detected during
     * scanning. A small inward offset (0.1 blocks) is applied to ensure that occlusion culling
     * occurs inside the walls rather than outside, preventing objects from being incorrectly culled
     * when they're against the room walls.
     *
     * <p>The method also outputs debug information about the detected room dimensions to help with
     * troubleshooting room boundary detection.
     */
    private void updateOcclusionBounds() {
        BlockPos center = getBlockPos();

        // Room bounds with inward offset to keep culling inside walls
        double minX = center.getX() - westWall + WALL_OFFSET;
        double maxX = center.getX() + eastWall - WALL_OFFSET;
        double minY = center.getY() - downWall + WALL_OFFSET;
        double maxY = center.getY() + upWall - WALL_OFFSET;
        double minZ = center.getZ() - northWall + WALL_OFFSET;
        double maxZ = center.getZ() + southWall - WALL_OFFSET;

        occlusionBounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

        // Debug output only when debug mode is enabled
        if (Config.isDebugMode()) {
            System.out.println(
                    "Room at "
                            + center
                            + " detected walls: N="
                            + northWall
                            + " S="
                            + southWall
                            + " E="
                            + eastWall
                            + " W="
                            + westWall
                            + " U="
                            + upWall
                            + " D="
                            + downWall);
            System.out.println(
                    "Room bounds: "
                            + String.format(
                                    "X:[%.1f-%.1f] Y:[%.1f-%.1f] Z:[%.1f-%.1f]",
                                    minX, maxX, minY, maxY, minZ, maxZ));
        }
    }

    /**
     * Gets the calculated occlusion bounding box for this room.
     *
     * @return the {@link AABB} representing the room's boundaries, or null if not yet calculated
     */
    public AABB getOcclusionBounds() {
        return occlusionBounds;
    }

    /**
     * Determines if a world position is outside this room's boundaries.
     *
     * <p>This method is used by the occlusion system to determine whether objects at the given
     * position should be culled when a viewer is inside this room.
     *
     * @param x the X coordinate to check
     * @param y the Y coordinate to check
     * @param z the Z coordinate to check
     * @return true if the position is outside the room bounds (should be occluded)
     */
    public boolean isPositionOutsideRoom(final double x, final double y, final double z) {
        if (occlusionBounds == null) {
            return false;
        }

        // If the position is outside our room bounds, it should be occluded
        return !occlusionBounds.contains(x, y, z);
    }

    /**
     * Gets the distance to the north wall (negative Z direction).
     *
     * @return distance in blocks to the north wall
     */
    public int getNorthWall() {
        return northWall;
    }

    /**
     * Gets the distance to the south wall (positive Z direction).
     *
     * @return distance in blocks to the south wall
     */
    public int getSouthWall() {
        return southWall;
    }

    /**
     * Gets the distance to the east wall (positive X direction).
     *
     * @return distance in blocks to the east wall
     */
    public int getEastWall() {
        return eastWall;
    }

    /**
     * Gets the distance to the west wall (negative X direction).
     *
     * @return distance in blocks to the west wall
     */
    public int getWestWall() {
        return westWall;
    }

    /**
     * Gets the distance to the ceiling (positive Y direction).
     *
     * @return distance in blocks to the ceiling
     */
    public int getUpWall() {
        return upWall;
    }

    /**
     * Gets the distance to the floor (negative Y direction).
     *
     * @return distance in blocks to the floor
     */
    public int getDownWall() {
        return downWall;
    }

    /**
     * Called every game tick to manage room scanning and visual effects.
     *
     * <p>This method handles:
     *
     * <ul>
     *   <li>Periodic room rescanning to detect changes in room structure
     *   <li>Spawning particle effects to visualize room boundaries
     * </ul>
     *
     * Only operates on the client side.
     *
     * @param level the world level
     * @param pos the position of this block entity
     * @param state the block state
     * @param blockEntity the room block entity instance
     */
    public static void tick(
            final net.minecraft.world.level.Level level,
            final BlockPos pos,
            final BlockState state,
            final RoomBlockEntity blockEntity) {
        if (!level.isClientSide) {
            return;
        }

        // Rescan room periodically
        blockEntity.rescanTimer++;
        if (blockEntity.rescanTimer >= RESCAN_INTERVAL) {
            blockEntity.rescanTimer = 0;
            blockEntity.scanRoom();
        }

        // Spawn particles showing room boundaries every 2 seconds
        if (level.getGameTime() % PARTICLE_INTERVAL == 0) {
            blockEntity.spawnBoundaryParticles();
        }
    }

    /**
     * Spawns particle effects showing beams to each detected wall.
     *
     * <p>This method creates visual indicators of the room boundaries by spawning red dust
     * particles in lines from the room block to each detected wall. The particles help players
     * understand the detected room boundaries.
     */
    private void spawnBoundaryParticles() {
        if (level == null || !level.isClientSide) {
            return;
        }

        BlockPos center = getBlockPos();
        double centerX = center.getX() + BLOCK_CENTER_OFFSET;
        double centerY = center.getY() + BLOCK_CENTER_OFFSET;
        double centerZ = center.getZ() + BLOCK_CENTER_OFFSET;

        // Spawn beam particles in each direction
        spawnBeam(centerX, centerY, centerZ, Direction.NORTH, northWall);
        spawnBeam(centerX, centerY, centerZ, Direction.SOUTH, southWall);
        spawnBeam(centerX, centerY, centerZ, Direction.EAST, eastWall);
        spawnBeam(centerX, centerY, centerZ, Direction.WEST, westWall);
        spawnBeam(centerX, centerY, centerZ, Direction.UP, upWall);
        spawnBeam(centerX, centerY, centerZ, Direction.DOWN, downWall);
    }

    /**
     * Spawns a beam of particles in a specific direction to visualize a wall.
     *
     * <p>Creates red dust particles every 2 blocks along the beam from the room block to the
     * detected wall in the specified direction. This provides visual feedback about the room
     * boundary detection.
     *
     * @param centerX the X coordinate of the room block center
     * @param centerY the Y coordinate of the room block center
     * @param centerZ the Z coordinate of the room block center
     * @param direction the direction to spawn the beam
     * @param distance the distance to the wall in this direction
     */
    private void spawnBeam(
            final double centerX,
            final double centerY,
            final double centerZ,
            final Direction direction,
            final int distance) {
        // Spawn particles along the beam at regular intervals
        for (int i = 1; i <= distance; i += PARTICLE_SPACING) {
            double x = centerX + direction.getStepX() * i;
            double y = centerY + direction.getStepY() * i;
            double z = centerZ + direction.getStepZ() * i;

            // Use redstone particles for all beams (red color)
            DustParticleOptions dustOptions =
                    new DustParticleOptions(new Vector3f(1.0f, 0.0f, 0.0f), 1.0f);
            level.addParticle(dustOptions, x, y, z, 0, 0, 0);
        }
    }
}
