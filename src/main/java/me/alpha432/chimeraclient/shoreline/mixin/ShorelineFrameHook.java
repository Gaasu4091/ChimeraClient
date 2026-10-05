package me.alpha432.chimeraclient.shoreline.mixin;

import me.alpha432.chimeraclient.features.modules.combat.CrystalAura;
import me.alpha432.chimeraclient.shoreline.PortSupport;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MinecraftClient.class})
public class ShorelineFrameHook {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      remap = false
   )
   private void shoreline$runTick(boolean var1, CallbackInfo var2) {
      MinecraftClient var3 = MinecraftClient.getInstance();
      CrystalAura var4 = CrystalAura.getInstance();
      if (var3.player != null && var3.world != null && var4 != null && var4.isEnabled()) {
         var4.onRunTick(new PortSupport.RunTickEvent());
      }
   }
}
