package me.alpha432.chimeraclient.mio.support;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import me.alpha432.chimeraclient.mio.MioConfiguredModule;
import me.alpha432.chimeraclient.mio.mixin.MioLayerFactory;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.TextureTransform;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public final class GlintSupport implements Util {
   private static RenderPipeline pipeline;
   private static RenderPipeline chamsPipeline;
   private static RenderLayer chamsLayer;
   private static GpuBuffer color;

   public static boolean active(RenderPipeline var0) {
      return MioConfiguredModule.active("Glint") != null && var0 == RenderPipelines.GLINT || var0 == chamsPipeline && chamsPipeline != null;
   }

   public static RenderPipeline pipeline(RenderPipeline var0) {
      if (var0 == chamsPipeline || !active(var0)) {
         return var0;
      } else if (pipeline != null) {
         return pipeline;
      } else {
         Builder var1 = RenderPipeline.builder(new Snippet[0])
            .withLocation(Identifier.of("chimeraclient", "mio/glint"))
            .withVertexShader(var0.getVertexShader())
            .withFragmentShader(Identifier.of("chimeraclient", "core/mio_glint"))
            .withDepthTestFunction(var0.getDepthTestFunction())
            .withDepthWrite(var0.isWriteDepth())
            .withCull(var0.isCull())
            .withVertexFormat(var0.getVertexFormat(), var0.getVertexFormatMode())
            .withUniform("MioGlint", UniformType.UNIFORM_BUFFER);
         var0.getBlendFunction().ifPresent(var1::withBlend);
         var0.getUniforms().forEach(var1x -> var1.withUniform(var1x.name(), var1x.type()));
         var0.getSamplers().forEach(var1::withSampler);
         var0.getShaderDefines().flags().forEach(var1::withShaderDefine);
         pipeline = var1.build();
         return pipeline;
      }
   }

   public static RenderLayer chams() {
      if (chamsLayer != null) {
         return chamsLayer;
      } else {
         RenderPipeline var0 = RenderLayers.entityGlint().getRenderPipeline();
         Builder var1 = RenderPipeline.builder(new Snippet[0])
            .withLocation(Identifier.of("chimeraclient", "mio/chams_shine"))
            .withVertexShader(var0.getVertexShader())
            .withFragmentShader(Identifier.of("chimeraclient", "core/mio_glint"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .withVertexFormat(var0.getVertexFormat(), var0.getVertexFormatMode())
            .withUniform("MioGlint", UniformType.UNIFORM_BUFFER);
         var0.getBlendFunction().ifPresent(var1::withBlend);
         var0.getUniforms().forEach(var1x -> var1.withUniform(var1x.name(), var1x.type()));
         var0.getSamplers().forEach(var1::withSampler);
         var0.getShaderDefines().flags().forEach(var1::withShaderDefine);
         chamsPipeline = var1.build();
         TextureTransform var2 = new TextureTransform(
            "mio_chams_shine",
            () -> {
               MioConfiguredModule var0x = MioConfiguredModule.active("Chams");
               float var1x = var0x == null
                  ? 0.0F
                  : (var0x.f("speed") == 0.0F ? var0x.f("progress") / 100.0F : (float)(System.currentTimeMillis() * var0x.n("speed") * 3.0E-4 % 1.0));
               return new Matrix4f().translation(-var1x, var1x, 0.0F).rotateZ((float)Math.toRadians(10.0)).scale(0.16F);
            }
         );
         chamsLayer = MioLayerFactory.mio$of(
            "mio_chams_shine",
            RenderSetup.builder(chamsPipeline).texture("Sampler0", Identifier.of("chimeraclient", "mio/textures/shine.png")).textureTransform(var2).build()
         );
         return chamsLayer;
      }
   }

   public static boolean isChams(RenderPipeline var0) {
      return chamsPipeline != null && var0 == chamsPipeline;
   }

   public static void bind(RenderPass var0, RenderPipeline var1) {
      boolean var2 = isChams(var1);
      MioConfiguredModule var3 = MioConfiguredModule.active(var2 ? "Chams" : "Glint");
      if (var3 != null) {
         Color var4 = var3.c(var2 ? "shine" : "color");
         float var5 = var2 ? var3.f("strength") : 1.0F;
         float var6 = var4.getAlpha() / 255.0F * var5;
         ByteBuffer var7 = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
         var7.putFloat(var4.getRed() / 255.0F * var5).putFloat(var4.getGreen() / 255.0F * var5).putFloat(var4.getBlue() / 255.0F * var5).putFloat(var6).flip();
         if (color == null) {
            color = RenderSystem.getDevice().createBuffer(() -> "Mio glint color", 136, 16L);
         }

         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(color.slice(), var7);
         var0.setUniform("MioGlint", color);
      }
   }

   public static void close() {
      if (color != null) {
         color.close();
         color = null;
      }

      chamsPipeline = null;
      pipeline = null;
      chamsLayer = null;
   }
}
