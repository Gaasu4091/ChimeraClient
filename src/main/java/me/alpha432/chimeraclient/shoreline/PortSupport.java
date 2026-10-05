package me.alpha432.chimeraclient.shoreline;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.modules.player.SpeedMine;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.shoreline.mixin.PendingUpdatesAccess;
import me.alpha432.chimeraclient.shoreline.mixin.QuietPacketAccess;
import me.alpha432.chimeraclient.shoreline.rotation.Rotation;
import me.alpha432.chimeraclient.shoreline.rotation.RotationEvents;
import me.alpha432.chimeraclient.shoreline.rotation.RotationManager;
import me.alpha432.chimeraclient.shoreline.rotation.RotationPacketBridge;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Direction.Type;

public final class PortSupport {
   static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final Map<Integer, Long> totemTimes = new HashMap<>();
   private static final Map<Integer, PortSupport.MineProgress> mines = new HashMap<>();

   public static List<ItemStack> armor(LivingEntity var0) {
      return List.of(
         var0.getEquippedStack(EquipmentSlot.HEAD),
         var0.getEquippedStack(EquipmentSlot.CHEST),
         var0.getEquippedStack(EquipmentSlot.LEGS),
         var0.getEquippedStack(EquipmentSlot.FEET)
      );
   }

   public static void reset() {
      totemTimes.clear();
      mines.clear();
      PortSupport.Managers.INVENTORY.serverSlot = -1;
   }

   public static void observeInbound(Packet<?> var0) {
      if (mc.world != null) {
         if (var0 instanceof BundleS2CPacket var6) {
            for (Packet var3 : var6.getPackets()) {
               observeInbound(var3);
            }
         } else {
            if (var0 instanceof UpdateSelectedSlotS2CPacket var1) {
               PortSupport.Managers.INVENTORY.serverSlot = var1.slot();
            }

            if (var0 instanceof EntityStatusS2CPacket var4 && var4.getStatus() == 35) {
               Entity var2 = var4.getEntity(mc.world);
               if (var2 != null) {
                  totemTimes.put(var2.getId(), System.currentTimeMillis());
               }
            }

            if (var0 instanceof BlockBreakingProgressS2CPacket var5) {
               if (var5.getProgress() >= 0 && var5.getProgress() <= 9) {
                  mines.put(var5.getEntityId(), new PortSupport.MineProgress(var5.getPos().toImmutable(), var5.getProgress(), System.currentTimeMillis()));
               } else {
                  mines.remove(var5.getEntityId());
               }
            }
         }
      }
   }

   public static void observeOutbound(Packet<?> var0) {
      if (var0 instanceof UpdateSelectedSlotC2SPacket var1) {
         PortSupport.Managers.INVENTORY.serverSlot = var1.getSelectedSlot();
      }

      if (var0 instanceof PlayerMoveC2SPacket var2 && var2.changesLook() && mc.player != null) {
         PortSupport.Managers.ROTATION.onPacketOutbound(new RotationEvents.PacketEvent.Outbound(var0));
      }
   }

   public record AddEntityEvent(Entity entity) {
      public Entity getEntity() {
         return this.entity;
      }
   }

   public static class AutoMineModule {
      private static final PortSupport.AutoMineModule INSTANCE = new PortSupport.AutoMineModule();
      private static final Field TASK = field(SpeedMine.class, "currentTask");
      private static final Field SWAP = field(SpeedMine.class, "swap");

      private static Field field(Class<?> var0, String var1) {
         try {
            Field var2 = var0.getDeclaredField(var1);
            var2.setAccessible(true);
            return var2;
         } catch (Exception var3) {
            throw new ExceptionInInitializerError(var3);
         }
      }

      public static PortSupport.AutoMineModule getInstance() {
         return INSTANCE;
      }

      private SpeedMine module() {
         return ChimeraClient.moduleManager.getModuleByClass(SpeedMine.class);
      }

      public boolean isEnabled() {
         SpeedMine var1 = this.module();
         return var1 != null && var1.isEnabled();
      }

