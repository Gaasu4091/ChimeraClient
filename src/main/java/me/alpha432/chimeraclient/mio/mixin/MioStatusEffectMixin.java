package me.alpha432.chimeraclient.mio.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.alpha432.chimeraclient.mio.MioVisuals;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({LivingEntity.class})
public abstract class MioStatusEffectMixin {
   @ModifyReturnValue(
      method = {"getStatusEffect"},
      at = {@At("RETURN")}
   )
   private StatusEffectInstance mio$visual(StatusEffectInstance var1, RegistryEntry<StatusEffect> var2) {
      return (var2 != StatusEffects.BLINDNESS || !MioVisuals.noRender("self", "blindness"))
            && (var2 != StatusEffects.DARKNESS || !MioVisuals.noRender("self", "darkness"))
         ? var1
         : null;
   }
}
