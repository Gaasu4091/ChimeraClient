package me.alpha432.chimeraclient.mio.mixin;

import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({SimpleOption.class})
public interface MioOptionAccess<T> {
   @Accessor("value")
   void mio$setValue(T var1);
}
