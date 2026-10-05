package me.alpha432.chimeraclient.mio.mixin;

import java.util.Map;
import net.minecraft.client.render.OutputTarget;
import net.minecraft.client.render.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({RenderSetup.class})
public interface MioSetupAccess {
   @Accessor("textures")
   Map<String, Object> mio$textures();

   @Accessor("outputTarget")
   OutputTarget mio$target();
}
