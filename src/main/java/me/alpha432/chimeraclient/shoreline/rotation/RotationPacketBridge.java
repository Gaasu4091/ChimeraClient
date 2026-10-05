package me.alpha432.chimeraclient.shoreline.rotation;

import me.alpha432.chimeraclient.event.impl.network.PacketEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.shoreline.PortSupport;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.Packet;

public final class RotationPacketBridge {
   @Subscribe
   public void outbound(PacketEvent.Send var1) {
      PortSupport.observeOutbound(var1.getPacket());
   }

   @Subscribe
   public void inbound(PacketEvent.Receive var1) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientWorld var3 = var2.world;
      Packet var4 = var1.getPacket();
      var2.execute(() -> {
         if (var3 != null && var2.world == var3) {
            PortSupport.observeInbound(var4);
         }
      });
   }
}
