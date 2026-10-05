/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Constant
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.ModifyConstant
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.render.Animations;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.EndCrystalEntityModel;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={EndCrystalEntityModel.class})
public class MioAnimationsEndCrystalModelMixin {
    @Shadow
    @Final
    public ModelPart field_52899;
    @Shadow
    @Final
    public ModelPart field_52900;
    @Shadow
    @Final
    public ModelPart field_52901;
    @Shadow
    @Final
    public ModelPart field_52902;

    @ModifyConstant(method={"method_62083(Lnet/minecraft/class_10014;)V"}, constant={@Constant(floatValue=3.0f)})
    private float mio$rotationSpeed(float f) {
        Animations animations = Animations.INSTANCE;
        return animations != null && animations.isCrystals() ? f * animations.rotationSpeed.getValue().floatValue() : f;
    }

    @Inject(method={"method_62083(Lnet/minecraft/class_10014;)V"}, at={@At(value="TAIL")})
    private void mio$parts(EndCrystalEntityRenderState endCrystalEntityRenderState, CallbackInfo callbackInfo) {
        Animations animations = Animations.INSTANCE;
        boolean bl = animations != null;
        this.field_52899.hidden = bl && animations.hideBottom();
        this.field_52900.hidden = bl && animations.hideOuter();
        this.field_52901.hidden = bl && animations.hideInner();
        this.field_52902.hidden = bl && animations.hideCore();
    }
}

