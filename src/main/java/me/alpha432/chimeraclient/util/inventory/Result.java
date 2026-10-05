package me.alpha432.chimeraclient.util.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public record Result(int slot, ItemStack stack, ResultType type, boolean holding) {
   public Result(int slot, ItemStack stack, ResultType type) {
      this(slot, stack, type, isHolding(type, slot));
   }

   static Result fromOffhand(ItemStack stack) {
      return new Result(-1, stack, ResultType.OFFHAND);
   }

   public Hand hand() {
      return this.type == ResultType.OFFHAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
   }

   public boolean found() {
      return this.type != ResultType.NONE;
   }

   private static boolean isHolding(ResultType type, int slot) {
      return type == ResultType.OFFHAND || type == ResultType.HOTBAR && slot == InventoryUtil.selected();
   }
}
