package com.harrisfauntleroy.roomcull.mixins;

import com.harrisfauntleroy.roomcull.RoomManager;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.joml.FrustumIntersection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Frustum.class)
public class FrustumMixin {

    @Shadow @Final private FrustumIntersection intersection;

    /** Maximum reasonable size for room bounds validation (in blocks). */
    private static final double MAX_ROOM_SIZE = 1000.0;

    /**
     * Hook into the frustum's isVisible method to add room boundary frustum culling Instead of just
     * doing simple occlusion, we properly test against room boundary planes.
     */
    @Inject(
            method = "isVisible(Lnet/minecraft/world/phys/AABB;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void onIsVisible(final AABB aabb, final CallbackInfoReturnable<Boolean> cir) {

        // Early exit if no rooms active
        if (RoomManager.getActiveRoomCount() == 0) {
            return;
        }

        // Check if viewer is inside any room - if not, use normal frustum culling
        AABB roomBounds = RoomManager.getViewerRoomBounds();
        if (roomBounds == null) {
            return; // Viewer not in any room, use normal culling
        }

        // Validate bounds to prevent issues with very large rooms
        if (!isValidBounds(aabb) || !isValidBounds(roomBounds)) {
            return; // Use normal frustum culling for invalid bounds
        }

        // Test AABB against room boundary planes (6 planes forming a box)
        // If AABB is completely outside room bounds, cull it
        if (!aabb.intersects(roomBounds)) {
            cir.setReturnValue(false); // Outside room bounds, not visible
        }
    }

    /**
     * Validates that an AABB has reasonable bounds to prevent performance issues.
     *
     * @param aabb the bounding box to validate
     * @return true if the bounds are reasonable for processing
     */
    private boolean isValidBounds(final AABB aabb) {
        // Check for null or infinite bounds
        if (aabb == null) {
            return false;
        }

        // Check for reasonable size limits (max 1000 block room)
        return aabb.getXsize() <= MAX_ROOM_SIZE
                && aabb.getYsize() <= MAX_ROOM_SIZE
                && aabb.getZsize() <= MAX_ROOM_SIZE;
    }
}
