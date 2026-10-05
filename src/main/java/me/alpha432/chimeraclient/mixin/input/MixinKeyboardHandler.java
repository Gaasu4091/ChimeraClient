package me.alpha432.chimeraclient.mixin.input;

import me.alpha432.chimeraclient.event.impl.input.KeyInputEvent;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.client.Keyboard;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Keyboard.class})
public class MixinKeyboardHandler {
   @Inject(
      method = {"onKey"},
      at = {@At("TAIL")},
      cancellable = true
   )
   private void keyPress(long window, int action, KeyInput input, CallbackInfo ci) {
      if (action == 1) {
         if (Util.EVENT_BUS.post(new KeyInputEvent(input.key()))) {
            ci.cancel();
         }
      }
   }
}
