package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.support.TooltipsSupport;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Screen.class})
public abstract class MioTooltipScopeMixin {
   @Inject(
      method = {"renderWithTooltip"},
      at = {@At("HEAD")}
   )
   private void mio$begin(CallbackInfo var1) {
      TooltipsSupport.hovered = ItemStack.EMPTY;
   }

   @Inject(
      method = {"renderWithTooltip"},
      at = {@At("RETURN")}
   )
   private void mio$end(CallbackInfo var1) {
      TooltipsSupport.hovered = ItemStack.EMPTY;
   }
}
