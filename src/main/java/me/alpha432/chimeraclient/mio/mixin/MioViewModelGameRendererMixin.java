/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  org.joml.Matrix4f
 *  org.lwjgl.system.MemoryStack
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package me.alpha432.chimeraclient.mio.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import me.alpha432.chimeraclient.mio.render.ViewModel;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={GameRenderer.class})
public abstract class MioViewModelGameRendererMixin {
    @Unique
    private static GpuBuffer mio$handLights;
    @Unique
    private GpuBufferSlice mio$previousLights;
    @Shadow
    @Final
    private Camera field_18765;

    @Inject(method={"method_3172(FZLorg/joml/Matrix4f;)V"}, at={@At(value="HEAD")})
    private void mio$handLightsPre(float f, boolean bl, Matrix4f matrix4f, CallbackInfo callbackInfo) {
        this.mio$previousLights = null;
        ViewModel viewModel = ViewModel.INSTANCE;
        if (viewModel == null || !viewModel.isEnabled() || viewModel.shadow.getValue().booleanValue()) {
            return;
        }
        float f2 = this.field_18765.getPitch() * ((float)Math.PI / 180);
        float f3 = -(this.field_18765.getYaw() - 45.0f) * ((float)Math.PI / 180);
        float f4 = MathHelper.cos(f2);
        float f5 = MathHelper.sin(f3) * f4;
        float f6 = -MathHelper.sin(f2);
        float f7 = MathHelper.cos(f3) * f4;
        GpuDevice gpuDevice = RenderSystem.getDevice();
        if (mio$handLights == null) {
            long l = MathHelper.roundUpToMultiple(DiffuseLighting.UBO_SIZE, gpuDevice.getUniformOffsetAlignment());
            mio$handLights = gpuDevice.createBuffer(() -> "Mio ViewModel hand lighting UBO", 136, l);
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, DiffuseLighting.UBO_SIZE).putVec3(f5, f6, f7).putVec3(-f5, -f6, -f7).get();
            gpuDevice.createCommandEncoder().writeToBuffer(mio$handLights.slice(0L, DiffuseLighting.UBO_SIZE), byteBuffer);
        }
        this.mio$previousLights = RenderSystem.getShaderLights();
        RenderSystem.setShaderLights(mio$handLights.slice(0L, DiffuseLighting.UBO_SIZE));
    }

    @Inject(method={"method_3172(FZLorg/joml/Matrix4f;)V"}, at={@At(value="RETURN")})
    private void mio$handLightsPost(float f, boolean bl, Matrix4f matrix4f, CallbackInfo callbackInfo) {
        if (this.mio$previousLights != null) {
            RenderSystem.setShaderLights(this.mio$previousLights);
            this.mio$previousLights = null;
        }
    }

    @WrapOperation(method={"method_3188(Lnet/minecraft/class_9779;)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/class_757;method_3196(Lnet/minecraft/class_4184;FZ)F", ordinal=1)})
    private float mio$handFov(GameRenderer gameRenderer, Camera camera, float f, boolean bl, Operation<Float> operation) {
        float f2 = ((Float)operation.call(new Object[]{gameRenderer, camera, Float.valueOf(f), bl})).floatValue();
        ViewModel viewModel = ViewModel.INSTANCE;
        if (viewModel != null && viewModel.isEnabled() && viewModel.viewModelFov.getValue().booleanValue()) {
            return viewModel.fovAmount.getValue().intValue();
        }
        return f2;
    }
}

