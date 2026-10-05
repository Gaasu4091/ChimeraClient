package me.alpha432.chimeraclient.features.modules.client;

import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;

public final class ShorelineRotationsModule extends Module {
   private static ShorelineRotationsModule INSTANCE;
   final Setting<Float> preserveTicksConfig = this.num("PreserveTicks", 10.0F, 0.0F, 20.0F);
   final Setting<Boolean> movementFixConfig = this.bool("MovementFix", false);
   final Setting<Boolean> mouseSensFixConfig = this.bool("MouseSensFix", false);

   public ShorelineRotationsModule() {
      super("ShorelineRotations", "Shared Shoreline rotation controls", Module.Category.CLIENT);
      INSTANCE = this;
      this.drawn.setValueNoEvent(false);
   }

   public static ShorelineRotationsModule getInstance() {
      return INSTANCE == null ? new ShorelineRotationsModule() : INSTANCE;
   }

   public boolean getMovementFix() {
      return this.movementFixConfig.getValue();
   }

   public boolean getMouseSensFix() {
      return this.mouseSensFixConfig.getValue();
   }

   public float getPreserveTicks() {
      return this.preserveTicksConfig.getValue();
   }
}
