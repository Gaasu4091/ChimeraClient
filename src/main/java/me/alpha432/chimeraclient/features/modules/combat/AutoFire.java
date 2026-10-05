package me.alpha432.chimeraclient.features.modules.combat;

import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.ActionResult.SwingSource;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AutoFire extends Module {
   private final Setting<Double> range = this.num("Range", 5.0, 1.0, 7.0);
   private final Setting<Double> placeDelay = this.num("PlaceDelay", 0.0, 0.0, 20.0);
   private final Setting<Boolean> silent = this.bool("Silent", true);
   private int placeTicks = 0;

   public AutoFire() {
      super("AutoFire", "Ignites enemies in holes with flint and steel", Module.Category.COMBAT);
   }

   @Override
   public void onEnable() {
      this.placeTicks = 0;
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         this.placeTicks++;
         if (!mc.player.isUsingItem() || !mc.player.getActiveItem().contains(DataComponentTypes.FOOD)) {
            if (this.placeTicks >= this.placeDelay.getValue().intValue()) {
               for (PlayerEntity target : mc.world.getPlayers()) {
                  if (target != mc.player && !(mc.player.distanceTo(target) > this.range.getValue()) && target.isAlive() && this.isInHole(target)) {
                     BlockPos targetPos = target.getBlockPos();
                     if (mc.world.getBlockState(targetPos).isReplaceable() && this.ignite(targetPos)) {
                        this.placeTicks = 0;
                        break;
                     }
                  }
               }
            }
         }
      }
   }

   private boolean isInHole(PlayerEntity player) {
      if (player != null && mc.world != null) {
         BlockPos feet = player.getBlockPos();
         if (!this.isHoleSolid(feet.down())) {
            return false;
         } else {
            for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
               if (!this.isHoleSolid(feet.offset(dir))) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private boolean isHoleSolid(BlockPos pos) {
      Block b = mc.world.getBlockState(pos).getBlock();
      return b == Blocks.OBSIDIAN || b == Blocks.BEDROCK || b == Blocks.CRYING_OBSIDIAN;
   }

   private boolean ignite(BlockPos pos) {
      int slot = this.findFlintAndSteel();
      if (slot == -1) {
         return false;
      } else {
         Direction clickSide = Direction.UP;
         BlockPos clickTarget = pos.down();
         BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), clickSide, clickTarget, false);
         int original = mc.player.getInventory().selectedSlot;
         boolean needSwap = slot != original;
         if (needSwap) {
            mc.player.getInventory().selectedSlot = slot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         }

         float preYaw = mc.player.getYaw();
         float prePitch = mc.player.getPitch();
         float[] look = this.calcLookAtPos(Vec3d.ofCenter(pos));
         this.sendLookPacket(look[0], look[1]);
         if (mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit) instanceof Success success && success.swingSource() != SwingSource.NONE) {
            mc.player.swingHand(Hand.MAIN_HAND);
         }

         this.sendLookPacket(preYaw, prePitch);
         if (needSwap && this.silent.getValue()) {
            mc.player.getInventory().selectedSlot = original;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(original));
         }

         return true;
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

   private int findFlintAndSteel() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).isOf(Items.FLINT_AND_STEEL)) {
            return i;
         }
      }

      return -1;
   }
}
