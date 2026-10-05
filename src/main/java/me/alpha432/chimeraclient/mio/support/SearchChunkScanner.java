package me.alpha432.chimeraclient.mio.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

public final class SearchChunkScanner implements Util {
   private static final Set<SearchChunkScanner> ACTIVE = new CopyOnWriteArraySet<>();
   private final int threads;
   private final BiConsumer<ChunkPos, WorldChunk> onLoad;
   private final Consumer<ChunkPos> onUnload;
   private final BiConsumer<BlockPos, BlockState> onUpdate;
   private final String threadName;
   private volatile ExecutorService executor;

   public SearchChunkScanner(String var1, int var2, BiConsumer<ChunkPos, WorldChunk> var3, Consumer<ChunkPos> var4, BiConsumer<BlockPos, BlockState> var5) {
      this.threadName = var1;
      this.threads = Math.max(var2, 1);
      this.onLoad = var3;
      this.onUnload = var4;
      this.onUpdate = var5;
   }

   public static void chunkLoaded(WorldChunk var0) {
      for (SearchChunkScanner var2 : ACTIVE) {
         if (var2.onLoad != null) {
            var2.submit(() -> var2.onLoad.accept(var0.getPos(), var0));
         }
      }
   }

   public static List<WorldChunk> loadedChunks() {
      ArrayList var0 = new ArrayList();
      if (mc.world != null && mc.player != null) {
         int var1 = (Integer)mc.options.getViewDistance().getValue();
         ChunkPos var2 = mc.player.getChunkPos();

         for (int var3 = -var1; var3 <= var1; var3++) {
            for (int var4 = -var1; var4 <= var1; var4++) {
               WorldChunk var5 = mc.world.getChunkManager().getChunk(var2.x + var3, var2.z + var4, ChunkStatus.FULL, false);
               if (var5 != null) {
                  var0.add(var5);
               }
            }
         }

         return var0;
      } else {
         return var0;
      }
   }

   public void start() {
      this.executor = this.newPool();
      ACTIVE.add(this);
   }

   public void stop() {
      ACTIVE.remove(this);
      ExecutorService var1 = this.executor;
      this.executor = null;
      if (var1 != null) {
         var1.shutdownNow();
      }
   }

   public void reset() {
      ExecutorService var1 = this.executor;
      if (var1 != null) {
         var1.shutdownNow();
         this.executor = this.newPool();
      }
   }

   public boolean running() {
      return this.executor != null;
   }

   public void scanLoaded() {
      if (this.onLoad != null) {
         for (WorldChunk var2 : loadedChunks()) {
            this.submit(() -> this.onLoad.accept(var2.getPos(), var2));
         }
      }
   }

   public void onPacket(Packet<?> var1) {
      if (mc.world != null) {
         if (this.onUpdate != null) {
            if (var1 instanceof BlockUpdateS2CPacket var6) {
               BlockPos var7 = var6.getPos();
               BlockState var4 = var6.getState();
               this.submit(() -> this.onUpdate.accept(var7, var4));
               return;
            }

            if (var1 instanceof ChunkDeltaUpdateS2CPacket var5) {
               var5.visitUpdates((var1x, var2x) -> {
                  BlockPos var3x = var1x.toImmutable();
                  this.submit(() -> this.onUpdate.accept(var3x, var2x));
               });
               return;
            }
         }

         if (this.onUnload != null && var1 instanceof UnloadChunkS2CPacket var2) {
            ChunkPos var3 = new ChunkPos(var2.pos().x, var2.pos().z);
            this.submit(() -> this.onUnload.accept(var3));
         }
      }
   }

   private void submit(Runnable var1) {
      ExecutorService var2 = this.executor;
      if (var2 != null) {
         try {
            var2.execute(() -> {
               try {
                  var1.run();
               } catch (Throwable var2x) {
               }
            });
         } catch (RejectedExecutionException var4) {
         }
      }
   }

   private ExecutorService newPool() {
      return Executors.newFixedThreadPool(this.threads, var1 -> {
         Thread var2 = new Thread(var1, this.threadName);
         var2.setDaemon(true);
         return var2;
      });
   }
}
