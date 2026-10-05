package me.alpha432.chimeraclient.features.modules.movement;

import me.alpha432.chimeraclient.features.modules.Module;

public class Sprint extends Module {
   public Sprint() {
      super("Sprint", "Always keeps the player in a sprinting state.", Module.Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         if (mc.player.forwardSpeed > 0.0F && !mc.player.isSneaking() && !mc.player.horizontalCollision) {
            mc.player.setSprinting(true);
         }
      }
   }
}
