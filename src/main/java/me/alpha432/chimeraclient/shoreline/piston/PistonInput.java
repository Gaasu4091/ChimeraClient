package me.alpha432.chimeraclient.shoreline.piston;

import me.alpha432.chimeraclient.shoreline.PortSupport;
import me.alpha432.chimeraclient.shoreline.mixin.InputMovementAccess;
import me.alpha432.chimeraclient.shoreline.util.Globals;
import me.alpha432.chimeraclient.shoreline.util.player.EnchantmentUtil;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

public final class PistonInput implements Globals {
   private static boolean packetSneaking;

   private PistonInput() {
   }

   public static boolean isPacketSneaking() {
      return packetSneaking;
   }

   public static void setPacketSneaking(boolean var0) {
      packetSneaking = var0;
      PlayerInput var1 = mc.player.input.playerInput;
      PortSupport.Managers.NETWORK
         .sendPacket(new PlayerInputC2SPacket(new PlayerInput(var1.forward(), var1.backward(), var1.left(), var1.right(), var1.jump(), var0, var1.sprint())));
   }

   public static void applySneak() {
      float var0 = MathHelper.clamp(
         0.3F + EnchantmentUtil.getLevel(mc.player.getEquippedStack(EquipmentSlot.FEET), Enchantments.SWIFT_SNEAK) * 0.15F, 0.0F, 1.0F
      );
      InputMovementAccess var1 = (InputMovementAccess)mc.player.input;
      Vec2f var2 = var1.shoreline$getMovementVector();
      var1.shoreline$setMovementVector(new Vec2f(var2.x * var0, var2.y * var0));
   }
}
