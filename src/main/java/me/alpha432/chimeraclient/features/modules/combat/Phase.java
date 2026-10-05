package me.alpha432.chimeraclient.features.modules.combat;

import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.util.inventory.InventoryUtil;
import me.alpha432.chimeraclient.util.inventory.Result;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Type;

public class Phase extends Module {
   private final Setting<Boolean> inventory = this.bool("Inventory", false);
   private static final float CORNER_LOOK_THRESHOLD = 30.0F;

   public Phase() {
      super("Phase", "Throws an ender pearl at the closest floor/wall corner", Module.Category.COMBAT);
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         Result pearlResult = InventoryUtil.find(Items.ENDER_PEARL, this.inventory.getValue() ? InventoryUtil.FULL_SCOPE : InventoryUtil.HOTBAR_SCOPE);
         if (!pearlResult.found()) {
            this.disable();
         } else {
            boolean isForward = mc.options.forwardKey.isPressed();
            boolean isBack = mc.options.backKey.isPressed();
            boolean isLeft = mc.options.leftKey.isPressed();
            boolean isRight = mc.options.rightKey.isPressed();
            boolean isMoving = isForward || isBack || isLeft || isRight;
            float preYaw = mc.player.getYaw();
            float prePitch = mc.player.getPitch();
            float targetYaw;
            float targetPitch;
            if (isMoving) {
               float referenceYaw = preYaw;
               if (isForward) {
                  if (isLeft) {
                     referenceYaw = preYaw - 45.0F;
                  } else if (isRight) {
                     referenceYaw = preYaw + 45.0F;
                  }
               } else if (isBack) {
                  if (isLeft) {
                     referenceYaw = preYaw - 135.0F;
                  } else if (isRight) {
                     referenceYaw = preYaw + 135.0F;
                  } else {
                     referenceYaw = preYaw + 180.0F;
                  }
               } else if (isLeft) {
                  referenceYaw = preYaw - 90.0F;
               } else if (isRight) {
                  referenceYaw = preYaw + 90.0F;
               }

               Vec3d targetCorner = this.findLookingBlockCorner(30.0F, referenceYaw);
               if (targetCorner == null) {
                  targetCorner = this.findClosestCorner(referenceYaw);
               }

               if (targetCorner == null) {
                  this.disable();
                  return;
               }

               float[] rotations = this.calcLookAtPos(targetCorner);
               targetYaw = rotations[0];
               targetPitch = rotations[1];
            } else {
               Direction facing = mc.player.getHorizontalFacing();
               BlockPos playerPos = mc.player.getBlockPos();
               BlockPos frontPos = playerPos.offset(facing);
               BlockPos frontHeadPos = frontPos.up();
               boolean feetEmpty = mc.world.getBlockState(frontPos).isReplaceable();
               boolean headEmpty = mc.world.getBlockState(frontHeadPos).isReplaceable();
               if (feetEmpty && headEmpty) {
                  this.disable();
                  return;
               }

               Vec3d targetEdge = this.getCorner(playerPos, facing);
               float[] rotations = this.calcLookAtPos(targetEdge);
               targetYaw = preYaw;
               targetPitch = rotations[1];
            }

            mc.player.setYaw(targetYaw);
            mc.player.setPitch(targetPitch);
            mc.getNetworkHandler().sendPacket(new LookAndOnGround(targetYaw, targetPitch, mc.player.isOnGround(), mc.player.horizontalCollision));
            InventoryUtil.withSwap(pearlResult, () -> {
               mc.interactionManager.interactItem(mc.player, pearlResult.hand());
               mc.player.swingHand(pearlResult.hand());
            });
            mc.player.setYaw(preYaw);
            mc.player.setPitch(prePitch);
            mc.getNetworkHandler().sendPacket(new LookAndOnGround(preYaw, prePitch, mc.player.isOnGround(), mc.player.horizontalCollision));
            this.disable();
         }
      }
   }

   private Vec3d findLookingBlockCorner(float maxAngleDiff, float referenceYaw) {
      BlockPos pos = mc.player.getBlockPos();
      double x = pos.getX();
      double y = pos.getY();
      double z = pos.getZ();
      Direction[][] pairs = new Direction[][]{
         {Direction.NORTH, Direction.EAST}, {Direction.EAST, Direction.SOUTH}, {Direction.SOUTH, Direction.WEST}, {Direction.WEST, Direction.NORTH}
      };
      Vec3d[] corners = new Vec3d[]{new Vec3d(x + 1.0, y, z), new Vec3d(x + 1.0, y, z + 1.0), new Vec3d(x, y, z + 1.0), new Vec3d(x, y, z)};
      Vec3d bestCorner = null;
      float minAngleDiff = maxAngleDiff;

      for (int i = 0; i < 4; i++) {
         boolean wall1 = !mc.world.getBlockState(pos.offset(pairs[i][0])).isReplaceable();
         boolean wall2 = !mc.world.getBlockState(pos.offset(pairs[i][1])).isReplaceable();
         if (wall1 && wall2) {
            float[] rots = this.calcLookAtPos(corners[i]);
            float angleDiff = Math.abs(MathHelper.wrapDegrees(rots[0] - referenceYaw));
            if (angleDiff < minAngleDiff) {
               minAngleDiff = angleDiff;
               bestCorner = corners[i];
            }
         }
      }

      return bestCorner;
   }

   private Vec3d findClosestCorner(float referenceYaw) {
      BlockPos pos = mc.player.getBlockPos();
      Vec3d bestCorner = null;
      float minAngleDiff = Float.MAX_VALUE;

      for (Direction dir : Type.HORIZONTAL) {
         BlockPos wallPos = pos.offset(dir);
         if (!mc.world.getBlockState(wallPos).isReplaceable()) {
            Vec3d corner = this.getCorner(pos, dir);
            float[] rots = this.calcLookAtPos(corner);
            float angleDiff = Math.abs(MathHelper.wrapDegrees(rots[0] - referenceYaw));
            if (angleDiff < minAngleDiff) {
               minAngleDiff = angleDiff;
               bestCorner = corner;
            }
         }
      }

      return bestCorner;
   }

   private Vec3d getCorner(BlockPos pos, Direction dir) {
      double x = pos.getX();
      double y = pos.getY();
      double z = pos.getZ();
      switch (dir) {
         case NORTH:
            return new Vec3d(x + 0.5, y, z);
         case SOUTH:
            return new Vec3d(x + 0.5, y, z + 1.0);
         case EAST:
            return new Vec3d(x + 1.0, y, z + 0.5);
         case WEST:
            return new Vec3d(x, y, z + 0.5);
         default:
            return new Vec3d(x + 0.5, y, z + 0.5);
      }
   }

   private float[] calcLookAtPos(Vec3d target) {
      Vec3d eyes = mc.player.getEyePos();
      double dx = target.x - eyes.x;
      double dy = target.y - eyes.y;
      double dz = target.z - eyes.z;
      double dist = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)(MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
      float pitch = (float)(-(MathHelper.atan2(dy, dist) * (180.0 / Math.PI)));
      return new float[]{yaw, pitch};
   }
}
