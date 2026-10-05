package me.alpha432.chimeraclient.util.traits;

import me.alpha432.chimeraclient.event.system.EventBus;
import net.minecraft.client.MinecraftClient;

public interface Util {
   MinecraftClient mc = MinecraftClient.getInstance();
   EventBus EVENT_BUS = new EventBus();
}
