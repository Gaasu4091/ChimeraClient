package me.alpha432.chimeraclient.shoreline.rotation;

import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.Vec3d;

public final class RotationEvents {
   public static class AnticheatModule {
      private static final RotationEvents.AnticheatModule INSTANCE = new RotationEvents.AnticheatModule();

      public static RotationEvents.AnticheatModule getInstance() {
         return INSTANCE;
      }

      public boolean getWebJumpFix() {
         return false;
      }
   }

   public interface Globals extends Util {
   }

   public interface IClientPlayerEntity {
      float getLastSpoofedYaw();

      float getLastSpoofedPitch();
   }

   public static class Interpolation {
      public static float interpolateFloat(float var0, float var1, float var2) {
         return var0 + (var1 - var0) * var2;
      }
   }

   public static class KeyboardTickEvent {
      public float forward;
      public float sideways;

      public KeyboardTickEvent(float var1, float var2) {
         this.forward = var1;
         this.sideways = var2;
      }
   }

   public static class MovementPacketsEvent {
      private float yaw;
      private float pitch;
      private boolean canceled;

      public MovementPacketsEvent(float var1, float var2) {
         this.yaw = var1;
         this.pitch = var2;
      }

      public void cancel() {
         this.canceled = true;
      }

      public boolean isCanceled() {
         return this.canceled;
      }

      public float getYaw() {
         return this.yaw;
      }

      public float getPitch() {
         return this.pitch;
      }

      public void setYaw(float var1) {
         this.yaw = var1;
      }

      public void setPitch(float var1) {
         this.pitch = var1;
      }
   }

   public static class PacketEvent {
      public record Outbound(Packet<?> packet) {
         public Packet<?> getPacket() {
            return this.packet;
         }
      }
   }

   public record PlayerJumpEvent(RotationEvents.StageEvent.EventStage stage) {
      public RotationEvents.StageEvent.EventStage getStage() {
         return this.stage;
      }
   }

   public static class PlayerTickEvent {
   }

   public record PlayerUpdateEvent(RotationEvents.StageEvent.EventStage stage) {
      public RotationEvents.StageEvent.EventStage getStage() {
         return this.stage;
      }
   }

   public static class RenderPlayerEvent {
      private final Entity entity;
      private float yaw;
      private float pitch;

      public RenderPlayerEvent(Entity var1, float var2, float var3) {
         this.entity = var1;
         this.yaw = var2;
         this.pitch = var3;
      }

      public Entity getEntity() {
         return this.entity;
      }

      public float getYaw() {
         return this.yaw;
      }

      public float getPitch() {
         return this.pitch;
      }

      public void setYaw(float var1) {
         this.yaw = var1;
      }

      public void setPitch(float var1) {
         this.pitch = var1;
      }

      public void cancel() {
      }
   }

   public static class StageEvent {
      public static enum EventStage {
         PRE,
         POST;
      }
   }

   public static class UpdateVelocityEvent {
      private final Vec3d input;
      private final float speed;
      private Vec3d velocity;

      public UpdateVelocityEvent(Vec3d var1, float var2) {
         this.input = var1;
         this.speed = var2;
      }

      public void cancel() {
      }

      public Vec3d getMovementInput() {
         return this.input;
      }

      public float getSpeed() {
         return this.speed;
      }

      public void setVelocity(Vec3d var1) {
         this.velocity = var1;
      }
   }
}
