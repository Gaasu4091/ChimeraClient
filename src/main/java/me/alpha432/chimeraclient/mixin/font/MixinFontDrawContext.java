/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package me.alpha432.chimeraclient.mixin.font;

import me.alpha432.chimeraclient.features.gui.font.FontDraw;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={DrawContext.class})
public class MixinFontDrawContext {
    @Inject(method={"method_51430"}, at={@At(value="HEAD")}, cancellable=true, remap=false)
    private void chimera$styled(TextRenderer textRenderer, OrderedText orderedText, int n, int n2, int n3, boolean bl, CallbackInfo callbackInfo) {
        if (FontDraw.enabled() && !FontDraw.isFallback() && FontDraw.hasMarker(orderedText)) {
            FontDraw.drawMarked((DrawContext)((Object)this), textRenderer, orderedText, n, n2, n3, bl);
            callbackInfo.cancel();
        }
    }

    @Inject(method={"method_51433"}, at={@At(value="HEAD")}, cancellable=true, remap=false)
    private void chimera$chatInput(TextRenderer textRenderer, String string, int n, int n2, int n3, boolean bl, CallbackInfo callbackInfo) {
        if (string != null && FontDraw.chatInputScope()) {
            FontDraw.drawText((DrawContext)((Object)this), textRenderer, string, n, n2, n3, bl);
            callbackInfo.cancel();
        }
    }
}

