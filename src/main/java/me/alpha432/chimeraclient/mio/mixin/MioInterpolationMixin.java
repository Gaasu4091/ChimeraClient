/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.MioVisuals;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={Entity.class})
public abstract class MioInterpolationMixin {
    @Shadow
    public abstract void method_5814(double var1, double var3, double var5);

    @Shadow
    public abstract void method_60608(float var1, float var2);

    @Inject(method={"method_52532"}, at={@At(value="HEAD")}, cancellable=true)
    private void mio$instant(int n, double d, double d2, double d3, double d4, double d5, CallbackInfo callbackInfo) {
        if (MioVisuals.noRender("entities", "interpolation")) {
            this.method_5814(d, d2, d3);
            this.method_60608((float)d4, (float)d5);
            callbackInfo.cancel();
        }
    }
}

