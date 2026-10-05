package me.alpha432.chimeraclient.mio.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.mio.render.ViewModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public final class ViewModelSupport {
   private ViewModelSupport() {
   }

   public static final class AdjustScreen extends Screen {
      private static final int GUIDE_COLOR = 1694498815;
      private static final int TEXT_COLOR = -1258291201;
      private static final List<String> HELP = List.of(
         "Left Click - Rotate",
         "Right Click - Move",
         "Middle Click - Scale",
         "Mouse Scroll - Move by Z",
         "Mouse Scroll + Shift - Rotate by Z",
         "Mouse Scroll + Alt - Scale by Z",
         "Hold Ctrl - Modify one",
         "Hold Shift - Lock axis",
         "Ctrl + Z - Undo",
         "Delete - Reset to defaults"
      );
      private final ViewModel viewModel;
      private final LinkedList<ViewModelSupport.Snapshot> history = new LinkedList<>();
      private ViewModelSupport.Snapshot start;
      private ViewModelSupport.Mode mode = ViewModelSupport.Mode.NONE;
      private double startX;
      private double startY;
      private boolean leftSide;
      private long lastScroll;

      public AdjustScreen(ViewModel var1) {
         super(Text.literal("mio"));
         this.viewModel = var1;
         this.start = ViewModelSupport.Snapshot.of(var1);
         this.history.add(this.start);
      }

      public boolean shouldPause() {
         return false;
      }

      public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      }

      public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
         int var5 = context.getScaledWindowWidth();
         int var6 = context.getScaledWindowHeight();
         context.fill(var5 / 2 - 1, 0, var5 / 2 + 1, 50, 1694498815);
         int var7 = 2;

         for (String var9 : HELP) {
            context.drawText(this.textRenderer, var9, (var5 - this.textRenderer.getWidth(var9)) / 2, 50 + var7, -1258291201, true);
            var7 += 9 + 1;
         }

         context.fill(var5 / 2 - 1, var7 + 52, var5 / 2 + 1, var6, 1694498815);
         this.drag(mouseX, mouseY);
      }

      private void drag(int var1, int var2) {
         if (this.mode != ViewModelSupport.Mode.NONE) {
            ViewModelSupport.Snapshot var3 = this.start;
            ViewModel var4 = this.viewModel;
            float var5 = (float)floor2((this.startX - var1) * 0.005) * (this.leftSide ? -1 : 1);
            float var6 = (float)floor2((this.startY - var2) * 0.005);
            if (shift()) {
               if (Math.abs(var5) > Math.abs(var6)) {
                  var6 = 0.0F;
               } else {
                  var5 = 0.0F;
               }
            }

            switch (this.mode) {
               case ROTATE:
                  float var9 = var5 / 0.005F;
                  float var8 = var6 / 0.005F;
                  if (!ctrl() || this.leftSide) {
                     var4.offRotateX.setValue(wrap(var3.offRotateX() + var8));
                     var4.offRotateY.setValue(wrap(var3.offRotateY() + var9));
                  }

                  if (!ctrl() || !this.leftSide) {
                     var4.mainRotateX.setValue(wrap(var3.mainRotateX() + var8));
                     var4.mainRotateY.setValue(wrap(var3.mainRotateY() + var9));
                  }
                  break;
               case TRANSLATE:
                  if (!ctrl() || this.leftSide) {
                     var4.offX.setValue(var3.offX() + var5);
                     var4.offY.setValue(var3.offY() + var6);
                  }

                  if (!ctrl() || !this.leftSide) {
                     var4.mainX.setValue(var3.mainX() + var5);
                     var4.mainY.setValue(var3.mainY() + var6);
                  }
                  break;
               case SCALE:
                  if (shift()) {
                     var5 = Math.abs(Math.max(var5, var6)) * Math.signum(var5);
                     var6 = Math.abs(Math.max(var5, var6)) * Math.signum(var6);
                  }

                  float var7 = -var5;
                  if (!ctrl() || this.leftSide) {
                     var4.offScaleX.setValue(var3.offScaleX() + var7);
                     var4.offScaleY.setValue(var3.offScaleY() + var6);
                  }

                  if (!ctrl() || !this.leftSide) {
                     var4.mainScaleX.setValue(var3.mainScaleX() + var7);
                     var4.mainScaleY.setValue(var3.mainScaleY() + var6);
                  }
            }
         }
      }

      public boolean mouseClicked(Click click, boolean doubled) {
         boolean var3 = super.mouseClicked(click, doubled);

         this.mode = switch (click.button()) {
            case 0 -> ViewModelSupport.Mode.ROTATE;
            case 1 -> ViewModelSupport.Mode.TRANSLATE;
            case 2 -> ViewModelSupport.Mode.SCALE;
            default -> ViewModelSupport.Mode.NONE;
         };
         if (this.mode != ViewModelSupport.Mode.NONE) {
            this.start = ViewModelSupport.Snapshot.of(this.viewModel);
            this.history.add(this.start);
            this.leftSide = click.x() < this.width / 2.0;
            this.startX = click.x();
            this.startY = click.y();
         }

         return var3;
      }

      public boolean mouseReleased(Click click) {
         this.mode = ViewModelSupport.Mode.NONE;
         return super.mouseReleased(click);
      }

      public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
         if (verticalAmount > -1.0 && verticalAmount < 1.0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
         } else {
            boolean var9 = mouseX < this.width / 2.0;
            float var10 = (float)(verticalAmount * -0.1);
            long var11 = System.currentTimeMillis();
            if (var11 - this.lastScroll >= 100L) {
               this.history.add(ViewModelSupport.Snapshot.of(this.viewModel));
            }

            this.lastScroll = var11;
            ViewModel var13 = this.viewModel;
            if (!shift()) {
               if (alt()) {
                  nudge(var13.mainScaleZ, var13.offScaleZ, var9, var10);
               } else {
                  nudge(var13.mainZ, var13.offZ, var9, var10);
               }

               return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            } else {
               float var14 = wrap(var13.offRotateZ.getValue() + (float)verticalAmount * 10.0F);
               float var15 = wrap(var13.mainRotateZ.getValue() + (float)verticalAmount * 10.0F);
               if (!ctrl()) {
                  var13.offRotateZ.setValue(var14);
                  var13.mainRotateZ.setValue(var15);
               } else if (var9) {
                  var13.offRotateZ.setValue(var14);
               } else {
                  var13.mainRotateZ.setValue(var15);
               }

               return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            }
         }
      }

      public boolean keyPressed(KeyInput input) {
         if (ctrl() && input.key() == 90 && !this.history.isEmpty()) {
            this.history.getLast().apply(this.viewModel);
            if (this.history.size() != 1) {
               this.history.removeLast();
            }
         }

         if (input.key() == 261) {
            this.start = ViewModelSupport.Snapshot.DEFAULTS;
            this.start.apply(this.viewModel);
            this.history.add(this.start);
         }

         return super.keyPressed(input);
      }

      private static void nudge(Setting<Float> var0, Setting<Float> var1, boolean var2, float var3) {
         if (!ctrl()) {
            var0.setValue((Float)var0.getValue() + var3);
            var1.setValue((Float)var1.getValue() + var3);
         } else if (var2) {
            var1.setValue((Float)var1.getValue() + var3);
         } else {
            var0.setValue((Float)var0.getValue() + var3);
         }
      }

      private static double floor2(double var0) {
         return BigDecimal.valueOf(var0).setScale(2, RoundingMode.FLOOR).doubleValue();
      }

      private static float wrap(float var0) {
         return (var0 < 0.0F ? var0 + 360.0F : var0) % 360.0F;
      }

      private static boolean ctrl() {
         return key(341);
      }

      private static boolean shift() {
         return key(340);
      }

      private static boolean alt() {
         return key(342);
      }

      private static boolean key(int var0) {
         return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), var0);
      }
   }

   private static enum Mode {
      NONE,
      ROTATE,
      TRANSLATE,
      SCALE;
   }

   private record Snapshot(
      float mainX,
      float mainY,
      float mainZ,
      float offX,
      float offY,
      float offZ,
      float mainScaleX,
      float mainScaleY,
      float mainScaleZ,
      float offScaleX,
      float offScaleY,
      float offScaleZ,
      float mainRotateX,
      float mainRotateY,
      float mainRotateZ,
      float offRotateX,
      float offRotateY,
      float offRotateZ
   ) {
      static final ViewModelSupport.Snapshot DEFAULTS = new ViewModelSupport.Snapshot(
         0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F
      );

      static ViewModelSupport.Snapshot of(ViewModel var0) {
         return new ViewModelSupport.Snapshot(
            var0.mainX.getValue(),
            var0.mainY.getValue(),
            var0.mainZ.getValue(),
            var0.offX.getValue(),
            var0.offY.getValue(),
            var0.offZ.getValue(),
            var0.mainScaleX.getValue(),
            var0.mainScaleY.getValue(),
            var0.mainScaleZ.getValue(),
            var0.offScaleX.getValue(),
            var0.offScaleY.getValue(),
            var0.offScaleZ.getValue(),
            var0.mainRotateX.getValue(),
            var0.mainRotateY.getValue(),
            var0.mainRotateZ.getValue(),
            var0.offRotateX.getValue(),
            var0.offRotateY.getValue(),
            var0.offRotateZ.getValue()
         );
      }

      void apply(ViewModel var1) {
         var1.mainX.setValue(this.mainX);
         var1.mainY.setValue(this.mainY);
         var1.mainZ.setValue(this.mainZ);
         var1.offX.setValue(this.offX);
         var1.offY.setValue(this.offY);
         var1.offZ.setValue(this.offZ);
         var1.mainScaleX.setValue(this.mainScaleX);
         var1.mainScaleY.setValue(this.mainScaleY);
         var1.mainScaleZ.setValue(this.mainScaleZ);
         var1.offScaleX.setValue(this.offScaleX);
         var1.offScaleY.setValue(this.offScaleY);
         var1.offScaleZ.setValue(this.offScaleZ);
         var1.mainRotateX.setValue(this.mainRotateX);
         var1.mainRotateY.setValue(this.mainRotateY);
         var1.mainRotateZ.setValue(this.mainRotateZ);
         var1.offRotateX.setValue(this.offRotateX);
         var1.offRotateY.setValue(this.offRotateY);
         var1.offRotateZ.setValue(this.offRotateZ);
      }
   }
}
