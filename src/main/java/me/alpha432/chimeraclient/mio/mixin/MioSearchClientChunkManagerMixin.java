/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package me.alpha432.chimeraclient.mio.mixin;

import java.util.Map;
import java.util.function.Consumer;
import me.alpha432.chimeraclient.mio.support.SearchChunkScanner;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ClientChunkManager.class})
public class MioSearchClientChunkManagerMixin {
    @Inject(method={"method_16020(IILnet/minecraft/class_2540;Ljava/util/Map;Ljava/util/function/Consumer;)Lnet/minecraft/class_2818;"}, at={@At(value="RETURN")})
    private void mio$onChunkLoaded(int n, int n2, PacketByteBuf packetByteBuf, Map<Heightmap.Type, long[]> map, Consumer<ChunkData.BlockEntityVisitor> consumer, CallbackInfoReturnable<WorldChunk> callbackInfoReturnable) {
        WorldChunk worldChunk = (WorldChunk)callbackInfoReturnable.getReturnValue();
        if (worldChunk != null) {
            SearchChunkScanner.chunkLoaded(worldChunk);
        }
    }
}

