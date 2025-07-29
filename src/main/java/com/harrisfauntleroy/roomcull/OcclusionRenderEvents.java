package com.harrisfauntleroy.roomcull;

import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

/**
 * Event handler for implementing entity and particle occlusion culling during rendering.
 *
 * <p>This class intercepts entity and particle rendering events and applies room-based occlusion
 * culling. When a viewer is inside a room and an entity or particle is outside that room's
 * boundaries, the rendering is cancelled to improve performance and create a more realistic indoor
 * experience.
 *
 * <p>The class is client-side only since rendering and culling decisions are made on the client. It
 * integrates with {@link RoomManager} to determine which entities and particles should be occluded.
 *
 * @see RoomManager
 * @see RenderLivingEvent
 * @author Harris Fauntleroy
 * @since 1.0.0
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RoomCullMod.MODID, value = Dist.CLIENT)
public final class OcclusionRenderEvents {

    /** Private constructor to prevent instantiation of utility class. */
    private OcclusionRenderEvents() {
        // Utility class
    }

    /**
     * Handles the pre-render event for living entities to apply occlusion culling.
     *
     * <p>This method is called before each living entity is rendered. It checks with the {@link
     * RoomManager} to determine if the entity should be occluded based on the viewer's current
     * room. If the entity should be occluded, the rendering event is cancelled.
     *
     * <p>Debug information is logged when entities are occluded to help with troubleshooting the
     * occlusion system.
     *
     * @param event the pre-render event for a living entity
     */
    @SubscribeEvent
    public static void onRenderLiving(final RenderLivingEvent.Pre<?, ?> event) {
        Entity entity = event.getEntity();

        if (RoomManager.isEntityOccluded(entity)) {
            if (Config.isDebugMode()) {
                System.out.println(
                        "Occluding entity: "
                                + entity.getClass().getSimpleName()
                                + " at "
                                + entity.position());
            }
            event.setCanceled(true);
        }
    }
}
