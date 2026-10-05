package me.alpha432.chimeraclient.shoreline.util.collection;

import java.util.concurrent.ConcurrentLinkedDeque;
import org.jetbrains.annotations.NotNull;

public class EvictingQueue<E> extends ConcurrentLinkedDeque<E> {
   private final int limit;

   public EvictingQueue(int var1) {
      this.limit = var1;
   }

   @Override
   public boolean add(@NotNull E var1) {
      boolean var2 = super.add((E)var1);

      while (var2 && this.size() > this.limit) {
         super.remove();
      }

      return var2;
   }

   @Override
   public void addFirst(@NotNull E var1) {
      super.addFirst((E)var1);

      while (this.size() > this.limit) {
         super.removeLast();
      }
   }

   public int limit() {
      return this.limit;
   }
}
