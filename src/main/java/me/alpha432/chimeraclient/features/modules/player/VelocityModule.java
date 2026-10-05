package me.alpha432.chimeraclient.features.modules.player;

import me.alpha432.chimeraclient.event.impl.network.PacketEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.modules.Module;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;

public class VelocityModule extends Module {
   public VelocityModule() {
      super("Velocity", "Removes velocity from explosions and entities", Module.Category.PLAYER);
   }

   @Subscribe
   private void onPacketReceive(PacketEvent.Receive event) {
      if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket || event.getPacket() instanceof ExplosionS2CPacket) {
         event.cancel();
      }
   }
}
