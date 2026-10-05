package me.alpha432.chimeraclient.util.inventory.strategy;

import me.alpha432.chimeraclient.util.inventory.InventoryUtil;
import me.alpha432.chimeraclient.util.inventory.Result;
import me.alpha432.chimeraclient.util.inventory.ResultType;

public final class HotbarStrategy implements SwapStrategy {
   public static final HotbarStrategy INSTANCE = new HotbarStrategy();

   private HotbarStrategy() {
   }

   @Override
   public boolean swap(Result result) {
      if (result.type() == ResultType.HOTBAR) {
         InventoryUtil.swap(result.slot());
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean swapBack(int last, Result result) {
      if (result.type() == ResultType.HOTBAR) {
         InventoryUtil.swap(last);
         return true;
      } else {
         return false;
      }
   }
}