      public BlockPos getMiningBlock() {
         SpeedMine var1 = this.module();
         if (var1 != null && var1.isEnabled()) {
            try {
               SpeedMine.BlockBreakingTask var2 = (SpeedMine.BlockBreakingTask)TASK.get(var1);
               return var2 == null ? null : var2.getBlockPos();
            } catch (IllegalAccessException var3) {
               throw new IllegalStateException(var3);
            }
         } else {
            return null;
         }
      }

      public boolean isSilentSwapping() {
         SpeedMine var1 = this.module();
         if (var1 != null && var1.isEnabled()) {
            try {
               Setting var2 = (Setting)SWAP.get(var1);
               return var2.getValue().toString().contains("SILENT") && PortSupport.Managers.INVENTORY.isDesynced();
            } catch (IllegalAccessException var3) {
               throw new IllegalStateException(var3);
            }
         } else {
            return false;
         }
      }
   }

   public static class AutoXPModule {
      private static final PortSupport.AutoXPModule INSTANCE = new PortSupport.AutoXPModule();

      public static PortSupport.AutoXPModule getInstance() {
         return INSTANCE;
      }

      public boolean isEnabled() {
         Module var1 = ChimeraClient.moduleManager.getModuleByName("AutoXP");
         return var1 != null && var1.isEnabled();
      }
   }

   public static class BlockTracker {
      public Set<BlockPos> getMines(float var1) {
         HashSet var2 = new HashSet();

         for (PortSupport.MineProgress var4 : PortSupport.mines.values()) {
            if (var4.progress / 9.0F >= var1) {
               var2.add(var4.pos);
            }
         }

         return var2;
      }
   }

   public static class BooleanConfig extends Setting<Boolean> {
      public BooleanConfig(String var1, String var2, boolean var3) {
         super(var1, var3, var2);
      }

      public BooleanConfig(String var1, String var2, boolean var3, Supplier<Boolean> var4) {
         super(var1, var3, var2);
         this.setVisibility(var1x -> (Boolean)var4.get());
      }
   }

   public static class CombatModule extends Module {
      private final int rotationPriority;
      protected final Setting<Boolean> multitaskConfig = this.bool("Multitask", false);

      protected CombatModule(String var1, String var2, Module.Category var3, int var4) {
         super(var1, var2, var3);
         this.rotationPriority = var4;
      }

      protected boolean checkMultitask() {
         return mc.player != null && mc.player.isUsingItem();
      }

      protected void setRotation(float var1, float var2) {
         PortSupport.Managers.ROTATION.setRotation(new Rotation(this.rotationPriority, var1, var2));
      }

      protected void setRotationSilent(float var1, float var2) {
         PortSupport.Managers.ROTATION.setRotationSilent(var1, var2);
      }

      protected boolean isRotationBlocked() {
         return PortSupport.Managers.ROTATION.isRotationBlocked(this.rotationPriority);
      }

      public PlayerEntity getClosestPlayer(double var1) {
         return mc.world
            .getPlayers()
            .stream()
            .filter(var0 -> !(var0 instanceof ClientPlayerEntity) && !var0.isSpectator())
            .filter(var2 -> mc.player.squaredDistanceTo(var2) <= var1 * var1)
            .filter(var0 -> !PortSupport.Managers.SOCIAL.isFriend(var0.getName().getString()))
            .min(Comparator.comparingDouble(var0 -> mc.player.squaredDistanceTo(var0)))
            .orElse(null);
      }
   }

   public static class DisconnectEvent {
   }

   public static class EnumConfig<E extends Enum<E>> extends Setting<E> {
      public EnumConfig(String var1, String var2, E var3, E[] var4) {
         super(var1, (E)var3, var2);
      }

      public EnumConfig(String var1, String var2, E var3, E[] var4, Supplier<Boolean> var5) {
         super(var1, (E)var3, var2);
         this.setVisibility(var1x -> (Boolean)var5.get());
      }
   }

   public static class FastLatencyModule {
      private static final PortSupport.FastLatencyModule INSTANCE = new PortSupport.FastLatencyModule();

