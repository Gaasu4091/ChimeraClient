package me.alpha432.chimeraclient.features.gui.items.buttons;

import java.awt.Color;
import me.alpha432.chimeraclient.features.gui.font.FontDraw;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.util.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

public class BooleanButton extends Button {
   private final Setting<Boolean> setting;
   private float toggleAnim = 0.0F;
   private static final Color BG_NORMAL = new Color(18, 18, 26, 200);
   private static final Color BG_HOVER = new Color(28, 30, 48, 220);
   private static final Color TEXT_ON = new Color(210, 218, 255, 255);
   private static final Color TEXT_OFF = new Color(100, 108, 140, 255);
   private static final Color TOGGLE_OFF = new Color(45, 45, 60, 255);
   private static final Color TOGGLE_ON = new Color(100, 120, 255, 255);

   public BooleanButton(Setting<Boolean> setting) {
      super(setting.getName());
      this.setting = setting;
      this.width = 15;
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      boolean on = this.getState();
      boolean hovering = this.isHovering(mouseX, mouseY);
      this.toggleAnim = on ? Math.min(1.0F, this.toggleAnim + 0.2F) : Math.max(0.0F, this.toggleAnim - 0.2F);
      int bgColor = hovering ? BG_HOVER.getRGB() : BG_NORMAL.getRGB();
      RenderUtil.rect(context, this.x, this.y, this.x + this.width + 7.4F, this.y + this.height - 0.5F, bgColor);
      RenderUtil.rect(
         context, this.x, this.y + this.height - 1.0F, this.x + this.width + 7.4F, this.y + this.height - 0.5F, new Color(35, 35, 50, 180).getRGB()
      );
      float pillX = this.x + this.width + 7.4F - 18.0F;
      float pillY = this.y + 3.0F;
      float pillW = 13.0F;
      float pillH = 7.0F;
      Color pillBg = new Color(
         (int)(TOGGLE_OFF.getRed() + (TOGGLE_ON.getRed() - TOGGLE_OFF.getRed()) * this.toggleAnim),
         (int)(TOGGLE_OFF.getGreen() + (TOGGLE_ON.getGreen() - TOGGLE_OFF.getGreen()) * this.toggleAnim),
         (int)(TOGGLE_OFF.getBlue() + (TOGGLE_ON.getBlue() - TOGGLE_OFF.getBlue()) * this.toggleAnim),
         255
      );
      RenderUtil.rect(context, pillX, pillY, pillX + pillW, pillY + pillH, pillBg.getRGB());
      float knobOffset = this.toggleAnim * (pillW - pillH + 1.0F);
      RenderUtil.rect(context, pillX + 1.0F + knobOffset, pillY + 1.0F, pillX + pillH - 1.0F + knobOffset, pillY + pillH - 1.0F, Color.WHITE.getRGB());
      Color textColor = on ? TEXT_ON : TEXT_OFF;
      FontDraw.drawText(context, mc.textRenderer, this.getName(), (int)(this.x + 3.0F), (int)(this.y + 3.0F), textColor.getRGB(), false);
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
      this.setting.setValue(!this.setting.getValue());
   }

   @Override
   public boolean getState() {
      return this.setting.getValue();
   }
}
