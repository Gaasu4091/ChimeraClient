package me.alpha432.chimeraclient.features.modules.player;

import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.features.modules.Module;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;

public class NoFallModule extends Module {
   public NoFallModule() {
      super("NoFall", "Removes fall damage", Module.Category.PLAYER);
   }

   @Override
   public void onTick() {
      if (!mc.player.isOnGround() && ChimeraClient.positionManager.getFallDistance() > 3.0) {
         boolean bl = mc.player.horizontalCollision;
         Full pakcet = new Full(mc.player.getX(), mc.player.getY() + 1.0E-9, mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), false, bl);
         mc.player.networkHandler.sendPacket(pakcet);
      }
   }
}
