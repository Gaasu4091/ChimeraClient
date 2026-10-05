package me.alpha432.chimeraclient.features.modules.client;

import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.event.impl.input.MouseInputEvent;
import me.alpha432.chimeraclient.event.impl.render.Render2DEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.gui.HudEditorScreen;
import me.alpha432.chimeraclient.features.gui.font.FontDraw;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.util.render.RenderUtil;
import net.minecraft.client.gui.screen.ChatScreen;
import org.joml.Vector2f;

public abstract class HudModule extends Module {
   public final Setting<Vector2f> pos = this.vec2f("Position", 0.5F, 0.5F);
   private float dragX;
   private float dragY;
   private float width;
   private float height;
   private boolean dragging;
   private boolean button;

   public HudModule(String name, String description, float width, float height) {
      super(name, description, Module.Category.HUD);
      this.width = width;
      this.height = height;
   }

   public float getX() {
      return mc.getWindow().getScaledWidth() * this.pos.getValue().x();
   }

   public float getY() {
      float heightWithChat = mc.getWindow().getScaledHeight() - 14;
      float baseY = mc.getWindow().getScaledHeight() * this.pos.getValue().y();
      float combined = baseY + this.getHeight();
      if (mc.currentScreen instanceof ChatScreen) {
         baseY = Math.min(combined, heightWithChat) - this.getHeight();
      }

      return baseY;
   }

   @Subscribe
   public void onRender2DHud(Render2DEvent e) {
      this.render(e);
   }

   @Subscribe
   public void onMouse(MouseInputEvent e) {
      if (mc.currentScreen instanceof HudEditorScreen && !nullCheck()) {
         if (e.getAction() == 0) {
            this.button = false;
            this.dragging = false;
            HudEditorScreen.getInstance().currentDragging = null;
         }

         if (e.getAction() == 1 && this.isHovering()) {
            this.button = true;
         }
      }
   }

   protected void render(Render2DEvent e) {
      if (mc.currentScreen instanceof HudEditorScreen && !nullCheck()) {
         float x = this.getX();
         float y = this.getY();
         if (this.button) {
            if (!this.dragging && this.isHovering() && HudEditorScreen.getInstance().currentDragging == null) {
               this.dragX = this.getMouseX() - x;
               this.dragY = this.getMouseY() - y;
               this.dragging = true;
               HudEditorScreen.getInstance().currentDragging = this;
            }

            if (this.dragging) {
               float finalX = Math.min(Math.max(this.getMouseX() - this.dragX, 0.0F), mc.getWindow().getScaledWidth() - this.width);
               float finalY = Math.min(Math.max(this.getMouseY() - this.dragY, 0.0F), mc.getWindow().getScaledHeight() - this.height);
               this.pos.getValue().x = finalX / mc.getWindow().getScaledWidth();
               this.pos.getValue().y = finalY / mc.getWindow().getScaledHeight();
            }
         } else {
            this.dragging = false;
         }

         boolean shouldDrawDescription = this.isHovering() && !HudEditorScreen.getInstance().anyHover;
         if (HudEditorScreen.getInstance().currentDragging != null) {
            shouldDrawDescription = HudEditorScreen.getInstance().currentDragging == this;
         }

         if (shouldDrawDescription) {
            int textWidth = FontDraw.width(mc.textRenderer, this.getName());
            int textHeight = 9;
            float textX = x + this.width + 5.0F;
            if (textX + textWidth > mc.getWindow().getScaledWidth()) {
               textX = x - 5.0F - textWidth;
            }

            FontDraw.drawTextWithShadow(e.getContext(), mc.textRenderer, this.getName(), (int)textX, (int)(y + this.height / 2.0F - textHeight / 2.0F), -1);
            HudEditorScreen.getInstance().anyHover = true;
         }

         RenderUtil.rect(
            e.getContext(), x - 1.0F, y - 1.0F, x + this.width + 1.0F, y + this.height + 1.0F, ChimeraClient.colorManager.getColor().getRGB(), 1.0F
         );
      }
   }

   public int getMouseX() {
      return (int)(mc.mouse.getX() / mc.getWindow().getScaleFactor());
   }

   public int getMouseY() {
      return (int)(mc.mouse.getY() / mc.getWindow().getScaleFactor());
   }

   public void setBounds(float x, float y, float width, float height) {
      this.width = width;
      this.height = height;
      this.pos.getValue().x = x / mc.getWindow().getScaledWidth();
      this.pos.getValue().y = y / mc.getWindow().getScaledHeight();
   }

   public boolean isHovering() {
      float x = this.getX();
      float y = this.getY();
      int mouseX = this.getMouseX();
      int mouseY = this.getMouseY();
      return mouseX >= x - 1.0F && mouseX <= x + this.width + 1.0F && mouseY >= y - 1.0F && mouseY <= y + this.height + 1.0F;
   }

   public float getWidth() {
      return this.width;
   }

   public float getHeight() {
      return this.height;
   }

   public void setWidth(float width) {
      this.width = width;
   }

   public void setHeight(float height) {
      this.height = height;
   }
}
