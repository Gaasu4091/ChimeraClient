package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.MioVisuals;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntityRenderer.class})
public abstract class MioLivingNoRenderMixin {
   @Inject(
      method = {"updateRenderState"},
      at = {@At("RETURN")}
   )
   private void mio$hurt(LivingEntity var1, LivingEntityRenderState var2, float var3, CallbackInfo var4) {
      if (MioVisuals.noRender("entities", "hurt")) {
         var2.hurt = false;
      }

      if (MioVisuals.noRender("ui", "tint")) {
         var2.hurt = false;
      }
   }
}
