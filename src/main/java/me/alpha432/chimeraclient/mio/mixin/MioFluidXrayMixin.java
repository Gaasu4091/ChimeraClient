package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.MioConfiguredModule;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({FluidRenderer.class})
public abstract class MioFluidXrayMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mio$fluid(BlockRenderView var1, BlockPos var2, VertexConsumer var3, BlockState var4, FluidState var5, CallbackInfo var6) {
      MioConfiguredModule var7 = MioConfiguredModule.active("Xray");
      if (var7 != null && !var7.listed("whitelist", Registries.BLOCK.getId(var5.getBlockState().getBlock()).toString())) {
         var6.cancel();
      }
   }
}
