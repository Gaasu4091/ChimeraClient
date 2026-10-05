package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.MioConfiguredModule;
import me.alpha432.chimeraclient.mio.MioVisuals;
import me.alpha432.chimeraclient.mio.hud.MioHudConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({InGameHud.class})
public abstract class MioHudMixin {
   @Inject(
      method = {"renderCrosshair"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mio$crosshair(DrawContext var1, RenderTickCounter var2, CallbackInfo var3) {
      if (MioConfiguredModule.active("Crosshair") != null) {
         var3.cancel();
      }
   }

   @Inject(
      method = {"renderStatusEffectOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mio$effects(DrawContext var1, RenderTickCounter var2, CallbackInfo var3) {
      MioHudConfig var4 = MioHudConfig.INSTANCE;
      if (var4 != null && var4.isEnabled() && var4.icons.getValue() == MioHudConfig.Icons.HIDE) {
         var3.cancel();
      }
   }

   @Inject(
      method = {"renderHeldItemTooltip"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mio$held(DrawContext var1, CallbackInfo var2) {
      if (MioVisuals.noRender("ui", "heldTooltips")) {
         var2.cancel();
      }
   }

   @Inject(
      method = {"renderScoreboardSidebar"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mio$score(DrawContext var1, RenderTickCounter var2, CallbackInfo var3) {
      if (MioVisuals.noRender("ui", "scoreBoard")) {
         var3.cancel();
      }
   }
}
