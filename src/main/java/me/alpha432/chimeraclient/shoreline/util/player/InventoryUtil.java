package me.alpha432.chimeraclient.shoreline.util.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class InventoryUtil {
   public static int count(Item var0) {
      ClientPlayerEntity var1 = MinecraftClient.getInstance().player;
      if (var1 == null) {
         return 0;
      } else {
         int var2 = 0;

         for (int var3 = 0; var3 < var1.getInventory().size(); var3++) {
            ItemStack var4 = var1.getInventory().getStack(var3);
            if (var4.isOf(var0)) {
               var2 += var4.getCount();
            }
         }

         return var2;
      }
   }
}
