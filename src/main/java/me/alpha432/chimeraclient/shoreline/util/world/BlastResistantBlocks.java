package me.alpha432.chimeraclient.shoreline.util.world;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Set;
import me.alpha432.chimeraclient.shoreline.util.Globals;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class BlastResistantBlocks implements Globals {
   private static final Set<Block> BLAST_RESISTANT = new ReferenceOpenHashSet(
      Set.of(Blocks.OBSIDIAN, Blocks.ANVIL, Blocks.ENCHANTING_TABLE, Blocks.ENDER_CHEST, Blocks.BEACON)
   );
   private static final Set<Block> UNBREAKABLE = new ReferenceOpenHashSet(
      Set.of(Blocks.BEDROCK, Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, Blocks.END_PORTAL_FRAME, Blocks.BARRIER)
   );

   public static boolean isBreakable(BlockPos var0) {
      return mc.world == null ? false : isBreakable(mc.world.getBlockState(var0).getBlock());
   }

   public static boolean isBreakable(Block var0) {
      return !UNBREAKABLE.contains(var0);
   }

   public static boolean isUnbreakable(BlockPos var0) {
      return mc.world == null ? false : isUnbreakable(mc.world.getBlockState(var0).getBlock());
   }

   public static boolean isUnbreakable(Block var0) {
      return UNBREAKABLE.contains(var0);
   }

   public static boolean isBlastResistant(BlockPos var0) {
      return mc.world == null ? false : isBlastResistant(mc.world.getBlockState(var0).getBlock());
   }

   public static boolean isBlastResistant(BlockState var0) {
      return isBlastResistant(var0.getBlock());
   }

   public static boolean isBlastResistant(Block var0) {
      return BLAST_RESISTANT.contains(var0);
   }
}
