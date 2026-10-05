package me.alpha432.chimeraclient.manager;

import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.event.Stage;
import me.alpha432.chimeraclient.event.impl.entity.DeathEvent;
import me.alpha432.chimeraclient.event.impl.entity.player.TickEvent;
import me.alpha432.chimeraclient.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.chimeraclient.event.impl.input.KeyInputEvent;
import me.alpha432.chimeraclient.event.impl.input.MouseInputEvent;
import me.alpha432.chimeraclient.event.impl.network.ChatEvent;
import me.alpha432.chimeraclient.event.impl.network.PacketEvent;
import me.alpha432.chimeraclient.event.impl.render.Render2DEvent;
import me.alpha432.chimeraclient.event.impl.render.Render3DEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.Feature;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

public class EventManager extends Feature {
   public void init() {
      EVENT_BUS.register(this);
   }

   public void onUnload() {
      EVENT_BUS.unregister(this);
   }

   @Subscribe
   public void onTick(TickEvent event) {
      if (!nullCheck()) {
         ChimeraClient.moduleManager.onTick();

         for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != null && !(player.getHealth() > 0.0F)) {
               EVENT_BUS.post(new DeathEvent(player));
            }
         }
      }
   }

   @Subscribe
   public void onUpdateWalkingPlayer(UpdateWalkingPlayerEvent event) {
      if (!nullCheck()) {
         if (event.getStage() == Stage.PRE) {
            ChimeraClient.speedManager.update();
            ChimeraClient.rotationManager.updateRotations();
            ChimeraClient.positionManager.updatePosition();
         }

         if (event.getStage() == Stage.POST) {
            ChimeraClient.rotationManager.restoreRotations();
            ChimeraClient.positionManager.restorePosition();
         }
      }
   }

   @Subscribe
   public void onPacketReceive(PacketEvent.Receive event) {
      ChimeraClient.serverManager.onPacketReceived();
      if (event.getPacket() instanceof WorldTimeUpdateS2CPacket) {
         ChimeraClient.serverManager.update();
      }

      if (event.getPacket() instanceof CustomPayloadS2CPacket var2) {
         if (var2.payload() instanceof BrandCustomPayload brandPayload) {
            ChimeraClient.serverManager.setServerBrand(brandPayload.brand());
         }
      }
   }

   @Subscribe
   public void onWorldRender(Render3DEvent event) {
      ChimeraClient.moduleManager.onRender3D(event);
   }

   @Subscribe
   public void onRenderGameOverlayEvent(Render2DEvent event) {
      ChimeraClient.moduleManager.onRender2D(event);
   }

   @Subscribe
   public void onKeyInput(KeyInputEvent event) {
      ChimeraClient.moduleManager.onKeyPressed(event.getKey());
   }

   @Subscribe
   public void onMouseInput(MouseInputEvent event) {
      if (event.getAction() == 1) {
         ChimeraClient.moduleManager.onMouseClicked(event.getButton());
      }
   }

   @Subscribe
   public void onChatSent(ChatEvent event) {
      String message = event.getMessage();
      if (message.startsWith(ChimeraClient.commandManager.getCommandPrefix())) {
         event.cancel();
         ChimeraClient.commandManager.onChatSent(message);
      }
   }
}
