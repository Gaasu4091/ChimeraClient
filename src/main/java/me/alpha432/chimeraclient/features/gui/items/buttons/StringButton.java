package me.alpha432.chimeraclient.features.gui.items.buttons;

import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.features.gui.ChimeraClientGui;
import me.alpha432.chimeraclient.features.gui.font.FontDraw;
import me.alpha432.chimeraclient.features.modules.client.ClickGuiModule;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.util.models.Timer;
import me.alpha432.chimeraclient.util.render.RenderUtil;
import me.alpha432.chimeraclient.util.render.ScissorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;

public class StringButton extends Button {
   private static final Timer idleTimer = new Timer();
   private static boolean idle;
   private final Setting<String> setting;
   public boolean isListening;
   private StringButton.CurrentString currentString = new StringButton.CurrentString("");
   private int cursorPos = 0;

   public StringButton(Setting<String> setting) {
      super(setting.getName());
      this.setting = setting;
      this.width = 15;
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      RenderUtil.rect(
         context,
         this.x,
         this.y,
         this.x + this.width + 7.4F,
         this.y + this.height - 0.5F,
         this.getState()
            ? (
               !this.isHovering(mouseX, mouseY)
                  ? ChimeraClient.colorManager.getColorWithAlpha(this.y, ClickGuiModule.getInstance().color.getValue().getAlpha())
                  : ChimeraClient.colorManager.getColorWithAlpha(this.y, ClickGuiModule.getInstance().topColor.getValue().getAlpha())
            )
            : (!this.isHovering(mouseX, mouseY) ? 290805077 : -2007673515)
      );
      if (this.isListening) {
         String text = this.currentString.string();
         int clampedCursor = Math.min(this.cursorPos, text.length());
         String beforeCursor = text.substring(0, clampedCursor);
         String afterCursor = text.substring(clampedCursor);
         String displayText = beforeCursor + getIdleSign() + afterCursor;
         float cursorPixelX = FontDraw.width(mc.textRenderer, beforeCursor + getIdleSign());
         float availWidth = this.width + 7.4F - 5.0F;
         float scrollOffset = Math.max(0.0F, cursorPixelX - availWidth);
         int clipX1 = (int)(this.x + 2.3F);
         int clipY1 = (int)(this.y - 3.0F);
         int clipX2 = (int)(this.x + this.width + 7.4F - 0.5F);
         int clipY2 = (int)(this.y + this.height);
         ScissorUtil.enable(context, clipX1, clipY1, clipX2, clipY2);
         this.drawString(displayText, this.x + 2.3F - scrollOffset, this.y - 1.7F - ChimeraClientGui.getClickGui().getTextOffset(), -1);
         ScissorUtil.disable(context);
      } else {
         this.drawString(
            (this.setting.getName().equals("Buttons") ? "Buttons " : (this.setting.getName().equals("Prefix") ? "Prefix  " + Formatting.GRAY : ""))
               + this.setting.getValue(),
            this.x + 2.3F,
            this.y - 1.7F - ChimeraClientGui.getClickGui().getTextOffset(),
            this.getState() ? -1 : -5592406
         );
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (this.isHovering(mouseX, mouseY)) {
         mc.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK, 1.0F));
      }
   }

   @Override
   public void onKeyTyped(String typedChar, int keyCode) {
      if (this.isListening && typedChar != null && !typedChar.isEmpty()) {
         String s = this.currentString.string();
         int clampedCursor = Math.min(this.cursorPos, s.length());
         String newStr = s.substring(0, clampedCursor) + typedChar + s.substring(clampedCursor);
         this.setString(newStr);
         this.cursorPos = clampedCursor + typedChar.length();
      }
   }

   @Override
   public void onKeyPressed(int key) {
      if (this.isListening) {
         String s = this.currentString.string();
         int clampedCursor = Math.min(this.cursorPos, s.length());
         switch (key) {
            case 257:
               this.enterString();
            case 258:
            case 260:
            case 264:
            case 265:
            case 266:
            case 267:
            default:
               break;
            case 259:
               if (clampedCursor > 0) {
                  String newStr = s.substring(0, clampedCursor - 1) + s.substring(clampedCursor);
                  this.setString(newStr);
                  this.cursorPos = clampedCursor - 1;
               }
               break;
            case 261:
               if (clampedCursor < s.length()) {
                  String newStr = s.substring(0, clampedCursor) + s.substring(clampedCursor + 1);
                  this.setString(newStr);
               }
               break;
            case 262:
               if (this.cursorPos < s.length()) {
                  this.cursorPos++;
               }
               break;
            case 263:
               if (this.cursorPos > 0) {
                  this.cursorPos--;
               }
               break;
            case 268:
               this.cursorPos = 0;
               break;
            case 269:
               this.cursorPos = s.length();
         }
      }
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   private void enterString() {
      if (this.currentString.string().isEmpty()) {
         this.setting.setValue(this.setting.getDefaultValue());
      } else {
         this.setting.setValue(this.currentString.string());
      }

      this.setString("");
      this.cursorPos = 0;
      this.onMouseClick();
   }

   @Override
   public void toggle() {
      this.isListening = !this.isListening;
      if (this.isListening) {
         String current = this.setting.getValue();
         this.currentString = new StringButton.CurrentString(current != null ? current : "");
         this.cursorPos = this.currentString.string().length();
      }
   }

   @Override
   public boolean getState() {
      return !this.isListening;
   }

   public void setString(String newString) {
      this.currentString = new StringButton.CurrentString(newString != null ? newString : "");
      if (this.cursorPos > this.currentString.string().length()) {
         this.cursorPos = this.currentString.string().length();
      }
   }

   public static String getIdleSign() {
      if (idleTimer.passedMs(500L)) {
         idle = !idle;
         idleTimer.reset();
      }

      return idle ? "|" : "";
   }

   public record CurrentString(String string) {
   }
}
