package me.alpha432.chimeraclient.mio;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import me.alpha432.chimeraclient.event.impl.entity.player.TickEvent;
import me.alpha432.chimeraclient.event.impl.network.PacketEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.mio.mixin.MioInteractPacketAccess;
import me.alpha432.chimeraclient.mio.support.GhostSupport;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

public final class MioState implements Util {
   public static final Map<Integer, MioState.Breaking> breaks = new LinkedHashMap<>();
   public static final Map<UUID, Integer> pops = new HashMap<>();
   public static final Map<UUID, MioState.Logout> logouts = new LinkedHashMap<>();
   public static final Map<Long, Boolean> chunks = new LinkedHashMap<>();
   public static final Map<Vec3d, Long> chorus = new LinkedHashMap<>();
   public static String music = "";
   public static long hitTime;
   private static Object world;
   private static final Map<UUID, MioState.Logout> players = new HashMap<>();
   private static final MioState INSTANCE = new MioState();

   public static void init() {
      EVENT_BUS.register(INSTANCE);
   }

   @Subscribe
   public void onTick(TickEvent var1) {
      tick();
   }

   public static void tick() {
      if (mc.world != world) {
         world = mc.world;
         breaks.clear();
         pops.clear();
         logouts.clear();
         chunks.clear();
         chorus.clear();
         players.clear();
         GhostSupport.clear();
         hitTime = 0L;
      }

      if (mc.world != null) {
         for (PlayerEntity var1 : mc.world.getPlayers()) {
            players.put(
               var1.getUuid(),
               new MioState.Logout(
                  var1.getUuid(),
                  var1.getName().getString(),
                  var1.getBoundingBox(),
                  var1.getHealth() + var1.getAbsorptionAmount(),
                  System.currentTimeMillis(),
                  pops.getOrDefault(var1.getUuid(), 0)
               )
            );
         }

         breaks.values().removeIf(var0 -> System.currentTimeMillis() - var0.time() > 3000L);
         chorus.values().removeIf(var0 -> System.currentTimeMillis() - var0 > 3500L);
      }
   }

   @Subscribe
   public void receive(PacketEvent.Receive var1) {
      Packet var2 = var1.getPacket();
      mc.execute(() -> accept(var2));
   }

   @Subscribe
   public void send(PacketEvent.Send var1) {
      if (!var1.isCancelled() && MioConfiguredModule.active("Hitmarker") != null && var1.getPacket() instanceof PlayerInteractEntityC2SPacket var2) {
         var2.handle(new Handler() {
            public void interact(Hand hand) {
            }

            public void interactAt(Hand hand, Vec3d pos) {
            }

            public void attack() {
               int entityId = ((MioInteractPacketAccess)var2).mio$entityId();
               Util.mc.execute(() -> {
                  if (Util.mc.world != null) {
                     Entity var1xx = Util.mc.world.getEntityById(entityId);
                     if (var1xx != null) {
                        MioState.hit(var1xx);
                     }
                  }
               });
            }
         });
      }
   }

   private static void accept(Packet<?> var0) {
      if (mc.world != null) {
         if (var0 instanceof BundleS2CPacket var11) {
            for (Packet var17 : var11.getPackets()) {
               accept(var17);
            }
         } else {
            if (var0 instanceof BlockBreakingProgressS2CPacket var1) {
               if (var1.getProgress() >= 0 && var1.getProgress() <= 9) {
                  breaks.put(
                     var1.getEntityId(), new MioState.Breaking(var1.getPos().toImmutable(), var1.getProgress(), var1.getEntityId(), System.currentTimeMillis())
                  );
               } else {
                  breaks.remove(var1.getEntityId());
               }
            }

            if (var0 instanceof EntityStatusS2CPacket var6 && var6.getEntity(mc.world) instanceof PlayerEntity var2) {
               if (var6.getStatus() == 35) {
                  pops.merge(var2.getUuid(), 1, Integer::sum);
                  GhostSupport.pop(var2.getUuid(), false);
               }

               if (var6.getStatus() == 3) {
                  GhostSupport.pop(var2.getUuid(), true);
               }
            }

            if (var0 instanceof PlaySoundS2CPacket var7 && var7.getSound().value() == SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT) {
               chorus.put(new Vec3d(var7.getX(), var7.getY(), var7.getZ()), System.currentTimeMillis());

               while (chorus.size() > 256) {
                  chorus.remove(chorus.keySet().iterator().next());
               }
            }

            if (var0 instanceof PlayerRemoveS2CPacket var8) {
               for (UUID var15 : var8.profileIds()) {
                  MioState.Logout var4 = players.remove(var15);
                  if (var4 != null && mc.player != null && !mc.player.getUuid().equals(var15)) {
                     logouts.put(var15, var4);
                     GhostSupport.logout(var15);
                     MioConfiguredModule var5 = MioConfiguredModule.active("LogoutSpots");
                     if (var5 != null && var5.b("sounds") && var5.b("logout")) {
                        sound(var5.s("sound"), 1.0F);
                     }
                  }
               }
            }

            if (var0 instanceof PlayerListS2CPacket var9) {
               for (Entry var16 : var9.getPlayerAdditionEntries()) {
                  if (logouts.remove(var16.profileId()) != null) {
                     GhostSupport.login(var16.profileId());
                     MioConfiguredModule var18 = MioConfiguredModule.active("LogoutSpots");
                     if (var18 != null && var18.b("sounds") && var18.b("login")) {
                        sound(var18.s("sound2"), 1.0F);
                     }
                  }
               }
            }

            if (var0 instanceof ChunkDataS2CPacket var10) {
               chunks.put(ChunkPos.toLong(var10.getChunkX(), var10.getChunkZ()), false);

               while (chunks.size() > 8192) {
                  chunks.remove(chunks.keySet().iterator().next());
               }
            }
         }
      }
   }

   public static void hit(Entity var0) {
      MioConfiguredModule var1 = MioConfiguredModule.active("Hitmarker");
      if (var1 != null) {
         boolean var2 = var0 instanceof PlayerEntity
            ? var1.b("players")
            : (
               var0 instanceof EndCrystalEntity
                  ? var1.b("crystals")
                  : (var0 instanceof HostileEntity ? var1.b("hostiles") : var0 instanceof AnimalEntity && var1.b("animals"))
            );
         if (var2) {
            hitTime = System.currentTimeMillis();
            if (var1.b("sound2")) {
               sound(var1.s("sound"), var1.f("volume"));
            }
         }
      }
   }

   public static void sound(String var0, float var1) {
      String var2 = var0.substring(var0.indexOf(58) + 1);
      mc.getSoundManager().play(PositionedSoundInstance.ui(SoundEvent.of(Identifier.of("chimeraclient", "mio." + var2)), 1.0F, var1));
   }

   public record Breaking(BlockPos pos, int stage, int entityId, long time) {
   }

   public record Logout(UUID uuid, String name, Box box, float health, long time, int pops) {
   }
}
