package me.alpha432.chimeraclient.shoreline.mixin;

import me.alpha432.chimeraclient.shoreline.PortSupport;
import me.alpha432.chimeraclient.shoreline.rotation.RotationEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntity.class})
public class ShorelineJumpHook {
   @Inject(
      method = {"jump"},
      at = {@At("HEAD")},
      remap = false
   )
   private void shoreline$beforeJump(CallbackInfo var1) {
      if ((Object)this == MinecraftClient.getInstance().player) {
         PortSupport.Managers.ROTATION.onPlayerJump(new RotationEvents.PlayerJumpEvent(RotationEvents.StageEvent.EventStage.PRE));
      }
   }

   @Inject(
      method = {"jump"},
      at = {@At("RETURN")},
      remap = false
   )
   private void shoreline$afterJump(CallbackInfo var1) {
      if ((Object)this == MinecraftClient.getInstance().player) {
         PortSupport.Managers.ROTATION.onPlayerJump(new RotationEvents.PlayerJumpEvent(RotationEvents.StageEvent.EventStage.POST));
      }
   }
}
