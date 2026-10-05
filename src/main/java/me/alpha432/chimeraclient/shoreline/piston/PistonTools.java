package me.alpha432.chimeraclient.shoreline.piston;

import me.alpha432.chimeraclient.shoreline.util.Globals;
import me.alpha432.chimeraclient.shoreline.util.player.EnchantmentUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;

public final class PistonTools implements Globals {
   private PistonTools() {
   }

   public static int getBestToolNoFallback(BlockState var0) {
      if (var0.getBlock() == Blocks.COBWEB) {
         for (int var1 = 0; var1 < 9; var1++) {
            ItemStack var2 = mc.player.getInventory().getStack(var1);
            if (!var2.isEmpty() && var2.isIn(ItemTags.SWORDS)) {
               return var1;
            }
         }
      }

      int var7 = -1;
      float var8 = 0.0F;

      for (int var3 = 0; var3 < 9; var3++) {
         ItemStack var4 = mc.player.getInventory().getStack(var3);
         if (!var4.isEmpty()
            && (
               var4.isIn(ItemTags.SWORDS)
                  || var4.isIn(ItemTags.AXES)
                  || var4.isIn(ItemTags.PICKAXES)
                  || var4.isIn(ItemTags.SHOVELS)
                  || var4.isIn(ItemTags.HOES)
            )) {
            float var5 = var4.getMiningSpeedMultiplier(var0);
            int var6 = EnchantmentUtil.getLevel(var4, Enchantments.EFFICIENCY);
            if (var6 > 0) {
               var5 += var6 * var6 + 1.0F;
            }

            if (var5 > var8) {
               var8 = var5;
               var7 = var3;
            }
         }
      }

      return var7;
   }
}
