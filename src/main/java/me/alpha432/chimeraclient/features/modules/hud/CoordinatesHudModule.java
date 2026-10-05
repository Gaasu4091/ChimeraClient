package me.alpha432.chimeraclient.features.modules.hud;

import me.alpha432.chimeraclient.event.impl.render.Render2DEvent;
import me.alpha432.chimeraclient.features.gui.font.FontDraw;
import me.alpha432.chimeraclient.features.modules.client.HudModule;
import me.alpha432.chimeraclient.features.settings.Setting;

public class CoordinatesHudModule extends HudModule {
   public Setting<Boolean> nether = this.bool("Nether", false);

   public CoordinatesHudModule() {
      super("Coordinates", "Display coordinates", 150.0F, 20.0F);
   }

   @Override
   protected void render(Render2DEvent e) {
      super.render(e);
      if (!nullCheck()) {
         String coordsStr = String.format("X: %d Y: %d Z: %d", mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
         if (this.nether.getValue()) {
            int netherX = mc.player.getEntityWorld().getRegistryKey().getValue().getPath().equals("the_nether")
               ? mc.player.getBlockX() * 8
               : mc.player.getBlockX() / 8;
            int netherZ = mc.player.getEntityWorld().getRegistryKey().getValue().getPath().equals("the_nether")
               ? mc.player.getBlockZ() * 8
               : mc.player.getBlockZ() / 8;
            coordsStr = coordsStr + String.format(" [%d, %d]", netherX, netherZ);
         }

         FontDraw.drawTextWithShadow(e.getContext(), mc.textRenderer, coordsStr, (int)this.getX(), (int)this.getY(), -1);
         this.setWidth(FontDraw.width(mc.textRenderer, coordsStr));
         this.setHeight(9.0F);
      }
   }
}
