/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.ModifyArgs
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 *  org.spongepowered.asm.mixin.injection.invoke.arg.Args
 */
package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.MioConfiguredModule;
import me.alpha432.chimeraclient.mio.MioVisuals;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value={Camera.class})
public abstract class MioCameraMixin {
    @Shadow
    private Entity field_18711;
    @Shadow
    private float field_18721;
    @Shadow
    private float field_18722;

    @Inject(method={"method_19318"}, at={@At(value="HEAD")}, cancellable=true)
    private void mio$clip(float f, CallbackInfoReturnable<Float> callbackInfoReturnable) {
        MioConfiguredModule mioConfiguredModule = MioConfiguredModule.active("ViewClip");
        if (mioConfiguredModule != null) {
            callbackInfoReturnable.setReturnValue(Float.valueOf(MioVisuals.clipDistance(mioConfiguredModule)));
        }
    }

    @ModifyArgs(method={"method_19321"}, at=@At(value="INVOKE", target="Lnet/minecraft/class_4184;method_19325(FF)V", ordinal=0))
    private void mio$look(Args args) {
        if (MioConfiguredModule.active("FreeLook") != null && MioVisuals.lookReady) {
            args.set(0, (Object)Float.valueOf(MioVisuals.lookYaw));
            args.set(1, (Object)Float.valueOf(MioVisuals.lookPitch));
        }
    }

    @Inject(method={"method_19317"}, at={@At(value="HEAD")}, cancellable=true)
    private void mio$sneak(CallbackInfo callbackInfo) {
        Entity entity;
        if (!MioVisuals.noRender("self", "newSneaking") || !((entity = this.field_18711) instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity playerEntity = (PlayerEntity)entity;
        this.field_18722 = this.field_18721;
        this.field_18721 = playerEntity.isInPose(EntityPose.CROUCHING) ? 1.54f : (this.field_18721 >= 1.62f || !playerEntity.isInPose(EntityPose.STANDING) ? (this.field_18721 += (playerEntity.getStandingEyeHeight() - this.field_18721) * 0.5f) : 1.62f - (1.62f - this.field_18721) * 0.4f);
        callbackInfo.cancel();
    }
}
