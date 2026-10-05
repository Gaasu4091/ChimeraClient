package me.alpha432.chimeraclient.shoreline.piston;

public final class PistonCycle {
   private PistonCycle.Phase phase = PistonCycle.Phase.BASE;
   private long enteredAt = -1L;
   private long lastSentAt = -1L;
   private long lastAttackAt = -1L;
   private int attempts;
   private boolean attacked;
   private int failedStrokes;

   public PistonCycle.Phase phase() {
      return this.phase;
   }

   public static float miningCredit(float var0, float var1) {
      if (Float.isFinite(var0) && !(var0 <= 0.0F)) {
         if (!Float.isFinite(var1) || var1 <= 0.0F) {
            var1 = 20.0F;
         }

         return var0 * Math.min(20.0F, var1) / 20.0F;
      } else {
         return 0.0F;
      }
   }

   public boolean attacked() {
      return this.attacked;
   }

   public boolean finishing(boolean var1) {
      return this.attacked
         || this.phase == PistonCycle.Phase.PUSH
         || this.phase == PistonCycle.Phase.CLEANUP
         || this.phase == PistonCycle.Phase.RETRACT
         || this.phase == PistonCycle.Phase.CONFIRM_BREAK
         || this.phase == PistonCycle.Phase.POWER && var1;
   }

   public void reset() {
      this.phase = PistonCycle.Phase.BASE;
      this.enteredAt = this.lastSentAt = this.lastAttackAt = -1L;
      this.attempts = this.failedStrokes = 0;
      this.attacked = false;
   }

   private void enter(PistonCycle.Phase var1, long var2) {
      this.phase = var1;
      this.enteredAt = var2;
      this.lastSentAt = -1L;
      this.attempts = 0;
   }

   private void request(PistonCycle.Action var1, long var2, long var4, PistonCycle.Environment var6) {
      if (this.lastSentAt < 0L || var2 - this.lastSentAt >= var4) {
         if (var6.perform(var1)) {
            this.lastSentAt = var2;
            this.attempts++;
         }
      }
   }

   private boolean timedOut(long var1, long var3) {
      return this.attempts >= 3 && this.lastSentAt >= 0L && var1 - this.lastSentAt >= var3;
   }

   private void attack(long var1, long var3, PistonCycle.Environment var5) {
      if (this.lastAttackAt < 0L || var1 - this.lastAttackAt >= var3) {
         if (var5.perform(PistonCycle.Action.ATTACK)) {
            this.attacked = true;
            this.lastAttackAt = var1;
         }
      }
   }

