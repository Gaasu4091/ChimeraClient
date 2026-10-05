package me.alpha432.chimeraclient.util.render;

import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import me.alpha432.chimeraclient.util.render.state.RectRenderState;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Matrix3x2f;

public class RenderUtil implements Util {
   public static void rect(DrawContext context, float x1, float y1, float x2, float y2, int color) {
      int ix1 = Math.round(x1);
      int iy1 = Math.round(y1);
      int ix2 = Math.round(x2);
      int iy2 = Math.round(y2);
      context.fill(ix1, iy1, ix2, iy2, color);
   }

   public static void rect(DrawContext context, float x1, float y1, float x2, float y2, int color, float width) {
      int w = Math.max(1, Math.round(width));
      context.fill(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y1) + w, color);
      context.fill(Math.round(x2) - w, Math.round(y1), Math.round(x2), Math.round(y2), color);
      context.fill(Math.round(x1), Math.round(y2) - w, Math.round(x2), Math.round(y2), color);
      context.fill(Math.round(x1), Math.round(y1), Math.round(x1) + w, Math.round(y2), color);
   }

   public static void horizontalGradient(DrawContext context, float x1, float y1, float x2, float y2, Color left, Color right) {
      int ix1 = Math.round(x1);
      int iy1 = Math.round(y1);
      int ix2 = Math.round(x2);
      int iy2 = Math.round(y2);
      gradient(context, ix1, iy1, ix2, iy2, left.hashCode(), left.hashCode(), right.hashCode(), right.hashCode());
   }

   public static void verticalGradient(DrawContext context, float x1, float y1, float x2, float y2, Color top, Color bottom) {
      int ix1 = Math.round(x1);
      int iy1 = Math.round(y1);
      int ix2 = Math.round(x2);
      int iy2 = Math.round(y2);
      gradient(context, ix1, iy1, ix2, iy2, top.hashCode(), bottom.hashCode(), bottom.hashCode(), top.hashCode());
   }

   public static void gradient(DrawContext graphics, int x1, int y1, int x2, int y2, int topLeft, int bottomLeft, int bottomRight, int topRight) {
      graphics.state
         .addSimpleElement(
            new RectRenderState(
               RenderPipelines.GUI,
               TextureSetup.empty(),
               new Matrix3x2f(graphics.getMatrices()),
               x1,
               y1,
               x2,
               y2,
               topLeft,
               bottomLeft,
               bottomRight,
               topRight,
               graphics.scissorStack.peekLast()
            )
         );
   }

   public static void rect(MatrixStack stack, float x1, float y1, float x2, float y2, int color) {
      rectFilled(stack, x1, y1, x2, y2, color);
   }

   public static void rect(MatrixStack stack, float x1, float y1, float x2, float y2, int color, float width) {
      drawHorizontalLine(stack, x1, x2, y1, color, width);
      drawVerticalLine(stack, x2, y1, y2, color, width);
      drawHorizontalLine(stack, x1, x2, y2, color, width);
      drawVerticalLine(stack, x1, y1, y2, color, width);
   }

   protected static void drawHorizontalLine(MatrixStack matrices, float x1, float x2, float y, int color) {
      if (x2 < x1) {
         float i = x1;
         x1 = x2;
         x2 = i;
      }

      rectFilled(matrices, x1, y, x2 + 1.0F, y + 1.0F, color);
   }

   protected static void drawVerticalLine(MatrixStack matrices, float x, float y1, float y2, int color) {
      if (y2 < y1) {
         float i = y1;
         y1 = y2;
         y2 = i;
      }

      rectFilled(matrices, x, y1 + 1.0F, x + 1.0F, y2, color);
   }

   protected static void drawHorizontalLine(MatrixStack matrices, float x1, float x2, float y, int color, float width) {
      if (x2 < x1) {
         float i = x1;
         x1 = x2;
         x2 = i;
      }

      rectFilled(matrices, x1, y, x2 + width, y + width, color);
   }

   protected static void drawVerticalLine(MatrixStack matrices, float x, float y1, float y2, int color, float width) {
      if (y2 < y1) {
         float i = y1;
         y1 = y2;
         y2 = i;
      }

      rectFilled(matrices, x, y1 + width, x + width, y2, color);
   }

   public static void rectFilled(MatrixStack matrix, float x1, float y1, float x2, float y2, int color) {
      if (x1 < x2) {
         float i = x1;
         x1 = x2;
         x2 = i;
      }

      if (y1 < y2) {
         float i = y1;
         y1 = y2;
         y2 = i;
      }

      float f = (color >> 24 & 0xFF) / 255.0F;
      float g = (color >> 16 & 0xFF) / 255.0F;
      float h = (color >> 8 & 0xFF) / 255.0F;
      float j = (color & 0xFF) / 255.0F;
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x1, y2, 0.0F).color(g, h, j, f);
      bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x2, y2, 0.0F).color(g, h, j, f);
      bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x2, y1, 0.0F).color(g, h, j, f);
      bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x1, y1, 0.0F).color(g, h, j, f);
      Layers.quads().draw(bufferBuilder.end());
   }

   public static void horizontalGradient(MatrixStack matrix, float x1, float y1, float x2, float y2, Color left, Color right) {
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x1, y1, 0.0F)
         .color(left.getRed() / 255.0F, left.getGreen() / 255.0F, left.getBlue() / 255.0F, left.getAlpha() / 255.0F);
      bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x1, y2, 0.0F)
         .color(left.getRed() / 255.0F, left.getGreen() / 255.0F, left.getBlue() / 255.0F, left.getAlpha() / 255.0F);
      bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x2, y2, 0.0F)
         .color(right.getRed() / 255.0F, right.getGreen() / 255.0F, right.getBlue() / 255.0F, right.getAlpha() / 255.0F);
      bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x2, y1, 0.0F)
         .color(right.getRed() / 255.0F, right.getGreen() / 255.0F, right.getBlue() / 255.0F, right.getAlpha() / 255.0F);
      Layers.quads().draw(bufferBuilder.end());
   }

   public static void verticalGradient(MatrixStack matrix, float x1, float y1, float x2, float y2, Color top, Color bottom) {
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x1, y1, 0.0F)
         .color(top.getRed() / 255.0F, top.getGreen() / 255.0F, top.getBlue() / 255.0F, top.getAlpha() / 255.0F);
      bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x1, y2, 0.0F)
         .color(bottom.getRed() / 255.0F, bottom.getGreen() / 255.0F, bottom.getBlue() / 255.0F, bottom.getAlpha() / 255.0F);
      bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x2, y2, 0.0F)
         .color(bottom.getRed() / 255.0F, bottom.getGreen() / 255.0F, bottom.getBlue() / 255.0F, bottom.getAlpha() / 255.0F);
      bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x2, y1, 0.0F)
         .color(top.getRed() / 255.0F, top.getGreen() / 255.0F, top.getBlue() / 255.0F, top.getAlpha() / 255.0F);
      Layers.quads().draw(bufferBuilder.end());
   }

   public static void drawBoxFilled(MatrixStack stack, Box box, Color c) {
      float minX = (float)(box.minX - mc.getEntityRenderDispatcher().camera.getCameraPos().getX());
      float minY = (float)(box.minY - mc.getEntityRenderDispatcher().camera.getCameraPos().getY());
      float minZ = (float)(box.minZ - mc.getEntityRenderDispatcher().camera.getCameraPos().getZ());
      float maxX = (float)(box.maxX - mc.getEntityRenderDispatcher().camera.getCameraPos().getX());
      float maxY = (float)(box.maxY - mc.getEntityRenderDispatcher().camera.getCameraPos().getY());
      float maxZ = (float)(box.maxZ - mc.getEntityRenderDispatcher().camera.getCameraPos().getZ());
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), minX, minY, minZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), maxX, minY, minZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), maxX, minY, maxZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), minX, minY, maxZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), minX, maxY, minZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), minX, maxY, maxZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), maxX, maxY, maxZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), maxX, maxY, minZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), minX, minY, minZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), minX, maxY, minZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), maxX, maxY, minZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), maxX, minY, minZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), maxX, minY, minZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), maxX, maxY, minZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), maxX, maxY, maxZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), maxX, minY, maxZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), minX, minY, maxZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), maxX, minY, maxZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), maxX, maxY, maxZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), minX, maxY, maxZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), minX, minY, minZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), minX, minY, maxZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), minX, maxY, maxZ).color(c.getRGB());
      bufferBuilder.vertex(stack.peek().getPositionMatrix(), minX, maxY, minZ).color(c.getRGB());
      Layers.quads().draw(bufferBuilder.end());
   }

   public static void drawBoxFilled(MatrixStack stack, Vec3d vec, Color c) {
      drawBoxFilled(stack, Box.from(vec), c);
   }

   public static void drawBoxFilled(MatrixStack stack, BlockPos bp, Color c) {
      drawBoxFilled(stack, new Box(bp), c);
   }

   public static void drawBox(MatrixStack stack, Box box, Color c, float lineWidth) {
      drawBox(stack, VoxelShapes.cuboid(box), c, lineWidth);
   }

   public static void drawBox(MatrixStack stack, VoxelShape shape, Color c, float lineWidth) {
      Vec3d camera = mc.getEntityRenderDispatcher().camera.getCameraPos();
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
      Entry pose = stack.peek();
      int color = c.getRGB();
      shape.forEachEdge(
         (x1, y1, z1, x2, y2, z2) -> addLine(
            bufferBuilder, pose, color, lineWidth, x1 - camera.x, y1 - camera.y, z1 - camera.z, x2 - camera.x, y2 - camera.y, z2 - camera.z
         )
      );
      Layers.lines().draw(bufferBuilder.end());
   }

   private static void addLine(BufferBuilder buf, Entry pose, int color, float lineWidth, double x1, double y1, double z1, double x2, double y2, double z2) {
      float nx = (float)(x2 - x1);
      float ny = (float)(y2 - y1);
      float nz = (float)(z2 - z1);
      buf.vertex(pose, (float)x1, (float)y1, (float)z1).color(color).normal(pose, nx, ny, nz).lineWidth(lineWidth);
      buf.vertex(pose, (float)x2, (float)y2, (float)z2).color(color).normal(pose, nx, ny, nz).lineWidth(lineWidth);
   }

   public static void drawBox(MatrixStack stack, Vec3d vec, Color c, float lineWidth) {
      drawBox(stack, Box.from(vec), c, lineWidth);
   }

   public static void drawBox(MatrixStack stack, BlockPos bp, Color c, float lineWidth) {
      drawBox(stack, new Box(bp), c, lineWidth);
   }

   public static MatrixStack matrixFrom(Vec3d pos) {
      MatrixStack matrices = new MatrixStack();
      Camera camera = mc.gameRenderer.getCamera();
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      matrices.translate(pos.getX() - camera.getCameraPos().x, pos.getY() - camera.getCameraPos().y, pos.getZ() - camera.getCameraPos().z);
      return matrices;
   }
}
