package me.alpha432.chimeraclient.mio;

import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.util.List;
import me.alpha432.chimeraclient.util.render.Layers;
import me.alpha432.chimeraclient.util.render.RenderUtil;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class MioRender implements Util {
   public static final Matrix4f VIEW = new Matrix4f();
   public static final Matrix4f PROJECTION = new Matrix4f();

   private MioRender() {
   }

   public static Vec3d camera() {
      return mc.gameRenderer.getCamera().getCameraPos();
   }

   public static Color alpha(Color var0, int var1) {
      return new Color(var0.getRed(), var0.getGreen(), var0.getBlue(), MathHelper.clamp(var1, 0, 255));
   }

   public static Vec3d lerp(Entity var0, float var1) {
      return new Vec3d(
         MathHelper.lerp(var1, var0.lastRenderX, var0.getX()),
         MathHelper.lerp(var1, var0.lastRenderY, var0.getY()),
         MathHelper.lerp(var1, var0.lastRenderZ, var0.getZ())
      );
   }

   public static Box lerpBox(Entity var0, float var1) {
      Vec3d var2 = lerp(var0, var1);
      return var0.getBoundingBox().offset(var2.x - var0.getX(), var2.y - var0.getY(), var2.z - var0.getZ());
   }

   public static void fill(MatrixStack var0, Box var1, Color var2) {
      if (var2.getAlpha() != 0) {
         RenderUtil.drawBoxFilled(var0, var1, var2);
      }
   }

   public static void gradient(MatrixStack var0, Box var1, Color var2) {
      gradient(var0, var1, var2, alpha(var2, 0));
   }

   public static void gradient(MatrixStack var0, Box var1, Color var2, Color var3) {
      if (var2.getAlpha() != 0 || var3.getAlpha() != 0) {
         Vec3d var4 = camera();
         float var5 = (float)(var1.minX - var4.x);
         float var6 = (float)(var1.minY - var4.y);
         float var7 = (float)(var1.minZ - var4.z);
         float var8 = (float)(var1.maxX - var4.x);
         float var9 = (float)(var1.maxY - var4.y);
         float var10 = (float)(var1.maxZ - var4.z);
         int var11 = var2.getRGB();
         int var12 = var3.getRGB();
         Matrix4f var13 = var0.peek().getPositionMatrix();
         BufferBuilder var14 = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         var14.vertex(var13, var5, var6, var7).color(var11);
         var14.vertex(var13, var8, var6, var7).color(var11);
         var14.vertex(var13, var8, var6, var10).color(var11);
         var14.vertex(var13, var5, var6, var10).color(var11);
         var14.vertex(var13, var5, var9, var7).color(var12);
         var14.vertex(var13, var5, var9, var10).color(var12);
         var14.vertex(var13, var8, var9, var10).color(var12);
         var14.vertex(var13, var8, var9, var7).color(var12);
         var14.vertex(var13, var5, var6, var7).color(var11);
         var14.vertex(var13, var5, var9, var7).color(var12);
         var14.vertex(var13, var8, var9, var7).color(var12);
         var14.vertex(var13, var8, var6, var7).color(var11);
         var14.vertex(var13, var8, var6, var7).color(var11);
         var14.vertex(var13, var8, var9, var7).color(var12);
         var14.vertex(var13, var8, var9, var10).color(var12);
         var14.vertex(var13, var8, var6, var10).color(var11);
         var14.vertex(var13, var5, var6, var10).color(var11);
         var14.vertex(var13, var8, var6, var10).color(var11);
         var14.vertex(var13, var8, var9, var10).color(var12);
         var14.vertex(var13, var5, var9, var10).color(var12);
         var14.vertex(var13, var5, var6, var7).color(var11);
         var14.vertex(var13, var5, var6, var10).color(var11);
         var14.vertex(var13, var5, var9, var10).color(var12);
         var14.vertex(var13, var5, var9, var7).color(var12);
         Layers.quads().draw(var14.end());
      }
   }

   public static void outline(MatrixStack var0, Box var1, Color var2, float var3) {
      if (var2.getAlpha() != 0 && !(var3 <= 0.0F)) {
         RenderUtil.drawBox(var0, var1, var2, var3);
      }
   }

   public static void outline(MatrixStack var0, VoxelShape var1, Color var2, float var3) {
      if (var2.getAlpha() != 0 && !(var3 <= 0.0F) && !var1.isEmpty()) {
         RenderUtil.drawBox(var0, var1, var2, var3);
      }
   }

   public static void line(MatrixStack var0, Vec3d var1, Vec3d var2, Color var3, float var4) {
      line(var0, var1, var2, var3, var3, var4);
   }

   public static void line(MatrixStack var0, Vec3d var1, Vec3d var2, Color var3, Color var4, float var5) {
      Vec3d var6 = camera();
      BufferBuilder var7 = Tessellator.getInstance().begin(DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
      addLine(var7, var0.peek(), var1.subtract(var6), var2.subtract(var6), var3.getRGB(), var4.getRGB(), var5);
      Layers.lines().draw(var7.end());
   }

   public static void strip(MatrixStack var0, List<Vec3d> var1, Color var2, float var3) {
      if (var1.size() >= 2 && var2.getAlpha() != 0) {
         Vec3d var4 = camera();
         BufferBuilder var5 = Tessellator.getInstance().begin(DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
         int var6 = var2.getRGB();

         for (int var7 = 1; var7 < var1.size(); var7++) {
            addLine(var5, var0.peek(), ((Vec3d)var1.get(var7 - 1)).subtract(var4), ((Vec3d)var1.get(var7)).subtract(var4), var6, var6, var3);
         }

         Layers.lines().draw(var5.end());
      }
   }

   private static void addLine(BufferBuilder var0, Entry var1, Vec3d var2, Vec3d var3, int var4, int var5, float var6) {
      float var7 = (float)(var3.x - var2.x);
      float var8 = (float)(var3.y - var2.y);
      float var9 = (float)(var3.z - var2.z);
      var0.vertex(var1, (float)var2.x, (float)var2.y, (float)var2.z).color(var4).normal(var1, var7, var8, var9).lineWidth(var6);
      var0.vertex(var1, (float)var3.x, (float)var3.y, (float)var3.z).color(var5).normal(var1, var7, var8, var9).lineWidth(var6);
   }

   public static Vec3d project(Vec3d var0) {
      Vec3d var1 = var0.subtract(camera());
      Vector4f var2 = new Vector4f((float)var1.x, (float)var1.y, (float)var1.z, 1.0F);
      VIEW.transform(var2);
      PROJECTION.transform(var2);
      if (var2.w <= 0.0F) {
         return null;
      } else {
         float var3 = var2.x / var2.w;
         float var4 = var2.y / var2.w;
         float var5 = var2.z / var2.w;
         double var6 = mc.getWindow().getScaledWidth();
         double var8 = mc.getWindow().getScaledHeight();
         return new Vec3d((var3 + 1.0) * 0.5 * var6, (1.0 - var4) * 0.5 * var8, var5);
      }
   }
}
