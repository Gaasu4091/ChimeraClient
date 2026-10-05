package me.alpha432.chimeraclient.features.modules.client;

import java.awt.Color;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.event.impl.ClientEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.commands.Command;
import me.alpha432.chimeraclient.features.gui.ChimeraClientGui;
import me.alpha432.chimeraclient.features.gui.HeaderSettings;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;

public class ClickGuiModule extends Module {
   private static ClickGuiModule INSTANCE;
   public Setting<String> prefix = this.str("Prefix", ".");
   public Setting<Color> color = this.color("Color", 0, 0, 255, 180);
   public Setting<Color> topColor = this.color("TopColor", 0, 0, 150, 240);
   public Setting<Color> textColor = this.color("TextColor", 230, 230, 235, 255);
   public Setting<Color> textBackground = this.color("TextBackground", 14, 14, 18, 215);
   public Setting<Integer> opacity = this.num("Opacity", 185, 25, 255);
   public Setting<Boolean> rainbow = this.bool("Rainbow", false);
   public Setting<Integer> rainbowHue = this.num("Delay", 240, 0, 600);
   public Setting<Float> rainbowBrightness = this.num("Brightness", 150.0F, 1.0F, 255.0F);
   public Setting<Float> rainbowSaturation = this.num("Saturation", 150.0F, 1.0F, 255.0F);

   public ClickGuiModule() {
      super("ClickGui", "Opens the ClickGui", Module.Category.CLIENT);
      this.setBind(344);
      this.rainbowHue.setVisibility(value -> this.rainbow.getValue());
      this.rainbowBrightness.setVisibility(value -> this.rainbow.getValue());
      this.rainbowSaturation.setVisibility(value -> this.rainbow.getValue());
      INSTANCE = this;
      HeaderSettings.register(this);
   }

   @Subscribe
   public void onSettingChange(ClientEvent event) {
      if (event.getType() == ClientEvent.Type.SETTING_UPDATE && event.getSetting().getFeature().equals(this)) {
         if (event.getSetting().equals(this.prefix)) {
            ChimeraClient.commandManager.setCommandPrefix(this.prefix.getPlannedValue());
            Command.sendMessage("Prefix set to {global} %s", ChimeraClient.commandManager.getCommandPrefix());
         }

         if (event.getSetting().equals(this.color)) {
            ChimeraClient.colorManager.setColor(this.color.getPlannedValue());
         }
      }
   }

   @Override
   public void onEnable() {
      if (!nullCheck()) {
         mc.setScreen(ChimeraClientGui.getClickGui());
      }
   }

   @Override
   public void onLoad() {
      ChimeraClient.colorManager.setColor(this.color.getValue());
      ChimeraClient.commandManager.setCommandPrefix(this.prefix.getValue());
   }

   @Override
   public void onTick() {
      if (!(mc.currentScreen instanceof ChimeraClientGui)) {
         this.disable();
      }
   }

   public static ClickGuiModule getInstance() {
      return INSTANCE;
   }
}
