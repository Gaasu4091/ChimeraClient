package me.alpha432.chimeraclient.shoreline.util.world;

import java.util.Set;
import java.util.function.BiFunction;
import me.alpha432.chimeraclient.shoreline.PortSupport;
import me.alpha432.chimeraclient.shoreline.util.Globals;
import me.alpha432.chimeraclient.shoreline.util.player.EnchantmentUtil;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

public class ExplosionUtil implements Globals {
   public static double getDamageTo(Entity var0, Vec3d var1, boolean var2) {
      return getDamageTo(var0, var1, false, var2);
   }

   public static double getDamageTo(Entity var0, Vec3d var1, boolean var2, boolean var3) {
      return getDamageTo(var0, var1, var2, 12.0F, 0, var3);
   }

   public static double getDamageTo(Entity var0, Vec3d var1, boolean var2, int var3, boolean var4) {
      return getDamageTo(var0, var1, var2, 12.0F, var3, var4);
   }

   public static double getDamageTo(Entity var0, Vec3d var1, ExplosionUtil.IgnoreTerrain var2, boolean var3) {
      return getDamageTo(var0, var1, var2, 12.0F, 0, var3);
   }

   public static double getDamageTo(Entity var0, Vec3d var1, ExplosionUtil.IgnoreTerrain var2, int var3, boolean var4) {
      return getDamageTo(var0, var1, var2, 12.0F, var3, var4);
   }

   public static double getDamageTo(Entity var0, Vec3d var1, ExplosionUtil.IgnoreTerrain var2, float var3, int var4, boolean var5) {
      double var6 = var0.getX();
      double var8 = var0.getY();
      double var10 = var0.getZ();
      Vec3d var12 = Vec3d.ZERO;
      if (var4 != 0) {
         double var13 = (var6 - var0.lastX) * var4;
         double var15 = (var8 - var0.lastY) * var4 * 0.3;
         double var17 = (var10 - var0.lastZ) * var4;
         var6 += var13;
         var8 += var15;
         var10 += var17;
         var12 = new Vec3d(var13, var15, var17);
      }

      Vec3d var24 = new Vec3d(var6, var8, var10);
      double var14 = Math.sqrt(var24.squaredDistanceTo(var1));
      double var16 = getExposure(var1, var0.getBoundingBox().offset(var12), var2);
      double var18 = var14 / var3;
      double var20 = (1.0 - var18) * var16;
      double var22 = (int)((var20 * var20 + var20) / 2.0 * 7.0 * 12.0 + 1.0);
      var22 = getReduction(var0, mc.world.getDamageSources().explosion(null), var22, var5);
      return Math.max(0.0, var22);
   }

   public static double getDamageTo(Entity var0, Vec3d var1, boolean var2, Set<BlockPos> var3, boolean var4) {
      return getDamageTo(var0, var1, var2, 12.0F, var3, 0, var4);
   }

   public static double getDamageTo(Entity var0, Vec3d var1, boolean var2, Set<BlockPos> var3, int var4, boolean var5) {
      return getDamageTo(var0, var1, var2, 12.0F, var3, var4, var5);
   }

   public static double getDamageTo(Entity var0, Vec3d var1, boolean var2, float var3, Set<BlockPos> var4, int var5, boolean var6) {
      double var7 = var0.getX();
      double var9 = var0.getY();
      double var11 = var0.getZ();
      Vec3d var13 = Vec3d.ZERO;
      if (var5 != 0) {
         double var14 = (var7 - var0.lastX) * var5;
         double var16 = (var9 - var0.lastY) * var5 * 0.3;
         double var18 = (var11 - var0.lastZ) * var5;
         var7 += var14;
         var9 += var16;
         var11 += var18;
         var13 = new Vec3d(var14, var16, var18);
      }

      Vec3d var25 = new Vec3d(var7, var9, var11);
      double var15 = Math.sqrt(var25.squaredDistanceTo(var1));
      double var17 = getExposure(var1, var0.getBoundingBox().offset(var13), var2 ? ExplosionUtil.IgnoreTerrain.BLAST : ExplosionUtil.IgnoreTerrain.NONE, var4);
      double var19 = var15 / var3;
      double var21 = (1.0 - var19) * var17;
      double var23 = (int)((var21 * var21 + var21) / 2.0 * 7.0 * 12.0 + 1.0);
      var23 = getReduction(var0, mc.world.getDamageSources().explosion(null), var23, var6);
      return Math.max(0.0, var23);
   }

