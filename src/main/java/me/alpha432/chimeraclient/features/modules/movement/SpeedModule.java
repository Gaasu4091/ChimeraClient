package me.alpha432.chimeraclient.features.modules.movement;

import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import net.minecraft.util.math.Vec3d;

public class SpeedModule extends Module {
   private final Setting<Double> speed = this.num("Speed", 5.0, 1.0, 50.0);
   private final Setting<Boolean> strafe = this.bool("Strafe", false);
   private final Setting<Double> strafeSpeed = this.num("StrafeSpeed", 30.0, 1.0, 50.0);

   public SpeedModule() {
      super("Speed", "Walks fast and features true air strafing", Module.Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         boolean isStrafing = this.strafe.getValue();
         if (isStrafing || mc.player.isOnGround()) {
            double currentSetting = isStrafing ? this.strafeSpeed.getValue() : this.speed.getValue();
            double blocksPerTick = currentSetting * 0.5;
            float forward = mc.player.input.getMovementInput().y;
            float strafing = mc.player.input.getMovementInput().x;
            float yaw = mc.player.getYaw();
            if (forward == 0.0F && strafing == 0.0F) {
               mc.player.setVelocity(new Vec3d(0.0, mc.player.getVelocity().y, 0.0));
            } else {
               if (forward != 0.0F) {
                  if (strafing > 0.0F) {
                     yaw += forward > 0.0F ? -45.0F : 45.0F;
                  } else if (strafing < 0.0F) {
                     yaw += forward > 0.0F ? 45.0F : -45.0F;
                  }

                  strafing = 0.0F;
                  if (forward > 0.0F) {
                     forward = 1.0F;
                  } else if (forward < 0.0F) {
                     forward = -1.0F;
                  }
               }

               double rad = Math.toRadians(yaw + 90.0F);
               double cos = Math.cos(rad);
               double sin = Math.sin(rad);
               double velX = forward * blocksPerTick * cos + strafing * blocksPerTick * sin;
               double velZ = forward * blocksPerTick * sin - strafing * blocksPerTick * cos;
               mc.player.setVelocity(new Vec3d(velX, mc.player.getVelocity().y, velZ));
            }
         }
      }
   }
}
