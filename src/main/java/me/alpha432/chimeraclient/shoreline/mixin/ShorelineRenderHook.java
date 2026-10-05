package me.alpha432.chimeraclient.shoreline.mixin;

import me.alpha432.chimeraclient.shoreline.PortSupport;
import me.alpha432.chimeraclient.shoreline.rotation.RotationEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntityRenderer.class})
public class ShorelineRenderHook {
   @Inject(
      method = {"updateRenderState"},
      at = {@At("TAIL")},
      remap = false
   )
   private void shoreline$renderAngles(LivingEntity var1, LivingEntityRenderState var2, float var3, CallbackInfo var4) {
      if (var1 == MinecraftClient.getInstance().player && PortSupport.Managers.ROTATION.isRotating()) {
         RotationEvents.RenderPlayerEvent var5 = new RotationEvents.RenderPlayerEvent(var1, var2.bodyYaw, var2.pitch);
         PortSupport.Managers.ROTATION.onRenderPlayer(var5);
         var2.bodyYaw = var5.getYaw();
         var2.relativeHeadYaw = 0.0F;
         var2.pitch = var5.getPitch();
      }
   }
}
