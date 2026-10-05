package me.alpha432.chimeraclient.mio.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import me.alpha432.chimeraclient.mio.MioRender;
import me.alpha432.chimeraclient.mio.mixin.MioPhaseESPBlockAccess;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.world.chunk.Chunk;

public final class SearchSupport implements Util {
   public static final ExecutorService WORKERS = Executors.newCachedThreadPool(var0 -> {
      Thread var1 = new Thread(var0, "mio-render-worker");
      var1.setDaemon(true);
      return var1;
   });
   private static final List<Block> BASTION = List.of(Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.GILDED_BLACKSTONE);
   private static final List<Block> ANCIENT_CITY = List.of(
      Blocks.DEEPSLATE_BRICKS,
      Blocks.CRACKED_DEEPSLATE_BRICKS,
      Blocks.DEEPSLATE_TILES,
      Blocks.CRACKED_DEEPSLATE_TILES,
      Blocks.DEEPSLATE,
      Blocks.CHISELED_DEEPSLATE,
      Blocks.SCULK
   );
   private static final List<Block> TRIAL_CHAMBERS = List.of(
      Blocks.WAXED_OXIDIZED_COPPER, Blocks.WAXED_OXIDIZED_CUT_COPPER, Blocks.WAXED_COPPER_BLOCK, Blocks.WAXED_OXIDIZED_COPPER_GRATE
   );

   private SearchSupport() {
   }

   public static Frustum frustum() {
      Frustum var0 = new Frustum(MioRender.VIEW, MioRender.PROJECTION);
      Vec3d var1 = MioRender.camera();
      var0.setPosition(var1.x, var1.y, var1.z);
      return var0;
   }

   public static List<BlockPos> sphere(Vec3d var0, float var1) {
      ArrayList var2 = new ArrayList();
      BlockPos var3 = BlockPos.ofFloored(var0);

      for (float var4 = -var1; var4 < var1; var4++) {
         for (float var5 = -var1; var5 < var1; var5++) {
            for (float var6 = -var1; var6 < var1; var6++) {
               BlockPos var7 = var3.add((int)var4, (int)var5, (int)var6);
               if (var3.isWithinDistance(var7, var1)) {
                  var2.add(var7);
               }
            }
         }
      }

      return var2;
   }

   public static Block block(BlockPos var0) {
      return mc.world.getBlockState(var0).getBlock();
   }

   public static boolean collidable(BlockPos var0) {
      return ((MioPhaseESPBlockAccess)block(var0)).mio$isCollidable();
   }

   public static float fade(double var0, float var2, float var3) {
      double var4 = (var0 - var2) / (var3 - var2);
      return 1.0F - (float)Math.max(0.0, Math.min(1.0, var4));
   }

   public static boolean naturalChest(Chunk var0, BlockPos var1, boolean var2, boolean var3, boolean var4, boolean var5, boolean var6) {
      BlockState var7 = var0.getBlockState(var1);
      if (!var7.isOf(Blocks.CHEST)) {
         return false;
      } else {
         BlockState var8 = var0.getBlockState(var1.down());
         boolean var9 = var7.get(ChestBlock.CHEST_TYPE) == ChestType.SINGLE;
         String var10 = mc.world == null ? "overworld" : mc.world.getRegistryKey().getValue().getPath();
         if (var10.equals("the_nether")) {
            return var2 && var9 && var8.isOf(Blocks.NETHER_BRICKS) ? true : var3 && BASTION.contains(var8.getBlock());
         } else if (!var10.equals("the_end") && var9) {
            if (var6 && var1.getY() <= 34) {
               boolean var11 = var8.isOf(Blocks.CHISELED_TUFF_BRICKS);
               if (TRIAL_CHAMBERS.contains(var8.getBlock()) || var11) {
                  boolean var12 = false;

                  for (Direction var14 : Type.HORIZONTAL) {
                     BlockState var15 = var0.getBlockState(var1.offset(var14));
                     if (!var11) {
                        if (TRIAL_CHAMBERS.contains(var15.getBlock())) {
                           return true;
                        }
                     } else if (!var15.isOf(Blocks.AIR)) {
                        var12 = true;
                        break;
                     }
                  }

                  if (var11 && !var12) {
                     return true;
                  }
               }
            }

            return var4 && var1.getY() <= -32 && var1.getY() >= -51 && ANCIENT_CITY.contains(var8.getBlock())
               ? true
               : var5 && (var8.isOf(Blocks.MOSSY_COBBLESTONE) || var8.isOf(Blocks.COBBLESTONE));
         } else {
            return false;
         }
      }
   }
}
