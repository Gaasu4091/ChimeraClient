package me.alpha432.chimeraclient.features.modules.combat;

import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.util.inventory.InventoryUtil;
import me.alpha432.chimeraclient.util.inventory.Result;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class AutoXP extends Module {
   private final Setting<Double> delay = this.num("Delay", 1.0, 0.0, 20.0);
   private final Setting<Boolean> inventory = this.bool("Inventory", false);
   private final Setting<Boolean> antiPacketKick = this.bool("AntiPacketKick", false);
   private final Setting<Double> throwDuration = this.num("ThrowDuration", 4.0, 1.0, 100.0);
   private final Setting<Double> pauseDuration = this.num("PauseDuration", 1.0, 1.0, 100.0);
   private int actionTicks = 0;
   private int apkCycleTicks = 0;
   private boolean apkInThrowPhase = true;
   private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

   public AutoXP() {
      super("AutoXP", "Automatically throws experience bottles to repair armor", Module.Category.COMBAT);
   }

   @Override
   public void onEnable() {
      this.actionTicks = 0;
      this.apkCycleTicks = 0;
      this.apkInThrowPhase = true;
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         boolean needsRepair = false;

         for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = mc.player.getEquippedStack(slot);
            if (armor != null && !armor.isEmpty() && armor.isDamageable() && armor.getDamage() > 0) {
               needsRepair = true;
               break;
            }
         }

         if (!needsRepair) {
            this.disable();
         } else {
            if (this.antiPacketKick.getValue()) {
               this.apkCycleTicks++;
               if (!this.apkInThrowPhase) {
                  if (this.apkCycleTicks >= this.pauseDuration.getValue().intValue()) {
                     this.apkInThrowPhase = true;
                     this.apkCycleTicks = 0;
                     this.actionTicks = 0;
                  }

                  return;
               }

               if (this.apkCycleTicks >= this.throwDuration.getValue().intValue()) {
                  this.apkInThrowPhase = false;
                  this.apkCycleTicks = 0;
                  this.actionTicks = 0;
                  return;
               }
            }

            this.actionTicks++;
            if (this.actionTicks >= this.delay.getValue().intValue()) {
               Result xpResult = InventoryUtil.find(Items.EXPERIENCE_BOTTLE, this.inventory.getValue() ? InventoryUtil.FULL_SCOPE : InventoryUtil.HOTBAR_SCOPE);
               if (xpResult.found()) {
                  InventoryUtil.withSwap(xpResult, () -> {
                     mc.interactionManager.interactItem(mc.player, xpResult.hand());
                     mc.player.swingHand(xpResult.hand());
                  });
               }

               this.actionTicks = 0;
            }
         }
      }
   }
}
