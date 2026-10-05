package me.alpha432.chimeraclient.features.gui;

import java.awt.Color;
import me.alpha432.chimeraclient.features.modules.client.ClickGuiModule;
import me.alpha432.chimeraclient.features.settings.Setting;

public final class HeaderSettings {
   private HeaderSettings() {
   }

   public static void register(ClickGuiModule var0) {
      if (var0.getSettingByName("HeaderColor") == null) {
         var0.register(new Setting<>("HeaderColor", new Color(24, 24, 24, 240)));
      }
   }

   public static Color color() {
      ClickGuiModule var0 = ClickGuiModule.getInstance();
      Setting var1 = var0 == null ? null : var0.getSettingByName("HeaderColor");
      return var1 != null && var1.getValue() instanceof Color var2 ? var2 : new Color(24, 24, 24, 240);
   }
}
