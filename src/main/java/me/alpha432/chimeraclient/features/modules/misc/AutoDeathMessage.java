package me.alpha432.chimeraclient.features.modules.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.event.impl.network.PacketEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

public class AutoDeathMessage extends Module {
   private final Setting<Double> range = this.num("Range", 20.0, 1.0, 100.0);
   private final Setting<Integer> cooldown = this.num("Cooldown (MS)", 1000, 0, 10000);
   private final Setting<String> totemMessages = this.str("TotemMsgs", "<player> のせいでトーテム消費！ <pop>回目");
   private final Setting<String> deathMessages = this.str("DeathMsgs", "<player> に負けてて草");
   private boolean wasDead = false;
   private int myPopCount = 0;
   private final Random random = new Random();
   private final Queue<String> messageQueue = new ConcurrentLinkedQueue<>();
   private long lastMessageTime = 0L;

   public AutoDeathMessage() {
      super("AutoDeathMessage", "Sends messages when you pop a totem or die.", Module.Category.MISC);
   }

   @Override
   public void onDisable() {
      this.wasDead = false;
      this.myPopCount = 0;
      this.messageQueue.clear();
   }

   @Override
   public void onTick() {
      if (!nullCheck() && mc.world != null && mc.player != null) {
         boolean isDead = mc.player.isDead() || mc.player.getHealth() <= 0.0F;
         if (isDead) {
            if (!this.wasDead) {
               PlayerEntity killer = this.getClosestEnemy();
               this.enqueueMessage(killer, false);
               this.myPopCount = 0;
               this.wasDead = true;
            }
         } else {
            this.wasDead = false;
         }

         this.flushQueue();
      }
   }

   @Subscribe
   private void onPacketReceive(PacketEvent.Receive event) {
      if (!nullCheck() && mc.world != null && mc.player != null) {
         if (event.getPacket() instanceof EntityStatusS2CPacket pkt && pkt.getStatus() == 35) {
            Entity entity = pkt.getEntity(mc.world);
            if (entity == mc.player) {
               this.myPopCount++;
               PlayerEntity attacker = this.getClosestEnemy();
               this.enqueueMessage(attacker, true);
            }
         }
      }
   }

   private PlayerEntity getClosestEnemy() {
      PlayerEntity closest = null;
      double minDistance = this.range.getValue();

      for (PlayerEntity target : mc.world.getPlayers()) {
         if (target != mc.player && !ChimeraClient.friendManager.isFriend(target)) {
            double dist = mc.player.distanceTo(target);
            if (dist <= minDistance) {
               minDistance = dist;
               closest = target;
            }
         }
      }

      return closest;
   }

   private void enqueueMessage(PlayerEntity enemy, boolean isTotem) {
      if (enemy != null) {
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
               String enemyName = enemy.getGameProfile().name();
               String finalMessage = chosen.replace("<player>", enemyName).replace("<pop>", String.valueOf(this.myPopCount));
               this.messageQueue.offer(finalMessage);
            }
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
