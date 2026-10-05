package me.alpha432.chimeraclient.shoreline.piston;

import java.util.function.Function;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public final class PistonMotion {
   private PistonMotion() {
   }

   public static Vec3d stroke(Box var0, Direction var1, Function<Box, Iterable<VoxelShape>> var2) {
      double var3 = 0.0;
      double var5 = 0.0;
      double var7 = 0.0;

      for (int var9 = 0; var9 < 2; var9++) {
         Vec3d var10 = new Vec3d(var1.getOffsetX() * 0.51, var1.getOffsetY() * 0.51, var1.getOffsetZ() * 0.51);
         double var11 = var10.x + var10.y + var10.z;
         double var13 = VoxelShapes.calculateMaxOffset(var1.getAxis(), var0, (Iterable)var2.apply(var0.stretch(var10)), var11);
         double var15 = var13 * (var1.getOffsetX() + var1.getOffsetY() + var1.getOffsetZ());
         double var17 = var15 * var1.getOffsetX();
         double var19 = var15 * var1.getOffsetY();
         double var21 = var15 * var1.getOffsetZ();
         var3 += var17;
         var5 += var19;
         var7 += var21;
         var0 = var0.offset(var17, var19, var21);
      }

      return new Vec3d(var3, var5, var7);
   }

   public static boolean pushed(double var0, double var2, double var4, Direction var6, boolean var7) {
      double var8 = var0 * var6.getOffsetX() + var2 * var6.getOffsetY() + var4 * var6.getOffsetZ();
      double var10 = Math.max(0.0, var0 * var0 + var2 * var2 + var4 * var4 - var8 * var8);
      if (!(var10 <= 0.36)) {
         return false;
      } else if (!(var8 <= 1.75)) {
         return false;
      } else {
         double var12 = var7 ? 0.2 : -0.05;
         return var8 >= var12;
      }
   }
}
