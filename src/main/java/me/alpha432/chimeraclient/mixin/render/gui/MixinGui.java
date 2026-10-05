package me.alpha432.chimeraclient.mixin.render.gui;

import me.alpha432.chimeraclient.event.impl.render.Render2DEvent;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({InGameHud.class})
public class MixinGui {
   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   public void render(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (!MinecraftClient.getInstance().inGameHud.getDebugHud().shouldShowDebugHud()) {
         Render2DEvent event = new Render2DEvent(context, tickCounter.getTickProgress(true));
         Util.EVENT_BUS.post(event);
      }
   }
}
