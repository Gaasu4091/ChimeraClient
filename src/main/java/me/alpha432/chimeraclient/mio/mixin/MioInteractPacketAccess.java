package me.alpha432.chimeraclient.mio.mixin;

import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({PlayerInteractEntityC2SPacket.class})
public interface MioInteractPacketAccess {
   @Accessor("entityId")
   int mio$entityId();
}
