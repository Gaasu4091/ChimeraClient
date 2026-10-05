package me.alpha432.chimeraclient.features.modules.misc;

import me.alpha432.chimeraclient.event.impl.network.PacketEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.modules.Module;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;

public class Xcarry extends Module {
   public Xcarry() {
      super("Xcarry", "Keeps items in your crafting slots when closing the inventory.", Module.Category.MISC);
   }

   @Subscribe
   private void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck() && mc.player != null) {
         if (event.getPacket() instanceof CloseHandledScreenC2SPacket pkt && pkt.getSyncId() == mc.player.playerScreenHandler.syncId) {
            event.cancel();
         }
      }
   }
}
