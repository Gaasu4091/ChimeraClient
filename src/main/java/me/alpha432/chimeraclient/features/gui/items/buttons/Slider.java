package me.alpha432.chimeraclient.features.gui.items.buttons;

import java.awt.Color;
import me.alpha432.chimeraclient.features.gui.ChimeraClientGui;
import me.alpha432.chimeraclient.features.gui.Widget;
import me.alpha432.chimeraclient.features.gui.font.FontDraw;
import me.alpha432.chimeraclient.features.modules.client.ClickGuiModule;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.util.ColorUtil;
import me.alpha432.chimeraclient.util.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public class Slider extends Button {
   private final Number min;
   private final Number max;
   private final int difference;
   public Setting<Number> setting;
   private static final Color BG_TRACK = new Color(25, 25, 38, 220);
   private static final Color BG_HOVER = new Color(32, 32, 50, 230);
   private static final Color TRACK_EMPTY = new Color(40, 42, 60, 255);
   private static final Color HANDLE_COLOR = new Color(180, 190, 255, 255);

   public Slider(Setting<Number> setting) {
      super(setting.getName());
      this.setting = setting;
      this.min = setting.getMin();
      this.max = setting.getMax();
      this.difference = this.max.intValue() - this.min.intValue();
      this.width = 15;
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      this.dragSetting(mouseX, mouseY);
      boolean hovering = this.isHovering(mouseX, mouseY);
      float fullW = this.width + 7.4F;
      float h = this.height;
      RenderUtil.rect(context, this.x, this.y, this.x + fullW, this.y + h - 0.5F, hovering ? BG_HOVER.getRGB() : BG_TRACK.getRGB());
      RenderUtil.rect(context, this.x, this.y + h - 1.0F, this.x + fullW, this.y + h - 0.5F, new Color(35, 35, 50, 180).getRGB());
      float trackY = this.y + h - 5.0F;
      float trackH = 2.5F;
      RenderUtil.rect(context, this.x + 2.0F, trackY, this.x + fullW - 2.0F, trackY + trackH, TRACK_EMPTY.getRGB());
      float filled = fullW > 4.0F ? (fullW - 4.0F) * this.partialMultiplier() : 0.0F;
      if (filled > 0.5F) {
         Color accent = ClickGuiModule.getInstance().rainbow.getValue()
            ? ColorUtil.rainbow(ClickGuiModule.getInstance().rainbowHue.getValue())
            : ClickGuiModule.getInstance().color.getValue();
         RenderUtil.rect(context, this.x + 2.0F, trackY, this.x + 2.0F + filled, trackY + trackH, accent.getRGB());
         float knobX = this.x + 2.0F + filled - 1.5F;
         RenderUtil.rect(context, knobX, trackY - 1.0F, knobX + 3.0F, trackY + trackH + 1.0F, HANDLE_COLOR.getRGB());
      }

      String valStr = this.setting.getValue() instanceof Float
         ? String.valueOf(this.setting.getValue())
         : String.valueOf(Math.round(10.0 * this.setting.getValue().doubleValue()) / 10.0);
      FontDraw.drawText(
         context, mc.textRenderer, this.setting.getName(), (int)(this.x + 3.0F), (int)(this.y + 3.0F), new Color(180, 185, 210, 255).getRGB(), false
      );
      int valW = FontDraw.width(mc.textRenderer, valStr);
      FontDraw.drawText(
         context, mc.textRenderer, valStr, (int)(this.x + fullW - 2.0F - valW), (int)(this.y + 3.0F), new Color(130, 135, 170, 200).getRGB(), false
      );
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (this.isHovering(mouseX, mouseY)) {
         this.setSettingFromX(mouseX);
      }
   }

   @Override
   public boolean isHovering(int mouseX, int mouseY) {
      for (Widget widget : ChimeraClientGui.getClickGui().getComponents()) {
         if (widget.drag) {
            return false;
         }
      }

      return mouseX >= this.getX() && mouseX <= this.getX() + this.getWidth() + 8.0F && mouseY >= this.getY() && mouseY < this.getY() + this.height;
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   private void dragSetting(int mouseX, int mouseY) {
      if (this.isHovering(mouseX, mouseY) && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 1) {
         this.setSettingFromX(mouseX);
      }
   }

   private void setSettingFromX(int mouseX) {
      float percent = (mouseX - this.x) / (this.width + 7.4F);
      if (this.setting.getValue() instanceof Double) {
         double result = (Double)this.setting.getMin() + this.difference * percent;
         this.setting.setValue(Math.round(10.0 * result) / 10.0);
      } else if (this.setting.getValue() instanceof Float) {
         float result = this.setting.getMin().floatValue() + this.difference * percent;
         this.setting.setValue(Math.round(10.0F * result) / 10.0F);
      } else if (this.setting.getValue() instanceof Integer) {
         this.setting.setValue((Integer)this.setting.getMin() + (int)(this.difference * percent));
      }
   }

   private float middle() {
      return this.max.floatValue() - this.min.floatValue();
   }

   private float part() {
      return this.setting.getValue().floatValue() - this.min.floatValue();
   }

   private float partialMultiplier() {
      return this.part() / this.middle();
   }
}
