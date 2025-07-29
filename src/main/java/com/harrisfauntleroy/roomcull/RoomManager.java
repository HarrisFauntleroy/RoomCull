package com.harrisfauntleroy.roomcull;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Manages room-based occlusion culling for the RoomCull mod.
 *
 * <p>This class tracks all active room blocks in the world and provides methods to determine
 * whether entities, positions, or chunks should be occluded (hidden) based on room boundaries. The
 * core principle is that when a viewer is inside a room, objects outside that room's walls should
 * be culled to improve performance and create a more realistic indoor experience.
 *
 * <p>The occlusion system works by:
 *
 * <ul>
 *   <li>Tracking all {@link RoomBlockEntity} instances in the world
 *   <li>Determining if the viewer (camera) is inside any room
 *   <li>Culling entities, positions, and chunks that are outside the viewer's current room
 * </ul>
 *
 * @see RoomBlockEntity
 * @see RoomBlock
 * @author Harris Fauntleroy
 * @since 1.0.0
 */
@OnlyIn(Dist.CLIENT)
public final class RoomManager {

    /** Private constructor to prevent instantiation of utility class. */
    private RoomManager() {
        // Utility class
    }

    /**
     * List of all active room block entities in the world. This list is automatically managed as
     * room blocks are placed and removed.
     */
    private static final List<RoomBlockEntity> ROOMS = new ArrayList<>();

    /** Cache for the viewer's current room to avoid repeated calculations. */
    private static RoomBlockEntity cachedViewerRoom = null;

    private static Vec3 lastViewerPos = null;
    private static final double POSITION_THRESHOLD =
            0.1; // Minimum movement to trigger recalculation

    /** Interval for debug output in game ticks. */
    private static final int DEBUG_OUTPUT_INTERVAL = 200;

    /** Chunk center offset for occlusion calculations. */
    private static final double CHUNK_CENTER_OFFSET = 8.0;

    /**
     * Registers a room block entity for occlusion calculations.
     *
     * <p>This method is called automatically when a {@link RoomBlockEntity} is loaded into the
     * world. Duplicate additions are prevented.
     *
     * @param room the room block entity to register
     */
    public static void addRoom(final RoomBlockEntity room) {
        if (!ROOMS.contains(room)) {
            ROOMS.add(room);
        }
    }

    /**
     * Unregisters a room block entity from occlusion calculations.
     *
     * <p>This method is called automatically when a {@link RoomBlockEntity} is removed from the
     * world or unloaded.
     *
     * @param room the room block entity to unregister
     */
    public static void removeRoom(final RoomBlockEntity room) {
        ROOMS.remove(room);
    }

    /**
     * Determines if an entity should be occluded (hidden) from the viewer's perspective.
     *
     * <p>An entity is occluded if:
     *
     * <ul>
     *   <li>The viewer (camera) is currently inside a room
     *   <li>The entity is located outside that room's boundaries
     * </ul>
     *
     * This helps create a more realistic indoor experience by hiding objects that shouldn't be
     * visible through walls.
     *
     * @param entity the entity to check for occlusion
     * @return true if the entity should be hidden, false if it should be rendered
     */
    public static boolean isEntityOccluded(final Entity entity) {
        if (ROOMS.isEmpty()) {
            return false;
        }

        // Get viewer position
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameRenderer == null) {
            return false;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 viewerPos = camera.getPosition();
        Vec3 entityPos = entity.position();

        // Check each room to see if viewer is inside and entity is outside
        for (RoomBlockEntity room : ROOMS) {
            if (room.isRemoved()) {
                continue;
            }

            // Check if viewer is inside this room
            if (!room.isPositionOutsideRoom(viewerPos.x, viewerPos.y, viewerPos.z)) {
                // Viewer is inside this room
                // Check if entity is outside this room
                if (room.isPositionOutsideRoom(entityPos.x, entityPos.y, entityPos.z)) {
                    return true; // Entity is outside the room viewer is in, occlude it
                }
            }
        }

        // Either viewer is not in any room, or entity is in same room as viewer
        return false;
    }

