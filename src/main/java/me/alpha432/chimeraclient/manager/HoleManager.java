package me.alpha432.chimeraclient.manager;

import java.util.ArrayList;
import java.util.List;
import me.alpha432.chimeraclient.event.impl.entity.player.TickEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.Feature;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Type;
import org.jetbrains.annotations.Nullable;

public class HoleManager extends Feature {
   private static final int RANGE = 8;
   private final List<HoleManager.Hole> holes = new ArrayList<>();
   private final Mutable pos = new Mutable();

   public HoleManager() {
      EVENT_BUS.register(this);
   }

   @Subscribe
   private void onTick(TickEvent event) {
      this.holes.clear();

      for (int x = -8; x < 8; x++) {
         for (int y = -8; y < 8; y++) {
            for (int z = -8; z < 8; z++) {
               this.pos.set(mc.player.getX() + x, mc.player.getY() + y, mc.player.getZ() + z);
               HoleManager.Hole hole = this.getHole(this.pos);
               if (hole != null) {
                  this.holes.add(hole);
               }
            }
         }
      }
   }

   @Nullable
   public HoleManager.Hole getHole(BlockPos pos) {
      if (mc.world.getBlockState(pos).getBlock() != Blocks.AIR) {
         return null;
      } else {
         HoleManager.HoleType type = HoleManager.HoleType.BEDROCK;

         for (Direction direction : Type.HORIZONTAL) {
            Block block = mc.world.getBlockState(pos.offset(direction)).getBlock();
            if (block == Blocks.OBSIDIAN) {
               type = HoleManager.HoleType.UNSAFE;
            } else if (block != Blocks.BEDROCK) {
               return null;
            }
         }

         return new HoleManager.Hole(pos, type);
      }
   }

   public List<HoleManager.Hole> getHoles() {
      return this.holes;
   }

   public boolean isHole(BlockPos pos) {
      return this.holes.stream().anyMatch(hole -> hole.pos().equals(pos));
   }

   public record Hole(BlockPos pos, HoleManager.HoleType holeType) {
   }

   private static enum HoleType {
      BEDROCK,
      UNSAFE;
   }
}
