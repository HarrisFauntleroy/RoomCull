package com.harrisfauntleroy.roomcull;

import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RoomCullMod.MODID, value = Dist.CLIENT)
public class OcclusionRenderEvents {
    
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        Entity entity = event.getEntity();
        
        if (RoomManager.isEntityOccluded(entity)) {
            // Debug logging
            System.out.println("Occluding entity: " + entity.getClass().getSimpleName() + " at " + entity.position());
            event.setCanceled(true); // Cancel rendering of occluded entities
        }
    }
}