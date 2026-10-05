package me.alpha432.chimeraclient.shoreline.piston;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import me.alpha432.chimeraclient.shoreline.PortSupport;
import me.alpha432.chimeraclient.shoreline.util.Globals;
import me.alpha432.chimeraclient.shoreline.util.player.RotationUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.ActionResult.SwingSource;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public final class PistonInteraction implements Globals {
   public static final PistonInteraction INSTANCE = new PistonInteraction();
   private final Map<Integer, Integer> placedOnEntities = new ConcurrentHashMap<>();

   public boolean isPacketSneaking() {
      return PistonInput.isPacketSneaking();
   }

   public boolean canPlace(BlockPos var1, Block var2) {
      VoxelShape var3 = var2.getDefaultState().getCollisionShape(mc.world, var1, ShapeContext.absent()).offset(var1.getX(), var1.getY(), var1.getZ());
      if (!var3.isEmpty()) {
         for (Entity var5 : mc.world.getOtherEntities(null, var3.getBoundingBox())) {
            if (!var5.isRemoved()
               && var5.intersectionChecked
               && VoxelShapes.matchesAnywhere(var3, VoxelShapes.cuboid(var5.getBoundingBox()), BooleanBiFunction.AND)
               && (!(var5 instanceof EndCrystalEntity) || this.placedOnEntities.containsKey(var5.getId()) && this.placedOnEntities.get(var5.getId()) > 0)) {
               return false;
            }
         }
      }

      return true;
   }

   public boolean placeBlock(BlockPos var1, Block var2, int var3, boolean var4, boolean var5, RotationCallback var6) {
      return this.placeBlock(var1, var2, var3, var4, var5, var6, false);
   }

   public boolean placeBlock(BlockPos var1, Block var2, int var3, boolean var4, boolean var5, RotationCallback var6, boolean var7) {
      boolean var9;
      VoxelShape var8 = var2.getDefaultState().getCollisionShape(mc.world, var1, ShapeContext.absent()).offset(var1.getX(), var1.getY(), var1.getZ());
      var9 = false;
      label54:
      if (!var8.isEmpty()) {
         Iterator var10 = mc.world.getOtherEntities(null, var8.getBoundingBox()).iterator();

         while (true) {
            if (!var10.hasNext()) {
               break label54;
            }

            Entity var11 = (Entity)var10.next();
            if (!var11.isRemoved()
               && var11.intersectionChecked
               && VoxelShapes.matchesAnywhere(var8, VoxelShapes.cuboid(var11.getBoundingBox()), BooleanBiFunction.AND)) {
               if (!(var11 instanceof EndCrystalEntity)) {
                  break;
               }

               this.placedOnEntities.compute(var11.getId(), (var0, var1x) -> var1x != null ? var1x + 1 : 1);
               if (this.placedOnEntities.containsKey(var11.getId()) && this.placedOnEntities.get(var11.getId()) > 0) {
                  break;
               }
            }
         }

         var9 = true;
      }

      if (var9) {
         return false;
      } else {
         Direction var12 = this.getInteractDirectionInternal(var1, var4);
         if (!var7) {
            if (var12 == null) {
               return false;
            } else {
               BlockPos var14 = var1.offset(var12.getOpposite());
               return this.placeBlock(var14, var12, var3, var5, false, var6);
            }
         } else {
            var12 = Direction.DOWN;
            return this.placeBlock(var1, var12, var3, var5, false, var6);
         }
      }
   }

   public boolean placeBlock(BlockPos var1, Block var2, int var3, boolean var4, boolean var5, boolean var6, RotationCallback var7) {
      return this.placeBlock(var1, var2, var3, var4, var5, var6, false, var7);
   }

   public boolean placeBlock(BlockPos var1, Block var2, int var3, boolean var4, boolean var5, boolean var6, boolean var7, RotationCallback var8) {
      boolean var10;
      VoxelShape var9 = var2.getDefaultState().getCollisionShape(mc.world, var1, ShapeContext.absent()).offset(var1.getX(), var1.getY(), var1.getZ());
      var10 = false;
      label54:
      if (!var9.isEmpty()) {
         Iterator var11 = mc.world.getOtherEntities(null, var9.getBoundingBox()).iterator();

         while (true) {
            if (!var11.hasNext()) {
               break label54;
            }

            Entity var12 = (Entity)var11.next();
            if (!var12.isRemoved()
               && var12.intersectionChecked
               && VoxelShapes.matchesAnywhere(var9, VoxelShapes.cuboid(var12.getBoundingBox()), BooleanBiFunction.AND)) {
               if (!(var12 instanceof EndCrystalEntity)) {
                  break;
               }

               this.placedOnEntities.compute(var12.getId(), (var0, var1x) -> var1x != null ? var1x + 1 : 1);
               if (this.placedOnEntities.containsKey(var12.getId()) && this.placedOnEntities.get(var12.getId()) > 0) {
                  break;
               }
            }
         }

         var10 = true;
      }

      if (var10) {
         return false;
      } else {
         Direction var13 = this.getInteractDirectionInternal(var1, var4);
         if (!var7) {
            if (var13 == null) {
               return false;
            } else {
               BlockPos var15 = var1.offset(var13.getOpposite());
               return this.placeBlock(var15, var13, var3, var5, false, var6, var8);
            }
         } else {
            var13 = Direction.DOWN;
            return this.placeBlock(var1, var13, var3, var5, false, var8);
         }
      }
   }

   public boolean placeBlock(BlockPos var1, Direction var2, int var3, boolean var4, boolean var5, boolean var6, RotationCallback var7) {
      Vec3d var8 = var1.toCenterPos().add(new Vec3d(var2.getUnitVector()).multiply(0.5));
      return this.placeBlock(new BlockHitResult(var8, var2, var1, false), var3, var4, var5, var6, var7);
   }

   public boolean placeBlock(BlockPos var1, Direction var2, int var3, boolean var4, boolean var5, RotationCallback var6) {
      Vec3d var7 = var1.toCenterPos().add(new Vec3d(var2.getUnitVector()).multiply(0.5));
      return this.placeBlock(new BlockHitResult(var7, var2, var1, false), var3, var4, var5, var6);
   }

   public boolean placeBlock(BlockHitResult var1, int var2, boolean var3, boolean var4, boolean var5, RotationCallback var6) {
      boolean var8 = var2 != PortSupport.Managers.INVENTORY.getServerSlot();
      if (var8) {
         PortSupport.Managers.INVENTORY.setSlot(var2);
      }

      if (var4) {
         PortSupport.Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
      }

      boolean var7 = var6 != null;
      if (var7) {
         float[] var11 = RotationUtil.getRotationsTo(mc.player.getEyePos(), var1.getPos());
         var6.handleRotation(true, var11);
      }

      boolean var13 = this.placeBlockImmediately(var1, var4 ? Hand.OFF_HAND : Hand.MAIN_HAND, var3, var5);
      if (var7) {
         float[] var12 = RotationUtil.getRotationsTo(mc.player.getEyePos(), var1.getPos());
         var6.handleRotation(false, var12);
      }

      if (var4) {
         PortSupport.Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
      }

      if (var8) {
         PortSupport.Managers.INVENTORY.syncToClient();
      }

      return var13;
   }

   public boolean placeBlock(BlockHitResult var1, int var2, boolean var3, boolean var4, RotationCallback var5) {
      boolean var7 = var2 != PortSupport.Managers.INVENTORY.getServerSlot();
      if (var7) {
         PortSupport.Managers.INVENTORY.setSlot(var2);
      }

      if (var4) {
         PortSupport.Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
      }

      boolean var6 = var5 != null;
      if (var6) {
         float[] var10 = RotationUtil.getRotationsTo(mc.player.getEyePos(), var1.getPos());
         var5.handleRotation(true, var10);
      }

      boolean var12 = this.placeBlockImmediately(var1, var4 ? Hand.OFF_HAND : Hand.MAIN_HAND, var3, true);
      if (var6) {
         float[] var11 = RotationUtil.getRotationsTo(mc.player.getEyePos(), var1.getPos());
         var5.handleRotation(false, var11);
      }

      if (var4) {
         PortSupport.Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
      }

      if (var7) {
         PortSupport.Managers.INVENTORY.syncToClient();
      }

      return var12;
   }

   public boolean placeBlockImmediately(BlockHitResult var1, Hand var2, boolean var3, boolean var4) {
      BlockState var7 = mc.world.getBlockState(var1.getBlockPos());
      boolean var6 = SneakBlocks.isSneakBlock(var7) && !mc.player.isSneaking();
      if (var6) {
         PistonInput.setPacketSneaking(true);
         PistonInput.applySneak();
      }

      ActionResult var5 = var4 ? this.placeBlockPacket(var1, var2) : this.placeBlockInternally(var1, var2);
      if (var5.isAccepted() && var5 instanceof Success var10 && var10.swingSource() == SwingSource.CLIENT) {
         if (var3) {
            mc.player.swingHand(Hand.MAIN_HAND);
         } else {
            PortSupport.Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
         }
      }

      if (var6) {
         PistonInput.setPacketSneaking(false);
      }

      return var5.isAccepted();
   }

   private ActionResult placeBlockInternally(BlockHitResult var1, Hand var2) {
      return mc.interactionManager.interactBlock(mc.player, var2, var1);
   }

   public ActionResult placeBlockPacket(BlockHitResult var1, Hand var2) {
      PortSupport.Managers.NETWORK.sendSequencedPacket(var2x -> new PlayerInteractBlockC2SPacket(var2, var1, var2x));
      return ActionResult.SUCCESS;
   }

   public Direction getInteractDirection(BlockPos var1, boolean var2) {
      Direction var3 = this.getInteractDirectionInternal(var1, var2);
      return var3 == null ? Direction.UP : var3;
   }

   public Direction getInteractDirectionInternal(BlockPos var1, boolean var2) {
      Set var3 = this.getPlaceDirectionsNCP(mc.player.getEyePos(), var1.toCenterPos());
      Direction var4 = null;

      for (Direction var8 : Direction.values()) {
         BlockState var9 = mc.world.getBlockState(var1.offset(var8));
         if (!var9.isAir()
            && var9.getFluidState().isEmpty()
            && var9.getBlock() != Blocks.ANVIL
            && var9.getBlock() != Blocks.CHIPPED_ANVIL
            && var9.getBlock() != Blocks.DAMAGED_ANVIL
            && (!var2 || var3.contains(var8.getOpposite()))) {
            var4 = var8;
            break;
         }
      }

      return var4 == null ? null : var4.getOpposite();
   }

   public Direction getPlaceDirectionNCP(BlockPos var1, boolean var2) {
      Vec3d var3 = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getStandingEyeHeight(), mc.player.getZ());
      if (var1.getX() == var3.getX() && var1.getY() == var3.getY() && var1.getZ() == var3.getZ()) {
         return Direction.DOWN;
      } else {
         for (Direction var6 : this.getPlaceDirectionsNCP(var3, var1.toCenterPos())) {
            if (!var2 || mc.world.isAir(var1.offset(var6))) {
               return var6;
            }
         }

         return Direction.UP;
      }
   }

   public Set<Direction> getPlaceDirectionsNCP(Vec3d var1, Vec3d var2) {
      return this.getPlaceDirectionsNCP(var1.x, var1.y, var1.z, var2.x, var2.y, var2.z);
   }

   public Set<Direction> getPlaceDirectionsNCP(double var1, double var3, double var5, double var7, double var9, double var11) {
      double var13 = var1 - var7;
      double var15 = var3 - var9;
      double var17 = var5 - var11;
      HashSet var19 = new HashSet(6);
      if (var15 > 0.5) {
         var19.add(Direction.UP);
      } else if (var15 < -0.5) {
         var19.add(Direction.DOWN);
      } else {
         var19.add(Direction.UP);
         var19.add(Direction.DOWN);
      }

      if (var13 > 0.5) {
         var19.add(Direction.EAST);
      } else if (var13 < -0.5) {
         var19.add(Direction.WEST);
      } else {
         var19.add(Direction.EAST);
         var19.add(Direction.WEST);
      }

      if (var17 > 0.5) {
         var19.add(Direction.SOUTH);
      } else if (var17 < -0.5) {
         var19.add(Direction.NORTH);
      } else {
         var19.add(Direction.SOUTH);
         var19.add(Direction.NORTH);
      }

      return var19;
   }

   public boolean isInEyeRange(BlockPos var1) {
      return var1.getY() > mc.player.getY() + mc.player.getStandingEyeHeight();
   }
}
