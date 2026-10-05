package me.alpha432.chimeraclient.features.gui.items.buttons;

import java.awt.Color;
import me.alpha432.chimeraclient.features.gui.font.FontDraw;
import me.alpha432.chimeraclient.features.settings.Bind;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.util.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

public class BindButton extends Button {
   private final Setting<Bind> setting;
   public boolean isListening;
   private static final Color BG_NORMAL = new Color(18, 18, 26, 200);
   private static final Color BG_LISTENING = new Color(30, 20, 50, 230);
   private static final Color TEXT_NORMAL = new Color(160, 165, 195, 255);
   private static final Color TEXT_KEY = new Color(100, 120, 255, 220);
   private static final Color TEXT_LISTEN = new Color(200, 170, 255, 255);

   public BindButton(Setting<Bind> setting) {
      super(setting.getName());
      this.setting = setting;
      this.width = 15;
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      float fullW = this.width + 7.4F;
      int bgColor = this.isListening ? BG_LISTENING.getRGB() : BG_NORMAL.getRGB();
      RenderUtil.rect(context, this.x, this.y, this.x + fullW, this.y + this.height - 0.5F, bgColor);
      RenderUtil.rect(context, this.x, this.y + this.height - 1.0F, this.x + fullW, this.y + this.height - 0.5F, new Color(35, 35, 50, 180).getRGB());
      if (this.isListening) {
         RenderUtil.rect(context, this.x + 3.0F, this.y + 5.0F, this.x + 5.0F, this.y + 7.0F, new Color(200, 80, 255, 255).getRGB());
         FontDraw.drawText(context, mc.textRenderer, "Press a key...", (int)(this.x + 8.0F), (int)(this.y + 3.0F), TEXT_LISTEN.getRGB(), false);
      } else {
         String str = this.setting.getValue().toString().toUpperCase().replace("KEY.KEYBOARD", "").replace(".", " ").trim();
         FontDraw.drawText(context, mc.textRenderer, this.setting.getName(), (int)(this.x + 3.0F), (int)(this.y + 3.0F), TEXT_NORMAL.getRGB(), false);
         int keyW = FontDraw.width(mc.textRenderer, str);
         FontDraw.drawText(context, mc.textRenderer, str, (int)(this.x + fullW - 3.0F - keyW), (int)(this.y + 3.0F), TEXT_KEY.getRGB(), false);
      }
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (this.isListening) {
         if (mouseButton != 0 && mouseButton != 1) {
            this.setting.setValue(new Bind(-mouseButton - 2));
            this.onMouseClick();
         }
      } else if (this.isHovering(mouseX, mouseY)) {
         mc.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK, 1.0F));
      }
   }

   @Override
   public void onKeyPressed(int key) {
      if (this.isListening) {
         Bind bind = new Bind(key);
         if (key == 256) {
            bind = new Bind(-1);
         }

         this.setting.setValue(bind);
         this.isListening = false;
      }
   }

   @Override
   public void toggle() {
      this.isListening = !this.isListening;
   }

   @Override
   public boolean getState() {
      return this.isListening;
   }
}
