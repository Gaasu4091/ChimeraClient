package me.alpha432.chimeraclient.shoreline.rotation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import me.alpha432.chimeraclient.features.modules.client.ShorelineRotationsModule;
import me.alpha432.chimeraclient.shoreline.PortSupport;
import me.alpha432.chimeraclient.shoreline.util.player.PlayerUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RotationManager implements RotationEvents.Globals {
   private final List<Rotation> requests = new CopyOnWriteArrayList<>();
   private float serverYaw;
   private float serverPitch;
   private float lastServerYaw;
   private float lastServerPitch;
   private float prevJumpYaw;
   private float prevYaw;
   private float prevPitch;
   boolean rotate;
   private Rotation rotation;
   private int rotateTicks;
   private boolean webJumpFix;
   private boolean preJumpFix;

   public void onPacketOutbound(RotationEvents.PacketEvent.Outbound var1) {
      if (mc.player != null && mc.world != null) {
         Packet var3 = var1.getPacket();
         PlayerMoveC2SPacket var2;
         if (var3 instanceof PlayerMoveC2SPacket && (var2 = (PlayerMoveC2SPacket)var3).changesLook()) {
            float var4 = var2.getYaw(0.0F);
            float var5 = var2.getPitch(0.0F);
            this.serverYaw = var4;
            this.serverPitch = var5;
         }
      }
   }

   public void onUpdate(RotationEvents.PlayerTickEvent var1) {
      this.webJumpFix = PlayerUtil.inWeb(1.0);
      if (this.requests.isEmpty()) {
         this.rotation = null;
      } else {
         Rotation var2 = this.getRotationRequest();
         if (var2 == null) {
            if (this.isDoneRotating()) {
               this.rotation = null;
               return;
            }
         } else {
            this.rotation = var2;
         }

         if (this.rotation != null) {
            this.rotateTicks = 0;
            this.rotate = true;
         }
      }
   }

   public void onMovementPackets(RotationEvents.MovementPacketsEvent var1) {
      if (this.rotation != null) {
         if (this.rotate) {
            this.removeRotation(this.rotation);
            var1.cancel();
            var1.setYaw(this.rotation.getYaw());
            var1.setPitch(this.rotation.getPitch());
            this.rotate = false;
         }

         if (this.rotation.isSnap()) {
            this.rotation = null;
         }
      }
   }

   public void onPlayerUpdate(RotationEvents.PlayerUpdateEvent var1) {
      if (var1.getStage() == RotationEvents.StageEvent.EventStage.POST) {
         this.lastServerYaw = ((RotationEvents.IClientPlayerEntity)mc.player).getLastSpoofedYaw();
         this.lastServerPitch = ((RotationEvents.IClientPlayerEntity)mc.player).getLastSpoofedPitch();
      }
   }

   public void onKeyboardTick(RotationEvents.KeyboardTickEvent var1) {
      if (this.rotation != null && mc.player != null && ShorelineRotationsModule.getInstance().getMovementFix()) {
         float var2 = var1.forward;
         float var3 = var1.sideways;
         float var4 = (mc.player.getYaw() - this.rotation.getYaw()) * (float) (Math.PI / 180.0);
         float var5 = MathHelper.cos(var4);
         float var6 = MathHelper.sin(var4);
         var1.sideways = Math.round(var3 * var5 - var2 * var6);
         var1.forward = Math.round(var2 * var5 + var3 * var6);
      }
   }

   public void onUpdateVelocity(RotationEvents.UpdateVelocityEvent var1) {
      if (this.rotation != null && ShorelineRotationsModule.getInstance().getMovementFix()) {
         var1.cancel();
         var1.setVelocity(this.movementInputToVelocity(this.rotation.getYaw(), var1.getMovementInput(), var1.getSpeed()));
      }
   }

   public void onPlayerJump(RotationEvents.PlayerJumpEvent var1) {
      if (this.rotation != null && ShorelineRotationsModule.getInstance().getMovementFix()) {
         if (var1.getStage() == RotationEvents.StageEvent.EventStage.PRE) {
            this.prevJumpYaw = mc.player.getYaw();
            mc.player.setYaw(this.rotation.getYaw());
            if (RotationEvents.AnticheatModule.getInstance().getWebJumpFix() && this.webJumpFix) {
               this.preJumpFix = mc.player.isSprinting();
               mc.player.setSprinting(false);
            }
         } else {
            mc.player.setYaw(this.prevJumpYaw);
            if (this.webJumpFix) {
               mc.player.setSprinting(this.preJumpFix);
            }
         }
      }
   }

   public void onRenderPlayer(RotationEvents.RenderPlayerEvent var1) {
      if (var1.getEntity() == mc.player && this.rotation != null) {
         var1.setYaw(RotationEvents.Interpolation.interpolateFloat(this.prevYaw, this.getServerYaw(), mc.getRenderTickCounter().getTickProgress(true)));
         var1.setPitch(RotationEvents.Interpolation.interpolateFloat(this.prevPitch, this.getServerPitch(), mc.getRenderTickCounter().getTickProgress(true)));
         this.prevYaw = var1.getYaw();
         this.prevPitch = var1.getPitch();
         var1.cancel();
      }
   }

   public void setRotation(Rotation var1) {
      if (ShorelineRotationsModule.getInstance().getMouseSensFix()) {
         double var3 = Math.pow((Double)mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0) * 1.2;
         var1.setYaw((float)(var1.getYaw() - (var1.getYaw() - this.serverYaw) % var3));
         var1.setPitch((float)(var1.getPitch() - (var1.getPitch() - this.serverPitch) % var3));
      }

      if (var1.getPriority() == Integer.MAX_VALUE) {
         this.rotation = var1;
      }

      Rotation var2;
      if ((var2 = this.requests.stream().filter(var1x -> var1.getPriority() == var1x.getPriority()).findFirst().orElse(null)) == null) {
         this.requests.add(var1);
      } else {
         var2.setYaw(var1.getYaw());
         var2.setPitch(var1.getPitch());
      }
   }

   public void setRotationClient(float var1, float var2) {
      if (mc.player != null) {
         mc.player.setYaw(var1);
         mc.player.setPitch(MathHelper.clamp(var2, -90.0F, 90.0F));
      }
   }

   public void setRotationSilent(float var1, float var2) {
      this.setRotation(new Rotation(Integer.MAX_VALUE, var1, var2, true));
      PortSupport.Managers.NETWORK
         .sendPacket(new Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), var1, var2, mc.player.isOnGround(), mc.player.horizontalCollision));
   }

   public void setRotationSilentSync() {
      float var1 = mc.player.getYaw();
      float var2 = mc.player.getPitch();
      this.setRotation(new Rotation(Integer.MAX_VALUE, var1, var2, true));
      PortSupport.Managers.NETWORK
         .sendPacket(new Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), var1, var2, mc.player.isOnGround(), mc.player.horizontalCollision));
   }

   public boolean removeRotation(Rotation var1) {
      return this.requests.remove(var1);
   }

   public boolean isRotationBlocked(int var1) {
      return this.rotation != null && var1 < this.rotation.getPriority();
   }

   public boolean isDoneRotating() {
      return this.rotateTicks > ShorelineRotationsModule.getInstance().getPreserveTicks();
   }

   public boolean isRotating() {
      return this.rotation != null;
   }

   public float getRotationYaw() {
      return this.rotation.getYaw();
   }

   public float getRotationPitch() {
      return this.rotation.getPitch();
   }

   public float getServerYaw() {
      return this.serverYaw;
   }

   public float getWrappedYaw() {
      return MathHelper.wrapDegrees(this.serverYaw);
   }

   public float getServerPitch() {
      return this.serverPitch;
   }

   private Vec3d movementInputToVelocity(float var1, Vec3d var2, float var3) {
      double var4 = var2.lengthSquared();
      if (var4 < 1.0E-7) {
         return Vec3d.ZERO;
      } else {
         Vec3d var6 = (var4 > 1.0 ? var2.normalize() : var2).multiply(var3);
         float var7 = MathHelper.sin(var1 * (float) (Math.PI / 180.0));
         float var8 = MathHelper.cos(var1 * (float) (Math.PI / 180.0));
         return new Vec3d(var6.x * var8 - var6.z * var7, var6.y, var6.z * var8 + var6.x * var7);
      }
   }

   private Rotation getRotationRequest() {
      Rotation var1 = null;
      int var2 = 0;

      for (Rotation var4 : this.requests) {
         if (var4.getPriority() > var2) {
            var1 = var4;
            var2 = var4.getPriority();
         }
      }

      return var1;
   }
}
