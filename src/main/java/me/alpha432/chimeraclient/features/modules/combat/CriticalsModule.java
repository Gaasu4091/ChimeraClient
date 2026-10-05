package me.alpha432.chimeraclient.features.modules.combat;

import me.alpha432.chimeraclient.event.impl.network.PacketEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.modules.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.InteractType;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;

public class CriticalsModule extends Module {
   public CriticalsModule() {
      super("Criticals", "Makes you do critical hits", Module.Category.COMBAT);
   }

   @Subscribe
   private void onPacketSend(PacketEvent.Send event) {
      if (event.getPacket() instanceof PlayerInteractEntityC2SPacket packet && packet.type.getType() == InteractType.ATTACK) {
         Entity entity = mc.world.getEntityById(packet.entityId);
         if (entity == null || entity instanceof EndCrystalEntity || !mc.player.isOnGround() || !(entity instanceof LivingEntity)) {
            return;
         }

         boolean bl = mc.player.horizontalCollision;
         mc.player.networkHandler.sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.1F, mc.player.getZ(), false, bl));
         mc.player.networkHandler.sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, bl));
         mc.player.addCritParticles(entity);
      }
   }

   @Override
   public String getDisplayInfo() {
      return "Packet";
   }
}