   public PistonCycle.Result tick(long var1, long var3, long var5, PistonCycle.Environment var7) {
      var3 = Math.max(50L, var3);
      var5 = Math.max(var3, var5);
      if (this.enteredAt < 0L) {
         this.enteredAt = var1;
      }

      for (int var8 = 0; var8 < 16; var8++) {
         PistonCycle.View var9 = var7.inspect();

         boolean var10 = switch (this.phase) {
            case BASE -> var9.base;
            default -> true;
            case CRYSTAL_FIRST, CRYSTAL -> var9.crystal;
            case PISTON -> var9.piston;
            case POWER -> var9.powered;
         };
         if (!var10 && (this.timedOut(var1, var5) || this.lastSentAt < 0L && var1 - this.enteredAt >= var5 * 4L)) {
            this.enter(PistonCycle.Phase.FAILED, var1);
            return PistonCycle.Result.FAILED;
         }

         switch (this.phase) {
            case BASE:
               if (!var9.base) {
                  this.request(PistonCycle.Action.BASE, var1, var5, var7);
                  if (this.timedOut(var1, var5)) {
                     this.enter(PistonCycle.Phase.FAILED, var1);
                  }

                  return this.phase == PistonCycle.Phase.FAILED ? PistonCycle.Result.FAILED : PistonCycle.Result.WAITING;
               }

               this.enter(PistonCycle.Phase.PREPARE, var1);
               break;
            case PREPARE:
               if (var9.powered && !var9.piston && var9.crystalFirst) {
                  this.enter(PistonCycle.Phase.CRYSTAL_FIRST, var1);
               } else if (!var9.powered && var9.retracted) {
                  this.enter(PistonCycle.Phase.PISTON, var1);
               } else {
                  this.enter(PistonCycle.Phase.DEPOWER, var1);
               }
               break;
            case DEPOWER:
               if (!var9.powered && var9.retracted) {
                  this.enter(PistonCycle.Phase.PISTON, var1);
                  break;
               }

               if (var9.powered) {
                  var7.perform(PistonCycle.Action.MINE_POWER);
               } else if (var1 - this.enteredAt >= var5 * 4L) {
                  this.enter(PistonCycle.Phase.FAILED, var1);
                  return PistonCycle.Result.FAILED;
               }

               return PistonCycle.Result.WAITING;
            case CRYSTAL_FIRST:
            case CRYSTAL:
               if (var9.piston && var9.powered && !var9.crystal) {
                  this.enter(PistonCycle.Phase.DEPOWER, var1);
               } else {
                  if (!var9.crystal) {
                     this.request(PistonCycle.Action.CRYSTAL, var1, var5, var7);
                     if (this.timedOut(var1, var5)) {
                        this.enter(PistonCycle.Phase.FAILED, var1);
                     }

                     return this.phase == PistonCycle.Phase.FAILED ? PistonCycle.Result.FAILED : PistonCycle.Result.WAITING;
                  }

                  this.enter(this.phase == PistonCycle.Phase.CRYSTAL_FIRST ? PistonCycle.Phase.PISTON : PistonCycle.Phase.POWER, var1);
               }
               break;
            case PISTON:
               if (var9.powered && !var9.crystal) {
                  this.enter(!var9.piston && var9.crystalFirst ? PistonCycle.Phase.CRYSTAL_FIRST : PistonCycle.Phase.DEPOWER, var1);
               } else if (var9.piston && !var9.retracted && !var9.crystal) {
                  this.enter(PistonCycle.Phase.DEPOWER, var1);
               } else {
                  if (!var9.piston) {
                     this.request(PistonCycle.Action.PISTON, var1, var5, var7);
                     if (this.timedOut(var1, var5)) {
                        this.enter(PistonCycle.Phase.FAILED, var1);
                     }

                     return this.phase == PistonCycle.Phase.FAILED ? PistonCycle.Result.FAILED : PistonCycle.Result.WAITING;
                  }

                  this.enter(var9.crystal ? (var9.powered ? PistonCycle.Phase.PUSH : PistonCycle.Phase.POWER) : PistonCycle.Phase.CRYSTAL, var1);
               }
               break;
            case POWER:
               if (!var9.piston) {
                  this.enter(PistonCycle.Phase.PISTON, var1);
               } else if (!var9.crystal) {
                  this.enter(PistonCycle.Phase.CRYSTAL, var1);
               } else {
                  if (!var9.powered) {
                     this.request(PistonCycle.Action.POWER, var1, var5, var7);
                     if (this.timedOut(var1, var5)) {
                        this.enter(PistonCycle.Phase.FAILED, var1);
                     }

                     return this.phase == PistonCycle.Phase.FAILED ? PistonCycle.Result.FAILED : PistonCycle.Result.WAITING;
                  }

                  this.enter(PistonCycle.Phase.PUSH, var1);
               }
               break;
            case PUSH:
               if (!var9.crystal) {
                  this.enter(var9.cleanup ? PistonCycle.Phase.CLEANUP : PistonCycle.Phase.CONFIRM_BREAK, var1);
               } else {
                  if (var9.pushed) {
                     this.attack(var1, var3, var7);
                     if (this.attacked) {
                        this.enter(var9.cleanup ? PistonCycle.Phase.CLEANUP : PistonCycle.Phase.CONFIRM_BREAK, var1);
                        continue;
                     }
                  }

                  if (var1 - this.enteredAt < var5 || var9.pushed) {
                     return PistonCycle.Result.WAITING;
                  }

                  if (++this.failedStrokes >= 3) {
                     this.enter(PistonCycle.Phase.FAILED, var1);
                     return PistonCycle.Result.FAILED;
                  }

                  this.enter(PistonCycle.Phase.DEPOWER, var1);
               }
               break;
            case CLEANUP:
               if (var9.crystal && var9.pushed) {
                  this.attack(var1, var3, var7);
               }

               if (var9.powered) {
                  var7.perform(PistonCycle.Action.MINE_POWER);
                  return PistonCycle.Result.WAITING;
               }

               this.enter(PistonCycle.Phase.RETRACT, var1);
               break;
            case RETRACT:
               if (var9.crystal && var9.pushed) {
                  this.attack(var1, var3, var7);
               }

               if (var9.powered) {
                  this.enter(PistonCycle.Phase.CLEANUP, var1);
               } else {
                  if (!var9.retracted) {
                     if (var1 - this.enteredAt >= var5 * 4L) {
                        this.enter(PistonCycle.Phase.FAILED, var1);
                        return PistonCycle.Result.FAILED;
                     }

                     return PistonCycle.Result.WAITING;
                  }

                  this.enter(PistonCycle.Phase.CONFIRM_BREAK, var1);
               }
               break;
            case CONFIRM_BREAK:
               if (var9.crystal) {
                  if (var9.pushed) {
                     this.attack(var1, var3, var7);
                  } else {
                     this.enter(PistonCycle.Phase.DEPOWER, var1);
                  }

                  return PistonCycle.Result.WAITING;
               }

               return PistonCycle.Result.COMPLETE;
            case FAILED:
               return PistonCycle.Result.FAILED;
         }
      }

      return PistonCycle.Result.WAITING;
   }

   public static enum Action {
      BASE,
      PISTON,
      CRYSTAL,
      POWER,
      ATTACK,
      MINE_POWER;
   }

   public interface Environment {
      PistonCycle.View inspect();

      boolean perform(PistonCycle.Action var1);
   }

   public static enum Phase {
      BASE,
      PREPARE,
      DEPOWER,
      CRYSTAL_FIRST,
      PISTON,
      CRYSTAL,
      POWER,
      PUSH,
      CLEANUP,
      RETRACT,
      CONFIRM_BREAK,
      FAILED;
   }

   public static enum Result {
      WAITING,
      COMPLETE,
      FAILED;
   }

   public record View(boolean base, boolean piston, boolean powered, boolean retracted, boolean crystal, boolean pushed, boolean crystalFirst, boolean cleanup) {
   }
}
