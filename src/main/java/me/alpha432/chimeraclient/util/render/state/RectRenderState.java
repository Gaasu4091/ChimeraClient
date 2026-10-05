package me.alpha432.chimeraclient.util.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

public record RectRenderState(
   RenderPipeline pipeline,
   TextureSetup textureSetup,
   Matrix3x2f pose,
   int x0,
   int y0,
   int x1,
   int y1,
   int topLeft,
   int bottomLeft,
   int bottomRight,
   int topRight,
   @Nullable ScreenRect scissorArea,
   @Nullable ScreenRect bounds
) implements SimpleGuiElementRenderState {
   public RectRenderState(
      RenderPipeline renderPipeline,
      TextureSetup textureSetup,
      Matrix3x2f matrix3x2f,
      int x0,
      int y0,
      int x1,
      int y1,
      int topLeft,
      int bottomLeft,
      int bottomRight,
      int topRight,
      @Nullable ScreenRect screenRectangle
   ) {
      this(
         renderPipeline,
         textureSetup,
         matrix3x2f,
         x0,
         y0,
         x1,
         y1,
         topLeft,
         bottomLeft,
         bottomRight,
         topRight,
         screenRectangle,
         getBounds(x0, y0, x1, y1, matrix3x2f, screenRectangle)
      );
   }

   public void setupVertices(VertexConsumer vertices) {
      vertices.vertex(this.pose(), this.x0(), this.y0()).color(this.topLeft());
      vertices.vertex(this.pose(), this.x0(), this.y1()).color(this.bottomLeft());
      vertices.vertex(this.pose(), this.x1(), this.y1()).color(this.bottomRight());
      vertices.vertex(this.pose(), this.x1(), this.y0()).color(this.topRight());
   }

   @Nullable
   private static ScreenRect getBounds(int i, int j, int k, int l, Matrix3x2f matrix3x2f, @Nullable ScreenRect screenRectangle) {
      ScreenRect screenRectangle2 = new ScreenRect(i, j, k - i, l - j).transformEachVertex(matrix3x2f);
      return screenRectangle != null ? screenRectangle.intersection(screenRectangle2) : screenRectangle2;
   }
}
