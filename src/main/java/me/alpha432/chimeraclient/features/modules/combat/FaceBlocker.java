package me.alpha432.chimeraclient.features.modules.combat;

import java.util.ArrayList;
import java.util.List;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.ActionResult.SwingSource;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class FaceBlocker extends Module {
   private final Setting<Boolean> silent = this.bool("Silent", true);
   private final Setting<Boolean> placeInAir = this.bool("PlaceInAir", false);
   private final Setting<Integer> placesPerTick = this.num("PlacesPerTick", 1, 1, 8);
   private final Setting<Integer> delay = this.num("Delay", 0, 0, 20);
   private final Setting<Double> range = this.num("Range", 4.0, 1.0, 8.0);
   private int delayTimer = 0;

   public FaceBlocker() {
      super("FaceBlocker", "Places obsidian above your head when an enemy comes close", Module.Category.COMBAT);
   }

   @Override
   public void onDisable() {
      this.delayTimer = 0;
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         if (this.isInsideHole(mc.player.getBlockPos())) {
            if (this.isEnemyNear()) {
               int obbySlot = this.findObsidianInHotbar();
               if (obbySlot != -1) {
                  if (this.delayTimer < this.delay.getValue()) {
                     this.delayTimer++;
                  } else {
                     this.delayTimer = 0;
                     BlockPos target = mc.player.getBlockPos().up(2);
                     List<BlockPos> targetsToPlace = new ArrayList<>();
                     if (mc.world.getBlockState(target).isReplaceable()) {
                        if (!this.placeInAir.getValue() && !this.hasSolidNeighbor(target)) {
                           BlockPos support = target.down();
                           if (mc.world.getBlockState(support).isReplaceable() && !this.isBlockedByEntity(support)) {
                              targetsToPlace.add(support);
                           } else {
                              for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                                 BlockPos side = target.offset(dir);
                                 if (mc.world.getBlockState(side).isReplaceable() && !this.isBlockedByEntity(side)) {
                                    BlockPos sideSupport = side.down();
                                    if (!this.hasSolidNeighbor(side)
                                       && mc.world.getBlockState(sideSupport).isReplaceable()
                                       && !this.isBlockedByEntity(sideSupport)) {
                                       targetsToPlace.add(sideSupport);
                                    }

                                    targetsToPlace.add(side);
                                    break;
                                 }
                              }
                           }
                        }

                        targetsToPlace.add(target);
                     }

                     int placed = 0;
                     int max = this.placesPerTick.getValue();

                     for (BlockPos pos : targetsToPlace) {
                        if (placed >= max) {
                           break;
                        }

                        if (mc.world.getBlockState(pos).isReplaceable() && this.placeBlock(pos, obbySlot)) {
                           placed++;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean isInsideHole(BlockPos feet) {
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

   private boolean isEnemyNear() {
      double r = this.range.getValue();

      for (PlayerEntity player : mc.world.getPlayers()) {
         if (player != mc.player && !player.isDead() && !ChimeraClient.friendManager.isFriend(player) && mc.player.distanceTo(player) <= r) {
            return true;
         }
      }

      return false;
   }

   private boolean isBlockedByEntity(BlockPos pos) {
      return mc.player != null && mc.world != null
         ? !mc.world
            .getEntitiesByClass(Entity.class, new Box(pos), e -> e != mc.player && !(e instanceof ItemEntity) && !(e instanceof ExperienceOrbEntity))
            .isEmpty()
         : false;
   }

   private boolean hasSolidNeighbor(BlockPos pos) {
      for (Direction dir : Direction.values()) {
         if (!mc.world.getBlockState(pos.offset(dir)).isReplaceable()) {
            return true;
         }
      }

      return false;
   }

   private boolean placeBlock(BlockPos pos, int slot) {
      if (mc.player != null && mc.world != null) {
         Direction clickSide = Direction.UP;
         BlockPos clickTarget = pos;
         boolean foundNeighbor = false;

         for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            if (!mc.world.getBlockState(neighbor).isReplaceable()) {
               clickSide = dir.getOpposite();
               clickTarget = neighbor;
               foundNeighbor = true;
               break;
            }
         }

         if (!foundNeighbor && !this.placeInAir.getValue()) {
            return false;
         } else {
            if (!foundNeighbor && this.placeInAir.getValue()) {
               clickSide = Direction.UP;
               clickTarget = pos;
            }

            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), clickSide, clickTarget, false);
            int original = mc.player.getInventory().selectedSlot;
            boolean needSwap = slot != original;
            if (needSwap) {
               mc.player.getInventory().selectedSlot = slot;
               mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            }

            float preYaw = mc.player.getYaw();
            float prePitch = mc.player.getPitch();
            Vec3d lookTarget = Vec3d.ofCenter(clickTarget);
            float[] look = this.calcLookAtPos(lookTarget);
            this.sendLookPacket(look[0], look[1]);
            if (mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit) instanceof Success success && success.swingSource() != SwingSource.NONE) {
               mc.player.swingHand(Hand.MAIN_HAND);
            }

            if (this.silent.getValue()) {
               this.sendLookPacket(preYaw, prePitch);
            }

            if (needSwap && this.silent.getValue()) {
               mc.player.getInventory().selectedSlot = original;
               mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(original));
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private void sendLookPacket(float yaw, float pitch) {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(new LookAndOnGround(yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision));
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

   private int findObsidianInHotbar() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).isOf(Items.OBSIDIAN)) {
            return i;
         }
      }

      return -1;
   }
}