   public static double getDamageTo(Entity var0, Vec3d var1, ExplosionUtil.IgnoreTerrain var2, Set<BlockPos> var3, boolean var4) {
      return getDamageTo(var0, var1, var2, 12.0F, var3, 0, var4);
   }

   public static double getDamageTo(Entity var0, Vec3d var1, ExplosionUtil.IgnoreTerrain var2, float var3, Set<BlockPos> var4, int var5, boolean var6) {
      double var7 = var0.getX();
      double var9 = var0.getY();
      double var11 = var0.getZ();
      Vec3d var13 = Vec3d.ZERO;
      if (var5 != 0) {
         double var14 = (var7 - var0.lastX) * var5;
         double var16 = (var9 - var0.lastY) * var5 * 0.3;
         double var18 = (var11 - var0.lastZ) * var5;
         var7 += var14;
         var9 += var16;
         var11 += var18;
         var13 = new Vec3d(var14, var16, var18);
      }

      Vec3d var25 = new Vec3d(var7, var9, var11);
      double var15 = Math.sqrt(var25.squaredDistanceTo(var1));
      double var17 = getExposure(var1, var0.getBoundingBox().offset(var13), var2, var4);
      double var19 = var15 / var3;
      double var21 = (1.0 - var19) * var17;
      double var23 = (int)((var21 * var21 + var21) / 2.0 * 7.0 * 12.0 + 1.0);
      var23 = getReduction(var0, mc.world.getDamageSources().explosion(null), var23, var6);
      return Math.max(0.0, var23);
   }

   public static double getDamageTo(Entity var0, Vec3d var1, boolean var2, float var3, int var4, boolean var5) {
      double var6 = var0.getX();
      double var8 = var0.getY();
      double var10 = var0.getZ();
      Vec3d var12 = Vec3d.ZERO;
      if (var4 != 0) {
         double var13 = (var6 - var0.lastX) * var4;
         double var15 = (var8 - var0.lastY) * var4 * 0.3;
         double var17 = (var10 - var0.lastZ) * var4;
         var6 += var13;
         var8 += var15;
         var10 += var17;
         var12 = new Vec3d(var13, var15, var17);
      }

      Vec3d var24 = new Vec3d(var6, var8, var10);
      double var14 = Math.sqrt(var24.squaredDistanceTo(var1));
      double var16 = getExposure(var1, var0.getBoundingBox().offset(var12), var2 ? ExplosionUtil.IgnoreTerrain.BLAST : ExplosionUtil.IgnoreTerrain.NONE);
      double var18 = var14 / var3;
      double var20 = (1.0 - var18) * var16;
      double var22 = (int)((var20 * var20 + var20) / 2.0 * 7.0 * 12.0 + 1.0);
      var22 = getReduction(var0, mc.world.getDamageSources().explosion(null), var22, var5);
      return Math.max(0.0, var22);
   }

