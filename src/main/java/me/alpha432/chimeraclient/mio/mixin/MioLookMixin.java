package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.MioConfiguredModule;
import me.alpha432.chimeraclient.mio.MioVisuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public abstract class MioLookMixin {
   @Inject(
      method = {"changeLookDirection"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mio$look(double var1, double var3, CallbackInfo var5) {
      if ((Object)this == MinecraftClient.getInstance().player && MioConfiguredModule.active("FreeLook") != null && MioVisuals.lookReady) {
         MioVisuals.lookYaw += (float)var1 * 0.15F;
         MioVisuals.lookPitch = MathHelper.clamp(MioVisuals.lookPitch + (float)var3 * 0.15F, -90.0F, 90.0F);
         var5.cancel();
      }
   }
}
