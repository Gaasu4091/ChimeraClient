/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package me.alpha432.chimeraclient.mio.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import me.alpha432.chimeraclient.mio.MioConfiguredModule;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.texture.NativeImage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={LightmapTextureManager.class})
public abstract class MioLightmapMixin {
    @Shadow
    @Final
    private GpuTexture field_57927;

    @Inject(method={"method_3313"}, at={@At(value="RETURN")})
    private void mio$lightmap(float f, CallbackInfo callbackInfo) {
        MioConfiguredModule mioConfiguredModule = MioConfiguredModule.active("Ambience");
        if (mioConfiguredModule == null || !mioConfiguredModule.choice("brightness", "SCREEN")) {
            return;
        }
        try (NativeImage nativeImage = new NativeImage(16, 16, false);){
            for (int i = 0; i < 16; ++i) {
                for (int j = 0; j < 16; ++j) {
                    nativeImage.setColorArgb(j, i, j == 15 && i == 15 ? -1 : mioConfiguredModule.c("color").getRGB());
                }
            }
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.field_57927, nativeImage);
        }
    }

    @Inject(method={"method_23687"}, at={@At(value="HEAD")}, cancellable=true)
    private static void mio$pack(int n, int n2, CallbackInfoReturnable<Integer> callbackInfoReturnable) {
        MioConfiguredModule mioConfiguredModule = MioConfiguredModule.active("Ambience");
        if (mioConfiguredModule != null && mioConfiguredModule.choice("brightness", "SCREEN")) {
            callbackInfoReturnable.setReturnValue(0);
        } else if (mioConfiguredModule != null && mioConfiguredModule.choice("brightness", "SKY")) {
            callbackInfoReturnable.setReturnValue(n << 4 | Math.max(n2, mioConfiguredModule.i("lightLevel")) << 20);
        }
    }
}