   public static double getDamageToPos(Vec3d var0, Entity var1, Vec3d var2, boolean var3, boolean var4) {
      Box var5 = var1.getBoundingBox();
      double var6 = var0.getX() - var5.minX;
      double var8 = var0.getY() - var5.minY;
      double var10 = var0.getZ() - var5.minZ;
      Box var12 = var5.offset(var6, var8, var10);
      ExplosionUtil.RaycastFactory var13 = getRaycastFactory(var3 ? ExplosionUtil.IgnoreTerrain.BLAST : ExplosionUtil.IgnoreTerrain.NONE);
      double var14 = getExposure(var2, var12, var13);
      double var16 = Math.sqrt(var0.squaredDistanceTo(var2)) / 12.0;
      double var18 = (1.0 - var16) * var14;
      double var20 = (int)((var18 * var18 + var18) / 2.0 * 7.0 * 12.0 + 1.0);
      var20 = getReduction(var1, mc.world.getDamageSources().explosion(null), var20, var4);
      return Math.max(0.0, var20);
   }

   private static double getReduction(Entity var0, DamageSource var1, double var2, boolean var4) {
      if (var1.isScaledWithDifficulty()) {
         switch (mc.world.getDifficulty()) {
            case EASY:
               var2 = Math.min(var2 / 2.0 + 1.0, var2);
               break;
            case HARD:
               var2 *= 1.5;
         }
      }

      if (var0 instanceof LivingEntity var5) {
         var2 = DamageUtil.getDamageLeft(var5, (float)var2, var1, getArmor(var5), (float)var5.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS));
         var2 = getResistanceReduction(var5, var2);
         var2 = getProtectionReduction(var5, var2, var1, var4);
      }

