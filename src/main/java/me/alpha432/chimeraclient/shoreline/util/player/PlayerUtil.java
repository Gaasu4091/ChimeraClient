package me.alpha432.chimeraclient.shoreline.util.player;

import net.minecraft.block.CobwebBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public final class PlayerUtil {
   public static boolean inWeb(double var0) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2 != null && var2.player != null && var2.world != null) {
         Box var3 = var2.player.getBoundingBox().expand(var0);

         for (BlockPos var5 : BlockPos.iterate(BlockPos.ofFloored(var3.minX, var3.minY, var3.minZ), BlockPos.ofFloored(var3.maxX, var3.maxY, var3.maxZ))) {
            if (var2.world.getBlockState(var5).getBlock() instanceof CobwebBlock) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean isHotbarKeysPressed() {
      for (KeyBinding var3 : MinecraftClient.getInstance().options.hotbarKeys) {
         if (var3.isPressed()) {
            return true;
         }
      }

      return false;
   }
}
