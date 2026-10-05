/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package me.alpha432.chimeraclient.mio.mixin;

import java.awt.Color;
import java.util.concurrent.ThreadLocalRandom;
import me.alpha432.chimeraclient.mio.MioConfiguredModule;
import me.alpha432.chimeraclient.mio.MioVisuals;
import me.alpha432.chimeraclient.mio.mixin.MioFireworkMarker;
import me.alpha432.chimeraclient.mio.mixin.MioParticleAlphaAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.AshParticle;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.BlockDustParticle;
import net.minecraft.client.particle.DamageParticle;
import net.minecraft.client.particle.FireworksSparkParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.PortalParticle;
import net.minecraft.client.particle.SpellParticle;
import net.minecraft.client.particle.TotemParticle;
import net.minecraft.client.particle.WhiteAshParticle;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ParticleManager.class})
public abstract class MioParticleManagerMixin {
    @Inject(method={"method_3056(Lnet/minecraft/class_2394;DDDDDD)Lnet/minecraft/class_703;"}, at={@At(value="HEAD")}, cancellable=true)
    private void mio$filter(ParticleEffect particleEffect, double d, double d2, double d3, double d4, double d5, double d6, CallbackInfoReturnable<Particle> callbackInfoReturnable) {
        MioConfiguredModule mioConfiguredModule = MioConfiguredModule.active("NoRender");
        if (mioConfiguredModule == null) {
            return;
        }
        String string = Registries.PARTICLE_TYPE.getId(particleEffect.getType()).toString();
        if (mioConfiguredModule.b("explosions") && string.contains("explosion") || mioConfiguredModule.b("particles") && !MioVisuals.selected(mioConfiguredModule, "selection2", "particleTypes", string)) {
            callbackInfoReturnable.setReturnValue(null);
        }
    }

    @Inject(method={"method_3058(Lnet/minecraft/class_703;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void mio$adjust(Particle particle, CallbackInfo callbackInfo) {
        MioConfiguredModule mioConfiguredModule = MioConfiguredModule.active("NoRender");
        if (mioConfiguredModule != null && (mioConfiguredModule.b("noBlockBreaking") && particle instanceof BlockDustParticle || mioConfiguredModule.b("potions") && particle instanceof SpellParticle)) {
            callbackInfo.cancel();
            return;
        }
        MioConfiguredModule mioConfiguredModule2 = MioConfiguredModule.active("Particles");
        if (mioConfiguredModule2 == null) {
            return;
        }
        Color color = null;
        float f = 1.0f;
        float f2 = 1.0f;
        if (particle instanceof TotemParticle) {
            if (mioConfiguredModule2.b("ignoreSelf") && MinecraftClient.getInstance().player != null && particle.getBoundingBox().intersects(MinecraftClient.getInstance().player.getBoundingBox())) {
                callbackInfo.cancel();
                return;
            }
            if (mioConfiguredModule2.b("totems")) {
                color = mioConfiguredModule2.c(ThreadLocalRandom.current().nextInt(4) == 0 ? "totemsColor1" : "totemsColor2");
                f = mioConfiguredModule2.f("totemsScale");
                f2 = mioConfiguredModule2.f("totemsVelocity");
            }
        } else if ((particle instanceof FireworksSparkParticle.FireworkParticle || particle instanceof MioFireworkMarker) && mioConfiguredModule2.b("rockets")) {
            color = mioConfiguredModule2.c("rocketColor");
            f = mioConfiguredModule2.f("rocketScale");
        } else if (particle instanceof DamageParticle && mioConfiguredModule2.b("damage")) {
            color = mioConfiguredModule2.c("damageColor");
            f = mioConfiguredModule2.f("damageScale");
            f2 = mioConfiguredModule2.f("damageVelocity");
        } else if (particle instanceof PortalParticle && mioConfiguredModule2.b("portal")) {
            color = mioConfiguredModule2.c("portalColor");
            f = mioConfiguredModule2.f("portalScale");
        } else if (particle instanceof WhiteAshParticle && mioConfiguredModule2.b("dust")) {
            color = mioConfiguredModule2.c("to").brighter();
        } else if (particle instanceof AshParticle && mioConfiguredModule2.b("dust")) {
            float f3 = ThreadLocalRandom.current().nextFloat();
            Color color2 = mioConfiguredModule2.c("from");
            Color color3 = mioConfiguredModule2.c("to");
            color = new Color((int)((float)color2.getRed() * (1.0f - f3) + (float)color3.getRed() * f3), (int)((float)color2.getGreen() * (1.0f - f3) + (float)color3.getGreen() * f3), (int)((float)color2.getBlue() * (1.0f - f3) + (float)color3.getBlue() * f3), (int)((float)color2.getAlpha() * (1.0f - f3) + (float)color3.getAlpha() * f3));
        }
        if (color != null && particle instanceof BillboardParticle) {
            BillboardParticle billboardParticle = (BillboardParticle)particle;
            billboardParticle.setColor((float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f);
            ((MioParticleAlphaAccess)((Object)particle)).mio$alpha((float)color.getAlpha() / 255.0f);
            particle.scale(f);
            particle.move(f2);
        }
    }
}

