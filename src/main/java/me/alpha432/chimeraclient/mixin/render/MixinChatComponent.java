/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 */
package me.alpha432.chimeraclient.mixin.render;

import java.util.List;
import me.alpha432.chimeraclient.ducks.render.IChatComponent;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.network.message.MessageSignatureData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value={ChatHud.class})
public abstract class MixinChatComponent
implements IChatComponent {
    @Final
    @Shadow
    private List<ChatHudLine> field_2061;

    @Shadow
    private void method_44813() {
    }

    @Override
    public void chimeraclient$addMessage(ChatHudLine message) {
        this.field_2061.addFirst(message);
        this.method_44813();
    }

    @Override
    public void chimeraclient$removeMessage(MessageSignatureData signature) {
        this.field_2061.removeIf(message -> signature.equals(message.signature()));
        this.method_44813();
    }
}

