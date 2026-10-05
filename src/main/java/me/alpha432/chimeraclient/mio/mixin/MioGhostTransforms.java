package me.alpha432.chimeraclient.mio.mixin;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({LivingEntityRenderer.class})
public interface MioGhostTransforms {
   @Invoker("setupTransforms")
   void mio$transforms(LivingEntityRenderState var1, MatrixStack var2, float var3, float var4);

   @Invoker("scale")
   void mio$scale(LivingEntityRenderState var1, MatrixStack var2);
}
