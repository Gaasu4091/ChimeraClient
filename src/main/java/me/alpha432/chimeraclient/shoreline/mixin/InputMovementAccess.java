package me.alpha432.chimeraclient.shoreline.mixin;

import net.minecraft.client.input.Input;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Input.class})
public interface InputMovementAccess {
   @Accessor(
      value = "movementVector",
      remap = false
   )
   Vec2f shoreline$getMovementVector();

   @Accessor(
      value = "movementVector",
      remap = false
   )
   void shoreline$setMovementVector(Vec2f var1);
}
