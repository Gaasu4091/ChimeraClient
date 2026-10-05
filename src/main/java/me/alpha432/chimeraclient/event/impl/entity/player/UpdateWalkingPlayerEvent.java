package me.alpha432.chimeraclient.event.impl.entity.player;

import me.alpha432.chimeraclient.event.Event;
import me.alpha432.chimeraclient.event.Stage;

public class UpdateWalkingPlayerEvent extends Event {
   private final Stage stage;

   public UpdateWalkingPlayerEvent(Stage stage) {
      this.stage = stage;
   }

   public Stage getStage() {
      return this.stage;
   }
}
