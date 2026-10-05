package me.alpha432.chimeraclient.features.gui;

import java.awt.Color;
import me.alpha432.chimeraclient.features.modules.client.ClickGuiModule;

public final class GuiTheme {
   private GuiTheme() {
   }

   public static Color text() {
      return withOpacity(gui().textColor.getValue());
   }

   public static Color background() {
      return withOpacity(gui().textBackground.getValue());
   }

   public static int opacity() {
      return gui().opacity.getValue();
   }

   public static Color withOpacity(Color color) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() * opacity() / 255);
   }

   private static ClickGuiModule gui() {
      return ClickGuiModule.getInstance();
   }
}
