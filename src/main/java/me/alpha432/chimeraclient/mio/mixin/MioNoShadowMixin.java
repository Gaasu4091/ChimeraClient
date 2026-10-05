package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.MioVisuals;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({DrawContext.class})
public abstract class MioNoShadowMixin {
   @ModifyVariable(
      method = {"drawText", "drawText", "drawText"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private boolean mio$shadow(boolean var1) {
      return var1 && !MioVisuals.noRender("ui", "textShadow");
   }
}
