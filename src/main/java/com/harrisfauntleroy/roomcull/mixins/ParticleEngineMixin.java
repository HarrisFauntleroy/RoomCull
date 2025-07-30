package com.harrisfauntleroy.roomcull.mixins;

import com.harrisfauntleroy.roomcull.RoomManager;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept particle rendering and apply room-based occlusion culling.
 *
 * <p>This mixin hooks into the ParticleEngine's tick method to remove particles that should be
 * occluded based on the viewer's current room. When a viewer is inside a room, particles outside
 * that room are removed to maintain the occlusion effect.
 *
 * @author Harris Fauntleroy
 * @since 1.0.0
 */
@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    /**
     * Intercepts particle tick updates to apply occlusion culling.
     *
     * <p>This method checks each particle's position against the current room bounds and removes
     * particles that should be occluded. This ensures particles don't appear through walls when the
     * viewer is inside a room.
     *
     * @param particle the particle being ticked
     * @param ci callback info for the injection
     */
    @Inject(method = "tickParticle", at = @At("HEAD"), cancellable = true)
    private void onTickParticle(final Particle particle, final CallbackInfo ci) {
        if (particle == null) {
            return;
        }

        try {
            // Access protected fields using accessor mixin
            ParticleAccessor accessor = (ParticleAccessor) particle;
            double x = accessor.getParticleX();
            double y = accessor.getParticleY();
            double z = accessor.getParticleZ();

            if (RoomManager.isPositionOccluded(x, y, z)) {
                // Remove occluded particles by marking them as expired
                particle.remove();
                ci.cancel();
            }
        } catch (Exception e) {
            // If we can't access particle position, don't occlude
        }
    }
}
