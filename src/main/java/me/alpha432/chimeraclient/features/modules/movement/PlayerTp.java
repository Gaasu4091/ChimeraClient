package me.alpha432.chimeraclient.features.modules.movement;

import java.util.ArrayList;
import java.util.List;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class PlayerTp extends Module {
   private final Setting<Integer> range = this.register(new Setting<>("Range", 50, 1, 1000));
   private final Setting<Integer> ticksToTp = this.register(new Setting<>("Ticks", 10, 1, 100));
   private final Setting<Boolean> holeTp = this.register(new Setting<>("HoleTp", false));
   private PlayerEntity target = null;
   private List<Vec3d> path = new ArrayList<>();
   private double totalDistance = 0.0;
   private int currentSegmentIndex = 0;
   private double segmentProgress = 0.0;
   private Vec3d lockedDestination = null;
   private BlockPos lockedDestinationBlock = null;
   private boolean isLocked = false;

   public PlayerTp() {
      super("PlayerTp", "Teleports to a player, bypassing walls and roofs", Module.Category.MOVEMENT);
   }

   @Override
   public void onEnable() {
      if (!nullCheck()) {
         this.target = this.findClosestPlayer();
         if (this.target == null) {
            this.disable();
         } else {
            this.isLocked = false;
            this.lockedDestination = null;
            this.lockedDestinationBlock = null;
            this.calculatePath();
         }
      }
   }

   @Override
   public void onDisable() {
      this.target = null;
      this.path.clear();
      this.totalDistance = 0.0;
      this.currentSegmentIndex = 0;
      this.segmentProgress = 0.0;
      this.isLocked = false;
      this.lockedDestination = null;
      this.lockedDestinationBlock = null;
   }

   @Override
   public void onTick() {
      if (nullCheck() || this.target == null || !this.target.isAlive() || !mc.world.getPlayers().contains(this.target)) {
         this.disable();
      } else if (mc.player.getBoundingBox().intersects(this.target.getBoundingBox())) {
         this.disable();
      } else {
         if (this.path.isEmpty() || this.path.size() < 2) {
            this.calculatePath();
            if (this.path.isEmpty() || this.path.size() < 2) {
               return;
            }
         }

         if (this.ticksToTp.getValue() == 1) {
            for (int i = 1; i < this.path.size(); i++) {
               Vec3d p = this.path.get(i);
               this.updatePlayerPosition(p);
            }

            this.path.clear();
            this.currentSegmentIndex = 0;
            this.segmentProgress = 0.0;
            if (this.holeTp.getValue()) {
               this.disable();
            } else {
               this.isLocked = false;
            }
         } else {
            double remaining = this.totalDistance / this.ticksToTp.getValue().intValue();

            while (remaining > 0.0 && this.currentSegmentIndex < this.path.size() - 1) {
               Vec3d segStart = this.path.get(this.currentSegmentIndex);
               Vec3d segEnd = this.path.get(this.currentSegmentIndex + 1);
               double segLen = segStart.distanceTo(segEnd);
               if (segLen <= 1.0E-4) {
                  this.currentSegmentIndex++;
                  this.segmentProgress = 0.0;
               } else {
                  double distLeftInSeg = segLen - this.segmentProgress;
                  if (remaining < distLeftInSeg) {
                     this.segmentProgress += remaining;
                     double ratio = this.segmentProgress / segLen;
                     Vec3d pos = new Vec3d(
                        segStart.x + (segEnd.x - segStart.x) * ratio,
                        segStart.y + (segEnd.y - segStart.y) * ratio,
                        segStart.z + (segEnd.z - segStart.z) * ratio
                     );
                     this.updatePlayerPosition(pos);
                     remaining = 0.0;
                  } else {
                     remaining -= distLeftInSeg;
                     this.currentSegmentIndex++;
                     this.segmentProgress = 0.0;
                     this.updatePlayerPosition(segEnd);
                  }
               }
            }

            if (this.currentSegmentIndex >= this.path.size() - 1) {
               if (this.holeTp.getValue()) {
                  this.disable();
                  return;
               }

               this.isLocked = false;
               this.calculatePath();
            }
         }
      }
   }

   private void calculatePath() {
      if (this.target != null && mc.player != null) {
         Vec3d start = mc.player.getEntityPos();
         Vec3d end;
         BlockPos destFeet;
         if (!this.isLocked) {
            end = this.target.getEntityPos();
            destFeet = this.target.getBlockPos();
            if (this.holeTp.getValue()) {
               BlockPos hole = this.findClosestHoleToPlayer(this.target);
               if (hole != null) {
                  end = new Vec3d(hole.getX() + 0.5, hole.getY(), hole.getZ() + 0.5);
                  destFeet = hole;
               }
            }

            this.lockedDestination = end;
            this.lockedDestinationBlock = destFeet;
            this.isLocked = true;
         } else {
            end = this.lockedDestination;
            destFeet = this.lockedDestinationBlock;
         }

         this.path.clear();
         this.path.add(start);
         BlockPos myFeet = mc.player.getBlockPos();
         BlockPos myTowerTop = this.findTowerTop(myFeet);
         double safeStartY = myTowerTop != null ? myTowerTop.getY() : start.y;
         BlockPos tgtTowerTop = this.findTowerTop(destFeet);
         double safeEndY = tgtTowerTop != null ? tgtTowerTop.getY() : end.y;
         double transitY = Math.max(safeStartY, safeEndY);

         for (int i = 0; i < 10; i++) {
            Vec3d transitStart = new Vec3d(start.x, transitY, start.z);
            Vec3d transitEnd = new Vec3d(end.x, transitY, end.z);
            if (!this.hasObstacleBetween(transitStart, transitEnd)) {
               break;
            }

            double obstacleTop = this.findObstacleTopY(transitStart, transitEnd);
            transitY = Math.max(transitY, obstacleTop) + 1.0;
         }

         if (transitY > start.y + 0.01) {
            this.path.add(new Vec3d(start.x, transitY, start.z));
         }

         Vec3d lastPoint = this.path.get(this.path.size() - 1);
         if (Math.abs(lastPoint.x - end.x) > 0.01) {
            this.path.add(new Vec3d(end.x, lastPoint.y, lastPoint.z));
         }

         lastPoint = this.path.get(this.path.size() - 1);
         if (Math.abs(lastPoint.z - end.z) > 0.01) {
            this.path.add(new Vec3d(lastPoint.x, lastPoint.y, end.z));
         }

         lastPoint = this.path.get(this.path.size() - 1);
         if (Math.abs(lastPoint.y - end.y) > 0.01) {
            this.path.add(new Vec3d(lastPoint.x, end.y, lastPoint.z));
         }

         this.recalcTotalDistance();
      }
   }

   private void recalcTotalDistance() {
      this.totalDistance = 0.0;

      for (int i = 0; i < this.path.size() - 1; i++) {
         this.totalDistance = this.totalDistance + this.path.get(i).distanceTo(this.path.get(i + 1));
      }

      this.currentSegmentIndex = 0;
      this.segmentProgress = 0.0;
   }

   private boolean hasObstacleBetween(Vec3d start, Vec3d end) {
      if (mc.world == null) {
         return true;
      } else {
         HitResult result = mc.world.raycast(new RaycastContext(start, end, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
         return result.getType() != Type.MISS;
      }
   }

   private BlockPos findClosestHoleToPlayer(PlayerEntity player) {
      if (mc.world != null && mc.player != null) {
         BlockPos targetPos = player.getBlockPos();
         BlockPos myPos = mc.player.getBlockPos();
         BlockPos closestHole = null;
         double minDistanceSq = Double.MAX_VALUE;
         int r = 8;

         for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
               for (int z = -r; z <= r; z++) {
                  BlockPos pos = targetPos.add(x, y, z);
                  if (!pos.equals(targetPos) && !pos.equals(myPos) && this.isInsideHole(pos) && this.isSafeToStand(pos)) {
                     double distSq = pos.getSquaredDistanceFromCenter(myPos.getX() + 0.5, myPos.getY(), myPos.getZ() + 0.5);
                     if (distSq < minDistanceSq) {
                        minDistanceSq = distSq;
                        closestHole = pos;
                     }
                  }
               }
            }
         }

         return closestHole;
      } else {
         return null;
      }
   }

   private boolean isInsideHole(BlockPos feet) {
      if (mc.world == null) {
         return false;
      } else {
         for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            BlockPos neighbor = feet.offset(dir);
            Block block = mc.world.getBlockState(neighbor).getBlock();
            boolean isSolid = block == Blocks.OBSIDIAN || block == Blocks.BEDROCK || block == Blocks.CRYING_OBSIDIAN;
            if (!isSolid) {
               return false;
            }
         }

         return true;
      }
   }

   private boolean isSafeToStand(BlockPos pos) {
      if (mc.world == null) {
         return false;
      } else if (!mc.world.getBlockState(pos).isReplaceable()) {
         return false;
      } else {
         return !mc.world.getBlockState(pos.up()).isReplaceable() ? false : !mc.world.getBlockState(pos.down()).isReplaceable();
      }
   }

   private BlockPos findTowerTop(BlockPos feet) {
      if (mc.world == null) {
         return null;
      } else {
         BlockPos layer0 = feet.up(2);
         BlockPos layer1 = feet.up(3);
         BlockPos startScan = null;
         if (!mc.world.getBlockState(layer0).isReplaceable()) {
            startScan = layer0;
         } else if (!mc.world.getBlockState(layer1).isReplaceable()) {
            startScan = layer1;
         }

         if (startScan == null) {
            return null;
         } else {
            BlockPos current = startScan;

            while (!mc.world.getBlockState(current).isReplaceable()) {
               current = current.up();
               if (current.getY() >= mc.world.getTopYInclusive()) {
                  break;
               }
            }

            return current;
         }
      }
   }

   private double findObstacleTopY(Vec3d start, Vec3d end) {
      if (mc.world == null) {
         return start.y;
      } else {
         HitResult result = mc.world.raycast(new RaycastContext(start, end, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
         if (result.getType() == Type.MISS) {
            return start.y;
         } else {
            BlockPos hit = BlockPos.ofFloored(result.getPos());
            BlockPos current = hit;

            while (!mc.world.getBlockState(current).isReplaceable()) {
               current = current.up();
               if (current.getY() >= mc.world.getTopYInclusive()) {
                  break;
               }
            }

            return current.getY();
         }
      }
   }

   private void updatePlayerPosition(Vec3d pos) {
      mc.player.setPosition(pos.x, pos.y, pos.z);
      mc.getNetworkHandler().sendPacket(new PositionAndOnGround(pos.x, pos.y, pos.z, mc.player.isOnGround(), false));
   }

   private PlayerEntity findClosestPlayer() {
      if (mc.world != null && mc.player != null) {
         PlayerEntity closest = null;
         double minDistanceSq = (double)this.range.getValue().intValue() * this.range.getValue().intValue();

         for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && player.isAlive() && !ChimeraClient.friendManager.isFriend(player)) {
               double distSq = mc.player.squaredDistanceTo(player);
               if (distSq <= minDistanceSq) {
                  minDistanceSq = distSq;
                  closest = player;
               }
            }
         }

         return closest;
      } else {
         return null;
      }
   }
}
