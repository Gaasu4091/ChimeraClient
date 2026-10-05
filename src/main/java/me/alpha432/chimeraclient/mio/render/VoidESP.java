package me.alpha432.chimeraclient.mio.render;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import me.alpha432.chimeraclient.event.impl.entity.player.TickEvent;
import me.alpha432.chimeraclient.event.impl.render.Render3DEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.mio.MioRender;
import me.alpha432.chimeraclient.mio.support.SearchSupport;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class VoidESP extends Module {
   public final Setting<Float> lineWidth = this.num("LineWidth", 1.0F, 0.1F, 3.0F);
   public final Setting<Float> height = this.num("Height", 0.1F, -1.0F, 1.0F);
   public final Setting<Integer> radius = this.num("Radius", 10, 1, 16);
   public final Setting<Boolean> fade = this.bool("Fade", true);
   public final Setting<Float> fadeRadius = this.num("FadeRadius", 7.0F, 1.0F, 16.0F);
   public final Setting<Boolean> colors = this.bool("Colors", true);
   public final Setting<Color> fill = this.color("Fill", 255, 0, 0, 48);
   public final Setting<Color> line = this.color("Line", 255, 0, 0, 187);
   private final List<BlockPos> list = new CopyOnWriteArrayList<>();

   public VoidESP() {
      super("VoidESP", "Highlights void blocks.", Module.Category.RENDER);
      this.fadeRadius.setVisibility(var1 -> this.fade.getValue());
      this.fill.setVisibility(var1 -> this.colors.getValue());
      this.line.setVisibility(var1 -> this.colors.getValue());
   }

   @Override
   public void onDisable() {
      this.list.clear();
   }

   @Override
   public String getDisplayInfo() {
      return String.valueOf(this.list.size());
   }

   @Subscribe
   public void onPlayerTick(TickEvent var1) {
      if (!nullCheck()) {
         this.list.removeIf(var0 -> SearchSupport.block(var0) == Blocks.BEDROCK);
         int var2 = mc.world.getBottomY();

         for (BlockPos var4 : SearchSupport.sphere(MioRender.camera(), this.radius.getValue().intValue())) {
            if (var4.getY() == var2 && SearchSupport.block(var4) != Blocks.BEDROCK && !this.list.contains(var4)) {
               this.list.add(var4);
            }
         }
      }
   }

   @Subscribe
   public void onRenderWorld(Render3DEvent var1) {
      if (!nullCheck()) {
         Vec3d var2 = MioRender.camera();
         Frustum var3 = SearchSupport.frustum();

         for (BlockPos var5 : this.list) {
            if (var3.isVisible(new Box(var5))) {
               if (!var5.isWithinDistance(var2, this.radius.getValue().intValue())) {
                  this.list.remove(var5);
               } else {
                  Color var6 = this.fill.getValue();
                  Color var7 = this.line.getValue();
                  double var8 = var2.distanceTo(var5.toCenterPos());
                  if (this.fade.getValue() && var8 >= this.fadeRadius.getValue().floatValue()) {
                     float var10 = SearchSupport.fade(var8, this.fadeRadius.getValue(), this.radius.getValue().intValue());
                     var6 = MioRender.alpha(var6, (int)(var10 * var6.getAlpha()));
                     var7 = MioRender.alpha(var7, (int)(var10 * var7.getAlpha()));
                  }

                  Box var11 = new Box(var5).withMaxY(var5.getY() + this.height.getValue());
                  MioRender.fill(var1.getMatrix(), var11, var6);
                  MioRender.outline(var1.getMatrix(), var11, var7, this.lineWidth.getValue());
               }
            }
         }
      }
   }
}
