package me.alpha432.chimeraclient.shoreline.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.alpha432.chimeraclient.shoreline.PortSupport;
import me.alpha432.chimeraclient.shoreline.rotation.RotationEvents;
import net.minecraft.client.input.Input;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({Input.class})
public class ShorelineInputHook {
   @ModifyReturnValue(
      method = {"getMovementInput"},
      at = {@At("RETURN")},
      remap = false
   )
   private Vec2f shoreline$movementFix(Vec2f var1) {
      RotationEvents.KeyboardTickEvent var2 = new RotationEvents.KeyboardTickEvent(var1.y, var1.x);
      PortSupport.Managers.ROTATION.onKeyboardTick(var2);
      return new Vec2f(var2.sideways, var2.forward);
   }
}
