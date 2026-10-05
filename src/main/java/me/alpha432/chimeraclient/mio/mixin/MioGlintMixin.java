/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.ModifyExpressionValue
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 */
package me.alpha432.chimeraclient.mio.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTextureView;
import me.alpha432.chimeraclient.mio.support.GlintSupport;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={RenderLayer.class})
public abstract class MioGlintMixin
implements Util {
    @Shadow
    public abstract RenderPipeline method_73243();

    @ModifyExpressionValue(method={"method_60895"}, at={@At(value="FIELD", target="Lnet/minecraft/class_12247;field_63986:Lcom/mojang/blaze3d/pipeline/RenderPipeline;")})
    private RenderPipeline mio$pipeline(RenderPipeline renderPipeline) {
        return GlintSupport.pipeline(renderPipeline);
    }

    @WrapOperation(method={"method_60895"}, at={@At(value="INVOKE", target="Lcom/mojang/blaze3d/systems/RenderSystem;bindDefaultUniforms(Lcom/mojang/blaze3d/systems/RenderPass;)V")})
    private void mio$uniform(RenderPass renderPass, Operation<Void> operation) {
        operation.call(new Object[]{renderPass});
        if (GlintSupport.active(this.method_73243())) {
            GlintSupport.bind(renderPass, this.method_73243());
        }
    }

    @WrapOperation(method={"method_60895"}, at={@At(value="INVOKE", target="Lcom/mojang/blaze3d/systems/RenderPass;bindTexture(Ljava/lang/String;Lcom/mojang/blaze3d/textures/GpuTextureView;Lnet/minecraft/class_12137;)V")})
    private void mio$texture(RenderPass renderPass, String string, GpuTextureView gpuTextureView, GpuSampler gpuSampler, Operation<Void> operation) {
        if (string.equals("Sampler0") && GlintSupport.active(this.method_73243())) {
            AbstractTexture abstractTexture = mc.getTextureManager().getTexture(Identifier.of("chimeraclient", "mio/textures/shine.png"));
            gpuTextureView = abstractTexture.getGlTextureView();
            gpuSampler = abstractTexture.getSampler();
        }
        operation.call(new Object[]{renderPass, string, gpuTextureView, gpuSampler});
    }
}

