package me.alpha432.chimeraclient.shoreline.util.render.animation;

public class Animation {
   private final Easing easing;
   private float length;
   private long last = 0L;
   private boolean state;

   public Animation(float var1) {
      this(false, var1);
   }

   public Animation(boolean var1, float var2) {
      this(var1, var2, Easing.LINEAR);
   }

   public Animation(boolean var1, float var2, Easing var3) {
      this.length = var2;
      this.state = var1;
      this.easing = var3;
   }

   public void setState(boolean var1) {
      this.last = (long)(
         !var1 ? System.currentTimeMillis() - (1.0 - this.getFactor()) * this.length : System.currentTimeMillis() - this.getFactor() * this.length
      );
      this.state = var1;
   }

   public boolean getState() {
      return this.state;
   }

   public double getFactor() {
      return this.easing.ease(this.getLinearFactor());
   }

   public double getLinearFactor() {
      return this.state
         ? this.clamp((float)(System.currentTimeMillis() - this.last) / this.length)
         : this.clamp(1.0F - (float)(System.currentTimeMillis() - this.last) / this.length);
   }

   public double getCurrent() {
      return 1.0 + 1.0 * this.getFactor();
   }

   private double clamp(double var1) {
      return var1 < 0.0 ? 0.0 : Math.min(var1, 1.0);
   }

   public double getLength() {
      return this.length;
   }

   public void setLength(float var1) {
      this.length = var1;
   }

   public boolean isFinished() {
      return !this.getState() && this.getFactor() == 0.0 || this.getState() && this.getFactor() == 1.0;
   }

   public void reset() {
      this.last = System.currentTimeMillis();
   }
}
