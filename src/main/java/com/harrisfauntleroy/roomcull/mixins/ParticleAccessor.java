package com.harrisfauntleroy.roomcull.mixins;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accessor mixin to provide access to protected particle position fields. */
@Mixin(Particle.class)
public interface ParticleAccessor {

    @Accessor("x")
    double getParticleX();

    @Accessor("y")
    double getParticleY();

    @Accessor("z")
    double getParticleZ();
}
