package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.support.GlintSupport;
import me.alpha432.chimeraclient.mio.support.MioShaderSupport;
import me.alpha432.chimeraclient.mio.support.ModelSupport;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameRenderer.class})
public abstract class MioRenderFrameMixin {
   @Inject(
      method = {"renderWorld"},
      at = {@At("HEAD")}
   )
   private void mio$clear(RenderTickCounter var1, CallbackInfo var2) {
      MioShaderSupport.begin();
   }

   @Inject(
      method = {"renderWorld"},
      at = {@At("RETURN")}
   )
   private void mio$post(RenderTickCounter var1, CallbackInfo var2) {
      MioShaderSupport.draw();
   }

   @Inject(
      method = {"renderHand"},
      at = {@At("HEAD")}
   )
   private void mio$handStart(float var1, boolean var2, Matrix4f var3, CallbackInfo var4) {
      ModelSupport.hand = true;
   }

   @Inject(
      method = {"renderHand"},
      at = {@At("RETURN")}
   )
   private void mio$handEnd(float var1, boolean var2, Matrix4f var3, CallbackInfo var4) {
      ModelSupport.hand = false;
   }

   @Inject(
      method = {"close"},
      at = {@At("HEAD")}
   )
   private void mio$close(CallbackInfo var1) {
      MioShaderSupport.close();
      GlintSupport.close();
   }
}
