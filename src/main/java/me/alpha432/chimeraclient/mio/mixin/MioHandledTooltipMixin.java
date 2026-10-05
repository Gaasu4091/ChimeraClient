/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.support.TooltipsSupport;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={HandledScreen.class})
public abstract class MioHandledTooltipMixin {
    @Shadow
    protected Slot field_2787;

    @Inject(method={"method_2380"}, at={@At(value="HEAD")})
    private void mio$stack(DrawContext drawContext, int n, int n2, CallbackInfo callbackInfo) {
        ItemStack itemStack = TooltipsSupport.hovered = this.field_2787 == null ? ItemStack.EMPTY : this.field_2787.getStack();
        if (MinecraftClient.getInstance().isAltPressed()) {
            TooltipsSupport.openPreview(true);
        }
    }

    @Inject(method={"method_25402"}, at={@At(value="HEAD")}, cancellable=true)
    private void mio$preview(Click click, boolean bl, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (click.button() == 2 && this.field_2787 != null) {
            TooltipsSupport.hovered = this.field_2787.getStack();
            if (TooltipsSupport.openPreview()) {
                callbackInfoReturnable.setReturnValue(true);
            }
        }
    }
}
