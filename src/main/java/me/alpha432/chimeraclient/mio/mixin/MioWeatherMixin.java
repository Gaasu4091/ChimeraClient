/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.ModifyExpressionValue
 *  com.llamalad7.mixinextras.injector.ModifyReturnValue
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 */
package me.alpha432.chimeraclient.mio.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.alpha432.chimeraclient.mio.MioConfiguredModule;
import me.alpha432.chimeraclient.mio.MioRender;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={WeatherRendering.class})
public abstract class MioWeatherMixin {
    @ModifyExpressionValue(method={"method_62315"}, at={@At(value="INVOKE", target="Lnet/minecraft/class_1937;method_8624(Lnet/minecraft/class_2902$class_2903;II)I")})
    private int mio$height(int n) {
        MioConfiguredModule mioConfiguredModule = MioConfiguredModule.active("Ambience");
        return mioConfiguredModule != null && mioConfiguredModule.b("worldWeather") && mioConfiguredModule.b("force") ? MathHelper.floor(MioRender.camera().y) - 11 : n;
    }

    @ModifyReturnValue(method={"method_62317"}, at={@At(value="RETURN")})
    private Biome.Precipitation mio$weather(Biome.Precipitation precipitation, World world, BlockPos blockPos) {
        MioConfiguredModule mioConfiguredModule = MioConfiguredModule.active("Ambience");
        if (mioConfiguredModule == null || !mioConfiguredModule.b("worldWeather")) {
            return precipitation;
        }
        return mioConfiguredModule.choice("weather", "RAIN") ? Biome.Precipitation.RAIN : (mioConfiguredModule.choice("weather", "SNOW") ? Biome.Precipitation.SNOW : Biome.Precipitation.NONE);
    }
}

