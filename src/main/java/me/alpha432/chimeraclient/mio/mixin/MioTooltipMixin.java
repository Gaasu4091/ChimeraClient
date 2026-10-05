package me.alpha432.chimeraclient.mio.mixin;

import java.util.List;
import me.alpha432.chimeraclient.mio.support.TooltipsSupport;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({DrawContext.class})
public abstract class MioTooltipMixin {
   @ModifyVariable(
      method = {"drawTooltipImmediately"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private List<TooltipComponent> mio$components(List<TooltipComponent> var1) {
      return TooltipsSupport.components(var1);
   }
}
