package me.alpha432.chimeraclient.mio.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.alpha432.chimeraclient.mio.MioVisuals;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeInterpolator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({EnvironmentAttributeInterpolator.class})
public abstract class MioEnvironmentMixin {
   @ModifyReturnValue(
      method = {"get"},
      at = {@At("RETURN")}
   )
   private Object mio$environment(Object var1, EnvironmentAttribute<?> var2, float var3) {
      return MioVisuals.environment(var2, var1);
   }
}
