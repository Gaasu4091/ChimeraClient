package me.alpha432.chimeraclient.features.modules.misc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AutoRegear extends Module {
   private final Setting<Double> range = this.num("Range", 4.0, 1.0, 7.0);
   private final Setting<Boolean> rotate = this.bool("Rotate", true);
   private final Setting<Boolean> silent = this.bool("Silent", true);
   private final Setting<Boolean> autoSwitch = this.bool("AutoSwitch", true);

   public AutoRegear() {
      super("AutoRegear", "シュルカーを自動設置して即座に開きます", Module.Category.MISC);
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         int shulkerSlot = this.findShulkerInHotbar();
         if (shulkerSlot == -1) {
            this.disable();
         } else {
            if (this.scanAndPlaceShulker(shulkerSlot)) {
               this.disable();
            }
         }
      }
   }

   private boolean scanAndPlaceShulker(int slot) {
      double rangeVal = this.range.getValue();
      BlockPos origin = mc.player.getBlockPos();
      Vec3d playerCenter = mc.player.getEntityPos();
      List<AutoRegear.PlacementCandidate> allCandidates = new ArrayList<>();
      int r = (int)Math.ceil(rangeVal);

      for (int x = -r; x <= r; x++) {
         for (int y = -r; y <= r; y++) {
            for (int z = -r; z <= r; z++) {
               BlockPos pos = origin.add(x, y, z);
               double distSq = playerCenter.squaredDistanceTo(Vec3d.ofCenter(pos));
               if (!(Math.sqrt(distSq) > rangeVal) && mc.world.getBlockState(pos).isReplaceable() && !this.isBlockOccupiedByEntity(pos)) {
                  for (Direction dir : Direction.values()) {
                     BlockPos adjacent = pos.offset(dir);
                     Direction clickFace = dir.getOpposite();
                     if (this.canPlaceAgainst(adjacent, clickFace)) {
                        BlockPos openingPos = pos.offset(clickFace);
                        if (mc.world.getBlockState(openingPos).isReplaceable() && !this.isBlockOccupiedByEntity(openingPos)) {
                           allCandidates.add(new AutoRegear.PlacementCandidate(pos, adjacent, clickFace, clickFace, distSq));
                        }
                     }
                  }
               }
            }
         }
      }

      if (allCandidates.isEmpty()) {
         return false;
      } else {
         allCandidates.sort(Comparator.comparingInt(AutoRegear.PlacementCandidate::priorityScore).thenComparingDouble(c -> c.distSq));
         AutoRegear.PlacementCandidate best = allCandidates.get(0);
         this.doPlaceAndOpen(slot, best.clickTarget, best.clickFace, best.targetPos);
         return true;
      }
   }

   private boolean isBlockOccupiedByEntity(BlockPos pos) {
      Box blockBox = new Box(pos);
      List<Entity> entities = mc.world.getOtherEntities(null, blockBox);
      return !entities.isEmpty();
   }

   private void doPlaceAndOpen(int shulkerSlot, BlockPos clickTarget, Direction clickFace, BlockPos targetPos) {
      if (mc.player != null && mc.world != null) {
         Vec3d hitVec = Vec3d.ofCenter(clickTarget).add(clickFace.getOffsetX() * 0.5, clickFace.getOffsetY() * 0.5, clickFace.getOffsetZ() * 0.5);
         BlockHitResult placeResult = new BlockHitResult(hitVec, clickFace, clickTarget, false);
         int original = mc.player.getInventory().selectedSlot;
         boolean needSwap = shulkerSlot != original;
         if (needSwap) {
            mc.player.getInventory().selectedSlot = shulkerSlot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(shulkerSlot));
         }

         float preYaw = mc.player.getYaw();
         float prePitch = mc.player.getPitch();
         float[] look = this.calcLookAtPos(hitVec);
         this.sendLookPacket(look[0], look[1]);
         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, placeResult);
         mc.player.swingHand(Hand.MAIN_HAND);
         float[] openLook = this.calcLookAtPos(Vec3d.ofCenter(targetPos));
         this.sendLookPacket(openLook[0], openLook[1]);
         BlockHitResult openResult = new BlockHitResult(Vec3d.ofCenter(targetPos), clickFace, targetPos, false);
         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, openResult);
         mc.player.swingHand(Hand.MAIN_HAND);
         if (!this.rotate.getValue()) {
            this.sendLookPacket(preYaw, prePitch);
         }

         if (needSwap && (this.silent.getValue() || this.autoSwitch.getValue())) {
            mc.player.getInventory().selectedSlot = original;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(original));
         }
      }
   }

   private boolean canPlaceAgainst(BlockPos pos, Direction face) {
      if (mc.world.getBlockState(pos).isAir()) {
         return false;
      } else {
         Vec3d eyePos = mc.player.getEyePos();
         Vec3d faceCenter = Vec3d.ofCenter(pos).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
         Vec3d lookVec = faceCenter.subtract(eyePos);
         Vec3d normal = new Vec3d(face.getOffsetX(), face.getOffsetY(), face.getOffsetZ());
         return lookVec.dotProduct(normal) <= 0.0;
      }
   }

   private void sendLookPacket(float yaw, float pitch) {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(new LookAndOnGround(yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision));
         if (this.rotate.getValue()) {
            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
         }
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

   private int findShulkerInHotbar() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock) {
            return i;
         }
      }

      return -1;
   }

   private static class PlacementCandidate {
      final BlockPos targetPos;
      final BlockPos clickTarget;
      final Direction clickFace;
      final Direction openingDir;
      final double distSq;

      PlacementCandidate(BlockPos targetPos, BlockPos clickTarget, Direction clickFace, Direction openingDir, double distSq) {
         this.targetPos = targetPos;
         this.clickTarget = clickTarget;
         this.clickFace = clickFace;
         this.openingDir = openingDir;
         this.distSq = distSq;
      }

      int priorityScore() {
         switch (this.clickFace) {
            case EAST:
            case WEST:
            case NORTH:
            case SOUTH:
               return 0;
            case UP:
               return 1;
            case DOWN:
               return 2;
            default:
               return 3;
         }
      }
   }
}
