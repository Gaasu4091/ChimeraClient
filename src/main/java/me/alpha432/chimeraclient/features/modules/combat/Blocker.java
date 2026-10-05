package me.alpha432.chimeraclient.features.modules.combat;

import java.util.ArrayList;
import java.util.List;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.util.InteractionUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Blocker extends Module {
   private final Setting<Double> delay = this.num("Delay", 2.0, 0.0, 40.0);
   private final Setting<Integer> blocksPerTick = this.num("BlocksPerTick", 2, 1, 8);
   private int placeTicks = 0;

   public Blocker() {
      super("Blocker", "Surrounds the front, left, and right of your hole.", Module.Category.COMBAT);
   }

   @Override
   public void onDisable() {
      this.placeTicks = 0;
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         this.placeTicks++;
         if (!mc.player.isUsingItem() || !mc.player.getActiveItem().contains(DataComponentTypes.FOOD)) {
            if (this.placeTicks >= this.delay.getValue().intValue()) {
               BlockPos feet = mc.player.getBlockPos();
               if (this.isInsideHole(feet)) {
                  List<BlockPos> targetPositions = this.getTargetPositions(feet);
                  int blocksPlaced = 0;
                  int maxBlocks = this.blocksPerTick.getValue();

                  for (BlockPos pos : targetPositions) {
                     if (blocksPlaced >= maxBlocks) {
                        break;
                     }

                     if (mc.world.getBlockState(pos).isReplaceable()) {
                        this.placeObsidian(pos);
                        blocksPlaced++;
                     }
                  }

                  if (blocksPlaced > 0) {
                     this.placeTicks = 0;
                  }
               }
            }
         }
      }
   }

   private List<BlockPos> getTargetPositions(BlockPos feet) {
      List<BlockPos> targets = new ArrayList<>();
      Direction forward = mc.player.getHorizontalFacing();
      Direction left = forward.rotateYCounterclockwise();
      Direction right = forward.rotateYClockwise();
      targets.add(feet.offset(forward).offset(forward));
      targets.add(feet.offset(left).offset(left));
      targets.add(feet.offset(right).offset(right));
      targets.add(feet.offset(forward).offset(left));
      targets.add(feet.offset(forward).offset(right));
      return targets;
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

   private void placeObsidian(BlockPos pos) {
      int slot = this.findObsidianInHotbar();
      if (slot != -1) {
         int original = mc.player.getInventory().selectedSlot;
         mc.player.getInventory().selectedSlot = slot;
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         float preYaw = mc.player.getYaw();
         float prePitch = mc.player.getPitch();
         float[] look = this.calcLookAt(Vec3d.ofCenter(pos));
         this.sendLookPacket(look[0], look[1]);
         InteractionUtil.place(pos, true);
         this.sendLookPacket(preYaw, prePitch);
         mc.player.getInventory().selectedSlot = original;
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(original));
      }
   }

   private void sendLookPacket(float yaw, float pitch) {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(new LookAndOnGround(yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision));
      }
   }

   private float[] calcLookAt(Vec3d target) {
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
