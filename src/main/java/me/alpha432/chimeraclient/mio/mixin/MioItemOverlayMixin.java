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

import me.alpha432.chimeraclient.mio.support.TooltipsSupport;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={DrawContext.class})
public abstract class MioItemOverlayMixin {
    @Inject(method={"method_51432(Lnet/minecraft/class_327;Lnet/minecraft/class_1799;IILjava/lang/String;)V"}, at={@At(value="RETURN")})
    private void mio$overlay(TextRenderer textRenderer, ItemStack itemStack, int n, int n2, String string, CallbackInfo callbackInfo) {
        TooltipsSupport.itemOverlay((DrawContext)((Object)this), itemStack, n, n2);
    }
}

