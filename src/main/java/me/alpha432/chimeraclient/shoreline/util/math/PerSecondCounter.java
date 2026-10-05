package me.alpha432.chimeraclient.shoreline.util.math;

import java.util.LinkedList;

public class PerSecondCounter {
   private final LinkedList<Long> counter = new LinkedList<>();

   public void updateCounter() {
      this.counter.add(System.currentTimeMillis() + 1000L);
   }

   public int getPerSecond() {
      long var1 = System.currentTimeMillis();

      try {
         while (!this.counter.isEmpty() && this.counter.peek() != null && this.counter.peek() < var1) {
            this.counter.remove();
         }
      } catch (Exception var4) {
         this.counter.clear();
         var4.printStackTrace();
      }

      return this.counter.size();
   }
}
