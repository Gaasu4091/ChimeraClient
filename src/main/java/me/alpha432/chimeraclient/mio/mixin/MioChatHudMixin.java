package me.alpha432.chimeraclient.mio.mixin;

import me.alpha432.chimeraclient.mio.hud.MioHud;
import me.alpha432.chimeraclient.mio.hud.MioHudConfig;
import me.alpha432.chimeraclient.mio.hud.MioHudRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChatHud.class})
public abstract class MioChatHudMixin {
   @Unique
   private boolean mio$translated;

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void mio$start(DrawContext var1, TextRenderer var2, int var3, int var4, int var5, boolean var6, boolean var7, CallbackInfo var8) {
      MioHud var9 = MioHudRegistry.active(MioHud.Type.CHAT);
      this.mio$translated = var9 != null && MioHudConfig.INSTANCE.isEnabled();
      if (this.mio$translated) {
         var1.getMatrices().pushMatrix();
         var1.getMatrices().translate(var9.getX(), var9.getY() + var9.getHeight());
         float var10 = var9.size.getValue();
         var1.getMatrices().scale(var10, var10);
         var1.getMatrices().translate(-4.0F, -(MinecraftClient.getInstance().getWindow().getScaledHeight() - 40));
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   private void mio$end(DrawContext var1, TextRenderer var2, int var3, int var4, int var5, boolean var6, boolean var7, CallbackInfo var8) {
      if (this.mio$translated) {
         var1.getMatrices().popMatrix();
      }

      this.mio$translated = false;
   }
}
