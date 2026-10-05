package me.alpha432.chimeraclient.mio.mixin;

import java.util.Arrays;
import java.util.List;
import me.alpha432.chimeraclient.mio.MioConfiguredModule;
import me.alpha432.chimeraclient.mio.support.GlintSupport;
import me.alpha432.chimeraclient.mio.support.MaskVertices;
import me.alpha432.chimeraclient.mio.support.ModelSupport;
import me.alpha432.chimeraclient.mio.support.OpacityVertices;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.BatchingRenderCommandQueue;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.command.ModelCommandRenderer.CrumblingOverlayCommand;
import net.minecraft.client.render.command.OrderedRenderCommandQueue.Custom;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.item.ItemRenderState.Glint;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BatchingRenderCommandQueue.class})
public abstract class MioCommandQueueMixin {
   @Unique
   private boolean mio$extra;

   @Inject(
      method = {"submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private <S> void mio$model(
      Model<? super S> var1,
      S var2,
      MatrixStack var3,
      RenderLayer var4,
      int var5,
      int var6,
      int var7,
      Sprite var8,
      int var9,
      CrumblingOverlayCommand var10,
      CallbackInfo var11
   ) {
      if (!this.mio$extra) {
         if (ModelSupport.glint(var4)) {
            if (ModelSupport.opacity() == 0.0F) {
               var11.cancel();
            }
         } else {
            EntityRenderState var12 = var2 instanceof EntityRenderState var13 ? var13 : ModelSupport.scope;
            RenderCommandQueue var20 = (RenderCommandQueue)this;
            this.mio$extra = true;

            try {
               if (ModelSupport.shaderTarget(var12)) {
                  var20.submitModel(var1, var2, var3, ModelSupport.flat(var4, false, true), 15728880, var6, ModelSupport.maskColor(var12), var8, 0, null);
               }

               String var14 = var12 == null ? "OFF" : ModelSupport.chamsMode(var12);
               MioConfiguredModule var15 = MioConfiguredModule.active("Chams");
               if (!var14.equals("OFF")) {
                   S var16 = (S)ModelSupport.chamsState(var2);
                  if (var14.equals("BOTH") || var14.equals("FILL")) {
                     var20.submitModel(
                        var1, var16, var3, ModelSupport.flat(var4, false, false), 15728880, var6, ModelSupport.chamsColor(var12, false), var8, 0, null
                     );
                  }

                  if (var14.equals("BOTH") || var14.equals("LINE")) {
                     ModelSupport.edges(var20, var1, var16, var3, ModelSupport.chamsColor(var12, true), var15.f("lineWidth"));
                  }

                  if (var15.b("model") && var15.b("xqz") && var15.i("opacity") > 0) {
                     var20.submitModel(
                        var1, var2, var3, ModelSupport.textured(var4, true), 15728880, var6, ModelSupport.alpha(-1, var15.f("opacity") / 100.0F), var8, 0, null
                     );
                  }

                  if (var15.b("model") && var15.b("shine2")) {
                     var20.submitModel(var1, var2, var3, GlintSupport.chams(), 15728880, var6, -1, var8, 0, null);
                  }

                  if (var15 != null && !var15.b("model")) {
                     var11.cancel();
                  }
               }

               float var21 = ModelSupport.opacity();
               if (var21 < 1.0F) {
                  if (var21 > 0.0F) {
                     var20.submitModel(var1, var2, var3, ModelSupport.textured(var4, false), var5, var6, ModelSupport.alpha(var7, var21), var8, var9, var10);
                  }

                  var11.cancel();
               }
            } finally {
               this.mio$extra = false;
            }
         }
      }
   }

   @Inject(
      method = {"submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;ZZILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;I)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mio$part(
      ModelPart var1,
      MatrixStack var2,
      RenderLayer var3,
      int var4,
      int var5,
      Sprite var6,
      boolean var7,
      boolean var8,
      int var9,
      CrumblingOverlayCommand var10,
      int var11,
      CallbackInfo var12
   ) {
      if (!this.mio$extra) {
         this.mio$extra = true;

         try {
            RenderCommandQueue var13 = (RenderCommandQueue)this;
            if (ModelSupport.shaderTarget(ModelSupport.scope)) {
               var13.submitModelPart(var1, var2, ModelSupport.flat(var3, false, true), 15728880, var5, var6, var7, false, -1, null, 0);
            }

            float var14 = ModelSupport.opacity();
            if (var14 < 1.0F) {
               if (var14 > 0.0F) {
                  var13.submitModelPart(
                     var1, var2, ModelSupport.textured(var3, false), var4, var5, var6, var7, var8, ModelSupport.alpha(var9, var14), var10, var11
                  );
               }

               var12.cancel();
            }
         } finally {
            this.mio$extra = false;
         }
      }
   }

   @Inject(
      method = {"submitItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/item/ItemRenderState$Glint;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mio$item(
      MatrixStack var1,
      ItemDisplayContext var2,
      int var3,
      int var4,
      int var5,
      int[] var6,
      List<BakedQuad> var7,
      RenderLayer var8,
      Glint var9,
      CallbackInfo var10
   ) {
      if (!this.mio$extra) {
         this.mio$extra = true;

         try {
            RenderCommandQueue var11 = (RenderCommandQueue)this;
            if (ModelSupport.shaderTarget(ModelSupport.scope)) {
               int[] var12 = (int[])var6.clone();
               Arrays.fill(var12, ModelSupport.maskColor(ModelSupport.scope));
               var11.submitItem(var1, var2, 15728880, var4, 0, var12, var7, ModelSupport.flat(var8, false, true), Glint.NONE);
            }

            float var18 = ModelSupport.opacity();
            if (var18 < 1.0F) {
               if (var18 > 0.0F) {
                  RenderLayer var13 = ModelSupport.textured(var8, false);
                  int[] var14 = (int[])var6.clone();
                  var11.submitCustom(var1, var13, (var7x, var8x) -> {
                     MatrixStack var9x = new MatrixStack();
                     var9x.peek().copy(var7x);
                     ItemRenderer.renderItem(var2, var9x, var2xx -> new OpacityVertices(var8x, var18), var3, var4, var14, var7, var13, Glint.NONE);
                  });
               }

               var10.cancel();
            }
         } finally {
            this.mio$extra = false;
         }
      }
   }

   @Inject(
      method = {"submitCustom(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue$Custom;)V"},
      at = {@At("HEAD")}
   )
   private void mio$custom(MatrixStack var1, RenderLayer var2, Custom var3, CallbackInfo var4) {
      if (!this.mio$extra
         && !ModelSupport.glint(var2)
         && var2.getRenderPipeline().getSamplers().contains("Sampler0")
         && ModelSupport.shaderTarget(ModelSupport.scope)) {
         this.mio$extra = true;

         try {
            int var5 = ModelSupport.maskColor(ModelSupport.scope);
            ((RenderCommandQueue)this)
               .submitCustom(var1, ModelSupport.flat(var2, false, true), (var2x, var3x) -> var3.render(var2x, new MaskVertices(var3x, var5)));
         } finally {
            this.mio$extra = false;
         }
      }
   }
}
