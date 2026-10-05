package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.MioConfiguredModule;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({WorldRenderer.class})
public abstract class MioHighlightOutlineMixin {
   @Inject(
      method = {"renderTargetBlockOutline"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mio$outline(CallbackInfo var1) {
      if (MioConfiguredModule.active("Highlight") != null) {
         var1.cancel();
      }
   }
}
