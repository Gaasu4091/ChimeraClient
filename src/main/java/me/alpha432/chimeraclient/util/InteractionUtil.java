package me.alpha432.chimeraclient.util;

import java.util.Iterator;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.ActionResult.SwingSource;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.shape.VoxelShapes;

public class InteractionUtil implements Util {
   public static boolean canBreak(BlockPos blockPos, BlockState state) {
      return !mc.player.isCreative() && state.getHardness(mc.world, blockPos) < 0.0F ? false : state.getOutlineShape(mc.world, blockPos) != VoxelShapes.empty();
   }

   public static boolean isPlaceable(BlockPos pos, boolean entityCheck) {
      if (!mc.world.getBlockState(pos).isReplaceable()) {
         return false;
      } else {
         Iterator var2 = mc.world
            .getEntitiesByClass(
               Entity.class, new Box(pos), ex -> !(ex instanceof ExperienceBottleEntity) && !(ex instanceof ItemEntity) && !(ex instanceof ExperienceOrbEntity)
            )
            .iterator();
         if (var2.hasNext()) {
            Entity e = (Entity)var2.next();
            return e instanceof PlayerEntity ? false : !entityCheck;
         } else {
            return true;
         }
      }
   }

   public static boolean breakBlock(BlockPos pos) {
      if (!canBreak(pos, mc.world.getBlockState(pos))) {
         return false;
      } else {
         BlockPos bp = pos instanceof Mutable ? new BlockPos(pos) : pos;
         mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, bp, Direction.UP));
         mc.player.swingHand(Hand.MAIN_HAND);
         mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, bp, Direction.UP));
         mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
         return true;
      }
   }

   public static void useItem(BlockPos pos) {
      useItem(pos, Hand.MAIN_HAND);
   }

   public static void useItem(BlockPos pos, Hand hand) {
      if (mc.world != null && mc.player != null && mc.interactionManager != null) {
         Direction direction = mc.crosshairTarget instanceof BlockHitResult bhr ? bhr.getSide() : Direction.DOWN;
         if (mc.interactionManager.interactBlock(mc.player, hand, new BlockHitResult(Vec3d.ofCenter(pos), direction, pos, false)) instanceof Success success
            && success.swingSource() != SwingSource.NONE) {
            mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
         }
      }
   }

   public static boolean place(BlockPos pos, boolean airPlace) {
      return place(pos, airPlace, Hand.MAIN_HAND);
   }

   public static boolean place(BlockPos pos, boolean airPlace, Hand hand) {
      if (mc.world == null || mc.player == null || mc.interactionManager == null) {
         return false;
      } else if (!isPlaceable(pos, false)) {
         return false;
      } else {
         Direction direction = calcSide(pos);
         if (direction == null) {
            if (!airPlace) {
               return false;
            }

            direction = mc.crosshairTarget instanceof BlockHitResult bhr ? bhr.getSide() : Direction.DOWN;
         }

         BlockPos bp = airPlace ? pos : pos.offset(direction);
         if (mc.interactionManager
               .interactBlock(
                  mc.player,
                  hand,
                  new BlockHitResult(
                     airPlace ? Vec3d.ofCenter(pos) : Vec3d.ofCenter(bp).offset(direction.getOpposite(), 0.5),
                     airPlace ? direction : direction.getOpposite(),
                     bp,
                     false
                  )
               ) instanceof Success success
            && success.swingSource() != SwingSource.NONE) {
            mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
         }

         return true;
      }
   }

   public static Direction calcSide(BlockPos pos) {
      for (Direction d : Direction.values()) {
         if (!mc.world.getBlockState(pos.add(d.getVector())).isReplaceable()) {
            return d;
         }
      }

      return null;
   }

   public static double getBlockBreakingSpeed(int slot, BlockPos pos) {
      return getBlockBreakingSpeed(slot, mc.world.getBlockState(pos));
   }

   public static double getBlockBreakingSpeed(int slot, BlockState block) {
      double speed = ((ItemStack)mc.player.getInventory().getMainStacks().get(slot)).getMiningSpeedMultiplier(block);
      if (speed > 1.0) {
         ItemStack tool = mc.player.getInventory().getStack(slot);
         int efficiency = EnchantmentUtil.getLevel(Enchantments.EFFICIENCY, tool);
         if (efficiency > 0 && !tool.isEmpty()) {
            speed += efficiency * efficiency + 1;
         }
      }

      if (StatusEffectUtil.hasHaste(mc.player)) {
         speed *= 1.0F + (StatusEffectUtil.getHasteAmplifier(mc.player) + 1) * 0.2F;
      }

      if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
         float k = switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
            case 0 -> 0.3F;
            case 1 -> 0.09F;
            case 2 -> 0.0027F;
            default -> 8.1E-4F;
         };
         speed *= k;
      }

      if (mc.player.isSubmergedIn(FluidTags.WATER) && EnchantmentUtil.has(Enchantments.AQUA_AFFINITY, EquipmentSlot.HEAD)) {
         speed /= 5.0;
      }

      if (!mc.player.isOnGround()) {
         speed /= 5.0;
      }

      float hardness = block.getHardness(null, null);
      if (hardness == -1.0F) {
         return 0.0;
      } else {
         speed /= hardness / (block.isToolRequired() && !((ItemStack)mc.player.getInventory().getMainStacks().get(slot)).isSuitableFor(block) ? 100 : 30);
         float ticks = (float)(Math.floor(1.0 / speed) + 1.0);
         return (long)(ticks / 20.0F * 1000.0F);
      }
   }

   public static Direction right(Direction direction) {
      return switch (direction) {
         case EAST -> Direction.SOUTH;
         case SOUTH -> Direction.WEST;
         case WEST -> Direction.NORTH;
         case NORTH -> Direction.EAST;
         default -> throw new IllegalStateException("Unexpected value: " + direction);
      };
   }

   public static Direction left(Direction direction) {
      return switch (direction) {
         case EAST -> Direction.NORTH;
         case SOUTH -> Direction.EAST;
         case WEST -> Direction.SOUTH;
         case NORTH -> Direction.WEST;
         default -> throw new IllegalStateException("Unexpected value: " + direction);
      };
   }
}
