package me.alpha432.chimeraclient.shoreline.rotation;

public class Rotation {
   private final int priority;
   private float yaw;
   private float pitch;
   private boolean snap;

   public Rotation(int var1, float var2, float var3, boolean var4) {
      this.priority = var1;
      this.yaw = var2;
      this.pitch = var3;
      this.snap = var4;
   }

   public Rotation(int var1, float var2, float var3) {
      this(var1, var2, var3, false);
   }

   public int getPriority() {
      return this.priority;
   }

   public void setYaw(float var1) {
      this.yaw = var1;
   }

   public void setPitch(float var1) {
      this.pitch = var1;
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setSnap(boolean var1) {
      this.snap = var1;
   }

   public boolean isSnap() {
      return this.snap;
   }
}
