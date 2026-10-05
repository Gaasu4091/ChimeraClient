package me.alpha432.chimeraclient.mio.support;

import java.awt.Color;
import me.alpha432.chimeraclient.mio.MioConfiguredModule;
import me.alpha432.chimeraclient.mio.MioRender;
import me.alpha432.chimeraclient.mio.render.Animations;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class SkeletonSupport implements Util {
   public static void draw(MioConfiguredModule var0, MatrixStack var1, float var2) {
      for (AbstractClientPlayerEntity var4 : mc.world.getPlayers()) {
         if (var4 != mc.player && var4.isAlive() && mc.getEntityRenderDispatcher().getRenderer(var4) instanceof PlayerEntityRenderer var5) {
            PlayerEntityRenderState var17 = (PlayerEntityRenderState)var5.getAndUpdateRenderState(var4, var2);
            PlayerEntityModel var7 = (PlayerEntityModel)var5.getModel();
            var7.setAngles(var17);
            Vec3d var8 = MioRender.lerp(var4, var2);
            boolean var9 = var17.isInSneakingPose;
            float var10 = var9 ? 0.6F : 0.75F;
            float var11 = var9 ? 1.05F : 1.35F;
            float var12 = var9 ? 1.05F : 1.4F;
            float var13 = var9 ? 0.25F : 0.0F;
            Matrix4f var14 = new Matrix4f().translation((float)var8.x, (float)var8.y, (float)var8.z).rotateY((float)Math.toRadians(-var17.bodyYaw));
            if (var4.isInSwimmingPose() || var4.isGliding()) {
               var14.translate(0.0F, var4.isInSwimmingPose() ? 0.3F : 0.0F, 0.0F).rotateX((float)Math.toRadians(-90.0F - var17.pitch));
            }

            Animations var15 = Animations.INSTANCE;
            if (var15 != null && var15.isPlayerScaled()) {
               var14.scale(var15.playerScale.getValue());
            }

            Color var16 = var0.c("color");
            line(var1, var14, 0.0F, var10, var13, 0.0F, var12, 0.0F, var16);
            line(var1, var14, -0.375F, var11, 0.0F, 0.375F, var11, 0.0F, var16);
            line(var1, var14, -0.125F, var10, var13, 0.125F, var10, var13, var16);
            limb(var1, var14, var7.head, 0.0F, var12, 0.0F, 0.125F, var16);
            limb(var1, var14, var7.leftLeg, -0.125F, var10, var13, -0.75F, var16);
            limb(var1, var14, var7.rightLeg, 0.125F, var10, var13, -0.75F, var16);
            limb(var1, var14, var7.leftArm, -0.375F, var11, 0.0F, -0.7F, var16);
            limb(var1, var14, var7.rightArm, 0.375F, var11, 0.0F, -0.7F, var16);
         }
      }
   }

   private static void limb(MatrixStack var0, Matrix4f var1, ModelPart var2, float var3, float var4, float var5, float var6, Color var7) {
      Matrix4f var8 = new Matrix4f(var1).translate(var3, var4, var5).rotateZ(var2.roll).rotateY(-var2.yaw).rotateX(-var2.pitch);
      line(var0, var8, 0.0F, 0.0F, 0.0F, 0.0F, var6, 0.0F, var7);
   }

   private static void line(MatrixStack var0, Matrix4f var1, float var2, float var3, float var4, float var5, float var6, float var7, Color var8) {
      Vector3f var9 = var1.transformPosition(new Vector3f(var2, var3, var4));
      Vector3f var10 = var1.transformPosition(new Vector3f(var5, var6, var7));
      MioRender.line(var0, new Vec3d(var9.x, var9.y, var9.z), new Vec3d(var10.x, var10.y, var10.z), var8, 1.0F);
   }
}