      return Math.max(var2, 0.0);
   }

   private static float getArmor(LivingEntity var0) {
      return (float)Math.floor(var0.getAttributeValue(EntityAttributes.ARMOR));
   }

   private static float getProtectionReduction(Entity var0, double var1, DamageSource var3, boolean var4) {
      if (var0 instanceof LivingEntity var5) {
         float var6 = getProtectionAmount(PortSupport.armor(var5), var4);
         return DamageUtil.getInflictedDamage((float)var1, var6);
      } else {
         return 0.0F;
      }
   }

   private static float getProtectionAmount(Iterable<ItemStack> var0, boolean var1) {
      int var2 = 0;

      for (ItemStack var4 : var0) {
         if (var1 && EnchantmentUtil.isFakeEnchant2b2t(var4)) {
            EquippableComponent var5 = (EquippableComponent)var4.get(DataComponentTypes.EQUIPPABLE);
            var2 += var5 != null && var5.slot() == EquipmentSlot.LEGS ? 8 : 4;
         } else {
            var2 += 2 * EnchantmentUtil.getLevel(var4, Enchantments.BLAST_PROTECTION) + EnchantmentUtil.getLevel(var4, Enchantments.PROTECTION);
         }
      }

      return var2;
   }

   private static double getResistanceReduction(LivingEntity var0, double var1) {
      StatusEffectInstance var3 = var0.getStatusEffect(StatusEffects.RESISTANCE);
      if (var3 != null) {
         int var4 = var3.getAmplifier() + 1;
         var1 *= 1.0F - var4 * 0.2F;
      }

      return Math.max(var1, 0.0);
   }

   private static <T extends LivingEntity> DefaultAttributeContainer getDefaultForEntity(T var0) {
       return DefaultAttributeRegistry.get((EntityType<? extends LivingEntity>)(Object)var0.getType());
   }

   private static float getExposure(Vec3d var0, Box var1, ExplosionUtil.IgnoreTerrain var2, Set<BlockPos> var3) {
      ExplosionUtil.RaycastFactory var4 = getRaycastFactory(var2, var3);
      return getExposure(var0, var1, var4);
   }

   private static float getExposure(Vec3d var0, Box var1, ExplosionUtil.IgnoreTerrain var2) {
      ExplosionUtil.RaycastFactory var3 = getRaycastFactory(var2);
      return getExposure(var0, var1, var3);
   }

   private static float getExposure(Vec3d var0, Box var1, ExplosionUtil.RaycastFactory var2) {
      double var3 = var1.maxX - var1.minX;
      double var5 = var1.maxY - var1.minY;
      double var7 = var1.maxZ - var1.minZ;
      double var9 = 1.0 / (var3 * 2.0 + 1.0);
      double var11 = 1.0 / (var5 * 2.0 + 1.0);
      double var13 = 1.0 / (var7 * 2.0 + 1.0);
      if (var9 > 0.0 && var11 > 0.0 && var13 > 0.0) {
         int var15 = 0;
         int var16 = 0;
         double var17 = (1.0 - Math.floor(1.0 / var9) * var9) * 0.5;
         double var19 = (1.0 - Math.floor(1.0 / var13) * var13) * 0.5;
         var9 *= var3;
         var11 *= var5;
         var13 *= var7;
         double var21 = var1.minX + var17;
         double var23 = var1.minY;
         double var25 = var1.minZ + var19;
         double var27 = var1.maxX + var17;
         double var29 = var1.maxY;
         double var31 = var1.maxZ + var19;

         for (double var33 = var21; var33 <= var27; var33 += var9) {
            for (double var35 = var23; var35 <= var29; var35 += var11) {
               for (double var37 = var25; var37 <= var31; var37 += var13) {
                  Vec3d var39 = new Vec3d(var33, var35, var37);
                  if (raycast(new ExplosionUtil.ExposureRaycastContext(var39, var0), var2) == null) {
                     var15++;
                  }

                  var16++;
               }
            }
         }

         return (float)var15 / var16;
      } else {
         return 0.0F;
      }
   }

   private static ExplosionUtil.RaycastFactory getRaycastFactory(ExplosionUtil.IgnoreTerrain var0, Set<BlockPos> var1) {
      if (var0 == ExplosionUtil.IgnoreTerrain.BLAST) {
         return (var1x, var2) -> {
            if (var1.contains(var2)) {
               return null;
            } else {
               BlockState var3 = mc.world.getBlockState(var2);
               return var3.getBlock().getBlastResistance() < 600.0F ? null : var3.getCollisionShape(mc.world, var2).raycast(var1x.start(), var1x.end(), var2);
            }
         };
      } else {
         return var0 == ExplosionUtil.IgnoreTerrain.ALL ? (var0x, var1x) -> null : (var1x, var2) -> {
            if (var1.contains(var2)) {
               return null;
            } else {
               BlockState var3 = mc.world.getBlockState(var2);
               return var3.getCollisionShape(mc.world, var2).raycast(var1x.start(), var1x.end(), var2);
            }
         };
      }
   }

   private static ExplosionUtil.RaycastFactory getRaycastFactory(ExplosionUtil.IgnoreTerrain var0) {
      if (var0 == ExplosionUtil.IgnoreTerrain.BLAST) {
         return (var0x, var1) -> {
            BlockState var2 = mc.world.getBlockState(var1);
            return var2.getBlock().getBlastResistance() < 600.0F ? null : var2.getCollisionShape(mc.world, var1).raycast(var0x.start(), var0x.end(), var1);
         };
      } else {
         return var0 == ExplosionUtil.IgnoreTerrain.ALL ? (var0x, var1) -> null : (var0x, var1) -> {
            BlockState var2 = mc.world.getBlockState(var1);
            return var2.getCollisionShape(mc.world, var1).raycast(var0x.start(), var0x.end(), var1);
         };
      }
   }

   private static BlockHitResult raycast(ExplosionUtil.ExposureRaycastContext var0, ExplosionUtil.RaycastFactory var1) {
      return (BlockHitResult)BlockView.raycast(var0.start, var0.end, var0, var1, var0x -> null);
   }

   public record ExposureRaycastContext(Vec3d start, Vec3d end) {
   }

   public static enum IgnoreTerrain {
      ALL,
      BLAST,
      NONE;
   }

   @FunctionalInterface
   public interface RaycastFactory extends BiFunction<ExplosionUtil.ExposureRaycastContext, BlockPos, BlockHitResult> {
   }
}
