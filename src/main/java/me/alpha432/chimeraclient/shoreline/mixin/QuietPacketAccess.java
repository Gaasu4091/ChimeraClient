package me.alpha432.chimeraclient.shoreline.mixin;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({ClientConnection.class})
public interface QuietPacketAccess {
   @Invoker(
      value = "sendInternal",
      remap = false
   )
   void shoreline$sendInternal(Packet<?> var1, ChannelFutureListener var2, boolean var3);
}
