package me.alpha432.chimeraclient.mio.render;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import me.alpha432.chimeraclient.event.impl.entity.player.TickEvent;
import me.alpha432.chimeraclient.event.impl.network.PacketEvent;
import me.alpha432.chimeraclient.event.impl.render.Render3DEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.mio.MioRender;
import me.alpha432.chimeraclient.mio.support.SearchChunkScanner;
import me.alpha432.chimeraclient.mio.support.SearchSupport;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Type;

public class Tunnels extends Module {
   public final Setting<Boolean> verticals = this.bool("Verticals", true);
   public final Setting<Float> lineWidth = this.num("LineWidth", 1.0F, 0.1F, 3.0F);
   public final Setting<Float> height = this.num("Height", 0.1F, -1.0F, 2.0F);
   public final Setting<Integer> minLength = this.num("MinLength", 4, 1, 16);
   public final Setting<Color> fill = this.color("Fill", 255, 0, 0, 80);
   public final Setting<Color> outline = this.color("Outline", 255, 0, 0, 255);
   private final List<Tunnels.Tunnel> list = Collections.synchronizedList(new ArrayList<>());
   private final Queue<Tunnels.Tunnel> queue = new ConcurrentLinkedQueue<>();
   private final ObjectSet<Tunnels.Tunnel> merged = new ObjectOpenHashSet();
   private final SearchChunkScanner scanner;
   private final Mutable mutable = new Mutable();
   private long lastMerge;

   public Tunnels() {
      super("Tunnels", "Highlights dug-out tunnels.", Module.Category.RENDER);
      this.scanner = new SearchChunkScanner(
         "mio-tunnels", 1, (var1, var2) -> this.scan(var1), var1 -> this.list.removeIf(var1x -> inChunk(BlockPos.ofFloored(var1x.box.getCenter()), var1)), null
      );
   }

   @Override
   public void onEnable() {
      this.scanner.start();
      if (!nullCheck()) {
         this.scanner.scanLoaded();
      }
   }

   @Override
   public void onDisable() {
      this.list.clear();
      this.queue.clear();
      this.scanner.stop();
   }

   @Subscribe
   public void onPacketReceive(PacketEvent.Receive var1) {
      this.scanner.onPacket(var1.getPacket());
   }

   @Subscribe
   public void onPlayerTick(TickEvent var1) {
      if (!nullCheck()) {
         synchronized (this.list) {
            if (!this.queue.isEmpty() && System.currentTimeMillis() - this.lastMerge >= 1000L) {
               this.lastMerge = System.currentTimeMillis();
               ArrayList var3 = new ArrayList<>(this.list);
               SearchSupport.WORKERS.submit(() -> this.merge(var3));
            }

            this.list.removeIf(var0 -> Math.sqrt(mc.player.getEyePos().squaredDistanceTo(var0.box.getCenter())) > 256.0);
         }
      }
   }

   @Subscribe
   public void onRenderWorld(Render3DEvent var1) {
      if (!nullCheck()) {
         Frustum var2 = SearchSupport.frustum();
         synchronized (this.list) {
            for (Tunnels.Tunnel var5 : this.list) {
               if (!(var5.length() < this.minLength.getValue().intValue())) {
                  Box var6 = var5.box;
                  float var7 = var5.vertical ? (float)var6.getLengthY() : this.height.getValue();
                  Box var8 = var6.withMaxY(var6.minY + var7);
                  if (var2.isVisible(var8)) {
                     MioRender.fill(var1.getMatrix(), var8, this.fill.getValue());
                     MioRender.outline(var1.getMatrix(), var8, this.outline.getValue(), this.lineWidth.getValue());
                  }
               }
            }
         }
      }
   }

   private synchronized void merge(List<Tunnels.Tunnel> var1) {
      Tunnels.Tunnel var2;
      while ((var2 = this.queue.poll()) != null) {
         boolean var3 = false;

         for (Tunnels.Tunnel var5 : var1) {
            if (var5.connects(var2)) {
               var5.extend(BlockPos.ofFloored(var2.box.getCenter()));
               var3 = true;
               break;
            }
         }

         if (!var3) {
            var1.add(var2);
         }
      }

      for (int var8 = 0; var8 < var1.size(); var8++) {
         for (int var11 = var8; var11 < var1.size(); var11++) {
            if (((Tunnels.Tunnel)var1.get(var8)).connects((Tunnels.Tunnel)var1.get(var11))) {
               this.merged.add((Tunnels.Tunnel)var1.get(var11));
            }
         }
      }

      ObjectIterator var9 = this.merged.iterator();

      while (var9.hasNext()) {
         Tunnels.Tunnel var12 = (Tunnels.Tunnel)var9.next();
         var1.remove(var12);
      }

      this.merged.clear();
      synchronized (this.list) {
         this.list.clear();
         this.list.addAll(var1);
      }
   }

