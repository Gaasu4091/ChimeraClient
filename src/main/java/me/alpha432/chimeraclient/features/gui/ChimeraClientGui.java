package me.alpha432.chimeraclient.features.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.features.Feature;
import me.alpha432.chimeraclient.features.gui.items.Item;
import me.alpha432.chimeraclient.features.gui.items.buttons.ModuleButton;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.util.render.RenderUtil;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public class ChimeraClientGui extends Screen {
   private static ChimeraClientGui INSTANCE;
   private static Color colorClipboard;
   private final ArrayList<Widget> widgets = new ArrayList<>();

   public ChimeraClientGui() {
      super(Text.literal("ChimeraClient"));
      INSTANCE = this;
      this.load();
   }

   public static ChimeraClientGui getInstance() {
      return INSTANCE == null ? new ChimeraClientGui() : INSTANCE;
   }

   public static ChimeraClientGui getClickGui() {
      return getInstance();
   }

   private void load() {
      int x = 10;

      for (Module.Category category : ChimeraClient.moduleManager.getCategories()) {
         if (category != Module.Category.HUD) {
            Widget panel = new Widget(category.getName(), x, 14, true);
            ChimeraClient.moduleManager
               .stream()
               .filter(module -> module.getCategory() == category && !module.hidden)
               .map(ModuleButton::new)
               .forEach(panel::addButton);
            panel.getItems().sort(Comparator.comparing(Feature::getName));
            this.widgets.add(panel);
            x += panel.getWidth() + 4;
         }
      }
   }

   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      Item.context = context;
      context.fill(0, 0, this.width, this.height, GuiTheme.withOpacity(new Color(7, 8, 14, 150)).getRGB());
      RenderUtil.verticalGradient(
         context, 0.0F, 0.0F, (float)this.width, 34.0F, GuiTheme.withOpacity(new Color(0, 0, 0, 105)), GuiTheme.withOpacity(new Color(0, 0, 0, 0))
      );
      RenderUtil.verticalGradient(
         context,
         0.0F,
         (float)(this.height - 28),
         (float)this.width,
         (float)this.height,
         GuiTheme.withOpacity(new Color(0, 0, 0, 0)),
         GuiTheme.withOpacity(new Color(0, 0, 0, 80))
      );
      this.widgets.forEach(widget -> widget.drawScreen(context, mouseX, mouseY, deltaTicks));
   }

   public boolean mouseClicked(Click click, boolean doubled) {
      this.widgets.forEach(widget -> widget.mouseClicked((int)click.x(), (int)click.y(), click.button()));
      return super.mouseClicked(click, doubled);
   }

   public boolean mouseReleased(Click click) {
      this.widgets.forEach(widget -> widget.mouseReleased((int)click.x(), (int)click.y(), click.button()));
      return super.mouseReleased(click);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (verticalAmount != 0.0) {
         this.widgets.forEach(widget -> widget.setY(widget.getY() + (verticalAmount > 0.0 ? 10 : -10)));
      }

      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public boolean keyPressed(KeyInput input) {
      this.widgets.forEach(widget -> widget.onKeyPressed(input.key()));
      return super.keyPressed(input);
   }

   public boolean charTyped(CharInput input) {
      this.widgets.forEach(widget -> widget.onKeyTyped(input.asString(), input.modifiers()));
      return super.charTyped(input);
   }

   public boolean shouldPause() {
      return false;
   }

   public final ArrayList<Widget> getComponents() {
      return this.widgets;
   }

   public int getTextOffset() {
      return -6;
   }

   public static Color getColorClipboard() {
      return colorClipboard;
   }

   public static void setColorClipboard(Color color) {
      colorClipboard = color;
   }
}
