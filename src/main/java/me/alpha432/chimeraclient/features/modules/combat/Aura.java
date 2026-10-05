package me.alpha432.chimeraclient.features.modules.combat;

import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class Aura extends Module {
   private final Setting<Double> range = this.num("Range", 5.0, 1.0, 7.0);
   private final Setting<Double> wallRange = this.num("WallRange", 3.0, 1.0, 7.0);
   private final Setting<Boolean> autoSwap = this.bool("AutoSwap", false);
   private final Setting<Boolean> silent = this.bool("Silent", false);
   private final Setting<Boolean> noSwing = this.bool("NoSwing", false);
   private LivingEntity target = null;

   public Aura() {
      super("Aura", "Attacks entities automatically with perfect charge", Module.Category.COMBAT);
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         this.target = this.findTarget();
         if (this.target != null) {
            boolean holdingSword = mc.player.getMainHandStack().isIn(ItemTags.SWORDS);
            int swordSlot = this.findSwordInHotbar();
            if (!holdingSword) {
               if (this.silent.getValue() && swordSlot != -1) {
                  if (mc.player.getAttackCooldownProgress(0.0F) >= 1.0F) {
                     this.silentAttackTarget(this.target, swordSlot);
                  }

                  return;
               }

               if (this.autoSwap.getValue() && swordSlot != -1) {
                  this.switchToSlot(swordSlot);
                  holdingSword = true;
               }
            }

            if (holdingSword && mc.player.getAttackCooldownProgress(0.0F) >= 1.0F) {
               this.attackTarget(this.target);
            }
         }
      }
   }

   private void attackTarget(LivingEntity entity) {
      mc.interactionManager.attackEntity(mc.player, entity);
      if (!this.noSwing.getValue()) {
         mc.player.swingHand(Hand.MAIN_HAND);
      }
   }

   private void silentAttackTarget(LivingEntity entity, int swordSlot) {
      int oldSlot = mc.player.getInventory().selectedSlot;
      mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(swordSlot));
      mc.interactionManager.attackEntity(mc.player, entity);
      if (!this.noSwing.getValue()) {
         mc.player.swingHand(Hand.MAIN_HAND);
      }

      mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
   }

   private LivingEntity findTarget() {
      double maxDist = this.range.getValue();
      double maxWallDist = this.wallRange.getValue();
      LivingEntity best = null;
      double bestDist = Double.MAX_VALUE;

      for (PlayerEntity p : mc.world.getPlayers()) {
         if (p != mc.player && !p.isDead() && !ChimeraClient.friendManager.isFriend(p)) {
            double d = this.distance(mc.player.getEntityPos(), p.getEntityPos());
            boolean canSee = this.hasLineOfSight(p);
            if ((canSee || !(d > maxWallDist)) && (!canSee || !(d > maxDist)) && d < bestDist) {
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

   private int findSwordInHotbar() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).isIn(ItemTags.SWORDS)) {
            return i;
         }
      }

      return -1;
   }

   private double distance(Vec3d a, Vec3d b) {
      double dx = Math.abs(a.x - b.x);
      double dy = Math.abs(a.y - b.y);
      double dz = Math.abs(a.z - b.z);
      return Math.max(dx, Math.max(dy, dz));
   }

   private boolean hasLineOfSight(LivingEntity entity) {
      Vec3d eyes = mc.player.getEyePos();
      Vec3d targetPos = entity.getEyePos();
      return mc.world.raycast(new RaycastContext(eyes, targetPos, ShapeType.COLLIDER, FluidHandling.NONE, mc.player)).getType() == Type.MISS;
   }

   @Override
   public String getDisplayInfo() {
      return this.target == null ? "§7No Target" : "§a" + this.target.getName().getString();
   }
}