      public static PortSupport.FastLatencyModule getInstance() {
         return INSTANCE;
      }

      public boolean isEnabled() {
         return false;
      }

      public long getLatency() {
         return PortSupport.Managers.NETWORK.getClientLatency();
      }
   }

   public static class Inventory {
      private volatile int serverSlot = -1;

      public int getServerSlot() {
         return this.serverSlot < 0 ? PortSupport.mc.player.getInventory().getSelectedSlot() : this.serverSlot;
      }

      public boolean isDesynced() {
         return PortSupport.mc.player != null && PortSupport.mc.player.getInventory().getSelectedSlot() != this.getServerSlot();
      }

      public ItemStack getServerItem() {
         return PortSupport.mc.player.getInventory().getStack(this.getServerSlot());
      }

      public void setSlot(int var1) {
         if (var1 >= 0 && var1 <= 8 && PortSupport.mc.player != null && this.getServerSlot() != var1) {
            PortSupport.Managers.NETWORK.sendPacket(new UpdateSelectedSlotC2SPacket(var1));
            this.serverSlot = var1;
         }
      }

      public void setClientSlot(int var1) {
         if (PortSupport.mc.player != null && var1 >= 0 && var1 <= 8 && PortSupport.mc.player.getInventory().getSelectedSlot() != var1) {
            PortSupport.mc.player.getInventory().setSelectedSlot(var1);
            PortSupport.Managers.NETWORK.sendPacket(new UpdateSelectedSlotC2SPacket(var1));
            this.serverSlot = var1;
         }
      }

      public void syncToClient() {
         if (this.isDesynced()) {
            this.setSlot(PortSupport.mc.player.getInventory().getSelectedSlot());
         }
      }
   }

   public static class Managers {
      public static final PortSupport.Network NETWORK = new PortSupport.Network();
      public static final PortSupport.Inventory INVENTORY = new PortSupport.Inventory();
      public static final RotationManager ROTATION = new RotationManager();
      public static final PortSupport.Social SOCIAL = new PortSupport.Social();
      public static final PortSupport.BlockTracker BLOCK = new PortSupport.BlockTracker();
      public static final PortSupport.Totems TOTEM = new PortSupport.Totems();

      public static void tick() {
         if (INVENTORY.serverSlot < 0 && PortSupport.mc.player != null) {
            INVENTORY.serverSlot = PortSupport.mc.player.getInventory().getSelectedSlot();
            ROTATION.onPacketOutbound(
               new RotationEvents.PacketEvent.Outbound(
                  new LookAndOnGround(
                     PortSupport.mc.player.getYaw(),
                     PortSupport.mc.player.getPitch(),
                     PortSupport.mc.player.isOnGround(),
                     PortSupport.mc.player.horizontalCollision
                  )
               )
            );
         }

         PortSupport.mines.values().removeIf(var0 -> System.currentTimeMillis() - var0.time > 3000L);
         PortSupport.totemTimes.values().removeIf(var0 -> System.currentTimeMillis() - var0 > 10000L);
      }

      static {
         Util.EVENT_BUS.register(new RotationPacketBridge());
      }
   }

   private record MineProgress(BlockPos pos, int progress, long time) {
   }

   public static class Network {
      public void sendPacket(Packet<?> var1) {
         if (PortSupport.mc.getNetworkHandler() != null) {
            PortSupport.mc.getNetworkHandler().sendPacket(var1);
         }
      }

      public void sendQuietPacket(Packet<?> var1) {
         if (PortSupport.mc.getNetworkHandler() != null) {
            ((QuietPacketAccess)PortSupport.mc.getNetworkHandler().getConnection()).shoreline$sendInternal(var1, null, true);
         }
      }

