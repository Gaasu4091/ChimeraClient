package me.alpha432.chimeraclient.shoreline.util.player;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Set;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;

public class EnchantmentUtil {
   public static int getLevel(ItemStack var0, RegistryKey<Enchantment> var1) {
      if (!var0.getComponents().contains(DataComponentTypes.ENCHANTMENTS)) {
         return 0;
      } else {
         for (Entry var3 : ((ItemEnchantmentsComponent)var0.getComponents().get(DataComponentTypes.ENCHANTMENTS)).getEnchantmentEntries()) {
            if (((RegistryEntry)var3.getKey()).getKey().isPresent() && ((RegistryKey)((RegistryEntry)var3.getKey()).getKey().get()).equals(var1)) {
               return var3.getIntValue();
            }
         }

         return 0;
      }
   }

   public static boolean isFakeEnchant2b2t(ItemStack var0) {
       Set<it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<RegistryEntry<Enchantment>>> var1 = EnchantmentHelper.getEnchantments(var0).getEnchantmentEntries();
      if (var1.size() > 1) {
         return false;
      } else {
          for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<RegistryEntry<Enchantment>> var3 : var1) {
             RegistryEntry<Enchantment> var4 = var3.getKey();
            int var5 = var3.getIntValue();
            if (var5 == 0 && var4.getKey().isPresent() && var4.getKey().get() == Enchantments.PROTECTION) {
               return true;
            }
         }

         return false;
      }
   }
}
