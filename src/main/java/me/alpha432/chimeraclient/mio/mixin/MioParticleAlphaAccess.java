package me.alpha432.chimeraclient.mio.mixin;

import net.minecraft.client.particle.BillboardParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({BillboardParticle.class})
public interface MioParticleAlphaAccess {
   @Invoker("setAlpha")
   void mio$alpha(float var1);
}
