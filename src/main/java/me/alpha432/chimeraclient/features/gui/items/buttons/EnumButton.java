package me.alpha432.chimeraclient.features.gui.items.buttons;

import java.awt.Color;
import me.alpha432.chimeraclient.features.gui.font.FontDraw;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.util.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

public class EnumButton extends Button {
   public Setting<Enum<?>> setting;
   private float hoverAnim = 0.0F;
   private static final Color BG_NORMAL = new Color(18, 18, 26, 200);
   private static final Color BG_HOVER = new Color(28, 30, 48, 220);
   private static final Color TEXT_NAME = new Color(170, 175, 205, 255);
   private static final Color TEXT_VAL = new Color(100, 120, 255, 220);

   public EnumButton(Setting<Enum<?>> setting) {
      super(setting.getName());
      this.setting = setting;
      this.width = 15;
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      boolean hovering = this.isHovering(mouseX, mouseY);
      this.hoverAnim = hovering ? Math.min(1.0F, this.hoverAnim + 0.2F) : Math.max(0.0F, this.hoverAnim - 0.2F);
      float fullW = this.width + 7.4F;
      int bgColor = this.blendColor(BG_NORMAL, BG_HOVER, this.hoverAnim);
      RenderUtil.rect(context, this.x, this.y, this.x + fullW, this.y + this.height - 0.5F, bgColor);
      RenderUtil.rect(context, this.x, this.y + this.height - 1.0F, this.x + fullW, this.y + this.height - 0.5F, new Color(35, 35, 50, 180).getRGB());
      FontDraw.drawText(context, mc.textRenderer, this.setting.getName(), (int)(this.x + 3.0F), (int)(this.y + 3.0F), TEXT_NAME.getRGB(), false);
      String valStr = this.setting.currentEnumName();
      int valW = FontDraw.width(mc.textRenderer, valStr);
      FontDraw.drawText(context, mc.textRenderer, valStr, (int)(this.x + fullW - 3.0F - valW), (int)(this.y + 3.0F), TEXT_VAL.getRGB(), false);
   }

   private int blendColor(Color a, Color b, float t) {
      int r = (int)(a.getRed() + (b.getRed() - a.getRed()) * t);
      int g = (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
      int bl = (int)(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
      int al = (int)(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);
      return new Color(r, g, bl, al).getRGB();
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (this.isHovering(mouseX, mouseY)) {
         mc.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK, 1.0F));
      }
   }

   @Override
   public void toggle() {
      this.setting.increaseEnum();
   }

   @Override
   public boolean getState() {
      return true;
   }
}
