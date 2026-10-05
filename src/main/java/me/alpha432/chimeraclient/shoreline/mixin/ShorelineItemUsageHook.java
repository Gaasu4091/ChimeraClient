package me.alpha432.chimeraclient.shoreline.mixin;

import me.alpha432.chimeraclient.shoreline.PortSupport;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ItemUsageContext.class})
public abstract class ShorelineItemUsageHook {
   @Inject(
      method = {"getStack"},
      at = {@At("RETURN")},
      cancellable = true,
      remap = false
   )
   private void shoreline$serverPlacementStack(CallbackInfoReturnable<ItemStack> var1) {
      ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
      if (var2 != null && ((ItemStack)var1.getReturnValue()).equals(var2.getMainHandStack()) && PortSupport.Managers.INVENTORY.isDesynced()) {
         var1.setReturnValue(PortSupport.Managers.INVENTORY.getServerItem());
      }
   }
}
