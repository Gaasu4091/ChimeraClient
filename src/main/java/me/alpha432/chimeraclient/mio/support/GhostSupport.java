package me.alpha432.chimeraclient.mio.support;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.alpha432.chimeraclient.mio.MioConfiguredModule;
import me.alpha432.chimeraclient.mio.MioRender;
import me.alpha432.chimeraclient.mio.mixin.MioGhostTransforms;
import me.alpha432.chimeraclient.util.render.Layers;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class GhostSupport implements Util {
   private static final Map<UUID, GhostSupport.Ghost> LAST = new LinkedHashMap<>();
   private static final Map<UUID, GhostSupport.Ghost> LOGOUT = new LinkedHashMap<>();
   private static final List<GhostSupport.Ghost> POP = new ArrayList<>();
   private static final List<Field> FIELDS = Arrays.stream(PlayerEntityRenderState.class.getFields())
      .filter(var0 -> !Modifier.isStatic(var0.getModifiers()) && !Modifier.isFinal(var0.getModifiers()))
      .toList();

   public static void clear() {
      LAST.clear();
      LOGOUT.clear();
      POP.clear();
   }

   public static PlayerEntityRenderState copy(PlayerEntityRenderState var0) {
      PlayerEntityRenderState var1 = new PlayerEntityRenderState();

      try {
         for (Field var3 : FIELDS) {
            var3.set(var1, var3.get(var0));
         }

         return var1;
      } catch (IllegalAccessException var4) {
         throw new IllegalStateException("Player render-state snapshot", var4);
      }
   }

   public static void observe(Entity var0, EntityRenderState var1) {
      if (var0 instanceof PlayerEntity var2 && var1 instanceof PlayerEntityRenderState var3 && var0 != mc.player) {
         if (MioConfiguredModule.active("LogoutSpots") != null || MioConfiguredModule.active("Chams") != null) {
            PlayerEntityRenderState var4 = copy(var3);
            boolean var5 = true;

            for (EquipmentSlot var7 : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
               if (!var2.getEquippedStack(var7).isEmpty()) {
                  var5 = false;
               }
            }

            LAST.put(var0.getUuid(), new GhostSupport.Ghost(var4, var0.getEntityPos(), var5, System.currentTimeMillis()));

            while (LAST.size() > 512) {
               LAST.remove(LAST.keySet().iterator().next());
            }
         }
      }
   }

   public static void logout(UUID var0) {
      GhostSupport.Ghost var1 = LAST.remove(var0);
      if (var1 != null) {
         LOGOUT.put(var0, var1);
      }

      while (LOGOUT.size() > 512) {
         LOGOUT.remove(LOGOUT.keySet().iterator().next());
      }
   }

   public static void login(UUID var0) {
      LOGOUT.remove(var0);
   }

   public static void pop(UUID var0, boolean var1) {
      MioConfiguredModule var2 = MioConfiguredModule.active("Chams");
      if (var2 != null && var2.b("pop") && (!var1 || var2.b("death"))) {
         GhostSupport.Ghost var3 = LAST.get(var0);
         if (var3 != null) {
            POP.add(new GhostSupport.Ghost(var3.state, var3.pos, var3.naked, System.currentTimeMillis()));

            while (POP.size() > 256) {
               POP.removeFirst();
            }
         }
      }
   }

   public static boolean naked(UUID var0) {
      GhostSupport.Ghost var1 = LOGOUT.get(var0);
      return var1 != null && var1.naked;
   }

   public static void logout(MioConfiguredModule var0, UUID var1, MatrixStack var2) {
      GhostSupport.Ghost var3 = LOGOUT.get(var1);
      if (var3 != null) {
         draw(var3, var2, var0.c("modelFill"), var0.c("modelLine"), var0.f("alpha"), 0.0F, var0.b("textured"), false, false, var0.f("width"));
      }
   }

   public static void pops(MioConfiguredModule var0, MatrixStack var1) {
      long var2 = System.currentTimeMillis();
      float var4 = var0.f("time") * 1000.0F;
      POP.removeIf(var3 -> var4 <= 0.0F || (float)(var2 - var3.time) >= var4);

      for (GhostSupport.Ghost var6 : POP) {
         float var7 = 1.0F - (float)(var2 - var6.time) / var4;
         Color var8 = var0.choice("wireframe", "LINE") ? new Color(0, true) : var0.c("popFill");
         Color var9 = var0.choice("wireframe", "FILL") ? new Color(0, true) : var0.c("popLine");
         draw(var6, var1, var8, var9, var7, (1.0F - var7) * var0.f("motion"), false, var0.b("animate"), var0.b("boost"), var0.f("lineWidth"));
      }
   }

   private static void draw(
      GhostSupport.Ghost var0, MatrixStack var1, Color var2, Color var3, float var4, float var5, boolean var6, boolean var7, boolean var8, float var9
   ) {
      PlayerEntityRenderState var10 = var0.state;
      if (var10.skinTextures != null) {
         if (mc.getEntityRenderDispatcher().getRenderer(var10) instanceof PlayerEntityRenderer var11) {
            PlayerEntityModel var19 = (PlayerEntityModel)var11.getModel();
            float var13 = var10.limbSwingAnimationProgress;
            float var14 = var10.limbSwingAmplitude;
            if (!var7) {
               var10.limbSwingAmplitude = var10.limbSwingAnimationProgress = 0.0F;
            } else if (var8) {
               var10.limbSwingAmplitude = 0.5F;
               var10.limbSwingAnimationProgress = var10.limbSwingAnimationProgress + (float)(System.currentTimeMillis() - var0.time) * 0.01F;
            }

            var1.push();
            Vec3d var15 = var0.pos.subtract(MioRender.camera());
            var1.translate(var15.x, var15.y + var5, var15.z);
            var1.scale(var10.baseScale, var10.baseScale, var10.baseScale);
            ((MioGhostTransforms)var11).mio$transforms(var10, var1, var10.bodyYaw, var10.baseScale);
            var1.scale(-1.0F, -1.0F, 1.0F);
            ((MioGhostTransforms)var11).mio$scale(var10, var1);
            var1.translate(0.0F, -1.501F, 0.0F);
            var19.setAngles(var10);
            RenderLayer var16 = RenderLayers.entityTranslucent(var10.skinTextures.body().texturePath());
            if (var2.getAlpha() > 0) {
               pass(
                  var19,
                  var10,
                  var1,
                  var6 ? ModelSupport.textured(var16, true) : ModelSupport.flat(var16, false, false),
                  MioRender.alpha(var2, (int)(var2.getAlpha() * var4)).getRGB()
               );
            }

            if (var3.getAlpha() > 0) {
               RenderLayer var17 = Layers.lines();
               BufferBuilder var18 = Tessellator.getInstance()
                  .begin(var17.getRenderPipeline().getVertexFormatMode(), var17.getRenderPipeline().getVertexFormat());
               var19.render(
                  var1, new ModelEdges(var18, MioRender.alpha(var3, (int)(var3.getAlpha() * var4)).getRGB(), var9), 15728880, OverlayTexture.DEFAULT_UV, -1
               );
               var17.draw(var18.end());
            }

            var1.pop();
            var10.limbSwingAnimationProgress = var13;
            var10.limbSwingAmplitude = var14;
         }
      }
   }

   private static void pass(PlayerEntityModel var0, PlayerEntityRenderState var1, MatrixStack var2, RenderLayer var3, int var4) {
      BufferBuilder var5 = Tessellator.getInstance().begin(var3.getRenderPipeline().getVertexFormatMode(), var3.getRenderPipeline().getVertexFormat());
      var0.render(var2, var5, 15728880, OverlayTexture.DEFAULT_UV, var4);
      var3.draw(var5.end());
   }

   private record Ghost(PlayerEntityRenderState state, Vec3d pos, boolean naked, long time) {
   }
}
