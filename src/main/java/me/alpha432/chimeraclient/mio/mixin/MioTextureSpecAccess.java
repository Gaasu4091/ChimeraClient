/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package me.alpha432.chimeraclient.mio.mixin;

import java.util.function.Supplier;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets={"net/minecraft/class_12247$class_12338"})
public interface MioTextureSpecAccess {
    @Accessor(value="comp_5228")
    public Identifier mio$location();

    @Accessor(value="comp_5229")
    public Supplier<GpuSampler> mio$sampler();
}

