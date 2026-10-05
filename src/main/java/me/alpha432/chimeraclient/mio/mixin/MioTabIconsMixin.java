/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.v2.WrapWithCondition
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package me.alpha432.chimeraclient.mio.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import me.alpha432.chimeraclient.mio.MioVisuals;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={PlayerListHud.class})
public abstract class MioTabIconsMixin {
    @Inject(method={"method_1923"}, at={@At(value="HEAD")}, cancellable=true)
    private void mio$ping(CallbackInfo callbackInfo) {
        if (MioVisuals.noRender("ui", "tabIcons")) {
            callbackInfo.cancel();
        }
    }

    @WrapWithCondition(method={"method_1919"}, at={@At(value="INVOKE", target="Lnet/minecraft/class_7532;method_44445(Lnet/minecraft/class_332;Lnet/minecraft/class_2960;IIIZZI)V")})
    private boolean mio$head(DrawContext drawContext, Identifier identifier, int n, int n2, int n3, boolean bl, boolean bl2, int n4) {
        return !MioVisuals.noRender("ui", "tabIcons");
    }
}

