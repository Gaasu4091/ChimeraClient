/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  io.netty.channel.Channel
 *  io.netty.channel.ChannelFutureListener
 *  io.netty.channel.ChannelHandlerContext
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package me.alpha432.chimeraclient.mixin.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import me.alpha432.chimeraclient.event.impl.network.PacketEvent;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ClientConnection.class})
public class MixinConnection {
    @Shadow
    private Channel field_11651;
    @Shadow
    @Final
    private NetworkSide field_11643;

    @Inject(method={"method_10770"}, at={@At(value="HEAD")}, cancellable=true)
    public void channelRead0(ChannelHandlerContext chc, Packet<?> packet, CallbackInfo ci) {
        if (this.field_11651.isOpen() && packet != null) {
            try {
                if (Util.EVENT_BUS.post(new PacketEvent.Receive(packet))) {
                    ci.cancel();
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    @Inject(method={"method_10764"}, at={@At(value="HEAD")}, cancellable=true)
    private void sendPacket(Packet<?> packet, ChannelFutureListener callbacks, boolean flush, CallbackInfo ci) {
        if (this.field_11643 != NetworkSide.CLIENTBOUND) {
            return;
        }
        try {
            if (Util.EVENT_BUS.post(new PacketEvent.Send(packet))) {
                ci.cancel();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}

