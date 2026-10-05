package me.alpha432.chimeraclient.shoreline.piston;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public final class PistonPatterns {
   private static final Direction[] SIDES = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

   private PistonPatterns() {
   }

   public static List<PistonPatterns.Route> contact(BlockPos var0, boolean var1) {
      ArrayList var2 = new ArrayList();

      for (Direction var6 : var1 ? Direction.values() : SIDES) {
         for (int var7 = -1; var7 <= 1; var7++) {
            for (int var8 = -1; var8 <= 1; var8++) {
               for (int var9 = 0; var9 <= 1; var9++) {
                  var2.add(new PistonPatterns.Route(var0, var0.add(var7, var9, var8).offset(var6), var6));
               }
            }
         }
      }

      return var2;
   }

   public static List<PistonPatterns.Route> chimera(BlockPos var0) {
      LinkedHashSet var1 = new LinkedHashSet();

      for (int var5 : new int[]{0, 1, 2, -1, -2}) {
         for (Direction var9 : SIDES) {
            Direction var10 = var9.rotateYClockwise();

            for (int var11 = -1; var11 <= 1; var11++) {
               BlockPos var12 = var0.add(0, var5 + 1, 0).offset(var9).offset(var10, var11);
               if (var12.getY() >= var0.getY()) {
                  for (int var13 = 0; var13 <= 1; var13++) {
                     for (int var14 = -1; var14 <= 1; var14++) {
                        for (int var15 = -1; var15 <= 1; var15++) {
                           var1.add(new PistonPatterns.Route(var12, var12.add(var14, var13, var15), var9));
                        }
                     }
                  }

                  var1.add(new PistonPatterns.Route(var12, var12.offset(var9, 2), var9));
               }
            }

            BlockPos var16 = var0.add(0, var5, 0).offset(var9);

            for (int var17 = 0; var17 <= 1; var17++) {
               BlockPos var18 = var16.down().offset(var9.getOpposite(), var17);
               if (var18.getY() >= var0.getY()) {
                  var1.add(new PistonPatterns.Route(var18, var16, var9));
               }
            }
         }
      }

      return List.copyOf(var1);
   }

   public static Set<PistonPatterns.Route> union(Collection<PistonPatterns.Route> var0, Collection<PistonPatterns.Route> var1) {
      LinkedHashSet var2 = new LinkedHashSet();

      for (PistonPatterns.Route var4 : var0) {
         if (geometric(var4)) {
            var2.add(var4);
         }
      }

      for (PistonPatterns.Route var6 : var1) {
         if (geometric(var6)) {
            var2.add(var6);
         }
      }

      return var2;
   }

   public static boolean geometric(PistonPatterns.Route var0) {
      BlockPos var1 = var0.crystal();
      BlockPos var2 = var0.piston();
      Box var3 = new Box(var1.getX(), var1.getY(), var1.getZ(), var1.getX() + 1, var1.getY() + 2, var1.getZ() + 1);
      Box var4 = EntityType.END_CRYSTAL.getDimensions().getBoxAt(var1.getX() + 0.5, var1.getY(), var1.getZ() + 0.5);
      return !var2.equals(var1.down()) && !var3.intersects(new Box(var2)) && var4.intersects(new Box(var2.offset(var0.outward().getOpposite())));
   }

   public record Route(BlockPos crystal, BlockPos piston, Direction outward) {
   }
}
