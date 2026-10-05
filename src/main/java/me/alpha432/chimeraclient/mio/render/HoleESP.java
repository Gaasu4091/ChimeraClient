package me.alpha432.chimeraclient.mio.render;

import java.awt.Color;
import me.alpha432.chimeraclient.event.impl.render.Render3DEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.mio.MioRender;
import me.alpha432.chimeraclient.mio.support.HoleESPSupport;
import me.alpha432.chimeraclient.mio.support.SearchSupport;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class HoleESP extends Module {
   public final Setting<HoleESP.HoleESPMode> fill = this.mode("Fill", HoleESP.HoleESPMode.GRADIENT);
   public final Setting<Float> lineWidth = this.num("LineWidth", 1.0F, 0.1F, 3.0F);
   public final Setting<Float> height = this.num("Height", 0.0F, -1.0F, 1.0F);
   public final Setting<Integer> radius = this.num("Radius", 10, 1, 32);
   public final Setting<Boolean> hideOwn = this.bool("HideOwn", false);
   public final Setting<Boolean> fade = this.bool("Fade", false);
   public final Setting<Float> fadeRadius = this.num("FadeRadius", 7.0F, 1.0F, 16.0F);
   public final Setting<Boolean> safe = this.bool("Safe", true);
   public final Setting<Color> safeFill = this.color("SafeFill", 53, 113, 32, 32);
   public final Setting<Color> safeOutline = this.color("SafeOutline", 38, 134, 55, 194);
   public final Setting<Boolean> unsafe = this.bool("Unsafe", true);
   public final Setting<Color> unsafeFill = this.color("UnsafeFill", 206, 0, 0, 37);
   public final Setting<Color> unsafeOutline = this.color("UnsafeOutline", 206, 0, 0, 191);
   public final Setting<Boolean> trapped = this.bool("Trapped", false);
   public final Setting<Color> trappedFill = this.color("TrappedFill", 236, 205, 0, 13);
   public final Setting<Color> trappedOutline = this.color("TrappedOutline", 255, 242, 0, 191);

   public HoleESP() {
      super("HoleESP", "Highlights the spots that are safe from end crystals.", Module.Category.RENDER);
      this.fadeRadius.setVisibility(var1 -> this.fade.getValue());
      this.safeFill.setVisibility(var1 -> this.safe.getValue());
      this.safeOutline.setVisibility(var1 -> this.safe.getValue());
      this.unsafeFill.setVisibility(var1 -> this.unsafe.getValue());
      this.unsafeOutline.setVisibility(var1 -> this.unsafe.getValue());
      this.trappedFill.setVisibility(var1 -> this.trapped.getValue());
      this.trappedOutline.setVisibility(var1 -> this.trapped.getValue());
   }

   @Override
   public void onEnable() {
      HoleESPSupport.acquire();
   }

   @Override
   public void onDisable() {
      HoleESPSupport.release();
   }

   @Override
   public String getDisplayInfo() {
      return String.valueOf(HoleESPSupport.holes().size());
   }

   @Subscribe
   public void onRenderWorld(Render3DEvent var1) {
      if (!nullCheck()) {
         MatrixStack var2 = var1.getMatrix();
         double var3 = MathHelper.lerp(var1.getDelta(), mc.player.lastRenderY, mc.player.getY());
         Vec3d var5 = MioRender.camera();
         Box var6 = new Box(HoleESPSupport.feet(mc.player));
         Frustum var7 = SearchSupport.frustum();

         for (HoleESPSupport.Hole var9 : HoleESPSupport.holes()) {
            if (var7.isVisible(var9.box())
               && (this.trapped.getValue() || !var9.trapped())
               && (var9.trapped() || (var9.mode() == HoleESPSupport.Mode.UNSAFE ? this.unsafe : this.safe).getValue())
               && var9.pos().isWithinDistance(var5, this.radius.getValue().intValue())
               && (!mc.player.getBoundingBox().intersects(var9.box()) || !this.hideOwn.getValue() || this.fade.getValue())) {
               Color var10 = this.safeFill.getValue();
               Color var11 = this.safeOutline.getValue();
               if (var9.trapped()) {
                  var10 = this.trappedFill.getValue();
                  var11 = this.trappedOutline.getValue();
               } else if (var9.mode() == HoleESPSupport.Mode.UNSAFE) {
                  var10 = this.unsafeFill.getValue();
                  var11 = this.unsafeOutline.getValue();
               }

               double var12 = var5.distanceTo(var9.pos().toCenterPos());
               boolean var14 = var9.box().intersects(var6) && this.hideOwn.getValue();
               if (this.fade.getValue() && (var12 >= this.fadeRadius.getValue().floatValue() || var14)) {
                  float var15 = SearchSupport.fade(var12, this.fadeRadius.getValue(), this.radius.getValue().intValue());
                  if (var14) {
                     var15 = (float)(var3 - Math.floor(var3));
                  }

                  var10 = MioRender.alpha(var10, (int)(var15 * var10.getAlpha()));
                  var11 = MioRender.alpha(var11, (int)(var15 * var11.getAlpha()));
               }

               Box var16 = var9.box().withMaxY(var9.pos().getY() + this.height.getValue());
               switch ((HoleESP.HoleESPMode)this.fill.getValue()) {
                  case SOLID:
                     MioRender.fill(var2, var16, var10);
                     break;
                  case GRADIENT:
                     MioRender.gradient(var2, var9.box().withMaxY(var9.pos().getY() + 1), var10);
               }

               MioRender.outline(var2, var16, var11, this.lineWidth.getValue());
            }
         }
      }
   }

   public static enum HoleESPMode {
      NONE,
      SOLID,
      GRADIENT;
   }
}
