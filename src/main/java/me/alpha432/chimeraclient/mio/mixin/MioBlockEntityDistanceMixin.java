package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.MioConfiguredModule;
import me.alpha432.chimeraclient.mio.MioRender;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BlockEntityRenderManager.class})
public abstract class MioBlockEntityDistanceMixin implements Util {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mio$distance(BlockEntityRenderState var1, MatrixStack var2, OrderedRenderCommandQueue var3, CameraRenderState var4, CallbackInfo var5) {
      MioConfiguredModule var6 = MioConfiguredModule.active("NoRender");
      if (var6 != null
         && var6.b("tileEntities")
         && (!var6.b("ignoreESP") || MioConfiguredModule.active("ESP") == null)
         && var1.pos.getSquaredDistance(MioRender.camera()) > var6.n("tileDistance") * var6.n("tileDistance")) {
         var5.cancel();
      }
   }
}
