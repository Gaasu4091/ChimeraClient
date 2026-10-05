package me.alpha432.chimeraclient.mio.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.textures.GpuTextureView;
import me.alpha432.chimeraclient.mio.support.MioShaderSupport;
import net.minecraft.client.render.OutputTarget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({RenderLayer.class})
public abstract class MioMaskTargetMixin {
   @ModifyExpressionValue(
      method = {"draw"},
      at = {@At(
         value = "FIELD",
         target = "Lcom/mojang/blaze3d/systems/RenderSystem;outputColorTextureOverride:Lcom/mojang/blaze3d/textures/GpuTextureView;"
      )}
   )
   private GpuTextureView mio$target(GpuTextureView var1) {
      RenderSetup var2 = ((MioLayerAccess)this).mio$setup();
       OutputTarget var3 = ((MioSetupAccess)(Object)var2).mio$target();
      return var3 != MioShaderSupport.TARGET && var3 != MioShaderSupport.HAND_TARGET ? var1 : null;
   }
}
