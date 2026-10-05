package me.alpha432.chimeraclient.features.modules.combat;

import me.alpha432.chimeraclient.event.impl.input.MouseInputEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.util.inventory.InventoryUtil;
import me.alpha432.chimeraclient.util.inventory.Result;
import me.alpha432.chimeraclient.util.inventory.ResultType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.ThrowablePotionItem;
import net.minecraft.item.WindChargeItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.screen.slot.SlotActionType;

public class Offhand extends Module {
   private final Setting<Offhand.Mode> mode = this.mode("Mode", Offhand.Mode.TOTEM);
   private final Setting<Double> totemHealth = this.num("TotemHealth", 10.0, 0.0, 36.0);
   private final Setting<Double> gappleHealth = this.num("GappleHealth", 10.0, 0.0, 36.0);
   private final Setting<Boolean> craftSlots = this.bool("CraftSlots", true);
   private final Setting<Boolean> clickGapple = this.bool("ClickGapple", true);

   public Offhand() {
      super("Offhand", "Manages totem, crystal or gapple in your offhand", Module.Category.COMBAT);
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         double health = mc.player.getHealth();
         Item target = null;
         boolean isRightClicking = mc.options.useKey.isPressed();
         boolean canClickGapple = this.clickGapple.getValue()
            && isRightClicking
            && !this.hasUseAction(mc.player.getMainHandStack())
            && health > this.gappleHealth.getValue();
         if (health <= this.totemHealth.getValue()) {
            target = Items.TOTEM_OF_UNDYING;
         } else if (canClickGapple) {
            target = Items.ENCHANTED_GOLDEN_APPLE;
         } else {
            switch ((Offhand.Mode)this.mode.getValue()) {
               case TOTEM:
                  target = Items.TOTEM_OF_UNDYING;
                  break;
               case CRYSTAL:
                  target = Items.END_CRYSTAL;
                  break;
               case GAPPLE:
                  if (health > this.gappleHealth.getValue()) {
                     target = Items.ENCHANTED_GOLDEN_APPLE;
                  }
            }
         }

         if (target != null) {
            this.equip(target);
         }
      }
   }

   @Subscribe
   private void onMouseInput(MouseInputEvent event) {
      if (this.clickGapple.getValue()) {
         if (event.getButton() == 1 && event.getAction() == 1) {
            if (!nullCheck() && mc.currentScreen == null) {
               if (!(mc.player.getHealth() <= this.gappleHealth.getValue())) {
                  ItemStack mainHand = mc.player.getMainHandStack();
                  if (!this.hasUseAction(mainHand)) {
                     this.equip(Items.ENCHANTED_GOLDEN_APPLE);
                  }
               }
            }
         }
      }
   }

   private boolean hasUseAction(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         Item item = stack.getItem();
         if (item instanceof BlockItem) {
            return true;
         } else if (item instanceof BucketItem) {
            return true;
         } else if (item instanceof SpawnEggItem) {
            return true;
         } else {
            return stack.getUseAction() != UseAction.NONE
               ? true
               : item instanceof EnderPearlItem
                  || item instanceof SnowballItem
                  || item instanceof EggItem
                  || item instanceof ExperienceBottleItem
                  || item instanceof ThrowablePotionItem
                  || item instanceof FireworkRocketItem
                  || item instanceof WindChargeItem;
         }
      }
   }

   private void equip(Item item) {
      if (!mc.player.getOffHandStack().isOf(item)) {
         Result result = this.locate(item);
         if (result.found() && result.type() != ResultType.OFFHAND) {
            int slot = this.toContainerSlot(result);
            if (slot >= 0) {
               if (mc.player != null && mc.interactionManager != null) {
                  mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, 40, SlotActionType.SWAP, mc.player);
               }
            }
         }
      }
   }

   private Result locate(Item item) {
      Result result = InventoryUtil.find(item, InventoryUtil.FULL_SCOPE);
      if (result.found()) {
         return result;
      } else {
         if (this.craftSlots.getValue() && mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
            for (int i = 1; i <= 4; i++) {
               ItemStack stack = mc.player.playerScreenHandler.getSlot(i).getStack();
               if (stack.isOf(item)) {
                  return new Result(i, stack, ResultType.INVENTORY);
               }
            }
         }

         return result;
      }
   }

   private int toContainerSlot(Result result) {
      return switch (result.type()) {
         case HOTBAR -> result.slot() + 36;
         case INVENTORY -> result.slot();
         case OFFHAND -> 45;
         default -> -1;
      };
   }

   @Override
   public String getDisplayInfo() {
      return this.mode.getValue().name().charAt(0) + this.mode.getValue().name().substring(1).toLowerCase();
   }

   public static enum Mode {
      TOTEM,
      CRYSTAL,
      GAPPLE;
   }
}
