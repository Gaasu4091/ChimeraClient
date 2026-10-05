package me.alpha432.chimeraclient.mio.support;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import me.alpha432.chimeraclient.mio.MioConfiguredModule;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.OutputTarget;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;

public final class MioShaderSupport implements Util {
   private static SimpleFramebuffer mask;
   private static SimpleFramebuffer handMask;
   private static final long START = System.nanoTime();
   private static GpuBuffer settings;
   private static final Map<String, RenderPipeline> PIPELINES = new HashMap<>();
   public static final OutputTarget TARGET = new OutputTarget("Mio entity/hand mask", MioShaderSupport::mask);
   public static final OutputTarget HAND_TARGET = new OutputTarget("Mio hand mask", MioShaderSupport::handMask);

   private static Framebuffer mask() {
      int var0 = mc.getWindow().getFramebufferWidth();
      int var1 = mc.getWindow().getFramebufferHeight();
      if (mask == null) {
         mask = new SimpleFramebuffer("Mio mask", var0, var1, false);
      } else if (mask.textureWidth != var0 || mask.textureHeight != var1) {
         mask.resize(var0, var1);
      }

      return mask;
   }

   private static Framebuffer handMask() {
      int var0 = mc.getWindow().getFramebufferWidth();
      int var1 = mc.getWindow().getFramebufferHeight();
      if (handMask == null) {
         handMask = new SimpleFramebuffer("Mio hands", var0, var1, false);
      } else if (handMask.textureWidth != var0 || handMask.textureHeight != var1) {
         handMask.resize(var0, var1);
      }

      return handMask;
   }

   public static void begin() {
      if (MioConfiguredModule.active("Shader") != null) {
         CommandEncoder var0 = RenderSystem.getDevice().createCommandEncoder();
         var0.clearColorTexture(mask().getColorAttachment(), 0);
         var0.clearColorTexture(handMask().getColorAttachment(), 0);
      }
   }

   public static void draw() {
      MioConfiguredModule var0 = MioConfiguredModule.active("Shader");
      if (var0 != null && mask != null) {
         draw(var0, mask, false);
         if (var0.b("hands") && handMask != null) {
            draw(var0, handMask, true);
         }
      }
   }

   private static void draw(MioConfiguredModule var0, SimpleFramebuffer var1, boolean var2) {
      String var3 = ((Enum)var0.options.get("shader").getValue()).name().toLowerCase(Locale.ROOT);
      RenderPipeline var4 = PIPELINES.computeIfAbsent(
         var3,
         var0x -> RenderPipeline.builder(new Snippet[0])
            .withLocation(Identifier.of("chimeraclient", "mio/post/" + var0x))
            .withVertexShader(Identifier.of("chimeraclient", "core/mio_post"))
            .withFragmentShader(Identifier.of("chimeraclient", "core/mio_" + var0x))
            .withUniform("MioSettings", UniformType.UNIFORM_BUFFER)
            .withSampler("u_Texture")
            .withSampler("u_Overlay")
            .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .build()
      );
      ByteBuffer var5 = ByteBuffer.allocateDirect(144).order(ByteOrder.nativeOrder());
      float var6 = (float)(System.nanoTime() - START) / 1.0E9F;
      vector(var5, var1.textureWidth, var1.textureHeight, var6 * var0.f("speed"), var0.f("step"));
      color(var5, var2 && !var0.b("fill2") ? new Color(0, 0, 0, 0) : var0.c("fill"));
      color(var5, var0.c("outline2"));
      color(var5, var0.c("fillSecond"));
      color(var5, var0.c("outlineSecond"));
      vector(var5, var0.f("overlayAlpha"), var0.f("glow"), var6 * var0.f("fillSpeed"), var6 * var0.f("outlineSpeed"));
      MioConfiguredModule var7 = MioConfiguredModule.active("NoRender");
      vector(var5, var0.f("fillStrength"), var0.f("outlineStrength"), var0.f("alpha"), !var2 && var7 != null && var7.b("noCluster") ? 1.0F : 0.0F);
      var5.putInt(var2 && !var0.b("outline") ? 0 : var0.i("lineWidth")).putInt(var0.b("fastLines") ? 1 : 0).putInt(2).putInt(var0.b("image") ? 1 : 0);
      var5.putInt(var0.b("decorator") ? ((Enum)var0.options.get("type").getValue()).ordinal() : 0).putInt(var0.i("radius")).putInt(var0.i("quality")).putInt(0);
      var5.flip();
      if (settings == null) {
         settings = RenderSystem.getDevice().createBuffer(() -> "Mio shader settings", 136, 144L);
      }

      CommandEncoder var8 = RenderSystem.getDevice().createCommandEncoder();
      var8.writeToBuffer(settings.slice(), var5);
      AbstractTexture var9 = mc.getTextureManager().getTexture(Identifier.of("chimeraclient", "mio/textures/overlay.png"));
      RenderPass var10 = var8.createRenderPass(() -> "Mio " + var3 + " shader", mc.getFramebuffer().getColorAttachmentView(), OptionalInt.empty());

      try {
         var10.setPipeline(var4);
         var10.setUniform("MioSettings", settings);
         var10.bindTexture("u_Texture", var1.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
         var10.bindTexture("u_Overlay", var9.getGlTextureView(), var9.getSampler());
         var10.draw(0, 3);
      } catch (Throwable var14) {
         if (var10 != null) {
            try {
               var10.close();
            } catch (Throwable var13) {
               var14.addSuppressed(var13);
            }
         }

         throw var14;
      }

      if (var10 != null) {
         var10.close();
      }
   }

   private static void vector(ByteBuffer var0, float var1, float var2, float var3, float var4) {
      var0.putFloat(var1).putFloat(var2).putFloat(var3).putFloat(var4);
   }

   private static void color(ByteBuffer var0, Color var1) {
      vector(var0, var1.getRed() / 255.0F, var1.getGreen() / 255.0F, var1.getBlue() / 255.0F, var1.getAlpha() / 255.0F);
   }

   public static void close() {
      if (mask != null) {
         mask.delete();
         mask = null;
      }

      if (handMask != null) {
         handMask.delete();
         handMask = null;
      }

      if (settings != null) {
         settings.close();
         settings = null;
      }

      PIPELINES.clear();
   }
}
