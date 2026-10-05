/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package me.alpha432.chimeraclient.shoreline.mixin;

import me.alpha432.chimeraclient.shoreline.PortSupport;
import me.alpha432.chimeraclient.shoreline.piston.PistonInteraction;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={ClientPlayerInteractionManager.class})
public abstract class ShorelineInteractionHook {
    @Redirect(method={"method_41934"}, at=@At(value="INVOKE", target="Lnet/minecraft/class_746;method_21823()Z"), remap=false)
    private boolean shoreline$packetSneaking(ClientPlayerEntity clientPlayerEntity) {
        return clientPlayerEntity.isSneaking() || PistonInteraction.INSTANCE.isPacketSneaking();
    }

    @Redirect(method={"method_41934"}, at=@At(value="INVOKE", target="Lnet/minecraft/class_746;method_5998(Lnet/minecraft/class_1268;)Lnet/minecraft/class_1799;"), remap=false)
    private ItemStack shoreline$serverHeldItem(ClientPlayerEntity clientPlayerEntity, Hand hand) {
        if (hand == Hand.OFF_HAND || !PortSupport.Managers.INVENTORY.isDesynced()) {
            return clientPlayerEntity.getStackInHand(hand);
        }
        return PortSupport.Managers.INVENTORY.getServerItem();
    }

    @Redirect(method={"method_41934"}, at=@At(value="INVOKE", target="Lnet/minecraft/class_1799;method_7960()Z", ordinal=0), remap=false)
    private boolean shoreline$serverMainHandEmpty(ItemStack itemStack) {
        return PortSupport.Managers.INVENTORY.isDesynced() ? PortSupport.Managers.INVENTORY.getServerItem().isEmpty() : itemStack.isEmpty();
    }
}

