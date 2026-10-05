package me.alpha432.chimeraclient.features.modules.render;

import java.awt.Color;
import me.alpha432.chimeraclient.event.impl.render.Render3DEvent;
import me.alpha432.chimeraclient.event.impl.render.RenderBlockOutlineEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.util.render.RenderUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

public class BlockHighlightModule extends Module {
   public Setting<Color> color = this.color("Color", 255, 0, 0, 255);
   public Setting<Float> lineWidth = this.num("LineWidth", 1.0F, 0.1F, 5.0F);

   public BlockHighlightModule() {
      super("BlockHighlight", "Draws box at the block that you are looking at", Module.Category.RENDER);
   }

   @Subscribe
   @Override
   public void onRender3D(Render3DEvent event) {
      if (mc.crosshairTarget instanceof BlockHitResult result) {
         VoxelShape shape = mc.world.getBlockState(result.getBlockPos()).getOutlineShape(mc.world, result.getBlockPos());
         if (shape.isEmpty() || result.getType() == Type.MISS) {
            return;
         }

         Box box = shape.getBoundingBox();
         box = box.offset(result.getBlockPos());
         RenderUtil.drawBox(event.getMatrix(), box, this.color.getValue(), this.lineWidth.getValue());
      }
   }

   @Subscribe
   public void onRenderBlockOutline(RenderBlockOutlineEvent event) {
      event.cancel();
   }
}
