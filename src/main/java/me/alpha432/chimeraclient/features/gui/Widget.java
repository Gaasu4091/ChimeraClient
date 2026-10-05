package me.alpha432.chimeraclient.features.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import me.alpha432.chimeraclient.features.Feature;
import me.alpha432.chimeraclient.features.gui.font.FontDraw;
import me.alpha432.chimeraclient.features.gui.items.Item;
import me.alpha432.chimeraclient.features.gui.items.buttons.Button;
import me.alpha432.chimeraclient.util.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class Widget extends Feature {
   private static final int HEADER_HEIGHT = 15;
   private static final int PANEL_WIDTH = 104;
   protected DrawContext context;
   private final List<Item> items = new ArrayList<>();
   public boolean drag;
   private int x;
   private int y;
   private int dragX;
   private int dragY;
   private int width = 104;
   private int height = 15;
   private boolean open;
   private boolean hidden;

   public Widget(String name, int x, int y, boolean open) {
      super(name);
      this.x = x;
      this.y = y;
      this.open = open;
   }

   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      this.context = context;
      if (this.drag) {
         this.x = mouseX + this.dragX;
         this.y = mouseY + this.dragY;
      }

      int bodyBottom = this.y + 15 + (this.open ? this.getTotalItemHeight() : 0);
      if (this.open) {
         RenderUtil.rect(context, (float)this.x, (float)(this.y + 15), (float)(this.x + this.width), (float)bodyBottom, GuiTheme.background().getRGB());
         RenderUtil.rect(
            context, (float)this.x, (float)(this.y + 15), (float)(this.x + 1), (float)bodyBottom, GuiTheme.withOpacity(new Color(72, 72, 72, 210)).getRGB()
         );
         RenderUtil.rect(
            context,
            (float)(this.x + this.width - 1),
            (float)(this.y + 15),
            (float)(this.x + this.width),
            (float)bodyBottom,
            GuiTheme.withOpacity(new Color(72, 72, 72, 210)).getRGB()
         );
         RenderUtil.rect(
            context,
            (float)this.x,
            (float)(bodyBottom - 1),
            (float)(this.x + this.width),
            (float)bodyBottom,
            GuiTheme.withOpacity(new Color(72, 72, 72, 210)).getRGB()
         );
      }

      float var10001 = this.x;
      float var10002 = this.y;
      float var10003 = this.x + this.width;
      float var10004 = this.y + 15;
      new Color(24, 24, 24, 240);
      RenderUtil.rect(context, var10001, var10002, var10003, var10004, GuiTheme.withOpacity(HeaderSettings.color()).getRGB());
      RenderUtil.rect(context, (float)this.x, (float)this.y, (float)(this.x + this.width), (float)(this.y + 1), GuiTheme.text().getRGB());
      RenderUtil.rect(
         context,
         (float)this.x,
         (float)(this.y + 15 - 1),
         (float)(this.x + this.width),
         (float)(this.y + 15),
         GuiTheme.withOpacity(new Color(72, 72, 72, 210)).getRGB()
      );
      FontDraw.drawText(context, MinecraftClient.getInstance().textRenderer, this.getName(), this.x + 5, this.y + 4, GuiTheme.text().getRGB(), false);
      String var9 = this.open ? "−" : "+";
      int var10 = this.x + this.width - 11;
      FontDraw.drawText(context, MinecraftClient.getInstance().textRenderer, var9, var10, this.y + 4, GuiTheme.text().getRGB(), false);
      if (this.open) {
         float itemY = this.y + 15 + 2;

         for (Item item : this.items) {
            item.update();
            if (!item.isHidden()) {
               item.setLocation(this.x + 2, itemY);
               item.setWidth(this.width - 4);
               item.drawScreen(context, mouseX, mouseY, partialTicks);
               itemY += item.getHeight() + 1;
            }
         }
      }
   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (this.isHovering(mouseX, mouseY)) {
         if (mouseButton == 0) {
            this.dragX = this.x - mouseX;
            this.dragY = this.y - mouseY;
            ChimeraClientGui.getClickGui().getComponents().forEach(panel -> panel.drag = false);
            this.drag = true;
         } else if (mouseButton == 1) {
            this.open = !this.open;
         }
      } else {
         if (this.open) {
            this.items.forEach(item -> item.mouseClicked(mouseX, mouseY, mouseButton));
         }
      }
   }

   public void mouseReleased(int mouseX, int mouseY, int button) {
      if (button == 0) {
         this.drag = false;
      }

      if (this.open) {
         this.items.forEach(item -> item.mouseReleased(mouseX, mouseY, button));
      }
   }

   public void onKeyTyped(String typedChar, int modifiers) {
      if (this.open) {
         this.items.forEach(item -> item.onKeyTyped(typedChar, modifiers));
      }
   }

   public void onKeyPressed(int key) {
      if (this.open) {
         this.items.forEach(item -> item.onKeyPressed(key));
      }
   }

   public void addButton(Button button) {
      this.items.add(button);
   }

   public List<Item> getItems() {
      return this.items;
   }

   public int getX() {
      return this.x;
   }

   public void setX(int x) {
      this.x = x;
   }

   public int getY() {
      return this.y;
   }

   public void setY(int y) {
      this.y = y;
   }

   public int getWidth() {
      return this.width;
   }

   public void setWidth(int width) {
      this.width = width;
   }

   public int getHeight() {
      return this.height;
   }

   public void setHeight(int height) {
      this.height = height;
   }

   public boolean isHidden() {
      return this.hidden;
   }

   public void setHidden(boolean hidden) {
      this.hidden = hidden;
   }

   public boolean isOpen() {
      return this.open;
   }

   public boolean isHovering(int mouseX, int mouseY) {
      return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + 15;
   }

   private int getTotalItemHeight() {
      int total = 3;

      for (Item item : this.items) {
         if (!item.isHidden()) {
            total += item.getHeight() + 1;
         }
      }

      return total;
   }
}
