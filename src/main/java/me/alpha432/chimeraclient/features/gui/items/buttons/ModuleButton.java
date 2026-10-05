package me.alpha432.chimeraclient.features.gui.items.buttons;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import me.alpha432.chimeraclient.features.gui.GuiTheme;
import me.alpha432.chimeraclient.features.gui.font.FontDraw;
import me.alpha432.chimeraclient.features.gui.items.Item;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Bind;
import me.alpha432.chimeraclient.features.settings.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class ModuleButton extends Button {
   private final Module module;
   private final List<Item> items = new ArrayList<>();
   private boolean subOpen;

   public ModuleButton(Module module) {
      super(module.getName());
      this.module = module;
      this.initSettings();
   }

   private void initSettings() {
      for (Setting<?> setting : this.module.getSettings()) {
         Object value = setting.getValue();
         if (value instanceof Boolean && !setting.getName().equals("Enabled")) {
            this.items.add(new BooleanButton((Setting<Boolean>)setting));
         } else if (value instanceof Bind && !setting.getName().equalsIgnoreCase("Keybind") && !this.module.getName().equalsIgnoreCase("Hud")) {
            this.items.add(new BindButton((Setting<Bind>)setting));
         } else if ((value instanceof String || value instanceof Character) && !setting.getName().equalsIgnoreCase("displayName")) {
            this.items.add(new StringButton((Setting<String>)setting));
         } else if (setting.isColorSetting()) {
            this.items.add(new ColorButton((Setting<Color>)setting));
         } else if (setting.isNumberSetting() && setting.hasRestriction()) {
            this.items.add(new Slider((Setting<Number>)setting));
         } else if (setting.isEnumSetting()) {
            this.items.add(new EnumButton((Setting<Enum<?>>)setting));
         }
      }

      Setting<?> keybind = this.module.getSettingByName("Keybind");
      if (keybind != null) {
         this.items.add(new BindButton((Setting<Bind>)keybind));
      }
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      super.drawScreen(context, mouseX, mouseY, partialTicks);
      if (!this.items.isEmpty()) {
         String var10002 = this.subOpen ? "−" : "+";
         int var10003 = (int)(this.x + this.width - 9.0F);
         FontDraw.drawText(context, MinecraftClient.getInstance().textRenderer, var10002, var10003, (int)this.y + 3, GuiTheme.text().getRGB(), false);
      }

      if (this.subOpen) {
         float childY = this.y + 15.0F;

         for (Item item : this.items) {
            item.update();
            if (!item.isHidden()) {
               item.setLocation(this.x + 4.0F, childY);
               item.setWidth(this.width - 8);
               item.drawScreen(context, mouseX, mouseY, partialTicks);
               childY += item.getHeight() + 1;
            }
         }
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (mouseButton == 1 && this.isHovering(mouseX, mouseY) && !this.items.isEmpty()) {
         this.subOpen = !this.subOpen;
      }

      if (this.subOpen) {
         this.items.forEach(item -> item.mouseClicked(mouseX, mouseY, mouseButton));
      }
   }

   @Override
   public void mouseReleased(int mouseX, int mouseY, int button) {
      super.mouseReleased(mouseX, mouseY, button);
      if (this.subOpen) {
         this.items.forEach(item -> item.mouseReleased(mouseX, mouseY, button));
      }
   }

   @Override
   public void onKeyTyped(String text, int modifiers) {
      if (this.subOpen) {
         this.items.forEach(item -> item.onKeyTyped(text, modifiers));
      }
   }

   @Override
   public void onKeyPressed(int key) {
      if (this.subOpen) {
         this.items.forEach(item -> item.onKeyPressed(key));
      }
   }

   @Override
   public int getHeight() {
      int total = 14;
      if (this.subOpen) {
         for (Item item : this.items) {
            if (!item.isHidden()) {
               total += item.getHeight() + 1;
            }
         }
      }

      return total;
   }

   public Module getModule() {
      return this.module;
   }

   @Override
   public void toggle() {
      this.module.toggle();
   }

   @Override
   public boolean getState() {
      return this.module.isEnabled();
   }
}
