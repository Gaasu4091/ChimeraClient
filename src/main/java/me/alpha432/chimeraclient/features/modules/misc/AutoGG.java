package me.alpha432.chimeraclient.features.modules.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.event.impl.network.PacketEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

public class AutoGG extends Module {
   private final Setting<Double> range = this.num("Range", 20.0, 1.0, 100.0);
   private final Setting<Integer> cooldown = this.num("Cooldown (MS)", 1000, 0, 10000);
   private final Setting<String> totemMessages = this.str("TotemMsgs", "<player> トーテム消費！ <pop>");
   private final Setting<String> deathMessages = this.str("DeathMsgs", "<player> 負けてて草");
   private final Set<PlayerEntity> deadPlayers = ConcurrentHashMap.newKeySet();
   private final Map<String, Integer> popCounts = new ConcurrentHashMap<>();
   private final Random random = new Random();
   private final Queue<String> messageQueue = new ConcurrentLinkedQueue<>();
   private long lastMessageTime = 0L;

   public AutoGG() {
      super("AutoGG", "Sends messages when an enemy pops a totem or dies.", Module.Category.MISC);
   }

   @Override
   public void onDisable() {
      this.deadPlayers.clear();
      this.popCounts.clear();
      this.messageQueue.clear();
   }

   @Override
   public void onTick() {
      if (!nullCheck() && mc.world != null && mc.player != null) {
         for (PlayerEntity target : mc.world.getPlayers()) {
            if (target != mc.player && !ChimeraClient.friendManager.isFriend(target) && !(mc.player.distanceTo(target) > this.range.getValue())) {
               if (!target.isDead() && !(target.getHealth() <= 0.0F)) {
                  this.deadPlayers.remove(target);
               } else if (this.deadPlayers.add(target)) {
                  this.enqueueMessage(target, false);
                  this.popCounts.remove(target.getGameProfile().name());
               }
            }
         }

         this.flushQueue();
      }
   }

   @Subscribe
   private void onPacketReceive(PacketEvent.Receive event) {
      if (!nullCheck() && mc.world != null && mc.player != null) {
         if (event.getPacket() instanceof EntityStatusS2CPacket pkt
            && pkt.getStatus() == 35
            && pkt.getEntity(mc.world) instanceof PlayerEntity target
            && target != mc.player) {
            if (ChimeraClient.friendManager.isFriend(target)) {
               return;
            }

            if (mc.player.distanceTo(target) <= this.range.getValue()) {
               String name = target.getGameProfile().name();
               this.popCounts.put(name, this.popCounts.getOrDefault(name, 0) + 1);
               this.enqueueMessage(target, true);
            }
         }
      }
   }

   private void enqueueMessage(PlayerEntity target, boolean isTotem) {
      String raw = isTotem ? this.totemMessages.getValue() : this.deathMessages.getValue();
      if (raw != null && !raw.trim().isEmpty()) {
         List<String> validMessages = new ArrayList<>();

         for (String part : raw.split("\\|")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
               validMessages.add(trimmed);
            }
         }

         if (!validMessages.isEmpty()) {
            String chosen = validMessages.get(this.random.nextInt(validMessages.size()));
            String playerName = target.getGameProfile().name();
            int pops = this.popCounts.getOrDefault(playerName, 0);
            String finalMessage = chosen.replace("<player>", playerName).replace("<pop>", String.valueOf(pops));
            this.messageQueue.offer(finalMessage);
         }
      }
   }

   private void flushQueue() {
      if (!this.messageQueue.isEmpty()) {
         if (mc.getNetworkHandler() != null) {
            long now = System.currentTimeMillis();
            if (now - this.lastMessageTime >= this.cooldown.getValue().intValue()) {
               String message = this.messageQueue.poll();
               if (message != null) {
                  mc.getNetworkHandler().sendChatMessage(message);
                  this.lastMessageTime = now;
               }
            }
         }
      }
   }
}
