package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.render.Animations;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({PlayerEntityRenderer.class})
public abstract class MioAnimationsPlayerRendererMixin {
   @Unique
   private PlayerEntityRenderState mio$scaled;

   @Inject(
      method = {"scale"},
      at = {@At("HEAD")}
   )
   private void mio$scalePre(PlayerEntityRenderState var1, MatrixStack var2, CallbackInfo var3) {
      this.mio$scaled = var1;
   }

   @ModifyArgs(
      method = {"scale"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"
      )
   )
   private void mio$playerScale(Args var1) {
      Animations var2 = Animations.INSTANCE;
      if (var2 != null && var2.isPlayerScaled() && !mio$isSelf(this.mio$scaled)) {
         float var3 = var2.playerScale.getValue();
         var1.set(0, (Float)var1.get(0) * var3);
         var1.set(1, (Float)var1.get(1) * var3);
         var1.set(2, (Float)var1.get(2) * var3);
      }
   }

   @Inject(
      method = {"updateRenderState"},
      at = {@At("TAIL")}
   )
   private void mio$sneak(PlayerLikeEntity var1, PlayerEntityRenderState var2, float var3, CallbackInfo var4) {
      Animations var5 = Animations.INSTANCE;
      if (var5 != null && var5.isSneak() && var1 != MinecraftClient.getInstance().player) {
         var2.isInSneakingPose = true;
      }
   }

   @Unique
   private static boolean mio$isSelf(PlayerEntityRenderState var0) {
      MinecraftClient var1 = MinecraftClient.getInstance();
      return var0 != null && var1.player != null && var0.id == var1.player.getId();
   }
}
