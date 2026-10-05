package me.alpha432.chimeraclient.mio.mixin;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({RenderLayer.class})
public interface MioLayerAccess {
   @Accessor("renderSetup")
   RenderSetup mio$setup();
}
