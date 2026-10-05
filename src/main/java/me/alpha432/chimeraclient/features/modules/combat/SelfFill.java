package me.alpha432.chimeraclient.features.modules.combat;

import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.ActionResult.SwingSource;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class SelfFill extends Module {
   private final Setting<SelfFill.BlockMode> blockMode = this.register(new Setting<>("Block", SelfFill.BlockMode.OBSIDIAN));
   private final Setting<Boolean> autoDisable = this.bool("AutoDisable", true);
   private double prevY;

   public SelfFill() {
      super("SelfFill", "Fill the players feet (Burrow)", Module.Category.COMBAT);
   }

   @Override
   public void onEnable() {
      if (!nullCheck()) {
         this.prevY = mc.player.getY();
      }
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         double dy = Math.abs(mc.player.getY() - this.prevY);
         if (!(dy > 0.5) && mc.player.isOnGround()) {
            BlockPos feetPos = mc.player.getBlockPos();
            if (!mc.world.getBlockState(feetPos).isReplaceable()) {
               if (this.autoDisable.getValue()) {
                  this.disable();
               }
            } else {
               int slot = this.getSlotForMode();
               if (slot == -1) {
                  this.disable();
               } else {
                  if (this.blockMode.getValue() == SelfFill.BlockMode.WEB) {
                     this.placeBlock(feetPos, slot);
                  } else {
                     Box jumpBox = mc.player.getBoundingBox().offset(0.0, 2.34, 0.0);
                     if (!mc.world.isSpaceEmpty(jumpBox)) {
                        this.disable();
                        return;
                     }

                     double x = mc.player.getX();
                     double y = mc.player.getY();
                     double z = mc.player.getZ();
                     mc.getNetworkHandler().sendPacket(new PositionAndOnGround(x, y + 0.42, z, true, false));
                     mc.getNetworkHandler().sendPacket(new PositionAndOnGround(x, y + 0.75, z, true, false));
                     mc.getNetworkHandler().sendPacket(new PositionAndOnGround(x, y + 1.01, z, true, false));
                     mc.getNetworkHandler().sendPacket(new PositionAndOnGround(x, y + 1.16, z, true, false));
                     mc.player.setPosition(x, y + 1.16, z);
                     this.placeBlock(feetPos, slot);
                     mc.player.setPosition(x, y, z);
                     mc.getNetworkHandler().sendPacket(new PositionAndOnGround(x, y + 2.34, z, false, false));
                  }

                  if (this.autoDisable.getValue()) {
                     this.disable();
                  }
               }
            }
         } else {
            this.disable();
         }
      }
   }

   private int getSlotForMode() {
      if (mc.player == null) {
         return -1;
      } else {
         Item targetItem = switch ((SelfFill.BlockMode)this.blockMode.getValue()) {
            case ENDER_CHEST -> Items.ENDER_CHEST;
            case WEB -> Items.COBWEB;
            default -> Items.OBSIDIAN;
         };

         for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(targetItem)) {
               return i;
            }
         }

         return -1;
      }
   }

   private boolean placeBlock(BlockPos pos, int slot) {
      if (mc.player != null && mc.world != null) {
         Direction clickSide = Direction.UP;
         BlockPos clickTarget = pos.down();

         for (Direction dir : Direction.values()) {
            BlockPos nb = pos.offset(dir);
            if (!mc.world.getBlockState(nb).isReplaceable()) {
               clickSide = dir.getOpposite();
               clickTarget = nb;
               break;
            }
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
         float[] look = this.calcLookAtPos(Vec3d.ofCenter(pos));
         this.sendLookPacket(look[0], look[1]);
         if (mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit) instanceof Success success && success.swingSource() != SwingSource.NONE) {
            mc.player.swingHand(Hand.MAIN_HAND);
         }

         this.sendLookPacket(preYaw, prePitch);
         if (needSwap) {
            mc.player.getInventory().selectedSlot = original;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(original));
         }

         return true;
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

   public static enum BlockMode {
      OBSIDIAN,
      ENDER_CHEST,
      WEB;
   }
}
