package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.support.ModelSupport;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ArmorFeatureRenderer.class})
public abstract class MioArmorScopeMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void mio$start(CallbackInfo var1) {
      ModelSupport.armor = true;
   }

   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   private void mio$end(CallbackInfo var1) {
      ModelSupport.armor = false;
   }
}
