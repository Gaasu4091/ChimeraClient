package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.MioVisuals;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({InGameOverlayRenderer.class})
public abstract class MioFireOverlayMixin {
   @Inject(
      method = {"renderFireOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void mio$fire(CallbackInfo var0) {
      if (MioVisuals.noRender("entities", "fire")) {
         var0.cancel();
      }
   }
}
