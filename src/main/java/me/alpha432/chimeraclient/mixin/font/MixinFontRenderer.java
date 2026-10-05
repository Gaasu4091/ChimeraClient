package me.alpha432.chimeraclient.mixin.font;

import me.alpha432.chimeraclient.features.gui.font.FontDraw;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({TextRenderer.class})
public abstract class MixinFontRenderer {
   @Inject(
      method = {"method_27516"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void chimera$advance(int var1, Style var2, CallbackInfoReturnable<Float> var3) {
      if (FontDraw.enabled() && !FontDraw.isFallback() && FontDraw.marker(var2)) {
         var3.setReturnValue(FontDraw.advance(var2, var1));
      }
   }

   @ModifyVariable(
      method = {"wrapLines", "wrapLinesWithoutLanguage"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0,
      remap = false
   )
   private StringVisitable chimera$chatLines(StringVisitable var1) {
      return FontDraw.chatLines(var1);
   }

   @Inject(
      method = {"getWidth"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void chimera$chatInputWidth(String var1, CallbackInfoReturnable<Integer> var2) {
      if (var1 != null && FontDraw.chatInputScope()) {
         var2.setReturnValue(FontDraw.width((TextRenderer)(Object)this, var1));
      }
   }
}
