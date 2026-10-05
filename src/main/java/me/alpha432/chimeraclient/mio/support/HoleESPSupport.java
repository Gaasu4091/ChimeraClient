package me.alpha432.chimeraclient.mio.support;

import java.util.ArrayList;
import java.util.List;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.RailBlock;
import net.minecraft.block.TorchBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos.Mutable;

public final class HoleESPSupport implements Util {
   private static volatile List<HoleESPSupport.Hole> holes = List.of();
   private static Thread thread;
   private static int users;

   private HoleESPSupport() {
   }

   public static List<HoleESPSupport.Hole> holes() {
      return holes;
   }

   public static synchronized void acquire() {
      if (users++ <= 0) {
         Thread var0 = new Thread(HoleESPSupport::loop, "mio-hole-scanner");
         var0.setDaemon(true);
         thread = var0;
         var0.start();
      }
   }

   public static synchronized void release() {
      if (users != 0 && --users <= 0) {
         if (thread != null) {
            thread.interrupt();
         }

         thread = null;
         holes = List.of();
      }
   }

   private static void loop() {
      Thread var0 = Thread.currentThread();

      while (!var0.isInterrupted()) {
         try {
            scan();
         } catch (Throwable var3) {
         }

         try {
            Thread.sleep(50L);
         } catch (InterruptedException var2) {
            return;
         }
      }
   }

   private static void scan() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      if (var0.player != null && var0.world != null) {
          ArrayList<HoleESPSupport.Hole> var1 = new ArrayList<>();
         BlockPos var2 = var0.player.getBlockPos();
         Mutable var3 = new Mutable();

         for (int var4 = -16; var4 < 16; var4++) {
            for (int var5 = -16; var5 < 16; var5++) {
               for (int var6 = -16; var6 < 16; var6++) {
                  var3.set(var2.getX() + var4, var2.getY() + var5, var2.getZ() + var6);
                  HoleESPSupport.Hole var7 = hole(var3, null);
                  if (var7 != null) {
                     boolean var8 = false;

                     for (HoleESPSupport.Hole var10 : var1) {
                        if (var7.box().intersects(var10.box())) {
                           var8 = true;
                           break;
                        }
                     }

                     if (!var8) {
                        var1.add(var7);
                     }
                  }
               }
            }
         }

         holes = List.copyOf(var1);
      }
   }

   public static HoleESPSupport.Hole hole(BlockPos var0, Direction var1) {
      if (!solid(var0) && solid(var0.down()) && !solid(var0.up())) {
         if (mc.world.getBlockState(var0.down()).isOf(Blocks.END_PORTAL)) {
            return null;
         } else {
            boolean var2 = solid(var0.up(2));
            HoleESPSupport.Mode var3 = HoleESPSupport.Mode.SAFE;
            Direction var4 = null;

            for (Direction var8 : Direction.values()) {
               if (var1 != var8.getOpposite() && var8 != Direction.UP) {
                  BlockPos var9 = var0.offset(var8);
                  Block var10 = mc.world.getBlockState(var9).getBlock();
                  if (solid(var9)) {
                     if (var10 == Blocks.RESPAWN_ANCHOR) {
                        return null;
                     }

                     if (var10.getBlastResistance() < 600.0F && var10.getBlastResistance() >= 0.0F || var10.getHardness() == 0.0F) {
                        return null;
                     }

                     if (var10.getBlastResistance() >= 600.0F && var10.getHardness() >= 0.0F) {
                        var3 = HoleESPSupport.Mode.UNSAFE;
                     }
                  } else {
                     if (var4 != null || var1 != null) {
                        return null;
                     }

                     HoleESPSupport.Hole var11 = hole(var9, var8);
                     if (var11 == null) {
                        return null;
                     }

                     if (!var11.trapped()) {
                        var2 = false;
                     }

                     if (var3 == HoleESPSupport.Mode.SAFE) {
                        var3 = var11.mode();
                     }

                     var4 = var8;
                  }
               }
            }

            Box var12 = var4 == null ? new Box(var0) : new Box(var0).stretch(var4.getVector().getX(), 0.0, var4.getVector().getZ());
            return new HoleESPSupport.Hole(var3, var0.toImmutable(), var12, var2);
         }
      } else {
         return null;
      }
   }

   public static boolean solid(BlockPos var0) {
      BlockState var1 = mc.world.getBlockState(var0);
      return !var1.isAir()
         && !var1.isOf(Blocks.FIRE)
         && !var1.isOf(Blocks.SOUL_FIRE)
         && !(var1.getBlock() instanceof ButtonBlock)
         && !(var1.getBlock() instanceof TorchBlock)
         && !(var1.getBlock() instanceof RailBlock)
         && !var1.isOf(Blocks.LIGHT);
   }

   public static BlockPos feet(LivingEntity var0) {
      return BlockPos.ofFloored(var0.getX(), Math.round(var0.getY()), var0.getZ());
   }

   public record Hole(HoleESPSupport.Mode mode, BlockPos pos, Box box, boolean trapped) {
   }

   public static enum Mode {
      SAFE,
      UNSAFE;
   }
}
