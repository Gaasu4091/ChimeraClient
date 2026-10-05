package me.alpha432.chimeraclient.mio.support;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.pipeline.RenderPipeline.UniformDescription;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.PolygonMode;
import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.mio.MioConfiguredModule;
import me.alpha432.chimeraclient.mio.MioRender;
import me.alpha432.chimeraclient.mio.mixin.MioLayerAccess;
import me.alpha432.chimeraclient.mio.mixin.MioLayerFactory;
import me.alpha432.chimeraclient.mio.mixin.MioSetupAccess;
import me.alpha432.chimeraclient.mio.mixin.MioTextureSpecAccess;
import me.alpha432.chimeraclient.util.render.Layers;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.util.Identifier;

public final class ModelSupport implements Util {
   private static final Map<EntityRenderState, Entity> ENTITIES = Collections.synchronizedMap(new WeakHashMap<>());
   public static EntityRenderState scope;
   public static boolean hand;
   public static boolean armor;
   private static final Map<RenderLayer, Map<String, RenderLayer>> LAYERS = new IdentityHashMap<>();

   public static void capture(Entity var0, EntityRenderState var1) {
      ENTITIES.put(var1, var0);
   }

   public static Entity entity(EntityRenderState var0) {
      return ENTITIES.get(var0);
   }

   public static String chamsMode(EntityRenderState var0) {
      MioConfiguredModule var1 = MioConfiguredModule.active("Chams");
      Entity var2 = entity(var0);
      if (var1 != null && var2 != null && mc.player != null && !(mc.player.distanceTo(var2) > var1.n("range"))) {
         String var3 = var2 == mc.player
            ? "self"
            : (
               var2 instanceof PlayerEntity
                  ? "players"
                  : (
                     var2 instanceof EndCrystalEntity
                        ? "crystals"
                        : (var2 instanceof HostileEntity ? "hostiles" : (var2 instanceof AnimalEntity ? "animals" : null))
                  )
            );
         return var3 == null ? "OFF" : ((Enum)var1.options.get(var3).getValue()).name();
      } else {
         return "OFF";
      }
   }

   public static int chamsColor(EntityRenderState var0, boolean var1) {
      MioConfiguredModule var2 = MioConfiguredModule.active("Chams");
      Color var3 = var2.c(var1 ? "outline" : "fill");
      Entity var4 = entity(var0);
      if (var2.b("friends") && var4 instanceof PlayerEntity var5 && ChimeraClient.friendManager.isFriend(var5)) {
         var3 = new Color(85, 255, 255, var3.getAlpha());
      }

      float var6 = var2.b("fade")
         ? (float)(
            1.0
               - Math.max(
                  0.0, (mc.player.distanceTo(var4) / var2.n("range") - var2.n("fadeRadius") / 100.0) / Math.max(0.001, 1.0 - var2.n("fadeRadius") / 100.0)
               )
         )
         : 1.0F;
      return MioRender.alpha(var3, (int)(var3.getAlpha() * Math.max(0.0F, var6))).getRGB();
   }

   public static float opacity() {
      MioConfiguredModule var0 = MioConfiguredModule.active("NoRender");
      return var0 == null ? 1.0F : (hand && var0.b("hands") ? var0.f("opacity2") : (armor && var0.b("armor") ? var0.f("opacity") : 1.0F));
   }

   public static int maskColor(EntityRenderState var0) {
      MioConfiguredModule var1 = MioConfiguredModule.active("NoRender");
      Entity var2 = entity(var0);
      if (!hand && var1 != null && var1.b("noCluster") && var2 != null && var2 != mc.player && mc.player != null && !(var2.getWidth() <= 0.0F)) {
         float var3 = mc.player.distanceTo(var2);
         int var4 = var3 >= var2.getWidth() ? 255 : var1.i("clusterAlpha") + (int)((255 - var1.i("clusterAlpha")) * var3 / var2.getWidth());
         return 0xFF000000 | var4 * 65793;
      } else {
         return -1;
      }
   }

   public static boolean glint(RenderLayer var0) {
      return var0.getRenderPipeline() == RenderPipelines.GLINT || GlintSupport.isChams(var0.getRenderPipeline());
   }

   public static int alpha(int var0, float var1) {
      return var0 & 16777215 | Math.max(0, Math.min(255, (int)((var0 >>> 24) * var1))) << 24;
   }

   public static <S> S chamsState(S var0) {
      MioConfiguredModule var1 = MioConfiguredModule.active("Chams");
      if (var1 != null && !var1.b("extraLayer") && var0 instanceof PlayerEntityRenderState var2) {
         PlayerEntityRenderState var3 = GhostSupport.copy(var2);
         var3.hatVisible = var3.jacketVisible = var3.leftSleeveVisible = var3.rightSleeveVisible = var3.leftPantsLegVisible = var3.rightPantsLegVisible = false;
         return (S)var3;
      } else {
         return (S)var0;
      }
   }

   public static <S> void edges(RenderCommandQueue var0, Model<? super S> var1, S var2, MatrixStack var3, int var4, float var5) {
      var0.submitCustom(var3, Layers.lines(), (var4x, var5x) -> {
         MatrixStack var6 = new MatrixStack();
         var6.peek().getPositionMatrix().set(var4x.getPositionMatrix());
         var6.peek().getNormalMatrix().set(var4x.getNormalMatrix());
         var1.setAngles(var2);
         var1.render(var6, new ModelEdges(var5x, var4, var5), 15728880, OverlayTexture.DEFAULT_UV, -1);
      });
   }

