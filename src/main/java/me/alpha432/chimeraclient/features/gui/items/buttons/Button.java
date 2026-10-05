package me.alpha432.chimeraclient.features.gui.items.buttons;

import java.awt.Color;
import me.alpha432.chimeraclient.features.gui.ChimeraClientGui;
import me.alpha432.chimeraclient.features.gui.GuiTheme;
import me.alpha432.chimeraclient.features.gui.Widget;
import me.alpha432.chimeraclient.features.gui.font.FontDraw;
import me.alpha432.chimeraclient.features.gui.items.Item;
import me.alpha432.chimeraclient.features.modules.client.ClickGuiModule;
import me.alpha432.chimeraclient.util.ColorUtil;
import me.alpha432.chimeraclient.util.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class Button extends Item {
   private boolean state;

   public Button(String name) {
      super(name);
      this.height = 14;
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      Color background = GuiTheme.background();
      if (this.isHovering(mouseX, mouseY)) {
         background = brighten(background, 15);
      }

      RenderUtil.rect(context, this.x, this.y, this.x + this.width, this.y + this.height - 0.5F, background.getRGB());
      if (this.getState()) {
         Color accent = ClickGuiModule.getInstance().rainbow.getValue()
            ? ColorUtil.rainbow(ClickGuiModule.getInstance().rainbowHue.getValue())
            : ClickGuiModule.getInstance().color.getValue();
         RenderUtil.rect(context, this.x, this.y, this.x + 2.0F, this.y + this.height - 0.5F, GuiTheme.withOpacity(accent).getRGB());
      }

      RenderUtil.rect(
         context,
         this.x,
         this.y + this.height - 1.0F,
         this.x + this.width,
         this.y + this.height - 0.5F,
         GuiTheme.withOpacity(new Color(62, 62, 68, 180)).getRGB()
      );
      FontDraw.drawText(
         context,
         MinecraftClient.getInstance().textRenderer,
         this.getName(),
         (int)this.x + (this.getState() ? 6 : 3),
         (int)this.y + 3,
         GuiTheme.text().getRGB(),
         false
      );
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
         this.onMouseClick();
      }
   }

   public void onMouseClick() {
      this.state = !this.state;
      this.toggle();
   }

   public void toggle() {
   }

   public boolean getState() {
      return this.state;
   }

   @Override
   public int getHeight() {
      return 14;
   }

   @Override
   public boolean isHovering(int mouseX, int mouseY) {
      for (Widget widget : ChimeraClientGui.getClickGui().getComponents()) {
         if (widget.drag) {
            return false;
         }
      }

      return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
   }

   private static Color brighten(Color color, int amount) {
      return new Color(
         Math.min(255, color.getRed() + amount), Math.min(255, color.getGreen() + amount), Math.min(255, color.getBlue() + amount), color.getAlpha()
      );
   }
}
