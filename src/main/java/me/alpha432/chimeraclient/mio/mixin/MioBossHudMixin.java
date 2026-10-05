package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.MioVisuals;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BossBarHud.class})
public abstract class MioBossHudMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mio$boss(DrawContext var1, CallbackInfo var2) {
      if (MioVisuals.noRender("ui", "bossBars")) {
         var2.cancel();
      }
   }
}
