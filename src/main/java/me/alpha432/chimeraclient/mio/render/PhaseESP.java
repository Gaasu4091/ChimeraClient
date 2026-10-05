package me.alpha432.chimeraclient.mio.render;

import java.awt.Color;
import me.alpha432.chimeraclient.event.impl.render.Render3DEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.mio.MioRender;
import me.alpha432.chimeraclient.mio.support.SearchSupport;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public class PhaseESP extends Module {
   public final Setting<Color> safe = this.color("Safe", 53, 113, 32, 32);
   public final Setting<Color> semiSafe = this.color("SemiSafe", 255, 221, 0, 37);
   public final Setting<Color> unsafe = this.color("Unsafe", 206, 0, 0, 37);
   public final Setting<Boolean> outline = this.bool("Outline", false);
   public final Setting<Float> alpha = this.num("Alpha", 1.0F, 0.0F, 1.0F);

   public PhaseESP() {
      super("PhaseESP", "Highlights safe blocks to phase into.", Module.Category.RENDER);
      this.alpha.setVisibility(var1 -> this.outline.getValue());
   }

   @Subscribe
   public void onRenderWorld(Render3DEvent var1) {
      if (!nullCheck()) {
         Box var2 = mc.player.getBoundingBox();
         Box var3 = var2.withMaxY(var2.minY).expand(0.01, 0.0, 0.01);
         if (!SearchSupport.collidable(mc.player.getBlockPos()) && !mc.player.isInSwimmingPose()) {
            for (BlockPos var5 : BlockPos.iterate(BlockPos.ofFloored(var3.minX, var3.minY, var3.minZ), BlockPos.ofFloored(var3.maxX, var3.maxY, var3.maxZ))) {
               if (phaseable(var5)) {
                  Color var6 = this.color(var5);
                  VoxelShape var7 = mc.world.getBlockState(var5).getOutlineShape(mc.world, var5);
                  if (var7.isEmpty()) {
                     var7 = VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
                  }

                  Box var8 = var7.getBoundingBox().offset(var5).withMaxY(var5.getY());
                  MioRender.fill(var1.getMatrix(), var8, var6);
                  if (this.outline.getValue()) {
                     MioRender.outline(var1.getMatrix(), var8, MioRender.alpha(var6, Math.round(this.alpha.getValue() * 255.0F)), 1.0F);
                  }
               }
            }
         }
      }
   }

   private Color color(BlockPos var1) {
      boolean var2 = mc.world.getBlockState(var1).getBlock().getBlastResistance() >= 600.0F;
      BlockPos var3 = var1.down();
      if (SearchSupport.collidable(var3) && var2 && mc.world.getBlockState(var3).getBlock().getBlastResistance() >= 600.0F) {
         return !notBedrock(var1) && !notBedrock(var3) ? this.safe.getValue() : this.semiSafe.getValue();
      } else {
         return this.unsafe.getValue();
      }
   }

   private static boolean notBedrock(BlockPos var0) {
      return SearchSupport.block(var0) != Blocks.BEDROCK;
   }

   private static boolean phaseable(BlockPos var0) {
      BlockState var1 = mc.world.getBlockState(var0);
      return var1.getBlock().getBlastResistance() >= 600.0F && SearchSupport.collidable(var0) && !var1.isReplaceable();
   }
}
