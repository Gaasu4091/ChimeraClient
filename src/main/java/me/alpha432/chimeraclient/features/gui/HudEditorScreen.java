package me.alpha432.chimeraclient.features.gui;

import java.util.ArrayList;
import java.util.Comparator;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.features.Feature;
import me.alpha432.chimeraclient.features.gui.items.buttons.ModuleButton;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.modules.client.HudModule;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public class HudEditorScreen extends Screen {
   private static HudEditorScreen INSTANCE;
   private final ArrayList<Widget> components = new ArrayList<>();
   public HudModule currentDragging;
   public boolean anyHover;

   private HudEditorScreen() {
      super(Text.literal("chimeraclient-hudeditor"));
      this.load();
   }

   private void load() {
      Widget hud = new Widget("Hud", 50, 50, true);
      ChimeraClient.moduleManager.stream().filter(m -> m.getCategory() == Module.Category.HUD && !m.hidden).map(ModuleButton::new).forEach(hud::addButton);
      this.components.add(hud);
      this.components.forEach(component -> component.getItems().sort(Comparator.comparing(Feature::getName)));
   }

   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      this.anyHover = false;
      this.components.forEach(component -> component.drawScreen(context, mouseX, mouseY, deltaTicks));
   }

   public boolean mouseClicked(Click click, boolean doubled) {
      this.components.forEach(component -> component.mouseClicked((int)click.x(), (int)click.y(), click.button()));
      return super.mouseClicked(click, doubled);
   }

   public boolean mouseReleased(Click click) {
      this.components.forEach(component -> component.mouseReleased((int)click.x(), (int)click.y(), click.button()));
      return super.mouseReleased(click);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (verticalAmount < 0.0) {
         this.components.forEach(component -> component.setY(component.getY() - 10));
      } else if (verticalAmount > 0.0) {
         this.components.forEach(component -> component.setY(component.getY() + 10));
      }

      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public boolean keyPressed(KeyInput input) {
      this.components.forEach(component -> component.onKeyPressed(input.getKeycode()));
      return super.keyPressed(input);
   }

   public boolean charTyped(CharInput input) {
      this.components.forEach(component -> component.onKeyTyped(input.asString(), input.modifiers()));
      return super.charTyped(input);
   }

   public boolean shouldPause() {
      return false;
   }

   public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
   }

   public ArrayList<Widget> getComponents() {
      return this.components;
   }

   public static HudEditorScreen getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new HudEditorScreen();
      }

      return INSTANCE;
   }
}
