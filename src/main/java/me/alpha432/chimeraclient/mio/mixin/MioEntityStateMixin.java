package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.MioConfiguredModule;
import me.alpha432.chimeraclient.mio.MioVisuals;
import me.alpha432.chimeraclient.mio.support.GhostSupport;
import me.alpha432.chimeraclient.mio.support.ModelSupport;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityRenderer.class})
public abstract class MioEntityStateMixin {
   @Inject(
      method = {"getAndUpdateRenderState"},
      at = {@At("RETURN")}
   )
   private void mio$snapshot(Entity var1, float var2, CallbackInfoReturnable<EntityRenderState> var3) {
      GhostSupport.observe(var1, (EntityRenderState)var3.getReturnValue());
   }

   @Inject(
      method = {"updateRenderState"},
      at = {@At("RETURN")}
   )
   private void mio$state(Entity var1, EntityRenderState var2, float var3, CallbackInfo var4) {
      ModelSupport.capture(var1, var2);
      MioConfiguredModule var5 = MioConfiguredModule.active("NoRender");
      if (var5 != null && var5.b("fire") && var5.b("others")) {
         var2.onFire = false;
      }

      if (MioVisuals.noRender("entities", "nameTags") || var1 instanceof PlayerEntity && MioConfiguredModule.active("NameTags") != null) {
         var2.displayName = null;
      }
   }
}
