package me.alpha432.chimeraclient.util.inventory;

import java.util.EnumSet;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import me.alpha432.chimeraclient.util.inventory.strategy.HoldingStrategy;
import me.alpha432.chimeraclient.util.inventory.strategy.HotbarStrategy;
import me.alpha432.chimeraclient.util.inventory.strategy.InventoryStrategy;
import me.alpha432.chimeraclient.util.inventory.strategy.SwapStrategy;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public final class InventoryUtil implements Util {
   static final Result NONE = new Result(-1, ItemStack.EMPTY, ResultType.NONE);
   public static final EnumSet<ResultType> HOTBAR_SCOPE = EnumSet.of(ResultType.OFFHAND, ResultType.HOTBAR);
   public static final EnumSet<ResultType> INVENTORY_SCOPE = EnumSet.of(ResultType.OFFHAND, ResultType.INVENTORY);
   public static final EnumSet<ResultType> FULL_SCOPE = EnumSet.of(ResultType.OFFHAND, ResultType.HOTBAR, ResultType.INVENTORY);
   private static final List<SwapStrategy> STRATEGIES = List.of(HoldingStrategy.INSTANCE, HotbarStrategy.INSTANCE, InventoryStrategy.INSTANCE);

   private InventoryUtil() {
      throw new AssertionError();
   }

   public static ItemStack cursor() {
      return mc.player.currentScreenHandler.getCursorStack();
   }

   public static int selected() {
      return mc.player.getInventory().getSelectedSlot();
   }

   public static void click(int slot, int button, SlotActionType type) {
      int id = mc.player.currentScreenHandler.syncId;
      mc.interactionManager.clickSlot(id, slot, button, type, mc.player);
   }

   public static void withSwap(Result result, Runnable action) {
      withSwap(result, r -> action.run());
   }

   public static void withSwap(Result result, Consumer<Result> action) {
      int lastSlot = selected();
      if (swap(result)) {
         action.accept(result);
         swapBack(result, lastSlot);
      }
   }

   public static void swap(int to) {
      if (to >= 0 && to <= 8) {
         mc.player.getInventory().setSelectedSlot(to);
         mc.interactionManager.syncSelectedSlot();
      }
   }

   public static boolean swap(Result result) {
      for (SwapStrategy strategy : STRATEGIES) {
         if (strategy.swap(result)) {
            return true;
         }
      }

      return false;
   }

   public static boolean swapBack(Result result, int last) {
      for (SwapStrategy strategy : STRATEGIES) {
         if (strategy.swapBack(last, result)) {
            return true;
         }
      }

      return false;
   }

   public static Result find(Item target, EnumSet<ResultType> scopes) {
      return find((Predicate<ItemStack>)(stack -> stack.isOf(target)), scopes);
   }

   public static Result find(Predicate<ItemStack> predicate, EnumSet<ResultType> scopes) {
      return find((item, scope) -> scopes.contains(scope) && predicate.test(item));
   }

   public static Result find(BiPredicate<ItemStack, ResultType> predicate) {
      ItemStack offhand = mc.player.getOffHandStack();
      if (predicate.test(offhand, ResultType.OFFHAND)) {
         return Result.fromOffhand(offhand);
      } else {
         for (int i = 0; i < 36; i++) {
            ItemStack item = mc.player.getInventory().getStack(i);
            ResultType type = i < 9 ? ResultType.HOTBAR : ResultType.INVENTORY;
            if (predicate.test(item, type)) {
               return new Result(i, item, type);
            }
         }

         return NONE;
      }
   }
}
