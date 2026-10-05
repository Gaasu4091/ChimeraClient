/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.ModifyReturnValue
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 */
package me.alpha432.chimeraclient.mio.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.alpha432.chimeraclient.mio.MioVisuals;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={ChatHudLine.class})
public abstract class MioChatIndicatorMixin {
    @ModifyReturnValue(method={"comp_894"}, at={@At(value="RETURN")})
    private MessageIndicator mio$indicator(MessageIndicator messageIndicator) {
        return MioVisuals.noRender("ui", "messageIndicator") ? null : messageIndicator;
    }
}

