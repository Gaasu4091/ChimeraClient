package me.alpha432.chimeraclient.mixin.input;

import me.alpha432.chimeraclient.event.impl.input.MouseInputEvent;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Mouse.class})
public class MixinMouseHandler {
   @Inject(
      method = {"onMouseButton"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onButton(long window, MouseInput input, int action, CallbackInfo ci) {
      if (Util.EVENT_BUS.post(new MouseInputEvent(input.button(), action))) {
         ci.cancel();
      }
   }
}
