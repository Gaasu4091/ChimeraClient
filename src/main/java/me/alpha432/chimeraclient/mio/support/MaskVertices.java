package me.alpha432.chimeraclient.mio.support;

import net.minecraft.client.render.VertexConsumer;

public final class MaskVertices implements VertexConsumer {
   private final VertexConsumer out;
   private final int tint;

   public MaskVertices(VertexConsumer var1, int var2) {
      this.out = var1;
      this.tint = var2;
   }

   public VertexConsumer vertex(float x, float y, float z) {
      this.out.vertex(x, y, z);
      return this;
   }

   public VertexConsumer color(int red, int green, int blue, int alpha) {
      this.out.color(this.tint);
      return this;
   }

   public VertexConsumer color(int argb) {
      this.out.color(this.tint);
      return this;
   }

   public VertexConsumer texture(float u, float v) {
      this.out.texture(u, v);
      return this;
   }

   public VertexConsumer overlay(int u, int v) {
      this.out.overlay(u, v);
      return this;
   }

   public VertexConsumer light(int u, int v) {
      this.out.light(u, v);
      return this;
   }

   public VertexConsumer normal(float x, float y, float z) {
      this.out.normal(x, y, z);
      return this;
   }

   public VertexConsumer lineWidth(float width) {
      this.out.lineWidth(width);
      return this;
   }

   public void vertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
      this.out.vertex(x, y, z, this.tint, u, v, overlay, light, normalX, normalY, normalZ);
   }
}