      public void sendSequencedPacket(IntFunction<Packet<?>> var1) {
         if (PortSupport.mc.world != null && PortSupport.mc.getNetworkHandler() != null) {
            PendingUpdateManager var2 = ((PendingUpdatesAccess)PortSupport.mc.world).chimera$getPendingUpdates().incrementSequence();

            try {
               this.sendPacket((Packet<?>)var1.apply(var2.getSequence()));
            } catch (Throwable var6) {
               if (var2 != null) {
                  try {
                     var2.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (var2 != null) {
               var2.close();
            }
         }
      }

      public int getClientLatency() {
         if (PortSupport.mc.player != null && PortSupport.mc.getNetworkHandler() != null) {
            PlayerListEntry var1 = PortSupport.mc.getNetworkHandler().getPlayerListEntry(PortSupport.mc.player.getUuid());
            return var1 == null ? 0 : var1.getLatency();
         } else {
            return 0;
         }
      }

      private String address() {
         return PortSupport.mc.getCurrentServerEntry() == null ? "" : PortSupport.mc.getCurrentServerEntry().address.toLowerCase(Locale.ROOT);
      }

      public boolean is2b2t() {
         return this.address().contains("2b2t.org");
      }

      public boolean isCrystalPvpCC() {
         return this.address().contains("crystalpvp.cc");
      }
   }

   public static class NumberConfig<T extends Number> extends Setting<T> {
      public NumberConfig(String var1, String var2, T var3, T var4, T var5) {
         super(var1, (T)var4, (T)var3, (T)var5, var2);
      }

      public NumberConfig(String var1, String var2, T var3, T var4, T var5, Supplier<Boolean> var6) {
         super(var1, (T)var4, (T)var3, (T)var5, var1x -> (Boolean)var6.get(), var2);
      }

      public NumberConfig(String var1, String var2, T var3, T var4, T var5, PortSupport.NumberDisplay var6, Supplier<Boolean> var7) {
         this(var1, var2, (T)var3, (T)var4, (T)var5, var7);
      }
   }

   public static enum NumberDisplay {
      DEGREES,
      PERCENT;
   }

   public static class PlayerTickEvent {
   }

   public static class Rotations {
      private float serverYaw;

      public float getWrappedYaw() {
         return MathHelper.wrapDegrees(this.serverYaw);
      }

      public void setRotation(float var1, float var2) {
         this.setRotation(var1, var2, false);
      }

      public void setRotation(float var1, float var2, boolean var3) {
         if (PortSupport.mc.player != null) {
            this.serverYaw = var1;
            if (!var3) {
               ChimeraClient.rotationManager.setPlayerRotations(var1, var2);
            }

            PortSupport.Managers.NETWORK
               .sendPacket(new LookAndOnGround(var1, var2, PortSupport.mc.player.isOnGround(), PortSupport.mc.player.horizontalCollision));
         }
      }
   }

   public static class RunTickEvent {
   }

   public static class Social {
      public boolean isFriend(Text var1) {
         return ChimeraClient.friendManager.isFriend(var1.getString());
      }

      public boolean isFriend(String var1) {
         return ChimeraClient.friendManager.isFriend(var1);
      }
   }

   public static class SurroundModule {
      private static final PortSupport.SurroundModule INSTANCE = new PortSupport.SurroundModule();

      public static PortSupport.SurroundModule getInstance() {
         return INSTANCE;
      }

      public Set<BlockPos> getSurroundNoDown(PlayerEntity var1) {
         HashSet var2 = new HashSet();
         Box var3 = var1.getBoundingBox();

         for (int var4 = MathHelper.floor(var3.minX); var4 <= MathHelper.floor(var3.maxX - 1.0E-7); var4++) {
            for (int var5 = MathHelper.floor(var3.minZ); var5 <= MathHelper.floor(var3.maxZ - 1.0E-7); var5++) {
               BlockPos var6 = new BlockPos(var4, var1.getBlockY(), var5);

               for (Direction var8 : Type.HORIZONTAL) {
                  BlockPos var9 = var6.offset(var8);
                  if (!var3.intersects(new Box(var9))) {
                     var2.add(var9);
                  }
               }
            }
         }

         return var2;
      }
   }

   public static class Totems {
      public long getLastPopTime(Entity var1) {
         return PortSupport.totemTimes.getOrDefault(var1.getId(), -1L);
      }
   }
}
