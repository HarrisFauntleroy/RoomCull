package com.harrisfauntleroy.roomcull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class RoomManager {
    
    private static final List<RoomBlockEntity> rooms = new ArrayList<>();
    
    public static void addRoom(RoomBlockEntity room) {
        if (!rooms.contains(room)) {
            rooms.add(room);
        }
    }
    
    public static void removeRoom(RoomBlockEntity room) {
        rooms.remove(room);
    }
    
    /**
     * Check if an entity should be occluded from the viewer's perspective
     * Only occlude if viewer is inside a room and entity is outside room walls
     */
    public static boolean isEntityOccluded(Entity entity) {
        if (rooms.isEmpty()) return false;
        
        // Get viewer position
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameRenderer == null) return false;
        
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 viewerPos = camera.getPosition();
        Vec3 entityPos = entity.position();
        
        // Check each room to see if viewer is inside and entity is outside
        for (RoomBlockEntity room : rooms) {
            if (room.isRemoved()) continue;
            
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
     * Check if a position should be occluded from the viewer's perspective
     * Only occlude if viewer is inside a room and target is outside room walls
     */
    public static boolean isPositionOccluded(double x, double y, double z) {
        if (rooms.isEmpty()) return false;
        
        // Get viewer position
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameRenderer == null) return false;
        
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 viewerPos = camera.getPosition();
        
        // Check each room to see if viewer is inside and target is outside
        for (RoomBlockEntity room : rooms) {
            if (room.isRemoved()) continue;
            
            boolean viewerInside = !room.isPositionOutsideRoom(viewerPos.x, viewerPos.y, viewerPos.z);
            boolean targetOutside = room.isPositionOutsideRoom(x, y, z);
            
            // Debug output occasionally
            if (mc.level.getGameTime() % 100 == 0) {
                System.out.println("Room occlusion check: viewer(" + String.format("%.1f,%.1f,%.1f", viewerPos.x, viewerPos.y, viewerPos.z) + 
                                 ") inside=" + viewerInside + ", target(" + String.format("%.1f,%.1f,%.1f", x, y, z) + ") outside=" + targetOutside);
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
     * Clean up removed rooms periodically
     */
    public static void cleanupRemovedRooms() {
        rooms.removeIf(BlockEntity::isRemoved);
    }
    
    public static int getActiveRoomCount() {
        return rooms.size();
    }
    
    /**
     * Check if an entire chunk should be occluded from the viewer's perspective
     * Only occlude if viewer is inside a room and chunk is outside room walls
     */
    public static boolean isChunkOccluded(BlockPos chunkOrigin) {
        if (rooms.isEmpty()) return false;
        
        // Get chunk center position
        double chunkCenterX = chunkOrigin.getX() + 8.0;
        double chunkCenterY = chunkOrigin.getY() + 8.0;
        double chunkCenterZ = chunkOrigin.getZ() + 8.0;
        
        return isPositionOccluded(chunkCenterX, chunkCenterY, chunkCenterZ);
    }
    
    /**
     * Get the room bounds that the viewer is currently inside
     * Returns null if viewer is not inside any room
     */
    public static net.minecraft.world.phys.AABB getViewerRoomBounds() {
        if (rooms.isEmpty()) return null;
        
        // Get viewer position
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameRenderer == null) return null;
        
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 viewerPos = camera.getPosition();
        
        // Find the room that contains the viewer
        for (RoomBlockEntity room : rooms) {
            if (room.isRemoved()) continue;
            
            // Check if viewer is inside this room
            if (!room.isPositionOutsideRoom(viewerPos.x, viewerPos.y, viewerPos.z)) {
                return room.getOcclusionBounds(); // Return this room's bounds
            }
        }
        
        return null; // Viewer not inside any room
    }
}