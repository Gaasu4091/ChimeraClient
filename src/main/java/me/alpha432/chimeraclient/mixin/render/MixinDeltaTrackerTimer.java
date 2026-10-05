/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package me.alpha432.chimeraclient.mixin.render;

import me.alpha432.chimeraclient.ChimeraClient;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={RenderTickCounter.Dynamic.class})
public class MixinDeltaTrackerTimer {
    @Shadow
    private float field_51958;

    @Inject(method={"method_60639(J)I"}, at={@At(value="FIELD", target="Lnet/minecraft/class_9779$class_9781;field_51962:J", opcode=181)})
    public void advanceGameTime(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        this.field_51958 *= ChimeraClient.TIMER;
    }
}

