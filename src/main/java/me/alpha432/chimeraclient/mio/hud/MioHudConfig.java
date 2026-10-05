package me.alpha432.chimeraclient.mio.hud;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import me.alpha432.chimeraclient.features.gui.HudEditorScreen;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import net.minecraft.util.math.MathHelper;

public final class MioHudConfig extends Module {
   public static MioHudConfig INSTANCE;
   public final Setting<Boolean> hudEditor = this.bool("HudEditor", false);
   public final Setting<MioHudConfig.Icons> icons = this.mode("Icons", MioHudConfig.Icons.MOVE);
   public final Setting<Boolean> smoothWidth = this.bool("SmoothWidth", false);
   public final Setting<Integer> safeX = this.num("SafeX", 1, 0, 20);
   public final Setting<Integer> safeY = this.num("SafeY", 1, 0, 10);
   public final Setting<Boolean> colors = this.bool("Colors", true);
   public final Setting<Color> color = this.color("Color", new Color(189, 153, 255, 255));
   public final Setting<Color> second = this.color("SecondColor", new Color(105, 86, 143, 255));
   public final Setting<MioHudConfig.Fade> fade = this.mode("Fade", MioHudConfig.Fade.NONE);
   public final Setting<Integer> offset = this.num("FadeOffset", 5, 1, 30);
   public final Setting<Integer> amount = this.num("ColorAmount", 5, 2, 9);
   public final Setting<Integer> speed = this.num("FadeSpeed", 50, 20, 100);
   public final Setting<Boolean> mix = this.bool("MixColors", true);
   public final List<Setting<Color>> palette = new ArrayList<>();

   public MioHudConfig() {
      super("HUD", "Mio HUD colors, anchors and layout", Module.Category.CLIENT);
      INSTANCE = this;
      this.setDrawn(false);

      for (int var1 = 0; var1 < 9; var1++) {
         int var2 = var1;
         Setting var3 = this.color("Color" + (var1 + 1), Color.WHITE);
         var3.setVisibility(var2x -> this.colors.getValue() && this.fade.getValue() == MioHudConfig.Fade.RAW && var2 < this.amount.getValue());
         this.palette.add(var3);
      }

      this.color.setVisibility(var1x -> this.colors.getValue());
      this.fade.setVisibility(var1x -> this.colors.getValue());
      this.offset.setVisibility(var1x -> this.colors.getValue() && this.fade.getValue() != MioHudConfig.Fade.NONE);
      this.second.setVisibility(var1x -> this.colors.getValue() && this.fade.getValue() == MioHudConfig.Fade.PULSE);
      this.amount.setVisibility(var1x -> this.colors.getValue() && this.fade.getValue() == MioHudConfig.Fade.RAW);
      this.speed.setVisibility(var1x -> this.colors.getValue() && this.fade.getValue() == MioHudConfig.Fade.RAW);
      this.mix.setVisibility(var1x -> this.colors.getValue() && this.fade.getValue() == MioHudConfig.Fade.RAW);
      this.enabled.setValue(true);
   }

   @Override
   public void onTick() {
      if (this.hudEditor.getValue()) {
         this.hudEditor.setValue(false);
         mc.setScreen(HudEditorScreen.getInstance());
      }
   }

   public int color(float var1) {
      if (!this.colors.getValue()) {
         return -1;
      } else {
         Color var2 = this.color.getValue();
         Color var3 = this.second.getValue();
         if (this.fade.getValue() == MioHudConfig.Fade.NONE) {
            return var2.getRGB();
         } else {
            double var4 = (double)System.currentTimeMillis() / (1200 - this.speed.getValue() * 8)
               + var1 / Math.max(1.0, this.offset.getValue().intValue() * 10.0);
            if (this.fade.getValue() == MioHudConfig.Fade.PULSE) {
               return blend(var2, var3, (float)((Math.sin(var4) + 1.0) * 0.5)).getRGB();
            } else {
               double var6 = (var4 % this.amount.getValue().intValue() + this.amount.getValue().intValue()) % this.amount.getValue().intValue();
               int var8 = (int)var6;
               var2 = this.palette.get(var8).getValue();
               var3 = this.palette.get((var8 + 1) % this.amount.getValue()).getValue();
               return (this.mix.getValue() ? blend(var2, var3, (float)(var6 - var8)) : var2).getRGB();
            }
         }
      }
   }

   private static Color blend(Color var0, Color var1, float var2) {
      var2 = MathHelper.clamp(var2, 0.0F, 1.0F);
      return new Color(
         (int)(var0.getRed() * (1.0F - var2) + var1.getRed() * var2),
         (int)(var0.getGreen() * (1.0F - var2) + var1.getGreen() * var2),
         (int)(var0.getBlue() * (1.0F - var2) + var1.getBlue() * var2),
         (int)(var0.getAlpha() * (1.0F - var2) + var1.getAlpha() * var2)
      );
   }

   public static enum Fade {
      NONE,
      PULSE,
      RAW;
   }

   public static enum Icons {
      KEEP,
      MOVE,
      HIDE;
   }
}
