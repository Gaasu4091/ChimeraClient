package me.alpha432.chimeraclient.mio.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import me.alpha432.chimeraclient.mio.MioRender;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({WorldRenderer.class})
public class MioMatrixCapture {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void mio$capture(
      ObjectAllocator var1,
      RenderTickCounter var2,
      boolean var3,
      Camera var4,
      Matrix4f var5,
      Matrix4f var6,
      Matrix4f var7,
      GpuBufferSlice var8,
      Vector4f var9,
      boolean var10,
      CallbackInfo var11
   ) {
      MioRender.VIEW.set(var5);
      MioRender.PROJECTION.set(var6);
   }
}
