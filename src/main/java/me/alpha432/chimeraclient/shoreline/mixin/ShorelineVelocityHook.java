package me.alpha432.chimeraclient.shoreline.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.alpha432.chimeraclient.features.modules.client.ShorelineRotationsModule;
import me.alpha432.chimeraclient.shoreline.PortSupport;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({Entity.class})
public class ShorelineVelocityHook {
   @ModifyExpressionValue(
      method = {"updateVelocity"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/Entity;getYaw()F"
      )},
      remap = false
   )
   private float shoreline$velocityYaw(float var1) {
      return (Object)this == MinecraftClient.getInstance().player
            && ShorelineRotationsModule.getInstance().getMovementFix()
            && PortSupport.Managers.ROTATION.isRotating()
         ? PortSupport.Managers.ROTATION.getRotationYaw()
         : var1;
   }
}
