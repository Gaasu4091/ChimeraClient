package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.MioConfiguredModule;
import me.alpha432.chimeraclient.mio.MioVisuals;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Mouse.class})
public abstract class MioMouseMixin {
   @Inject(
      method = {"onMouseScroll"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mio$scroll(long var1, double var3, double var5, CallbackInfo var7) {
      MioConfiguredModule var8 = MioConfiguredModule.active("Zoom");
      if (var8 != null && var8.b("scroll")) {
         MioVisuals.zoomScroll -= var5 * 10.0;
         var7.cancel();
      }
   }
}
