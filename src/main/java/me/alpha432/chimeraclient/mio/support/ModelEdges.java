package me.alpha432.chimeraclient.mio.support;

import net.minecraft.client.render.VertexConsumer;

public final class ModelEdges implements VertexConsumer {
   private final VertexConsumer out;
   private final int color;
   private final float width;
   private final float[][] points = new float[4][3];
   private int count;

   public ModelEdges(VertexConsumer var1, int var2, float var3) {
      this.out = var1;
      this.color = var2;
      this.width = var3;
   }

   public void vertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
      this.points[this.count][0] = x;
      this.points[this.count][1] = y;
      this.points[this.count][2] = z;
      if (++this.count == 4) {
         for (int var12 = 0; var12 < 4; var12++) {
            float[] var13 = this.points[var12];
            float[] var14 = this.points[(var12 + 1) % 4];
            float var15 = var14[0] - var13[0];
            float var16 = var14[1] - var13[1];
            float var17 = var14[2] - var13[2];
            if (var15 * var15 + var16 * var16 + var17 * var17 != 0.0F) {
               this.out.vertex(var13[0], var13[1], var13[2]).color(this.color).normal(var15, var16, var17).lineWidth(this.width);
               this.out.vertex(var14[0], var14[1], var14[2]).color(this.color).normal(var15, var16, var17).lineWidth(this.width);
            }
         }

         this.count = 0;
      }
   }

   public VertexConsumer vertex(float x, float y, float z) {
      this.points[this.count][0] = x;
      this.points[this.count][1] = y;
      this.points[this.count][2] = z;
      return this;
   }

   public VertexConsumer normal(float x, float y, float z) {
      float[] var4 = this.points[this.count];
      this.vertex(var4[0], var4[1], var4[2], this.color, 0.0F, 0.0F, 0, 0, x, y, z);
      return this;
   }

   public VertexConsumer color(int red, int green, int blue, int alpha) {
      return this;
   }

   public VertexConsumer color(int argb) {
      return this;
   }

   public VertexConsumer texture(float u, float v) {
      return this;
   }

   public VertexConsumer overlay(int u, int v) {
      return this;
   }

   public VertexConsumer light(int u, int v) {
      return this;
   }

   public VertexConsumer lineWidth(float width) {
      return this;
   }
}
