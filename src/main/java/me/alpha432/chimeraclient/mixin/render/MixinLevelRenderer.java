package me.alpha432.chimeraclient.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import me.alpha432.chimeraclient.event.impl.render.Render3DEvent;
import me.alpha432.chimeraclient.event.impl.render.RenderBlockOutlineEvent;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({WorldRenderer.class})
public class MixinLevelRenderer {
   @Inject(
      method = {"renderTargetBlockOutline"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void renderBlockOutline(Immediate bufferSource, MatrixStack poseStack, boolean bl, WorldRenderState levelRenderState, CallbackInfo ci) {
      if (Util.EVENT_BUS.post(new RenderBlockOutlineEvent())) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   private void render(
      ObjectAllocator allocator,
      RenderTickCounter tickCounter,
      boolean renderBlockOutline,
      Camera camera,
      Matrix4f positionMatrix,
      Matrix4f matrix4f,
      Matrix4f projectionMatrix,
      GpuBufferSlice fogBuffer,
      Vector4f fogColor,
      boolean renderSky,
      CallbackInfo ci,
      @Local Profiler profiler
   ) {
      MatrixStack stack = new MatrixStack();
      stack.push();
      stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(Util.mc.gameRenderer.getCamera().getPitch()));
      stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(Util.mc.gameRenderer.getCamera().getYaw() + 180.0F));
      profiler.push("chimeraclient-render-3d");
      Render3DEvent event = new Render3DEvent(stack, tickCounter.getTickProgress(true));
      Util.EVENT_BUS.post(event);
      stack.pop();
      profiler.pop();
   }
}
