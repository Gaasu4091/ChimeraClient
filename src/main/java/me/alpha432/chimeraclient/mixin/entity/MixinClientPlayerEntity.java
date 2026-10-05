/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package me.alpha432.chimeraclient.mixin.entity;

import me.alpha432.chimeraclient.event.Stage;
import me.alpha432.chimeraclient.event.impl.entity.player.TickEvent;
import me.alpha432.chimeraclient.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ClientPlayerEntity.class})
public class MixinClientPlayerEntity {
    @Inject(method={"method_5773"}, at={@At(value="TAIL")})
    private void tickHook(CallbackInfo ci) {
        Util.EVENT_BUS.post(new TickEvent());
    }

    @Inject(method={"method_5773"}, at={@At(value="INVOKE", target="Lnet/minecraft/class_742;method_5773()V", shift=At.Shift.AFTER)})
    private void tickHook2(CallbackInfo ci) {
        Util.EVENT_BUS.post(new UpdateWalkingPlayerEvent(Stage.PRE));
    }

    @Inject(method={"method_5773"}, at={@At(value="INVOKE", target="Lnet/minecraft/class_746;method_3136()V", shift=At.Shift.AFTER)})
    private void tickHook3(CallbackInfo ci) {
        Util.EVENT_BUS.post(new UpdateWalkingPlayerEvent(Stage.POST));
    }
}

