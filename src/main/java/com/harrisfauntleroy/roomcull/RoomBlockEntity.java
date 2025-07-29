package com.harrisfauntleroy.roomcull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public class RoomBlockEntity extends BlockEntity {
    
    // Room boundaries (distance from room block to walls in each direction)
    private int northWall = 0;
    private int southWall = 0;
    private int eastWall = 0;
    private int westWall = 0;
    private int upWall = 0;
    private int downWall = 0;
    
    // The occlusion bounding box (room + 1 block buffer)
    private AABB occlusionBounds;
    
    // How often to rescan for walls
    private int rescanTimer = 0;
    private static final int RESCAN_INTERVAL = 100; // Every 5 seconds
    
    public RoomBlockEntity(BlockPos pos, BlockState blockState) {
        super(RoomCullMod.ROOM_BLOCK_ENTITY.get(), pos, blockState);
    }
    
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            scanRoom();
            RoomManager.addRoom(this);
        }
    }
    
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            RoomManager.removeRoom(this);
        }
    }
    
    /**
     * Scan in each cardinal direction to find walls, floor, and ceiling
     */
    private void scanRoom() {
        if (level == null) return;
        
        BlockPos center = getBlockPos();
        
        // Scan North (negative Z)
        northWall = scanDirection(center, Direction.NORTH, 50);
        
        // Scan South (positive Z)  
        southWall = scanDirection(center, Direction.SOUTH, 50);
        
        // Scan East (positive X)
        eastWall = scanDirection(center, Direction.EAST, 50);
        
        // Scan West (negative X)
        westWall = scanDirection(center, Direction.WEST, 50);
        
        // Scan Up (positive Y)
        upWall = scanDirection(center, Direction.UP, 50);
        
        // Scan Down (negative Y)
        downWall = scanDirection(center, Direction.DOWN, 50);
        
        updateOcclusionBounds();
    }
    
    /**
     * Scan in a specific direction until we hit a solid block
     */
    private int scanDirection(BlockPos center, Direction direction, int maxDistance) {
        BlockPos.MutableBlockPos pos = center.mutable();
        
        for (int i = 1; i <= maxDistance; i++) {
            pos.move(direction);
            BlockState blockState = level.getBlockState(pos);
            
            // If we hit a solid block (not air, not transparent), this is our wall
            if (!blockState.isAir() && blockState.isSolidRender(level, pos)) {
                return i;
            }
        }
        
        // If we didn't find a wall within max distance, use max distance
        return maxDistance;
    }
    
    /**
     * Update the occlusion bounding box based on detected room boundaries
     * Use precise wall positions with small inward offset to prevent outside culling
     */
    private void updateOcclusionBounds() {
        BlockPos center = getBlockPos();
        
        // Room bounds with 0.1 block inward offset to keep culling inside walls
        double minX = center.getX() - westWall + 0.1;
        double maxX = center.getX() + eastWall - 0.1;
        double minY = center.getY() - downWall + 0.1;
        double maxY = center.getY() + upWall - 0.1;
        double minZ = center.getZ() - northWall + 0.1;
        double maxZ = center.getZ() + southWall - 0.1;
        
        occlusionBounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        
        // Debug output
        System.out.println("Room at " + center + " detected walls: N=" + northWall + " S=" + southWall + 
                         " E=" + eastWall + " W=" + westWall + " U=" + upWall + " D=" + downWall);
        System.out.println("Room bounds: " + String.format("X:[%.1f-%.1f] Y:[%.1f-%.1f] Z:[%.1f-%.1f]", 
                         minX, maxX, minY, maxY, minZ, maxZ));
    }
    
    public AABB getOcclusionBounds() {
        return occlusionBounds;
    }
    
    /**
     * Check if a position is outside the room (should be occluded)
     */
    public boolean isPositionOutsideRoom(double x, double y, double z) {
        if (occlusionBounds == null) return false;
        
        // If the position is outside our room bounds, it should be occluded
        return !occlusionBounds.contains(x, y, z);
    }
    
    /**
     * Get wall distances for rendering beams
     */
    public int getNorthWall() { return northWall; }
    public int getSouthWall() { return southWall; }
    public int getEastWall() { return eastWall; }
    public int getWestWall() { return westWall; }
    public int getUpWall() { return upWall; }
    public int getDownWall() { return downWall; }
    
    /**
     * Called every tick to manage room scanning and particle effects
     */
    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, RoomBlockEntity blockEntity) {
        if (!level.isClientSide) return;
        
        // Rescan room periodically
        blockEntity.rescanTimer++;
        if (blockEntity.rescanTimer >= RESCAN_INTERVAL) {
            blockEntity.rescanTimer = 0;
            blockEntity.scanRoom();
        }
        
        // Spawn particles showing room boundaries every 2 seconds
        if (level.getGameTime() % 40 == 0) {
            blockEntity.spawnBoundaryParticles();
        }
    }
    
    /**
     * Spawn particles showing beams to each detected wall
     */
    private void spawnBoundaryParticles() {
        if (level == null || !level.isClientSide) return;
        
        BlockPos center = getBlockPos();
        double centerX = center.getX() + 0.5;
        double centerY = center.getY() + 0.5;
        double centerZ = center.getZ() + 0.5;
        
        // Spawn beam particles in each direction
        spawnBeam(centerX, centerY, centerZ, Direction.NORTH, northWall);
        spawnBeam(centerX, centerY, centerZ, Direction.SOUTH, southWall);
        spawnBeam(centerX, centerY, centerZ, Direction.EAST, eastWall);
        spawnBeam(centerX, centerY, centerZ, Direction.WEST, westWall);
        spawnBeam(centerX, centerY, centerZ, Direction.UP, upWall);
        spawnBeam(centerX, centerY, centerZ, Direction.DOWN, downWall);
    }
    
    /**
     * Spawn a beam of particles in a specific direction
     */
    private void spawnBeam(double centerX, double centerY, double centerZ, Direction direction, int distance) {
        // Spawn particles every 2 blocks along the beam
        for (int i = 1; i <= distance; i += 2) {
            double x = centerX + direction.getStepX() * i;
            double y = centerY + direction.getStepY() * i;
            double z = centerZ + direction.getStepZ() * i;
            
            // Use redstone particles for all beams (red color)
            DustParticleOptions dustOptions = new DustParticleOptions(new Vector3f(1.0f, 0.0f, 0.0f), 1.0f);
            level.addParticle(dustOptions, x, y, z, 0, 0, 0);
        }
    }
}