package me.alpha432.chimeraclient.util;

import java.awt.Color;
import me.alpha432.chimeraclient.features.modules.client.ClickGuiModule;

public class ColorUtil {
   public static Color rainbow(int delay) {
      double rainbowState = Math.ceil((System.currentTimeMillis() + delay) / 20.0);
      return Color.getHSBColor(
         (float)(rainbowState % 360.0 / 360.0),
         ClickGuiModule.getInstance().rainbowSaturation.getValue() / 255.0F,
         ClickGuiModule.getInstance().rainbowBrightness.getValue() / 255.0F
      );
   }
}
