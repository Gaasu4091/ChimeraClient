package me.alpha432.chimeraclient.features.modules.movement;

import me.alpha432.chimeraclient.features.modules.Module;

public class ReverseStepModule extends Module {
   public ReverseStepModule() {
      super("ReverseStep", "step but reversed..", Module.Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         if (!mc.player.isInLava() && !mc.player.isTouchingWater() && mc.player.isOnGround()) {
            mc.player.addVelocity(0.0, -1.0, 0.0);
         }
      }
   }
}
