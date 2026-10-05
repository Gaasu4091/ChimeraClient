package me.alpha432.chimeraclient.mio.mixin;

import net.minecraft.block.AbstractBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({AbstractBlock.class})
public interface MioPhaseESPBlockAccess {
   @Accessor("collidable")
   boolean mio$isCollidable();
}
