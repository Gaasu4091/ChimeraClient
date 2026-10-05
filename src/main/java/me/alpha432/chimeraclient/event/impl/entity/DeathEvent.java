package me.alpha432.chimeraclient.event.impl.entity;

import me.alpha432.chimeraclient.event.Event;
import net.minecraft.entity.LivingEntity;

public class DeathEvent extends Event {
   private final LivingEntity entity;

   public DeathEvent(LivingEntity entity) {
      this.entity = entity;
   }

   public LivingEntity getEntity() {
      return this.entity;
   }
}
