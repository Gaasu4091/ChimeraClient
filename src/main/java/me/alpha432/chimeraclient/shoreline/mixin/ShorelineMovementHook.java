/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.ModifyExpressionValue
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package me.alpha432.chimeraclient.shoreline.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.alpha432.chimeraclient.features.modules.combat.CrystalAura;
import me.alpha432.chimeraclient.features.modules.combat.TwoBPiston;
import me.alpha432.chimeraclient.shoreline.PortSupport;
import me.alpha432.chimeraclient.shoreline.rotation.RotationEvents;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ClientPlayerEntity.class})
public abstract class ShorelineMovementHook
implements RotationEvents.IClientPlayerEntity {
    @Shadow(remap=false)
    private float field_3941;
    @Shadow(remap=false)
    private float field_3925;
    @Unique
    private RotationEvents.MovementPacketsEvent shoreline$movement;

    @Override
    public float getLastSpoofedYaw() {
        return this.field_3941;
    }

    @Override
    public float getLastSpoofedPitch() {
        return this.field_3925;
    }

    @Inject(method={"method_5773"}, at={@At(value="INVOKE", target="Lnet/minecraft/class_742;method_5773()V", shift=At.Shift.BEFORE)}, remap=false)
    private void shoreline$preTick(CallbackInfo callbackInfo) {
        TwoBPiston twoBPiston;
        PortSupport.Managers.tick();
        CrystalAura crystalAura = CrystalAura.getInstance();
        if (crystalAura != null && crystalAura.isEnabled()) {
            crystalAura.shorelinePlayerTick();
        }
        if ((twoBPiston = TwoBPiston.getInstance()) != null && twoBPiston.isEnabled()) {
            twoBPiston.shorelinePlayerTick();
        }
        PortSupport.Managers.ROTATION.onUpdate(new RotationEvents.PlayerTickEvent());
    }

    @Inject(method={"method_3136"}, at={@At(value="HEAD")}, remap=false)
    private void shoreline$movementStart(CallbackInfo callbackInfo) {
        ClientPlayerEntity clientPlayerEntity = (ClientPlayerEntity)((Object)this);
        this.shoreline$movement = new RotationEvents.MovementPacketsEvent(clientPlayerEntity.getYaw(), clientPlayerEntity.getPitch());
        PortSupport.Managers.ROTATION.onMovementPackets(this.shoreline$movement);
    }

    @ModifyExpressionValue(method={"method_3136"}, at={@At(value="INVOKE", target="Lnet/minecraft/class_746;method_36454()F")}, remap=false)
    private float shoreline$yaw(float f) {
        return this.shoreline$movement != null && this.shoreline$movement.isCanceled() ? this.shoreline$movement.getYaw() : f;
    }

    @ModifyExpressionValue(method={"method_3136"}, at={@At(value="INVOKE", target="Lnet/minecraft/class_746;method_36455()F")}, remap=false)
    private float shoreline$pitch(float f) {
        return this.shoreline$movement != null && this.shoreline$movement.isCanceled() ? this.shoreline$movement.getPitch() : f;
    }

    @Inject(method={"method_3136"}, at={@At(value="RETURN")}, remap=false)
    private void shoreline$movementEnd(CallbackInfo callbackInfo) {
        PortSupport.Managers.ROTATION.onPlayerUpdate(new RotationEvents.PlayerUpdateEvent(RotationEvents.StageEvent.EventStage.POST));
        this.shoreline$movement = null;
    }
}

