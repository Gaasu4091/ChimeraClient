package me.alpha432.chimeraclient.mio.mixin;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({RenderLayer.class})
public interface MioLayerFactory {
   @Invoker("of")
   static RenderLayer mio$of(String var0, RenderSetup var1) {
      throw new AssertionError();
   }
}