   public static boolean shaderTarget(EntityRenderState var0) {
      MioConfiguredModule var1 = MioConfiguredModule.active("Shader");
      if (var1 == null || mc.player == null) {
         return false;
      } else if (hand) {
         return var1.b("hands");
      } else {
         Entity var2 = entity(var0);
         if (var2 != null && (var2.isAlive() || var1.b("dead"))) {
            String var3 = var2 == mc.player
               ? "self"
               : (
                  var2 instanceof PlayerEntity
                     ? "players"
                     : (
                        var2 instanceof EndCrystalEntity
                           ? "crystals"
                           : (
                              var2 instanceof ItemEntity
                                 ? "items"
                                 : (
                                    var2 instanceof HostileEntity
                                       ? "hostiles"
                                       : (
                                          var2 instanceof AnimalEntity
                                             ? "animals"
                                             : (
                                                var2 instanceof EnderPearlEntity
                                                   ? "pearls"
                                                   : (
                                                      !(var2 instanceof ExperienceBottleEntity) && !(var2 instanceof ExperienceOrbEntity)
                                                         ? (var2 instanceof AbstractMinecartEntity ? "minecarts" : null)
                                                         : "exp"
                                                   )
                                             )
                                       )
                                 )
                           )
                     )
               );
            String var4 = var2 instanceof EndCrystalEntity ? "range" : (var2 instanceof ItemEntity ? "range3" : "range2");
            return var3 != null && var1.b(var3) && mc.player.distanceTo(var2) <= var1.n(var4);
         } else {
            return false;
         }
      }
   }

   public static RenderPipeline copy(RenderPipeline var0, String var1, Identifier var2, PolygonMode var3) {
      return copy(var0, var1, var2, var3, true);
   }

   public static RenderPipeline copy(RenderPipeline var0, String var1, Identifier var2, PolygonMode var3, boolean var4) {
      Builder var5 = RenderPipeline.builder(new Snippet[0])
         .withLocation(Identifier.of("chimeraclient", var1))
         .withVertexShader(var0.getVertexShader())
         .withFragmentShader(var2)
         .withVertexFormat(var0.getVertexFormat(), var0.getVertexFormatMode())
         .withDepthTestFunction(var4 ? DepthTestFunction.NO_DEPTH_TEST : var0.getDepthTestFunction())
         .withDepthWrite(false)
         .withCull(false)
         .withBlend(BlendFunction.TRANSLUCENT)
         .withPolygonMode(var3);

      for (UniformDescription var7 : var0.getUniforms()) {
         var5.withUniform(var7.name(), var7.type());
      }

      for (String var9 : var0.getSamplers()) {
         var5.withSampler(var9);
      }

      var0.getShaderDefines().flags().forEach(var5::withShaderDefine);
      var0.getShaderDefines().values().forEach((var1x, var2x) -> {
         try {
            var5.withShaderDefine(var1x, Float.parseFloat(var2x));
         } catch (NumberFormatException var4x) {
            throw new IllegalStateException("Unexpected vanilla shader define " + var1x + "=" + var2x);
         }
      });
      var5.withShaderDefine("NO_CARDINAL_LIGHTING");
      return var5.build();
   }

   public static RenderLayer flat(RenderLayer var0, boolean var1, boolean var2) {
      boolean var3 = hand;
      String var4 = var1 + ":" + var2 + ":" + var3;
      return LAYERS.computeIfAbsent(var0, var0x -> new HashMap<>())
         .computeIfAbsent(
            var4,
            var4x -> {
               RenderPipeline var5 = copy(
                  var0.getRenderPipeline(),
                  "mio/entity/" + System.identityHashCode(var0) + "_" + var1 + "_" + var2 + "_" + var3,
                  Identifier.of("chimeraclient", "core/mio_model"),
                  var1 ? PolygonMode.WIREFRAME : PolygonMode.FILL
               );
               net.minecraft.client.render.RenderSetup.Builder var6 = RenderSetup.builder(var5).useLightmap().useOverlay();
                Map<String, Object> var7 = ((MioSetupAccess)(Object)((MioLayerAccess)var0).mio$setup()).mio$textures();

               for (Entry var9 : var7.entrySet()) {
                  MioTextureSpecAccess var10 = (MioTextureSpecAccess)var9.getValue();
                  var6.texture((String)var9.getKey(), var10.mio$location(), var10.mio$sampler());
               }

               if (var2) {
                  var6.outputTarget(var3 ? MioShaderSupport.HAND_TARGET : MioShaderSupport.TARGET);
               }

               return MioLayerFactory.mio$of("mio_" + var4x, var6.build());
            }
         );
   }

   public static RenderLayer textured(RenderLayer var0, boolean var1) {
      String var2 = "texture:" + var1;
      return LAYERS.computeIfAbsent(var0, var0x -> new HashMap<>())
         .computeIfAbsent(
            var2,
            var2x -> {
               RenderPipeline var3 = copy(
                  var0.getRenderPipeline(),
                  "mio/textured/" + System.identityHashCode(var0) + "_" + var1,
                  var0.getRenderPipeline().getFragmentShader(),
                  PolygonMode.FILL,
                  var1
               );
               net.minecraft.client.render.RenderSetup.Builder var4 = RenderSetup.builder(var3).useLightmap().useOverlay();
                Map<String, Object> var5 = ((MioSetupAccess)(Object)((MioLayerAccess)var0).mio$setup()).mio$textures();

               for (Entry var7 : var5.entrySet()) {
                  MioTextureSpecAccess var8 = (MioTextureSpecAccess)var7.getValue();
                  var4.texture((String)var7.getKey(), var8.mio$location(), var8.mio$sampler());
               }

               return MioLayerFactory.mio$of("mio_textured", var4.build());
            }
         );
   }
}
