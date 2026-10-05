package me.alpha432.chimeraclient.shoreline;

public final class RotationStep {
   public static RotationStep.Result towards(float var0, float var1, float var2) {
      float var3 = (var1 - var0) % 360.0F;
      if (var3 >= 180.0F) {
         var3 -= 360.0F;
      }

      if (var3 < -180.0F) {
         var3 += 360.0F;
      }

      float var4 = Math.max(1.0F, Math.min(180.0F, var2));
      return Math.abs(var3) <= var4 ? new RotationStep.Result(var0 + var3, true) : new RotationStep.Result(var0 + Math.copySign(var4, var3), false);
   }

   private RotationStep() {
   }

   public record Result(float yaw, boolean ready) {
   }
}
