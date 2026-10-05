package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.MioVisuals;
import me.alpha432.chimeraclient.mio.support.ModelSupport;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityRenderManager.class})
public abstract class MioEntityScopeMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mio$start(
      EntityRenderState var1,
      CameraRenderState var2,
      double var3,
      double var5,
      double var7,
      MatrixStack var9,
      OrderedRenderCommandQueue var10,
      CallbackInfo var11
   ) {
      Entity var12 = ModelSupport.entity(var1);
      if (var12 != null && MioVisuals.hidden(var12)) {
         var11.cancel();
      } else {
         ModelSupport.scope = var1;
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   private void mio$end(
      EntityRenderState var1,
      CameraRenderState var2,
      double var3,
      double var5,
      double var7,
      MatrixStack var9,
      OrderedRenderCommandQueue var10,
      CallbackInfo var11
   ) {
      ModelSupport.scope = null;
   }
}
