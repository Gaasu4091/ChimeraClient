package me.alpha432.chimeraclient.features.modules.combat;

import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class AutoMace extends Module {
   private final Setting<Double> range = this.num("Range", 5.0, 1.0, 7.0);
   private final Setting<Double> height = this.num("Height", 3.0, 1.0, 10.0);
   private final Setting<Boolean> autoSwap = this.bool("AutoSwap", false);
   private final Setting<Boolean> silent = this.bool("Silent", false);
   private final Setting<Boolean> noSwing = this.bool("NoSwing", false);
   private LivingEntity target = null;

   public AutoMace() {
      super("AutoMace", "Teleports up and drops down to attack with a mace", Module.Category.COMBAT);
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         this.target = this.findTarget();
         if (this.target != null) {
            boolean holdingMace = mc.player.getMainHandStack().isOf(Items.MACE);
            int maceSlot = this.findMaceInHotbar();
            if (!holdingMace) {
               if (this.silent.getValue() && maceSlot != -1) {
                  this.executeMaceCombo(this.target, maceSlot, true);
                  return;
               }

               if (this.autoSwap.getValue() && maceSlot != -1) {
                  this.switchToSlot(maceSlot);
                  holdingMace = true;
               }
            }

            if (holdingMace) {
               this.executeMaceCombo(this.target, mc.player.getInventory().selectedSlot, false);
            }
         }
      }
   }

   private void executeMaceCombo(LivingEntity entity, int maceSlot, boolean isSilent) {
      Vec3d startPos = mc.player.getEntityPos();
      Vec3d targetUpPos = startPos.add(0.0, this.height.getValue(), 0.0);
      HitResult result = mc.world.raycast(new RaycastContext(startPos, targetUpPos, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
      if (result.getType() != Type.MISS) {
         BlockPos hit = BlockPos.ofFloored(result.getPos());
         BlockPos current = hit;

         while (mc.world != null && !mc.world.getBlockState(current).isReplaceable()) {
            current = current.up();
            if (current.getY() >= mc.world.getTopYInclusive()) {
               break;
            }
         }

         targetUpPos = new Vec3d(startPos.x, current.getY(), startPos.z);
      }

      this.updatePlayerPosition(targetUpPos);
      Vec3d dropPos = new Vec3d(startPos.x, startPos.y + 0.1, startPos.z);
      this.updatePlayerPosition(dropPos);
      if (isSilent) {
         this.silentAttackTarget(entity, maceSlot);
      } else {
         this.attackTarget(entity);
      }
   }

   private void updatePlayerPosition(Vec3d pos) {
      mc.player.setPosition(pos.x, pos.y, pos.z);
      mc.getNetworkHandler().sendPacket(new PositionAndOnGround(pos.x, pos.y, pos.z, false, false));
   }

   private void attackTarget(LivingEntity entity) {
      mc.interactionManager.attackEntity(mc.player, entity);
      if (!this.noSwing.getValue()) {
         mc.player.swingHand(Hand.MAIN_HAND);
      }
   }

   private void silentAttackTarget(LivingEntity entity, int maceSlot) {
      int oldSlot = mc.player.getInventory().selectedSlot;
      mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));
      mc.interactionManager.attackEntity(mc.player, entity);
      if (!this.noSwing.getValue()) {
         mc.player.swingHand(Hand.MAIN_HAND);
      }

      mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
   }

   private LivingEntity findTarget() {
      double maxDist = this.range.getValue();
      LivingEntity best = null;
      double bestDist = Double.MAX_VALUE;

      for (PlayerEntity p : mc.world.getPlayers()) {
         if (p != mc.player && !p.isDead() && !ChimeraClient.friendManager.isFriend(p)) {
            double d = mc.player.distanceTo(p);
            if (!(d > maxDist) && d < bestDist) {
               bestDist = d;
               best = p;
            }
         }
      }

      return best;
   }

   private void switchToSlot(int slot) {
      if (mc.player.getInventory().selectedSlot != slot) {
         mc.player.getInventory().selectedSlot = slot;
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      }
   }

   private int findMaceInHotbar() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).isOf(Items.MACE)) {
            return i;
         }
      }

      return -1;
   }

   @Override
   public String getDisplayInfo() {
      return this.target == null ? "§7No Target" : "§a" + this.target.getName().getString();
   }
}
