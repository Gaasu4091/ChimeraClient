package me.alpha432.chimeraclient.mio.mixin;

import java.util.List;
import me.alpha432.chimeraclient.mio.MioConfiguredModule;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BlockModelRenderer.class})
public abstract class MioXrayMixin {
   private static boolean show(BlockRenderView var0, BlockState var1, BlockPos var2) {
      MioConfiguredModule var3 = MioConfiguredModule.active("Xray");
      if (var3 == null) {
         return true;
      } else if (!var3.listed("whitelist", Registries.BLOCK.getId(var1.getBlock()).toString())) {
         return false;
      } else if (!var3.b("bypass")) {
         return true;
      } else {
         for (Direction var7 : Direction.values()) {
            if (!var0.getBlockState(var2.offset(var7)).isOpaque()) {
               return true;
            }
         }

         return false;
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mio$block(
      BlockRenderView var1,
      List<BlockModelPart> var2,
      BlockState var3,
      BlockPos var4,
      MatrixStack var5,
      VertexConsumer var6,
      boolean var7,
      int var8,
      CallbackInfo var9
   ) {
      if (!show(var1, var3, var4)) {
         var9.cancel();
      }
   }

   @Inject(
      method = {"shouldDrawFace"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void mio$faces(BlockRenderView var0, BlockState var1, boolean var2, Direction var3, BlockPos var4, CallbackInfoReturnable<Boolean> var5) {
      if (MioConfiguredModule.active("Xray") != null) {
         var5.setReturnValue(show(var0, var1, var4));
      }
   }
}
