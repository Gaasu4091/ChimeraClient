package me.alpha432.chimeraclient.features.modules.player;

import me.alpha432.chimeraclient.features.modules.Module;
import net.minecraft.item.Items;

public class FastPlaceModule extends Module {
   public FastPlaceModule() {
      super("FastPlace", "Makes you throw exp faster", Module.Category.PLAYER);
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         if (mc.player.isHolding(Items.EXPERIENCE_BOTTLE)) {
            mc.itemUseCooldown = 0;
         }
      }
   }
}
