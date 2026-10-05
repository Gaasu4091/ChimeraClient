package me.alpha432.chimeraclient.shoreline.util.entity;

import me.alpha432.chimeraclient.shoreline.util.Globals;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.util.math.BlockPos;

public class EntityUtil implements Globals {
   public static BlockPos getRoundedBlockPos(Entity var0) {
      return new BlockPos(var0.getBlockX(), (int)Math.round(var0.getY()), var0.getBlockZ());
   }

   public static float getHealth(Entity var0) {
      return var0 instanceof LivingEntity var1 ? var1.getHealth() + var1.getAbsorptionAmount() : 0.0F;
   }

   public static boolean isMonster(Entity var0) {
      return var0 instanceof Monster && !isNeutralInternal(var0);
   }

   private static boolean isNeutralInternal(Entity var0) {
      BeeEntity var1;
      IronGolemEntity var2;
      WolfEntity var3;
      ZombifiedPiglinEntity var4;
      EndermanEntity var5;
      return var0 instanceof EndermanEntity && !(var5 = (EndermanEntity)var0).isAttacking()
         || var0 instanceof ZombifiedPiglinEntity && !(var4 = (ZombifiedPiglinEntity)var0).isAttacking()
         || var0 instanceof WolfEntity && !(var3 = (WolfEntity)var0).isAttacking()
         || var0 instanceof IronGolemEntity && !(var2 = (IronGolemEntity)var0).isAttacking()
         || var0 instanceof BeeEntity && !(var1 = (BeeEntity)var0).isAttacking();
   }

   public static boolean isNeutral(Entity var0) {
      return var0 instanceof EndermanEntity || var0 instanceof ZombifiedPiglinEntity || var0 instanceof WolfEntity || var0 instanceof IronGolemEntity;
   }

   public static boolean isPassive(Entity var0) {
      return var0 instanceof PassiveEntity || var0 instanceof AmbientEntity || var0 instanceof SquidEntity;
   }

   public static boolean isVehicle(Entity var0) {
      return var0 instanceof AbstractBoatEntity
         || var0 instanceof MinecartEntity
         || var0 instanceof FurnaceMinecartEntity
         || var0 instanceof ChestMinecartEntity;
   }
}
