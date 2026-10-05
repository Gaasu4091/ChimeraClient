package me.alpha432.chimeraclient.shoreline.mixin;

import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({ClientWorld.class})
public interface PendingUpdatesAccess {
   @Invoker(
      value = "getPendingUpdateManager",
      remap = false
   )
   PendingUpdateManager chimera$getPendingUpdates();
}
