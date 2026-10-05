package me.alpha432.chimeraclient.mio.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.alpha432.chimeraclient.mio.MioConfiguredModule;
import me.alpha432.chimeraclient.mio.MioVisuals;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameRenderer.class})
public abstract class MioGameRendererMixin {
   @ModifyReturnValue(
      method = {"getFov"},
      at = {@At("RETURN")}
   )
   private float mio$zoom(float var1) {
      return MioVisuals.zoom(var1);
   }

   @Inject(
      method = {"bobView"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mio$bob(MatrixStack var1, float var2, CallbackInfo var3) {
      MioConfiguredModule var4 = MioConfiguredModule.active("NoBob");
      if (var4 != null && var4.f("multiplier") == 0.0F) {
         var3.cancel();
      }
   }

   @ModifyExpressionValue(
      method = {"bobView"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerLikeState;lerpMovement(F)F"
      )}
   )
   private float mio$bobAmount(float var1) {
      MioConfiguredModule var2 = MioConfiguredModule.active("NoBob");
      return var2 == null ? var1 : var1 * var2.f("multiplier");
   }

   @Inject(
      method = {"tiltViewWhenHurt"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mio$hurt(MatrixStack var1, float var2, CallbackInfo var3) {
      if (MioVisuals.noRender("self", "hurtCam")) {
         var3.cancel();
      }
   }

   @Inject(
      method = {"showFloatingItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mio$totem(ItemStack var1, CallbackInfo var2) {
      if (MioVisuals.noRender("self", "totemOverlay")) {
         var2.cancel();
      }
   }

   @Inject(
      method = {"renderHand"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mio$hands(float var1, boolean var2, Matrix4f var3, CallbackInfo var4) {
      MioConfiguredModule var5 = MioConfiguredModule.active("NoRender");
      if (var5 != null && var5.b("hands") && var5.f("opacity2") == 0.0F) {
         var4.cancel();
      }
   }
}