    /**
     * Determines if a specific world position should be occluded from the viewer's perspective.
     *
     * <p>A position is occluded if:
     *
     * <ul>
     *   <li>The viewer (camera) is currently inside a room
     *   <li>The target position is located outside that room's boundaries
     * </ul>
     *
     * This method includes debug logging every 200 game ticks to help with troubleshooting room
     * boundary detection.
     *
     * @param x the X coordinate to check
     * @param y the Y coordinate to check
     * @param z the Z coordinate to check
     * @return true if the position should be hidden, false if it should be rendered
     */
    public static boolean isPositionOccluded(final double x, final double y, final double z) {
        if (ROOMS.isEmpty()) {
            return false;
        }

        // Get viewer position
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameRenderer == null) {
            return false;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 viewerPos = camera.getPosition();

        // Check each room to see if viewer is inside and target is outside
        for (RoomBlockEntity room : ROOMS) {
            if (room.isRemoved()) {
                continue;
            }

            boolean viewerInside =
                    !room.isPositionOutsideRoom(viewerPos.x, viewerPos.y, viewerPos.z);
            boolean targetOutside = room.isPositionOutsideRoom(x, y, z);

            // Debug output occasionally (reduced frequency)
            if (Config.isDebugMode() && mc.level.getGameTime() % DEBUG_OUTPUT_INTERVAL == 0) {
                System.out.println(
                        "Room occlusion check: viewer("
                                + String.format(
                                        "%.1f,%.1f,%.1f", viewerPos.x, viewerPos.y, viewerPos.z)
                                + ") inside="
                                + viewerInside
                                + ", target("
                                + String.format("%.1f,%.1f,%.1f", x, y, z)
                                + ") outside="
                                + targetOutside);
            }

            // Check if viewer is inside this room
            if (viewerInside) {
                // Viewer is inside this room
                // Check if target position is outside this room
                if (targetOutside) {
                    return true; // Target is outside the room viewer is in, occlude it
                }
            }
        }

        // Either viewer is not in any room, or target is in same room as viewer
        return false;
    }

    /**
     * Removes all block entities that have been marked as removed from the room list.
     *
     * <p>This method should be called periodically to prevent memory leaks from keeping references
     * to destroyed room blocks. It uses the {@link BlockEntity#isRemoved()} method to identify
     * stale references.
     */
    public static void cleanupRemovedRooms() {
        ROOMS.removeIf(BlockEntity::isRemoved);
    }

    /**
     * Gets the current number of active room blocks being tracked.
     *
     * <p>This count includes all registered room blocks, including any that may have been marked as
     * removed but not yet cleaned up. For the most accurate count, call {@link
     * #cleanupRemovedRooms()} first.
     *
     * @return the number of room blocks currently in the tracking list
     */
    public static int getActiveRoomCount() {
        return ROOMS.size();
    }

    /**
     * Determines if an entire chunk should be occluded from the viewer's perspective.
     *
     * <p>This method provides chunk-level occlusion culling by checking if the center of a chunk
     * should be occluded. A chunk is considered occluded if:
     *
     * <ul>
     *   <li>The viewer is inside a room
     *   <li>The chunk's center point is outside that room's boundaries
     * </ul>
     *
     * This is useful for optimizing rendering performance by culling entire chunks that are outside
     * the viewer's current room.
     *
     * @param chunkOrigin the origin position of the chunk to check
     * @return true if the entire chunk should be hidden, false if it should be rendered
     */
    public static boolean isChunkOccluded(final BlockPos chunkOrigin) {
        if (ROOMS.isEmpty()) {
            return false;
        }

        // Get chunk center position
        double chunkCenterX = chunkOrigin.getX() + CHUNK_CENTER_OFFSET;
        double chunkCenterY = chunkOrigin.getY() + CHUNK_CENTER_OFFSET;
        double chunkCenterZ = chunkOrigin.getZ() + CHUNK_CENTER_OFFSET;

        return isPositionOccluded(chunkCenterX, chunkCenterY, chunkCenterZ);
    }

    /**
     * Gets the bounding box of the room that the viewer is currently inside.
     *
     * <p>This method determines which room (if any) contains the viewer's current position and
     * returns that room's occlusion bounds. The bounds represent the area within which objects
     * should be rendered when the viewer is in this room. Uses caching to improve performance and
     * responsiveness.
     *
     * @return the {@link AABB} bounds of the viewer's current room, or null if the viewer is not
     *     inside any room
     */
    public static net.minecraft.world.phys.AABB getViewerRoomBounds() {
        RoomBlockEntity currentRoom = getCurrentViewerRoom();
        return currentRoom != null ? currentRoom.getOcclusionBounds() : null;
    }

    /**
     * Gets the room that the viewer is currently inside, with caching for performance.
     *
     * @return the room containing the viewer, or null if not in any room
     */
    public static RoomBlockEntity getCurrentViewerRoom() {
        if (ROOMS.isEmpty()) {
            cachedViewerRoom = null;
            return null;
        }

        // Get viewer position
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameRenderer == null) {
            cachedViewerRoom = null;
            return null;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 viewerPos = camera.getPosition();

        // Check if we need to recalculate (viewer moved significantly or no cache)
        if (cachedViewerRoom == null
                || lastViewerPos == null
                || viewerPos.distanceToSqr(lastViewerPos)
                        > POSITION_THRESHOLD * POSITION_THRESHOLD) {

            lastViewerPos = viewerPos;
            cachedViewerRoom = null;

            // Find the room that contains the viewer
            for (RoomBlockEntity room : ROOMS) {
                if (room.isRemoved()) {
                    continue;
                }

                // Check if viewer is inside this room
                if (!room.isPositionOutsideRoom(viewerPos.x, viewerPos.y, viewerPos.z)) {
                    cachedViewerRoom = room;
                    break;
                }
            }
        }

        return cachedViewerRoom;
    }
}
