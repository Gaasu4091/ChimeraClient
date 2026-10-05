/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.ModifyExpressionValue
 *  com.llamalad7.mixinextras.sugar.Local
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 */
package me.alpha432.chimeraclient.mio.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import me.alpha432.chimeraclient.mio.render.Animations;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={LivingEntityRenderer.class})
public abstract class MioAnimationsLivingRendererMixin {
    @ModifyExpressionValue(method={"method_62355(Lnet/minecraft/class_1309;Lnet/minecraft/class_10042;F)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/class_1309;method_5765()Z")})
    private boolean mio$staticPlayers(boolean bl, @Local(argsOnly=true) LivingEntity livingEntity) {
        Animations animations = Animations.INSTANCE;
        if (animations != null && animations.isStatic() && livingEntity instanceof PlayerEntity) {
            return true;
        }
        return bl;
    }
}

