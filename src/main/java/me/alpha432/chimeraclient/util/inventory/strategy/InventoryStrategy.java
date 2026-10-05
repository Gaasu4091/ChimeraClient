package me.alpha432.chimeraclient.util.inventory.strategy;

import me.alpha432.chimeraclient.util.inventory.InventoryUtil;
import me.alpha432.chimeraclient.util.inventory.Result;
import me.alpha432.chimeraclient.util.inventory.ResultType;
import net.minecraft.screen.slot.SlotActionType;

public final class InventoryStrategy implements SwapStrategy {
   public static final InventoryStrategy INSTANCE = new InventoryStrategy();

   private InventoryStrategy() {
   }

   @Override
   public boolean swap(Result result) {
      if (result.type() != ResultType.INVENTORY && result.type() != ResultType.HOTBAR) {
         return false;
      } else {
         int slot = inventorySlot(result);
         InventoryUtil.click(slot, InventoryUtil.selected(), SlotActionType.SWAP);
         return true;
      }
   }

   @Override
   public boolean swapBack(int last, Result result) {
      return this.swap(result);
   }

   private static int inventorySlot(Result result) {
      return result.type() == ResultType.HOTBAR ? result.slot() + 36 : result.slot();
   }
}
