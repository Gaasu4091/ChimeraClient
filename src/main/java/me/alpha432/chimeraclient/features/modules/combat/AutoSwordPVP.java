package me.alpha432.chimeraclient.features.modules.combat;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AutoSwordPVP extends Module {
   private final Setting<Float> targetRange = this.register(new Setting<>("TargetRange", 50.0F, 1.0F, 100.0F));
   private final Setting<Float> attackRange = this.register(new Setting<>("AttackRange", 3.0F, 0.1F, 6.0F));
   private final Setting<Float> keepDistance = this.register(new Setting<>("KeepDistance", 2.8F, 0.1F, 6.0F));
   private final Setting<Boolean> autoCrit = this.bool("AutoCrit", true);
   private final Setting<Boolean> shiftCrit = this.bool("ShiftCrit", true);
   private final Setting<Boolean> sprintReset = this.bool("SprintReset", true);
   private final Setting<Boolean> sTap = this.bool("STap", true);
   private final Setting<Boolean> kbHoldS = this.bool("KBHoldS", true);
   private final Setting<Boolean> circleStrafe = this.bool("CircleStrafe", true);
   private final Setting<Boolean> jumpReset = this.bool("JumpReset", true);
   private final Setting<Boolean> counterStrafe = this.bool("CounterStrafe", true);
   private final Setting<Boolean> uppercut = this.bool("Uppercut", true);
   private final Setting<Boolean> jookyMovement = this.bool("JookyMovement", true);
   private final Setting<Boolean> predictAim = this.bool("PredictAim", true);
   private PlayerEntity currentTarget = null;
   private int strafeDirection = 1;
   private int strafeTick = 0;
   private int sprintResetTick = 0;
   private int sTapTick = 0;
   private boolean isHoldingSForKB = false;
   private boolean jumpResetArmed = false;
   private int jumpResetCooldown = 0;
   private boolean isAttemptingCrit = false;
   private boolean isCritInPlace = false;
   private double prevTargetDistance = 0.0;
   private int ticksSinceLastAttack = 999;
   private boolean uppercutArmed = false;
   private int uppercutCooldown = 0;
   private final Random random = new Random();
   private int jookyPhaseTick = 0;
   private int jookyDirection = 1;
   private Vec3d prevTargetVelocity = Vec3d.ZERO;
   private final LinkedList<AutoSwordPVP.MoveDir> moveHistory = new LinkedList<>();
   private static final int HISTORY_SIZE = 60;
   private AutoSwordPVP.MoveDir lastOpponentStrafe = AutoSwordPVP.MoveDir.NONE;
   private int prevHurtTime = 0;
   private boolean hasCounterAttackedThisHit = false;
   private boolean isCountering = false;
   private int counterTick = 0;

   public AutoSwordPVP() {
      super("AutoSwordPVP", "1.9+ SwordPvP bot: CircleStrafe/JumpReset/Uppercut/Jooky", Module.Category.COMBAT);
   }

   @Override
   public void onDisable() {
      this.resetInputs();
      this.currentTarget = null;
      this.moveHistory.clear();
      this.prevTargetVelocity = Vec3d.ZERO;
      this.sprintResetTick = 0;
      this.sTapTick = 0;
      this.isHoldingSForKB = false;
      this.jumpResetArmed = false;
      this.jumpResetCooldown = 0;
      this.prevTargetDistance = 0.0;
      this.ticksSinceLastAttack = 999;
      this.isAttemptingCrit = false;
      this.isCritInPlace = false;
      this.uppercutArmed = false;
      this.uppercutCooldown = 0;
      this.jookyPhaseTick = 0;
      this.prevHurtTime = 0;
      this.hasCounterAttackedThisHit = false;
      this.isCountering = false;
      this.counterTick = 0;
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         this.ticksSinceLastAttack++;
         if (this.jumpResetCooldown > 0) {
            this.jumpResetCooldown--;
         }

         if (this.uppercutCooldown > 0) {
            this.uppercutCooldown--;
         }

         this.findTarget();
         if (this.currentTarget == null) {
            this.resetInputs();
            this.moveHistory.clear();
            this.prevTargetVelocity = Vec3d.ZERO;
            this.prevTargetDistance = 0.0;
            this.isAttemptingCrit = false;
            this.isCritInPlace = false;
            this.sTapTick = 0;
            this.isHoldingSForKB = false;
            this.jumpResetArmed = false;
            this.uppercutArmed = false;
            this.prevHurtTime = 0;
            this.hasCounterAttackedThisHit = false;
            this.isCountering = false;
            this.counterTick = 0;
         } else {
            this.updateTargetHistory();
            if (!mc.player.canSee(this.currentTarget)) {
               this.resetInputs();
               this.prevHurtTime = mc.player.hurtTime;
            } else {
               double distance = mc.player.distanceTo(this.currentTarget);
               boolean inReach = distance <= this.attackRange.getValue().floatValue();
               boolean justHit = mc.player.hurtTime > this.prevHurtTime && mc.player.hurtTime == 10;
               this.aimAtTarget();
               this.handleMovement(distance, inReach, justHit);
               if (this.counterTick > 0) {
                  this.counterTick--;
                  if (this.counterTick == 0) {
                     this.isCountering = false;
                  }
               }

               if (justHit && inReach && !this.hasCounterAttackedThisHit) {
                  mc.interactionManager.attackEntity(mc.player, this.currentTarget);
                  mc.player.swingHand(Hand.MAIN_HAND);
                  this.hasCounterAttackedThisHit = true;
                  this.isCountering = true;
                  this.counterTick = 3;
                  this.ticksSinceLastAttack = 0;
               }

               if (!justHit && mc.player.hurtTime == 0) {
                  this.hasCounterAttackedThisHit = false;
               }

               if (this.isCountering) {
                  if (this.kbHoldS.getValue() && mc.player.hurtTime > 0 && !mc.player.isOnGround()) {
                     this.setKey(mc.options.forwardKey, false);
                     this.setKey(mc.options.backKey, true);
                     this.setKey(mc.options.leftKey, false);
                     this.setKey(mc.options.rightKey, false);
                     this.setKey(mc.options.jumpKey, false);
                     this.setKey(mc.options.sneakKey, false);
                     mc.player.setSprinting(false);
                  }

                  this.prevHurtTime = mc.player.hurtTime;
               } else {
                  this.handleAttack(inReach, distance);
                  this.prevHurtTime = mc.player.hurtTime;
               }
            }
         }
      }
   }

   private void findTarget() {
      if (this.currentTarget != null && (!this.currentTarget.isAlive() || mc.player.distanceTo(this.currentTarget) > this.targetRange.getValue())) {
         this.currentTarget = null;
      }

      if (this.currentTarget == null) {
         this.currentTarget = mc.world
            .getPlayers()
            .stream()
            .filter(p -> p != mc.player && p.isAlive() && mc.player.distanceTo(p) <= this.targetRange.getValue() && !ChimeraClient.friendManager.isFriend(p))
            .min(Comparator.comparingDouble(p -> {
               double score = mc.player.distanceTo(p);
               if (!mc.player.canSee(p)) {
                  score += 15.0;
               }

               Vec3d look = p.getRotationVector();
               Vec3d toMe = mc.player.getEntityPos().subtract(p.getEntityPos()).normalize();
               if (look.dotProduct(toMe) > 0.8) {
                  score -= 3.0;
               }

               return score;
            }))
            .orElse(null);
      }
   }

   private void updateTargetHistory() {
      AutoSwordPVP.MoveDir dir = this.getCurrentMoveDir(this.currentTarget);
      this.moveHistory.addLast(dir);
      if (this.moveHistory.size() > 60) {
         this.moveHistory.removeFirst();
      }
   }

   private AutoSwordPVP.MoveDir getCurrentMoveDir(PlayerEntity target) {
      Vec3d vel = target.getVelocity();
      if (vel.lengthSquared() < 1.0E-4) {
         return AutoSwordPVP.MoveDir.NONE;
      } else {
         double dx = target.getX() - mc.player.getX();
         double dz = target.getZ() - mc.player.getZ();
         double d = Math.sqrt(dx * dx + dz * dz);
         if (d == 0.0) {
            return AutoSwordPVP.MoveDir.NONE;
         } else {
            double fX = dx / d;
            double fZ = dz / d;
            double rX = -fZ;
            double dotRight = vel.x * rX + vel.z * fX;
            double dotFwd = vel.x * fX + vel.z * fZ;
            if (Math.abs(dotRight) > Math.abs(dotFwd)) {
               return dotRight > 0.0 ? AutoSwordPVP.MoveDir.RIGHT : AutoSwordPVP.MoveDir.LEFT;
            } else {
               return AutoSwordPVP.MoveDir.STRAIGHT;
            }
         }
      }
   }

   private AutoSwordPVP.MoveDir getMostFrequentMove(int lookBack) {
      if (this.moveHistory.isEmpty()) {
         return AutoSwordPVP.MoveDir.NONE;
      } else {
         int start = Math.max(0, this.moveHistory.size() - lookBack);
         List<AutoSwordPVP.MoveDir> sub = this.moveHistory.subList(start, this.moveHistory.size());
         int l = 0;
         int r = 0;
         int s = 0;

         for (AutoSwordPVP.MoveDir d : sub) {
            if (d == AutoSwordPVP.MoveDir.LEFT) {
               l++;
            } else if (d == AutoSwordPVP.MoveDir.RIGHT) {
               r++;
            } else if (d == AutoSwordPVP.MoveDir.STRAIGHT) {
               s++;
            }
         }

         int max = Math.max(l, Math.max(r, s));
         if (max == 0) {
            return AutoSwordPVP.MoveDir.NONE;
         } else if (max == l) {
            return AutoSwordPVP.MoveDir.LEFT;
         } else {
            return max == r ? AutoSwordPVP.MoveDir.RIGHT : AutoSwordPVP.MoveDir.STRAIGHT;
         }
      }
   }

   private void aimAtTarget() {
      Vec3d targetPos = this.currentTarget.getEntityPos().add(0.0, this.currentTarget.getStandingEyeHeight() / 2.0, 0.0);
      if (this.predictAim.getValue()) {
         Vec3d vel = this.currentTarget.getVelocity();
         Vec3d accel = vel.subtract(this.prevTargetVelocity);
         this.prevTargetVelocity = vel;
         double dist = mc.player.distanceTo(this.currentTarget);
         double predictTicks = Math.min(dist * 0.5, 3.0);
         AutoSwordPVP.MoveDir shortTerm = this.getMostFrequentMove(20);
         AutoSwordPVP.MoveDir midTerm = this.getMostFrequentMove(40);
         AutoSwordPVP.MoveDir longTerm = this.getMostFrequentMove(60);
         AutoSwordPVP.MoveDir actualMove = this.getCurrentMoveDir(this.currentTarget);
         AutoSwordPVP.MoveDir predicted = shortTerm == midTerm ? shortTerm : longTerm;
         if (predicted == actualMove && (predicted == AutoSwordPVP.MoveDir.LEFT || predicted == AutoSwordPVP.MoveDir.RIGHT)) {
            predictTicks *= 1.5;
         }

         double predX = vel.x * predictTicks + 0.5 * accel.x * predictTicks * predictTicks;
         double predZ = vel.z * predictTicks + 0.5 * accel.z * predictTicks * predictTicks;
         targetPos = targetPos.add(predX, 0.0, predZ);
      }

      float[] rot = this.calcRotations(targetPos);
      mc.player.setYaw(rot[0]);
      mc.player.setPitch(rot[1]);
   }

   private void handleMovement(double distance, boolean inReach, boolean justHit) {
      boolean forward = false;
      boolean back = false;
      boolean left = false;
      boolean right = false;
      boolean jump = false;
      boolean sprint = true;
      boolean shift = false;
      if (this.kbHoldS.getValue()) {
         if (mc.player.hurtTime > 0 && !mc.player.isOnGround()) {
            this.isHoldingSForKB = true;
         }

         if (mc.player.isOnGround()) {
            this.isHoldingSForKB = false;
         }
      } else {
         this.isHoldingSForKB = false;
      }

      if (this.jumpReset.getValue() && justHit && this.jumpResetCooldown == 0) {
         this.jumpResetArmed = true;
         this.jumpResetCooldown = 15;
      }

      if (this.jumpResetArmed && mc.player.isOnGround()) {
         jump = true;
         this.jumpResetArmed = false;
      }

      if (this.uppercut.getValue() && this.uppercutCooldown == 0) {
         Vec3d dir2t = this.currentTarget.getEntityPos().subtract(mc.player.getEntityPos()).normalize();
         double approachRate = this.currentTarget.getVelocity().dotProduct(dir2t);
         boolean targetFleeing = approachRate < -0.1;
         boolean nearEdge = distance > this.attackRange.getValue().floatValue() - 0.5 && distance < this.attackRange.getValue().floatValue() + 1.5;
         if (targetFleeing && nearEdge) {
            this.uppercutArmed = true;
         }
      }

      double dynamicKeep = this.keepDistance.getValue().floatValue();
      Vec3d toTarget = this.currentTarget.getEntityPos().subtract(mc.player.getEntityPos()).normalize();
      double approachRate = this.currentTarget.getVelocity().dotProduct(toTarget);
      if (approachRate > 0.1) {
         dynamicKeep = Math.max(1.5, dynamicKeep - 0.5);
      } else if (approachRate < -0.1) {
         dynamicKeep = Math.min(4.0, dynamicKeep + 0.3);
      }

      if (distance > dynamicKeep) {
         forward = true;
      } else if (distance < dynamicKeep - 0.5) {
         back = true;
         sprint = false;
      }

      if (this.uppercutArmed && mc.player.isOnGround()) {
         jump = true;
         forward = true;
         sprint = true;
         this.uppercutArmed = false;
         this.uppercutCooldown = 20;
      }

      if (this.circleStrafe.getValue()) {
         this.strafeTick++;
         int switchInterval = 20;
         if (this.counterStrafe.getValue()) {
            AutoSwordPVP.MoveDir opponentDir = this.getCurrentMoveDir(this.currentTarget);
            if (opponentDir != AutoSwordPVP.MoveDir.NONE && opponentDir == this.lastOpponentStrafe) {
               switchInterval = 8;
            }

            this.lastOpponentStrafe = opponentDir;
         }

         if (this.strafeTick >= switchInterval) {
            this.strafeDirection = -this.strafeDirection;
            this.strafeTick = 0;
         }

         if (!back) {
            if (this.strafeDirection == 1) {
               left = true;
            } else {
               right = true;
            }
         }
      }

      if (this.jookyMovement.getValue() && inReach) {
         this.jookyPhaseTick--;
         if (this.jookyPhaseTick <= 0) {
            this.jookyDirection = this.random.nextBoolean() ? 1 : -1;
            this.jookyPhaseTick = 1 + this.random.nextInt(4);
         }

         left = this.jookyDirection == 1;
         right = this.jookyDirection == -1;
      }

      if (mc.player.isOnGround()) {
         this.isAttemptingCrit = false;
         this.isCritInPlace = false;
      }

      if (this.autoCrit.getValue() && mc.player.isOnGround()) {
         double sweetMin = 2.5;
         double sweetMax = this.attackRange.getValue().floatValue();
         boolean currentlyClose = distance <= sweetMin;
         boolean currentlySweet = distance > sweetMin && distance <= sweetMax;
         boolean previouslyClose = this.prevTargetDistance <= sweetMin;
         if (currentlyClose) {
            this.isAttemptingCrit = true;
            this.isCritInPlace = false;
         } else if (currentlySweet && previouslyClose && distance > this.prevTargetDistance && mc.player.hurtTime == 0 && this.ticksSinceLastAttack < 20) {
            this.isAttemptingCrit = true;
            this.isCritInPlace = true;
         }

         if (this.isAttemptingCrit) {
            jump = true;
         }
      }

      if (this.autoCrit.getValue() && this.isAttemptingCrit && this.shiftCrit.getValue() && !mc.player.isOnGround()) {
         shift = true;
      }

      if (this.isAttemptingCrit && this.isCritInPlace) {
         forward = false;
         back = false;
         left = false;
         right = false;
      }

      this.prevTargetDistance = distance;
      if (this.sTapTick > 0) {
         forward = false;
         back = true;
         sprint = false;
         this.sTapTick--;
      }

      if (this.sprintResetTick > 0) {
         forward = false;
         sprint = false;
         this.sprintResetTick--;
      }

      if (this.isHoldingSForKB) {
         forward = false;
         back = true;
      }

      this.setKey(mc.options.forwardKey, forward);
      this.setKey(mc.options.backKey, back);
      this.setKey(mc.options.leftKey, left);
      this.setKey(mc.options.rightKey, right);
      this.setKey(mc.options.jumpKey, jump);
      this.setKey(mc.options.sneakKey, shift);
      mc.player.setSprinting(sprint && forward);
   }

   private void handleAttack(boolean inReach, double distance) {
      if (inReach) {
         float requiredCharge = 0.95F;
         if (this.autoCrit.getValue() && this.isAttemptingCrit) {
            requiredCharge = 1.0F;
         } else if (this.currentTarget.getHealth() <= 4.0F) {
            requiredCharge = 0.8F;
         } else if (distance <= 2.0) {
            requiredCharge = 0.85F;
         }

         if (!(mc.player.getAttackCooldownProgress(0.0F) < requiredCharge)) {
            boolean isFalling = mc.player.fallDistance > 0.0 && !mc.player.isOnGround();
            boolean shouldAttack = true;
            if (this.autoCrit.getValue() && this.isAttemptingCrit && !isFalling && !mc.player.isTouchingWater()) {
               shouldAttack = false;
            }

            if (shouldAttack) {
               mc.interactionManager.attackEntity(mc.player, this.currentTarget);
               mc.player.swingHand(Hand.MAIN_HAND);
               this.ticksSinceLastAttack = 0;
               if (this.sprintReset.getValue()) {
                  this.sprintResetTick = 2;
               }

               if (this.sTap.getValue()) {
                  this.sTapTick = 2;
               }

               this.isAttemptingCrit = false;
               this.isCritInPlace = false;
            }
         }
      }
   }

   private void resetInputs() {
      if (mc.options != null) {
         this.setKey(mc.options.forwardKey, false);
         this.setKey(mc.options.backKey, false);
         this.setKey(mc.options.leftKey, false);
         this.setKey(mc.options.rightKey, false);
         this.setKey(mc.options.jumpKey, false);
         this.setKey(mc.options.sneakKey, false);
         mc.player.setSprinting(false);
      }
   }

   private void setKey(KeyBinding key, boolean pressed) {
      key.setPressed(pressed);
   }

   private float[] calcRotations(Vec3d target) {
      Vec3d eyes = mc.player.getEyePos();
      double dx = target.x - eyes.x;
      double dy = target.y - eyes.y;
      double dz = target.z - eyes.z;
      double dist = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)(MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
      float pitch = (float)(-(MathHelper.atan2(dy, dist) * (180.0 / Math.PI)));
      return new float[]{yaw, pitch};
   }

   private static enum MoveDir {
      LEFT,
      RIGHT,
      STRAIGHT,
      NONE;
   }
}
