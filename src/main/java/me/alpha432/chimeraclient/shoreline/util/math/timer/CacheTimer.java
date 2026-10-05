package me.alpha432.chimeraclient.shoreline.util.math.timer;

import java.util.concurrent.TimeUnit;

public class CacheTimer implements Timer {
   private long time = System.nanoTime();
   private long lastResetTime;

   @Override
   public boolean passed(Number var1) {
      return var1.longValue() <= 0L ? true : this.getElapsedTime() > var1.longValue();
   }

   public boolean passed(Number var1, TimeUnit var2) {
      return this.passed(var2.toMillis(var1.longValue()));
   }

   @Override
   public long getElapsedTime() {
      return this.toMillis(System.nanoTime() - this.time);
   }

   @Override
   public void setElapsedTime(Number var1) {
      this.time = var1.longValue() == -255L ? 0L : System.nanoTime() - var1.longValue();
   }

   public void setDelay(Number var1) {
      this.time = this.time + var1.longValue();
   }

   public long getElapsedTime(TimeUnit var1) {
      return var1.convert(this.getElapsedTime(), TimeUnit.MILLISECONDS);
   }

   public long getLastResetTime() {
      return this.lastResetTime;
   }

   @Override
   public void reset() {
      long var1 = System.nanoTime();
      this.lastResetTime = var1 - this.time;
      this.time = var1;
   }

   private long toMillis(long var1) {
      return var1 / 1000000L;
   }
}
