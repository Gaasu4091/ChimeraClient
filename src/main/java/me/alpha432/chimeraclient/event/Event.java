package me.alpha432.chimeraclient.event;

public class Event {
   private boolean cancelled;

   public boolean isCancelled() {
      return this.cancelled;
   }

   public void cancel() {
      this.cancelled = true;
   }
}
