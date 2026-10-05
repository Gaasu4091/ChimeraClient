/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.MioVisuals;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.TutorialToast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={SystemToast.class, TutorialToast.class})
public abstract class MioToastMixin {
    @Inject(method={"method_1986"}, at={@At(value="HEAD")}, cancellable=true)
    private void mio$hide(CallbackInfo callbackInfo) {
        if ((Object)this instanceof TutorialToast && MioVisuals.noRender("ui", "tutorialToast") || (Object)this instanceof SystemToast systemToast && systemToast.getType() == SystemToast.Type.UNSECURE_SERVER_WARNING && MioVisuals.noRender("ui", "unsecureServer")) {
            callbackInfo.cancel();
        }
    }
}
