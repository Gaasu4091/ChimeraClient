package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.MioConfiguredModule;
import me.alpha432.chimeraclient.mio.MioVisuals;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.dimension.DimensionType.Skybox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SkyRendering.class})
public abstract class MioSkyMixin {
   @Inject(
      method = {"updateRenderState"},
      at = {@At("RETURN")}
   )
   private void mio$sky(ClientWorld var1, float var2, Camera var3, SkyRenderState var4, CallbackInfo var5) {
      MioConfiguredModule var6 = MioConfiguredModule.active("SkyColor");
      if (var6 != null) {
         String var7 = var1.getRegistryKey().getValue().getPath();
         if (var6.b(var7.equals("the_nether") ? "nether" : (var7.equals("the_end") ? "end" : "overworld"))) {
            if (var6.choice("type", "END")) {
               var4.skybox = Skybox.END;
            } else {
               var4.skyColor = var6.c("sky").getRGB();
               if (var6.choice("type", "FLAT")) {
                  var4.skybox = Skybox.OVERWORLD;
                  var4.starBrightness = 0.0F;
                  var4.sunriseAndSunsetColor = 0;
                  var4.rainGradient = 1.0F;
               }
            }
         }
      }

      if (MioVisuals.noRender("world", "skyLight")) {
         var4.skybox = Skybox.NONE;
      }
   }
}
