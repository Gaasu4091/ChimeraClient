package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.render.Animations;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({EndCrystalEntityRenderer.class})
public class MioAnimationsEndCrystalRendererMixin {
   @ModifyArgs(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V",
         ordinal = 0
      )
   )
   private void mio$crystalScale(Args var1) {
      Animations var2 = Animations.INSTANCE;
      if (var2 != null && var2.isCrystals()) {
         float var3 = var2.crystalScale.getValue();
         var1.set(0, (Float)var1.get(0) * var3);
         var1.set(1, (Float)var1.get(1) * var3);
         var1.set(2, (Float)var1.get(2) * var3);
      }
   }

   @Inject(
      method = {"getYOffset"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void mio$floatFactor(float var0, CallbackInfoReturnable<Float> var1) {
      Animations var2 = Animations.INSTANCE;
      if (var2 != null && var2.isCrystals()) {
         float var3 = MathHelper.sin(var0 * 0.2F) / 2.0F + 0.5F;
         var1.setReturnValue((var3 * var3 + var3) * var2.floatFactor.getValue() - 1.4F);
      }
   }
}
