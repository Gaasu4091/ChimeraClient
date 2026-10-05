package me.alpha432.chimeraclient.event.impl.network;

import me.alpha432.chimeraclient.event.Event;

public class ChatEvent extends Event {
   private final String content;

   public ChatEvent(String content) {
      this.content = content;
   }

   public String getMessage() {
      return this.content;
   }
}
