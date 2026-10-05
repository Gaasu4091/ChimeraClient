package me.alpha432.chimeraclient.shoreline.util.player;

import me.alpha432.chimeraclient.shoreline.util.Globals;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RotationUtil implements Globals {
   public static float[] getRotationsTo(Vec3d var0, Vec3d var1) {
      float var2 = (float)(Math.toDegrees(Math.atan2(var1.subtract(var0).z, var1.subtract(var0).x)) - 90.0);
      float var3 = (float)Math.toDegrees(-Math.atan2(var1.subtract(var0).y, Math.hypot(var1.subtract(var0).x, var1.subtract(var0).z)));
      return new float[]{MathHelper.wrapDegrees(var2), MathHelper.wrapDegrees(var3)};
   }

   public static float[] smooth(float[] var0, float[] var1, float var2) {
      float var3 = (1.0F - MathHelper.clamp(var2 / 100.0F, 0.1F, 0.9F)) * 10.0F;
      float[] var4 = new float[]{var1[0] + (float)(-getAngleDifference(var1[0], var0[0]) / var3), var1[1] + -(var1[1] - var0[1]) / var3};
      var4[1] = MathHelper.clamp(var4[1], -90.0F, 90.0F);
      return var4;
   }

   public static double getAngleDifference(float var0, float var1) {
      return ((var0 - var1) % 360.0 + 540.0) % 360.0 - 180.0;
   }

   public static double getAnglePitchDifference(float var0, float var1) {
      return ((var0 - var1) % 180.0 + 270.0) % 180.0 - 90.0;
   }

   public static Vec3d getRotationVector(float var0, float var1) {
      float var2 = var0 * (float) (Math.PI / 180.0);
      float var3 = -var1 * (float) (Math.PI / 180.0);
      float var4 = MathHelper.cos(var3);
      float var5 = MathHelper.sin(var3);
      float var6 = MathHelper.cos(var2);
      float var7 = MathHelper.sin(var2);
      return new Vec3d(var5 * var6, -var7, var4 * var6);
   }
}
