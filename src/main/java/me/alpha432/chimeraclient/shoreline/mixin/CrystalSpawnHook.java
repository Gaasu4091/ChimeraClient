package me.alpha432.chimeraclient.shoreline.mixin;

import me.alpha432.chimeraclient.features.modules.combat.CrystalAura;
import me.alpha432.chimeraclient.shoreline.PortSupport;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientWorld.class})
public class CrystalSpawnHook {
   @Inject(
      method = {"addEntity"},
      at = {@At("TAIL")},
      remap = false
   )
   private void chimera$crystalSpawn(Entity var1, CallbackInfo var2) {
      CrystalAura var3 = CrystalAura.getInstance();
      MinecraftClient var4 = MinecraftClient.getInstance();
      if (var4.player != null && var4.world == (Object)this && var1 instanceof EndCrystalEntity && var3 != null && var3.isEnabled()) {
         var3.onAddEntity(new PortSupport.AddEntityEvent(var1));
      }
   }
}
