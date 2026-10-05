package me.alpha432.chimeraclient.features.modules.combat;

import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.util.inventory.InventoryUtil;
import me.alpha432.chimeraclient.util.inventory.Result;
import net.minecraft.item.Items;

public class KeyPearlModule extends Module {
   private final Setting<Boolean> inventory = this.bool("Inventory", false);

   public KeyPearlModule() {
      super("KeyPearl", "Throws a pearl when enabled.", Module.Category.COMBAT);
   }

   @Override
   public void onEnable() {
      this.disable();
      if (!nullCheck()) {
         Result result = InventoryUtil.find(Items.ENDER_PEARL, this.inventory.getValue() ? InventoryUtil.FULL_SCOPE : InventoryUtil.HOTBAR_SCOPE);
         InventoryUtil.withSwap(result, () -> mc.interactionManager.interactItem(mc.player, result.hand()));
      }
   }
}
