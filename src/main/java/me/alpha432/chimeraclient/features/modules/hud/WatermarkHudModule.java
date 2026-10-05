package me.alpha432.chimeraclient.features.modules.hud;

import me.alpha432.chimeraclient.event.impl.render.Render2DEvent;
import me.alpha432.chimeraclient.features.gui.font.FontDraw;
import me.alpha432.chimeraclient.features.modules.client.HudModule;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.util.TextUtil;

public class WatermarkHudModule extends HudModule {
   public Setting<String> text = this.str("Text", "ChimeraClient");
   public Setting<Boolean> fullVersion = new Setting<>("FullVersion", false);

   public WatermarkHudModule() {
      super("Watermark", "Display watermark", 100.0F, 10.0F);
   }

   @Override
   protected void render(Render2DEvent e) {
      super.render(e);
      String watermarkString = "{global} %s {} %s";
      if (this.fullVersion.getValue()) {
      }

      FontDraw.drawTextWithShadow(
         e.getContext(), mc.textRenderer, TextUtil.text(watermarkString, this.text.getValue(), "1.0.0"), (int)this.getX(), (int)this.getY(), -1
      );
      this.setWidth(FontDraw.width(mc.textRenderer, watermarkString));
      this.setHeight(9.0F);
   }
}