   private void scan(ChunkPos var1) {
      int var2 = mc.world.getBottomY();
      int var3 = mc.world.getTopYInclusive();

      for (int var4 = 0; var4 < 16; var4++) {
         for (int var5 = var2; var5 <= var3; var5++) {
            for (int var6 = 0; var6 < 16; var6++) {
               this.mutable.set(var1.getStartX() + var4, var5, var1.getStartZ() + var6);
               if (open(this.mutable)) {
                  if (this.vertical(this.mutable)) {
                     this.queue.add(new Tunnels.Tunnel(this.mutable, true));
                  } else if (this.horizontal(this.mutable)) {
                     for (Direction var8 : Type.HORIZONTAL) {
                        if (this.horizontal(this.mutable.offset(var8))) {
                           this.queue.add(new Tunnels.Tunnel(this.mutable, false));
                           break;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean horizontal(BlockPos var1) {
      if (!open(var1.up()) || open(var1.down()) || open(var1.up().up())) {
         return false;
      } else {
         return open(var1.north()) && open(var1.south()) && open(var1.up().north()) && open(var1.up().south())
            ? !open(var1.east()) && !open(var1.west()) && !open(var1.up().east()) && !open(var1.up().west())
            : open(var1.east())
               && open(var1.west())
               && open(var1.up().east())
               && open(var1.up().west())
               && !open(var1.north())
               && !open(var1.south())
               && !open(var1.up().north())
               && !open(var1.up().south());
      }
   }

   private boolean vertical(BlockPos var1) {
      if (!this.verticals.getValue()) {
         return false;
      } else {
         for (Direction var3 : Type.HORIZONTAL) {
            BlockState var4 = mc.world.getBlockState(var1.offset(var3));
            if (!var4.isSolid() || var4.isOf(Blocks.BAMBOO_BLOCK) || var4.isOf(Blocks.BASALT)) {
               return false;
            }
         }

         for (Direction var6 : Type.VERTICAL) {
            BlockPos var7 = var1.offset(var6);
            if (!open(var7) || !open(var7.offset(var6))) {
               return false;
            }
         }

         return true;
      }
   }

   private static boolean inChunk(BlockPos var0, ChunkPos var1) {
      return var0.getX() >= var1.getStartX() && var0.getX() <= var1.getEndX() && var0.getZ() >= var1.getStartZ() && var0.getZ() <= var1.getEndZ();
   }

   private static boolean open(BlockPos var0) {
      return !mc.world.getBlockState(var0).isSolid();
   }

   private static final class Tunnel {
      final boolean vertical;
      volatile Box box;

      Tunnel(BlockPos var1, boolean var2) {
         this.box = new Box(var1);
         this.vertical = var2;
      }

      double length() {
         return Math.max(Math.max(this.box.getLengthX(), this.box.getLengthZ()), this.box.getLengthY());
      }

      boolean connects(Tunnels.Tunnel var1) {
         if (this.vertical != var1.vertical) {
            return false;
         } else {
            Box var2 = this.box;
            Box var3 = var1.box;
            if (!this.vertical) {
               if (var3.minY != var2.minY) {
                  return false;
               } else {
                  return var2.minX != var3.maxX && var2.maxX != var3.minX
                     ? (var2.minZ == var3.maxZ || var2.maxZ == var3.minZ) && var2.minX == var3.minX && var2.maxX == var3.maxX
                     : var2.minZ == var3.minZ && var2.maxZ == var3.maxZ;
               }
            } else {
               return var3.minY != var2.maxY && var3.maxY != var2.minY
                  ? false
                  : var2.minX == var3.minX && var2.maxX == var3.maxX && var2.minZ == var3.minZ && var2.maxZ == var3.maxZ;
            }
         }
      }

      void extend(BlockPos var1) {
         this.box = this.box.union(new Box(var1));
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.box, this.vertical);
      }

      @Override
      public boolean equals(Object var1) {
         return var1 instanceof Tunnels.Tunnel var2 && var2.box.equals(this.box) && var2.vertical == this.vertical;
      }
   }
}
