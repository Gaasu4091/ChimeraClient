/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package me.alpha432.chimeraclient.shoreline.mixin;

import me.alpha432.chimeraclient.features.modules.combat.CrystalAura;
import me.alpha432.chimeraclient.shoreline.PortSupport;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={MinecraftClient.class})
public class DisconnectHook {
    @Inject(method={"method_18096"}, at={@At(value="HEAD")}, remap=false)
    private void chimera$crystalDisconnect(Screen screen, boolean bl, boolean bl2, CallbackInfo callbackInfo) {
        CrystalAura crystalAura = CrystalAura.getInstance();
        if (crystalAura != null && crystalAura.isEnabled()) {
            crystalAura.onDisconnect(new PortSupport.DisconnectEvent());
        }
        PortSupport.reset();
    }
}

