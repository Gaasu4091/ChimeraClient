package me.alpha432.chimeraclient.mio;

import java.awt.Color;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Map.Entry;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.event.impl.render.Render2DEvent;
import me.alpha432.chimeraclient.event.impl.render.Render3DEvent;
import me.alpha432.chimeraclient.features.gui.font.FontDraw;
import me.alpha432.chimeraclient.mio.mixin.MioOptionAccess;
import me.alpha432.chimeraclient.mio.support.GhostSupport;
import me.alpha432.chimeraclient.mio.support.SkeletonSupport;
import me.alpha432.chimeraclient.mio.support.WaypointsSupport;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DecoratedPotBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.Ownable;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.LightType;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.biome.BiomeKeys;

public final class MioVisuals implements Util {
   public static float lookYaw;
   public static float lookPitch;
   public static boolean lookReady;
   public static double zoomScroll;
   private static Perspective perspective;
   private static boolean smoothCamera;
   private static final Map<String, Double> gammaOwners = new HashMap<>();
   private static double originalGamma;
   private static long zoomChanged;
   private static float zoomFrom;
   private static float zoomTarget;
   private static float zoomAmount;
   private static double lastZoomPower = 2.0;
   private static boolean zoomSmooth = true;
   private static float clip = 4.0F;
   private static final Map<UUID, Deque<MioVisuals.Trail>> trails = new HashMap<>();
   private static long lastTrail;
   private static String xrayList = "";
   private static int savedBlur;
   private static float crosshairProgress;
   private static long crosshairFrame;

   private MioVisuals() {
   }

   public static void enable(MioConfiguredModule var0) {
      String var1 = var0.getName();
      switch (var1) {
         case "Ambience":
         case "Xray":
            if (gammaOwners.isEmpty()) {
               originalGamma = (Double)mc.options.getGamma().getValue();
            }

            gammaOwners.put(var0.getName(), originalGamma);
            if (var0.getName().equals("Xray")) {
               mc.worldRenderer.reload();
            }
            break;
         case "FreeLook":
            perspective = mc.options.getPerspective();
            lookReady = false;
            initializeLook();
            break;
         case "Zoom":
            smoothCamera = mc.options.smoothCameraEnabled;
            if (var0.b("smoothCamera")) {
               mc.options.smoothCameraEnabled = true;
            }

            zoomScroll = 0.0;
            zoomFrom = zoomAmount;
            zoomTarget = 1.0F;
            zoomChanged = System.currentTimeMillis();
            lastZoomPower = var0.n("amount");
            zoomSmooth = var0.b("smooth");
            break;
         case "Trails":
            trails.clear();
            break;
         case "Hitmarker":
            MioState.hitTime = 0L;
            break;
         case "Crosshair":
            crosshairProgress = 0.0F;
            crosshairFrame = System.currentTimeMillis();
            break;
         case "LogoutSpots":
            MioState.logouts.clear();
            break;
         case "Blur":
            savedBlur = (Integer)mc.options.getMenuBackgroundBlurriness().getValue();
            mc.options.getMenuBackgroundBlurriness().setValue(var0.i("radius"));
      }
   }

   private static void gamma(double var0) {
      ((MioOptionAccess)(Object)mc.options.getGamma()).mio$setValue(var0);
   }

   public static void disable(MioConfiguredModule var0) {
      String var1 = var0.getName();
      switch (var1) {
         case "Ambience":
         case "Xray":
            Double var3 = gammaOwners.remove(var0.getName());
            if (var3 != null && gammaOwners.isEmpty()) {
               gamma(var3);
            }

            if (var0.getName().equals("Xray")) {
               mc.worldRenderer.reload();
            }

            if (mc.player != null) {
               StatusEffectInstance var4 = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
               if (var4 != null && var4.getAmplifier() == 68) {
                  mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
               }
            }
            break;
         case "FreeLook":
            if (perspective != null) {
               mc.options.setPerspective(perspective);
            }

            perspective = null;
            lookReady = false;
            break;
         case "Zoom":
            mc.options.smoothCameraEnabled = smoothCamera;
            zoomFrom = zoomAmount;
            zoomTarget = 0.0F;
            zoomChanged = System.currentTimeMillis();
            if (!zoomSmooth) {
               zoomFrom = 0.0F;
               zoomAmount = 0.0F;
            }
            break;
         case "Trails":
            trails.clear();
            break;
         case "Blur":
            mc.options.getMenuBackgroundBlurriness().setValue(savedBlur);
      }
   }

   public static void tick(MioConfiguredModule var0) {
      String var1 = var0.getName();
      switch (var1) {
         case "FreeLook":
            initializeLook();
            break;
         case "Ambience":
            if (var0.choice("brightness", "GAMMA")) {
               gamma(1000.0);
            } else if (gammaOwners.containsKey("Ambience") && active("Xray") == null) {
               gamma(gammaOwners.get("Ambience"));
            }

            if (var0.choice("brightness", "POTION")) {
               mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, -1, 68, false, false));
            } else {
               StatusEffectInstance var7 = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
               if (var7 != null && var7.getAmplifier() == 68) {
                  mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
               }
            }

            if (var0.b("worldTime")) {
               double var8 = var0.b("sync") ? LocalTime.now().toSecondOfDay() / 3600.0 : var0.n("time");
               mc.world.setTime(mc.world.getTime(), (long)(var8 * 1000.0 + 18000.0), true);
            }

            if (var0.b("worldWeather")) {
               mc.world.setRainGradient(var0.choice("weather", "CLEAR") ? 0.0F : var0.f("amount"));
               mc.world.setThunderGradient(0.0F);
               if (var0.choice("weather", "DUSTY")) {
                  for (int var9 = 0; var9 < (int)(24.0F * var0.f("amount")); var9++) {
                     Random var10 = mc.world.random;
                     Vec3d var11 = mc.player
                        .getEyePos()
                        .add(var10.nextDouble() * 20.0 - 10.0, var10.nextDouble() * 12.0 - 6.0, var10.nextDouble() * 20.0 - 10.0);
                     mc.world.addParticleClient(var10.nextBoolean() ? ParticleTypes.WHITE_ASH : ParticleTypes.ASH, var11.x, var11.y, var11.z, 0.0, 0.0, 0.0);
                  }
               }
            }
            break;
         case "Xray":
            gamma(1000.0);
            String var6 = var0.s("whitelist") + var0.b("bypass");
            if (!var6.equals(xrayList)) {
               xrayList = var6;
               mc.worldRenderer.reload();
            }
            break;
         case "NoRender":
            if (var0.b("entities2") && var0.choice("removal", "FULL")) {
               ArrayList<Entity> var3 = new ArrayList<>();

               for (Entity var5 : mc.world.getEntities()) {
                  if (var5 != mc.player && hidden(var5)) {
                     var3.add(var5);
                  }
               }

               var3.forEach(var0x -> mc.world.removeEntity(var0x.getId(), RemovalReason.DISCARDED));
            }
            break;
         case "Trails":
            recordTrails(var0);
            break;
         case "Waypoints":
            WaypointsSupport.purgeInvalid();
            break;
         case "Blur":
            mc.options.getMenuBackgroundBlurriness().setValue(var0.i("radius"));
      }
   }

   public static MioConfiguredModule active(String var0) {
      return MioConfiguredModule.active(var0);
   }

   private static void initializeLook() {
      if (!lookReady && mc.player != null) {
         lookYaw = mc.player.getYaw();
         lookPitch = mc.player.getPitch();
         lookReady = true;
         if (perspective == Perspective.FIRST_PERSON) {
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
         }
      }
   }

   public static boolean noRender(String var0, String var1) {
      MioConfiguredModule var2 = active("NoRender");
      return var2 != null && var2.b(var1);
   }

   public static boolean hidden(Entity var0) {
      MioConfiguredModule var1 = active("NoRender");
      if (var1 == null) {
         return false;
      } else {
         return var1.b("wardens") && var0 instanceof WardenEntity && mc.player != null && mc.player.distanceTo(var0) > var1.n("wardenDistance")
            ? true
            : var1.b("entities2") && !selected(var1, "selection", "entityTypes", Registries.ENTITY_TYPE.getId(var0.getType()).toString());
      }
   }

   public static boolean selected(MioConfiguredModule var0, String var1, String var2, String var3) {
      return var0.choice(var1, "ANY") || var0.choice(var1, "WHITELIST") == var0.listed(var2, var3);
   }

   public static float zoom(float var0) {
      MioConfiguredModule var1 = active("Zoom");
      double var2 = Math.min(1.0, (System.currentTimeMillis() - zoomChanged) / 230.0);
      zoomAmount = (float)(zoomFrom + (zoomTarget - zoomFrom) * (1.0 - Math.pow(1.0 - var2, 4.0)));
      if (var1 != null) {
         lastZoomPower = var1.n("amount");
         zoomSmooth = var1.b("smooth");
         if (!zoomSmooth) {
            zoomAmount = 1.0F;
         }
      }

      return var1 == null && zoomAmount == 0.0F
         ? var0
         : MathHelper.clamp((float)(var0 + (75.0 / lastZoomPower - var0) * zoomAmount + zoomScroll * zoomAmount), 10.0F, 150.0F);
   }

   public static float clipDistance(MioConfiguredModule var0) {
      float var1 = var0.f("range");
      clip = var0.b("smooth") ? clip + (var1 - clip) * 0.2F : var1;
      return clip;
   }

   public static Object environment(EnvironmentAttribute<?> var0, Object var1) {
      MioConfiguredModule var2 = active("SkyColor");
      if (var2 != null && mc.world != null && !var2.choice("type", "END")) {
         String var3 = mc.world.getRegistryKey().getValue().getPath();
         boolean var4 = var2.b(var3.equals("the_nether") ? "nether" : (var3.equals("the_end") ? "end" : "overworld"));
         if (var4) {
            if (var0 == EnvironmentAttributes.FOG_COLOR_VISUAL) {
               return var2.c("fog").getRGB() & 16777215;
            }

            if (var0 == EnvironmentAttributes.SKY_COLOR_VISUAL) {
               return var2.c("sky").getRGB() & 16777215;
            }

            if (var2.b("dense") && var0 == EnvironmentAttributes.FOG_START_DISTANCE_VISUAL) {
               return 0.0F;
            }

            if (var2.b("dense") && var0 == EnvironmentAttributes.FOG_END_DISTANCE_VISUAL) {
               return 16.0F;
            }
         }
      }

      MioConfiguredModule var5 = active("Ambience");
      if (var5 != null && (var5.choice("brightness", "SKY") || var5.choice("brightness", "SCREEN"))) {
         if (var0 == EnvironmentAttributes.SKY_LIGHT_COLOR_VISUAL) {
            return var5.c("color").getRGB() & 16777215;
         }

         if (var0 == EnvironmentAttributes.SKY_LIGHT_FACTOR_VISUAL) {
            return 1.0F;
         }
      }

      MioConfiguredModule var6 = active("NoRender");
      if (var6 != null && var6.b("fog")) {
         if (var0 == EnvironmentAttributes.FOG_START_DISTANCE_VISUAL) {
            return 0.0F;
         }

         if (var0 == EnvironmentAttributes.FOG_END_DISTANCE_VISUAL || var6.b("sky") && var0 == EnvironmentAttributes.SKY_FOG_END_DISTANCE_VISUAL) {
            return ((Integer)mc.options.getViewDistance().getValue()).intValue() * 16.0F * var6.f("range");
         }
      }

      return var1;
   }

   public static void world(MioConfiguredModule var0, Render3DEvent var1) {
      MatrixStack var2 = var1.getMatrix();
      float var3 = var1.getDelta();
      String var4 = var0.getName();
      switch (var4) {
         case "Borders":
            ChunkPos var17 = mc.player.getChunkPos();
            double var22 = var0.i("level") == -1 ? MioRender.lerp(mc.player, var3).y : var0.i("level");
            MioRender.outline(
               var2,
               new Box(var17.getStartX(), var22, var17.getStartZ(), var17.getStartX() + 16, var22, var17.getStartZ() + 16),
               var0.c("chunk"),
               var0.f("lineWidth")
            );
            int var29 = 128 * (1 << var0.i("mapSize"));
            int var31 = MathHelper.floor((mc.player.getBlockX() + 64.0) / var29) * var29 - 64;
            int var32 = MathHelper.floor((mc.player.getBlockZ() + 64.0) / var29) * var29 - 64;
            MioRender.outline(var2, new Box(var31, var22, var32, var31 + var29, var22, var32 + var29), var0.c("map"), var0.f("lineWidth"));
            break;
         case "Highlight":
            HitResult var16 = mc.crosshairTarget;
            if (var16 instanceof BlockHitResult var21 && var16.getType() != Type.MISS) {
               BlockState var25 = mc.world.getBlockState(var21.getBlockPos());
               VoxelShape var28 = var25.getOutlineShape(mc.world, var21.getBlockPos())
                  .offset(var21.getBlockPos().getX(), var21.getBlockPos().getY(), var21.getBlockPos().getZ());
               if (!var28.isEmpty()) {
                  for (Box var11 : var28.getBoundingBoxes()) {
                     MioRender.fill(var2, var11, var0.c("fillColor"));
                  }

                  if (var0.b("complex")) {
                     MioRender.outline(var2, var28, var0.c("color"), var0.f("lineWidth"));
                  } else {
                     MioRender.outline(var2, var28.getBoundingBox(), var0.c("color"), var0.f("lineWidth"));
                  }
               }
            }
            break;
         case "BreakHighlight":
            for (MioState.Breaking var20 : MioState.breaks.values()) {
               if (mc.player.squaredDistanceTo(var20.pos().toCenterPos()) <= var0.n("range") * var0.n("range")
                  && (!(!var0.b("friends") && mc.world.getEntityById(var20.entityId()) instanceof PlayerEntity var26) || !friend(var26))) {
                  Box var27 = new Box(var20.pos()).contract((1.0 - (var20.stage() + 1) / 10.0) * 0.5);
                  MioRender.fill(var2, var27, var0.c("fill"));
                  MioRender.outline(var2, var27, var0.c("outline"), 1.0F);
               }
            }
            break;
         case "ESP":
            esp(var0, var2, var3);
            break;
         case "Skeleton":
            SkeletonSupport.draw(var0, var2, var3);
            break;
         case "Tracers":
            Vec3d var14 = MioRender.camera().add(Vec3d.fromPolar(mc.gameRenderer.getCamera().getPitch(), mc.gameRenderer.getCamera().getYaw()).multiply(0.1));

            for (PlayerEntity var23 : mc.world.getPlayers()) {
               if (var23 != mc.player
                  && mc.player.distanceTo(var23) <= var0.n("maxDistance")
                  && (!var0.b("ignoreFriends") || !friend(var23))
                  && (!var0.b("ignoreNakeds") || !naked(var23))) {
                  Vec3d var9 = MioRender.lerp(var23, var3);
                  double var10 = var0.choice("hitbox", "HEAD") ? var23.getHeight() : (var0.choice("hitbox", "BODY") ? var23.getHeight() * 0.5 : 0.0);
                  Color var12 = friend(var23)
                     ? Color.CYAN
                     : (var0.b("distanceColor") ? blend(Color.RED, Color.GREEN, mc.player.distanceTo(var23) / var0.f("maxDistance")) : var0.c("color"));
                  MioRender.line(var2, var14, var9.add(0.0, var10, 0.0), var12, var0.f("lineWidth"));
                  if (var0.b("stem")) {
                     MioRender.line(var2, var9, var9.add(0.0, var23.getHeight(), 0.0), var12, var0.f("lineWidth"));
                  }
               }
            }
            break;
         case "Trails":
            drawTrails(var0, var2);
            break;
         case "Trajectories":
            trajectories(var0, var2, var3);
            break;
         case "Waypoints":
            for (WaypointsSupport.Waypoint var18 : WaypointsSupport.waypoints()) {
               Vec3d var8 = WaypointsSupport.converted(var18);
               if (var18.toggled
                  && var8 != null
                  && var18.server.equalsIgnoreCase(WaypointsSupport.server())
                  && !(mc.player.getEntityPos().distanceTo(var8) > var0.n("distance") * 1000.0)) {
                  if (var0.b("box")) {
                     MioRender.outline(var2, Box.of(var8, 1.0, 1.0, 1.0), var0.c("color"), var0.f("lineWidth"));
                  }

                  if (var0.b("beam")) {
                     MioRender.fill(
                        var2,
                        new Box(var8.x - 0.125, var8.y, var8.z - 0.125, var8.x + 0.125, mc.world.getTopYInclusive() + 1, var8.z + 0.125),
                        MioRender.alpha(var0.c("color"), var0.i("beamAlpha"))
                     );
                  }

                  if (var0.b("tracers")) {
                     MioRender.line(var2, mc.player.getEyePos(), var8, var0.c("color"), var0.f("lineWidth"));
                  }
               }
            }
            break;
         case "Chams":
            GhostSupport.pops(var0, var2);
            break;
         case "LogoutSpots":
            for (MioState.Logout var7 : MioState.logouts.values()) {
               if (logoutVisible(var0, var7)) {
                  if (var0.choice("model", "SIMPLE") || var0.choice("model", "BOTH")) {
                     MioRender.fill(var2, var7.box(), MioRender.alpha(var0.c("boxFill"), (int)(var0.c("boxFill").getAlpha() * var0.f("alpha"))));
                     MioRender.outline(
                        var2, var7.box(), MioRender.alpha(var0.c("boxLine"), (int)(var0.c("boxLine").getAlpha() * var0.f("alpha"))), var0.f("width")
                     );
                  }

                  if (var0.choice("model", "COMPLEX") || var0.choice("model", "BOTH")) {
                     GhostSupport.logout(var0, var7.uuid(), var2);
                  }

                  if (var0.b("tracer")) {
                     MioRender.line(var2, mc.player.getEyePos(), var7.box().getCenter(), var0.c("boxLine"), var0.f("width2"));
                  }
               }
            }
      }
   }

   private static boolean friend(PlayerEntity var0) {
      return ChimeraClient.friendManager.isFriend(var0);
   }

   private static boolean logoutVisible(MioConfiguredModule var0, MioState.Logout var1) {
      return var1.box().getCenter().distanceTo(mc.player.getEntityPos()) <= var0.n("distance")
         && (!var0.b("ignoreFriends") || !ChimeraClient.friendManager.isFriend(var1.name()))
         && (!var0.b("ignoreNakeds") || !GhostSupport.naked(var1.uuid()));
   }

   private static boolean naked(PlayerEntity var0) {
      for (EquipmentSlot var4 : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
         if (!var0.getEquippedStack(var4).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private static Color blend(Color var0, Color var1, float var2) {
      var2 = MathHelper.clamp(var2, 0.0F, 1.0F);
      return new Color(
         MathHelper.lerp(var2, var0.getRed(), var1.getRed()),
         MathHelper.lerp(var2, var0.getGreen(), var1.getGreen()),
         MathHelper.lerp(var2, var0.getBlue(), var1.getBlue()),
         MathHelper.lerp(var2, var0.getAlpha(), var1.getAlpha())
      );
   }

   private static void esp(MioConfiguredModule var0, MatrixStack var1, float var2) {
      for (Entity var4 : mc.world.getEntities()) {
         String var5 = null;
         String var6 = null;
         if (var4 != mc.player) {
            if (var4 instanceof PlayerEntity && var0.b("players")) {
               var5 = "fill6";
               var6 = "outline4";
            } else if (var4 instanceof HostileEntity && var0.b("hostiles")) {
               var5 = "fill5";
               var6 = "outline2";
            } else if (var4 instanceof AnimalEntity && var0.b("animals")) {
               var5 = "fill2";
               var6 = "outline6";
            } else if (var4 instanceof EnderPearlEntity && var0.b("pearls")) {
               var5 = "fill3";
               var6 = "outline";
            } else if (var4 instanceof ItemEntity && var0.b("items") && !var0.choice("mode", "TEXT") && mc.player.distanceTo(var4) <= var0.n("range")) {
               var5 = "fill4";
               var6 = "outline3";
            } else if (var4 instanceof ExperienceOrbEntity && var0.b("exp")) {
               var5 = "fill";
               var6 = "outline5";
            }

            if (var5 != null) {
               MioRender.fill(var1, MioRender.lerpBox(var4, var2), var0.c(var5));
               MioRender.outline(var1, MioRender.lerpBox(var4, var2), var0.c(var6), var0.f("lineWidth"));
            }
         }
      }

      for (BlockEntity var13 : mc.world.getBlockEntities()) {
         boolean var15 = var13 instanceof ChestBlockEntity || var13 instanceof BarrelBlockEntity
            ? var0.b("chests")
            : (
               var13 instanceof EnderChestBlockEntity
                  ? var0.b("eChests")
                  : (
                     var13 instanceof ShulkerBoxBlockEntity
                        ? var0.b("shulkers")
                        : (
                           var13 instanceof BedBlockEntity
                              ? var0.b("beds")
                              : (
                                 var13 instanceof SignBlockEntity
                                    ? var0.b("signs")
                                    : (
                                       var13 instanceof DispenserBlockEntity
                                          ? var0.b("dispensers")
                                          : (
                                             var13 instanceof HopperBlockEntity
                                                ? var0.b("hoppers")
                                                : (
                                                   var13 instanceof AbstractFurnaceBlockEntity
                                                      ? var0.b("furnaces")
                                                      : var13 instanceof DecoratedPotBlockEntity && var0.b("pots")
                                                )
                                          )
                                    )
                              )
                        )
                  )
            );
         if (var15) {
            Color var17 = var13 instanceof EnderChestBlockEntity
               ? new Color(160, 70, 255)
               : (var13 instanceof ChestBlockEntity ? new Color(255, 180, 40) : new Color(189, 153, 255));
            MioRender.fill(var1, new Box(var13.getPos()), MioRender.alpha(var17, 21));
            MioRender.outline(var1, new Box(var13.getPos()), MioRender.alpha(var17, 201), var0.f("lineWidth"));
         }
      }

      if (var0.b("light")) {
         BlockPos var12 = BlockPos.ofFloored(mc.player.getEyePos());

         for (BlockPos var16 : BlockPos.iterate(var12.add(-12, -12, -12), var12.add(12, 12, 12))) {
            BlockState var18 = mc.world.getBlockState(var16);
            if (var18.allowsSpawning(mc.world, var16, EntityType.ZOMBIE)
               && mc.world.getBlockState(var16.up()).isAir()
               && mc.world.getLightLevel(LightType.BLOCK, var16.up()) <= 7
               && !mc.world.getBiome(var16).matchesKey(BiomeKeys.MUSHROOM_FIELDS)) {
               double var7 = var16.toCenterPos().distanceTo(mc.player.getEyePos());
               float var9 = (float)MathHelper.clamp(1.0 - (var7 - 8.0) / 2.0, 0.0, 1.0);
               if (!(var9 <= 0.0F)) {
                  Color var10 = new Color(255, 0, 0, (int)(var9 * 255.0F));
                  MioRender.line(
                     var1,
                     new Vec3d(var16.getX() + 0.25, var16.getY() + 1.01, var16.getZ() + 0.25),
                     new Vec3d(var16.getX() + 0.75, var16.getY() + 1.01, var16.getZ() + 0.75),
                     var10,
                     1.0F
                  );
                  MioRender.line(
                     var1,
                     new Vec3d(var16.getX() + 0.75, var16.getY() + 1.01, var16.getZ() + 0.25),
                     new Vec3d(var16.getX() + 0.25, var16.getY() + 1.01, var16.getZ() + 0.75),
                     var10,
                     1.0F
                  );
               }
            }
         }
      }
   }

   private static void recordTrails(MioConfiguredModule var0) {
      long var1 = System.currentTimeMillis();
      if (!(var1 - lastTrail < var0.n("delay") * 1000.0)) {
         lastTrail = var1;

         for (Entity var4 : mc.world.getEntities()) {
            boolean var5 = var4 == mc.player
               ? var0.b("self")
               : (
                  var4 instanceof PlayerEntity
                     ? var0.b("players")
                     : (
                        var4 instanceof EnderPearlEntity
                           ? var0.b("pearls")
                           : (var4 instanceof PersistentProjectileEntity ? var0.b("arrows") : var4 instanceof ExperienceBottleEntity && var0.b("exp"))
                     )
               );
            if (var5) {
               Deque var6 = trails.computeIfAbsent(var4.getUuid(), var0x -> new ArrayDeque<>());
               Vec3d var7 = var4.getEntityPos();
               if (var6.isEmpty() || ((MioVisuals.Trail)var6.getLast()).pos().squaredDistanceTo(var7) > 1.0E-6) {
                  var6.addLast(new MioVisuals.Trail(var7, var1));
               }

               while (var6.size() > 4096) {
                  var6.removeFirst();
               }
            }
         }

         if (var0.b("fade")) {
            long var8 = var1 - (long)((var0.n("fadeDelay") + var0.n("fadeDuration")) * 1000.0);

             for (Deque<MioVisuals.Trail> var10 : trails.values()) {
               while (!var10.isEmpty() && ((MioVisuals.Trail)var10.getFirst()).time() < var8) {
                  var10.removeFirst();
               }
            }

            trails.values().removeIf(Collection::isEmpty);
         }
      }
   }

   private static void drawTrails(MioConfiguredModule var0, MatrixStack var1) {
      if (var0.b("render")) {
         long var2 = System.currentTimeMillis();

          for (Deque<MioVisuals.Trail> var5 : trails.values()) {
            MioVisuals.Trail var6 = null;

            for (MioVisuals.Trail var8 : var5) {
               if (var6 != null) {
                  Color var9 = var0.b("rainbow")
                     ? new Color(Color.HSBtoRGB((float)((var2 / 5000.0 + var8.pos().x * var0.n("threshold") * 0.05) % 1.0), 1.0F, 1.0F))
                     : var0.c("color");
                  if (var0.b("fade")) {
                     var9 = MioRender.alpha(
                        var9,
                        (int)(
                           var9.getAlpha()
                              * MathHelper.clamp(
                                 1.0 - (var2 - var8.time() - var0.n("fadeDelay") * 1000.0) / Math.max(1.0, var0.n("fadeDuration") * 1000.0), 0.0, 1.0
                              )
                        )
                     );
                  }

                  MioRender.line(var1, var6.pos(), var8.pos(), var9, var0.f("lineWidth"));
               }

               var6 = var8;
            }
         }
      }
   }

   private static void trajectories(MioConfiguredModule var0, MatrixStack var1, float var2) {
      for (Entity var4 : mc.world.getEntities()) {
         if (!(var4 instanceof ProjectileEntity var5 && var0.b("airborne") && (!var0.b("onlyOwn") || var5.getOwner() == mc.player))) {
            if (var4 instanceof PlayerEntity var6 && var0.b("predict") && (!var0.b("onlyOwn") || var6 == mc.player)) {
               ItemStack var7 = var6.getMainHandStack();
               String var8 = var7.isOf(Items.BOW)
                  ? "bow"
                  : (
                     var7.isOf(Items.CROSSBOW)
                        ? "xBow"
                        : (
                           var7.isOf(Items.TRIDENT)
                              ? "trident"
                              : (
                                 var7.isOf(Items.ENDER_PEARL)
                                    ? "pearls"
                                    : (var7.isOf(Items.EXPERIENCE_BOTTLE) ? "exp" : (!var7.isOf(Items.SNOWBALL) && !var7.isOf(Items.EGG) ? null : "others"))
                              )
                        )
                  );
               if (var8 != null && var0.b(var8) && (!var7.isOf(Items.BOW) || var6.isUsingItem())) {
                  float var9 = var7.isOf(Items.BOW)
                     ? BowItem.getPullProgress(var6.getItemUseTime()) * 3.0F
                     : (var7.isOf(Items.CROSSBOW) ? 3.15F : (var7.isOf(Items.TRIDENT) ? 2.5F : (var7.isOf(Items.EXPERIENCE_BOTTLE) ? 0.7F : 1.5F)));
                  Vec3d var10 = Vec3d.fromPolar(var6.getPitch() - (var7.isOf(Items.EXPERIENCE_BOTTLE) ? 20 : 0), var6.getYaw());
                  Vec3d var11 = var10.multiply(var9).add(var6.getVelocity().x, var6.isOnGround() ? 0.0 : var6.getVelocity().y, var6.getVelocity().z);
                  trajectory(
                     var0,
                     var1,
                     var6,
                     var6.getEyePos().add(0.0, -0.1, 0.0),
                     var11,
                     !var7.isOf(Items.BOW) && !var7.isOf(Items.CROSSBOW) && !var7.isOf(Items.TRIDENT)
                        ? (var7.isOf(Items.EXPERIENCE_BOTTLE) ? 0.07 : 0.03)
                        : 0.05
                  );
               }
            }
         } else {
            String var12 = var5 instanceof EnderPearlEntity
               ? "pearls"
               : (
                  var5 instanceof ExperienceBottleEntity
                     ? "exp"
                     : (var5 instanceof TridentEntity ? "trident" : (var5 instanceof PersistentProjectileEntity ? "bow" : "others"))
               );
            if (var0.b(var12)) {
               trajectory(
                  var0,
                  var1,
                  var4,
                  MioRender.lerp(var4, var2),
                  var4.getVelocity(),
                  var5 instanceof PersistentProjectileEntity ? 0.05 : (var5 instanceof ExperienceBottleEntity ? 0.07 : 0.03)
               );
            }
         }
      }
   }

   private static void trajectory(MioConfiguredModule var0, MatrixStack var1, Entity var2, Vec3d var3, Vec3d var4, double var5) {
      ArrayList<Vec3d> var7 = new ArrayList<>();
      var7.add(var3);
      HitResult var8 = null;

      for (int var9 = 0; var9 < 1000; var9++) {
         Vec3d var10 = var3.add(var4);
         BlockHitResult var11 = mc.world.raycast(new RaycastContext(var3, var10, ShapeType.COLLIDER, FluidHandling.NONE, var2));
         EntityHitResult var12 = ProjectileUtil.getEntityCollision(
            mc.world, var2, var3, var10, new Box(var3, var10).expand(1.0), var1x -> var1x != var2 && !var1x.isSpectator() && var1x.isAlive(), 0.3F
         );
         var8 = var12 == null || var11.getType() != Type.MISS && !(var12.getPos().squaredDistanceTo(var3) < var11.getPos().squaredDistanceTo(var3))
            ? var11
            : var12;
         if (var8.getType() != Type.MISS) {
            var7.add(var8.getPos());
            break;
         }

         var7.add(var10);
         var3 = var10;
         var4 = var4.multiply(mc.world.getFluidState(BlockPos.ofFloored(var10)).isEmpty() ? 0.99 : 0.8).add(0.0, -var5, 0.0);
         if (var10.y < mc.world.getBottomY() - 16) {
            break;
         }
      }

      MioRender.strip(var1, var7, var0.c("color"), var0.f("lineWidth"));
      if (var8 != null && var8.getType() != Type.MISS) {
         Box var13 = var8 instanceof EntityHitResult var14 ? var14.getEntity().getBoundingBox() : Box.of(var8.getPos(), 0.5, 0.01, 0.5);
         MioRender.fill(var1, var13, MioRender.alpha(var0.c("color"), 60));
         MioRender.outline(var1, var13, var0.c("color"), var0.f("lineWidth"));
      }
   }

   public static void screen(MioConfiguredModule var0, Render2DEvent var1) {
      DrawContext var2 = var1.getContext();
      int var3 = mc.getWindow().getScaledWidth() / 2;
      int var4 = mc.getWindow().getScaledHeight() / 2;
      String var5 = var0.getName();
      switch (var5) {
         case "Blur":
            if (mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen)) {
               var2.applyBlur();
            }
            break;
         case "Crosshair":
            if (!mc.options.getPerspective().isFirstPerson() && !var0.b("thirdPerson")) {
               return;
            }

            Input var25 = mc.player.input;
            float var28 = var25.getMovementInput().x == 0.0F && var25.getMovementInput().y == 0.0F && !var25.playerInput.jump() ? 0.0F : 1.0F;
            long var32 = System.currentTimeMillis();
            crosshairProgress = var0.b("smooth")
               ? crosshairProgress + (var28 - crosshairProgress) * (float)(1.0 - Math.exp(-Math.max(0L, var32 - crosshairFrame) / 120.0))
               : var28;
            crosshairFrame = var32;
            float var37 = var0.f("gap") + (var0.b("dynamic") ? crosshairProgress * var0.f("amplitude") : 0.0F);
            float var39 = mc.getWindow().getScaleFactor();
            var2.getMatrices().pushMatrix();
            var2.getMatrices().scale(1.0F / var39, 1.0F / var39);
            int var41 = mc.getWindow().getWidth() / 2;
            int var43 = mc.getWindow().getHeight() / 2;
            if (var0.b("shadow")) {
               crosshair(var2, var41 + var0.i("distance"), var43 + var0.i("distance"), var0, var37, -16777216);
            }

            crosshair(var2, var41, var43, var0, var37, var0.c("color").getRGB());
            var2.getMatrices().popMatrix();
            break;
         case "Hitmarker":
            double var24 = (System.currentTimeMillis() - MioState.hitTime) / 1000.0;
            if (!var0.b("draw") || MioState.hitTime == 0L || var24 > var0.n("time") + var0.n("fadeTime")) {
               return;
            }

            float var31 = (float)MathHelper.clamp(1.0 - (var24 - var0.n("time")) / Math.max(0.001, var0.n("fadeTime")), 0.0, 1.0);
            Color var35 = MioRender.alpha(var0.c("color"), (int)(var0.c("color").getAlpha() * var31));
            Color var36 = MioRender.alpha(var0.c("outlineColor"), (int)(var0.c("outlineColor").getAlpha() * var31));
            int var38 = (int)Math.ceil(var0.n("length"));

            for (int var16 : new int[]{-1, 1}) {
               for (int var20 : new int[]{-1, 1}) {
                  for (int var21 = 3; var21 < var38 + 3; var21++) {
                     var2.fill(var3 + var16 * var21 - 1, var4 + var20 * var21 - 1, var3 + var16 * var21 + 2, var4 + var20 * var21 + 2, var36.getRGB());
                     var2.fill(var3 + var16 * var21, var4 + var20 * var21, var3 + var16 * var21 + 1, var4 + var20 * var21 + 1, var35.getRGB());
                  }
               }
            }
            break;
         case "Markers":
            for (PlayerEntity var27 : mc.world.getPlayers()) {
               if (var27 != mc.player && mc.player.distanceTo(var27) <= 32.0F) {
                  float var30 = mc.player.distanceTo(var27);
                  float var34 = MathHelper.clamp(1.0F - (var30 - 28.0F) / 4.0F, 0.0F, 1.0F);
                  Vec3d var11 = MioRender.lerp(var27, var1.getDelta()).subtract(MioRender.lerp(mc.player, var1.getDelta()));
                  float var12 = (float)Math.toDegrees(Math.atan2(var11.z, var11.x)) - 90.0F;
                  if ((
                        !var0.b("setting2")
                           || !(MathHelper.angleBetween(var12, mc.player.getYaw()) <= ((Integer)mc.options.getFov().getValue()).intValue() / 2.0F)
                     )
                     && !(var34 < 0.1F)) {
                     float var13 = (float)Math.toRadians(var12 - mc.player.getYaw() - 90.0F);
                     float var14 = MathHelper.clamp(mc.player.getPitch() + 30.0F, -90.0F, 90.0F) / 90.0F;
                     Color var15 = friend(var27) ? Color.CYAN : blend(Color.RED, Color.GREEN, 1.0F - MathHelper.clamp((var30 - 8.0F) / 24.0F, 0.0F, 1.0F));
                     var2.getMatrices().pushMatrix();
                     var2.getMatrices().translate(var3 + (float)Math.cos(var13) * var0.f("setting"), var4 + (float)Math.sin(var13) * var0.f("setting") * var14);
                     var2.getMatrices().rotate(var13);
                     FontDraw.drawTextWithShadow(var2, mc.textRenderer, ">", -3, -4, MioRender.alpha(var15, (int)(var34 * 255.0F)).getRGB());
                     var2.getMatrices().popMatrix();
                  }
               }
            }
            break;
         case "NameTags":
            nameTags(var0, var2, var1.getDelta());
            break;
         case "ESP":
            espLabels(var0, var2, var1.getDelta());
            break;
         case "Waypoints":
            if (var0.b("name")) {
               for (WaypointsSupport.Waypoint var26 : WaypointsSupport.waypoints()) {
                  Vec3d var29 = WaypointsSupport.converted(var26);
                  if (var26.toggled
                     && var29 != null
                     && var26.server.equalsIgnoreCase(WaypointsSupport.server())
                     && !(mc.player.getEntityPos().distanceTo(var29) > var0.n("distance") * 1000.0)) {
                     String var33 = var26.name;
                     if (var0.choice("info", "Coords")) {
                        var33 = var33 + String.format(Locale.ROOT, " [%.1f, %.1f, %.1f]", var29.x, var29.y, var29.z);
                     }

                     if (var0.choice("info", "Distance")) {
                        var33 = var33 + " [" + (int)mc.player.getEntityPos().distanceTo(var29) + "m]";
                     }

                     label(
                        var2,
                        var29.add(0.0, var0.b("eyesAlign") ? mc.player.getEyeY() - var29.y : 1.0, 0.0),
                        var33,
                        var0.c("color"),
                        new Color(10, 10, 10, var0.b("textBackground") ? 77 : 0),
                        var0.f("textScale")
                     );
                  }
               }
            }
            break;
         case "LogoutSpots":
            if (var0.b("nameTag")) {
               for (MioState.Logout var8 : MioState.logouts.values()) {
                  Vec3d var9 = var8.box().getCenter();
                  if (logoutVisible(var0, var8)) {
                     String var10 = var8.name();
                     if (var0.b("health")) {
                        var10 = var10 + " " + (int)var8.health();
                     }

                     if (var0.b("totems")) {
                        var10 = var10 + " -" + var8.pops();
                     }

                     if (var0.choice("position", "COORDINATES")) {
                        var10 = var10 + String.format(Locale.ROOT, " [%.1f, %.1f, %.1f]", var9.x, var8.box().minY, var9.z);
                     }

                     if (var0.choice("position", "DISTANCE")) {
                        var10 = var10 + " " + (int)var9.distanceTo(mc.player.getEntityPos()) + "m";
                     }

                     if (var0.b("time")) {
                        var10 = var10 + " " + Instant.ofEpochMilli(var8.time()).atZone(ZoneId.systemDefault()).toLocalTime().withNano(0);
                     }

                     label(var2, new Vec3d(var9.x, var8.box().maxY + 0.3, var9.z), var10, var0.c("nameTagColor"), var0.c("fillColor"), var0.f("textScale"));
                  }
               }
            }
      }
   }

   private static void rect(DrawContext var0, int var1, int var2, int var3, int var4, int var5, MioConfiguredModule var6) {
      if (var6.b("shadow")) {
         var0.fill(var1 - var6.i("distance"), var2 - var6.i("distance"), var3 + var6.i("distance"), var4 + var6.i("distance"), -16777216);
      }

      var0.fill(var1, var2, var3, var4, var5);
   }

   private static void crosshair(DrawContext var0, int var1, int var2, MioConfiguredModule var3, float var4, int var5) {
      int var6 = var3.i("width");
      int var7 = var3.i("length");
      int var8 = var1 - var6 / 2;
      int var9 = var2 - var6 / 2;
      int var10 = Math.round(var4);
      if (var3.b("dot")) {
         var0.fill(var8, var9, var8 + var6, var9 + var6, var5);
      }

      if (var7 != 0) {
         var0.fill(var8, var2 - var10 - var7, var8 + var6, var2 - var10, var5);
         var0.fill(var1 + var10, var9, var1 + var10 + var7, var9 + var6, var5);
         var0.fill(var8, var2 + var10, var8 + var6, var2 + var10 + var7, var5);
         var0.fill(var1 - var10 - var7, var9, var1 - var10, var9 + var6, var5);
      }
   }

   private static void espLabels(MioConfiguredModule var0, DrawContext var1, float var2) {
      if (var0.b("chorus")) {
         for (Vec3d var4 : MioState.chorus.keySet()) {
            label(var1, var4, "Player teleport", var0.c("chorusText"), new Color(0, true), 1.0F);
         }
      }

      ArrayList var14 = new ArrayList();

      for (Entity var5 : mc.world.getEntities()) {
         if (var0.b("items") && !var0.choice("mode", "BOX") && var5 instanceof ItemEntity var6 && mc.player.distanceTo(var5) <= var0.n("range")) {
            var14.add(var6);
         }

         if (var0.b("mobOwner") && var5 instanceof Ownable var18 && var18.getOwner() != null) {
            label(
               var1,
               MioRender.lerp(var5, var2).add(0.0, var5.getHeight() + 0.25, 0.0),
               "Owner: " + var18.getOwner().getName().getString(),
               Color.WHITE,
               new Color(0, true),
               1.0F
            );
         }
      }

      while (!var14.isEmpty()) {
         ItemEntity var16 = (ItemEntity)var14.removeFirst();
         TreeMap<String, Integer> var17 = new TreeMap<>(Comparator.reverseOrder());
         var17.put(var16.getStack().getName().getString(), var16.getStack().getCount());
         Box var19 = MioRender.lerpBox(var16, var2);
         if (var0.b("group")) {
            Box var7 = Box.of(MioRender.lerp(var16, var2), 5.0, 5.0, 5.0);
            Iterator var8 = var14.iterator();

            while (var8.hasNext()) {
               ItemEntity var9 = (ItemEntity)var8.next();
               if (var7.intersects(var9.getBoundingBox())) {
                  var17.merge(var9.getStack().getName().getString(), var9.getStack().getCount(), Integer::sum);
                  var19 = var19.union(MioRender.lerpBox(var9, var2));
                  var8.remove();
               }
            }
         }

         Vec3d var20 = new Vec3d(var19.getCenter().x, var19.maxY + 0.25, var19.getCenter().z);
         Vec3d var21 = MioRender.project(var20);
         if (var21 != null && !(var21.z > 1.0)) {
            var1.getMatrices().pushMatrix();
            var1.getMatrices().translate((float)var21.x, (float)var21.y);
            var1.getMatrices().scale(var0.f("scale"), var0.f("scale"));
            byte var22 = 0;

            for (Entry<String, Integer> var11 : var17.entrySet()) {
               String var12 = var11.getKey() + (var11.getValue() > 1 ? " x" + var11.getValue() : "");
               int var13 = FontDraw.width(mc.textRenderer, var12);
               var1.fill(-var13 / 2 - 2, var22 - 1, var13 / 2 + 2, var22 + 9, var0.c("background").getRGB());
               FontDraw.drawTextWithShadow(var1, mc.textRenderer, var12, -var13 / 2, var22, var0.c("itemsText").getRGB());
               var22 += 10;
            }

            var1.getMatrices().popMatrix();
         }
      }
   }

   public static void label(DrawContext var0, Vec3d var1, String var2, Color var3, Color var4, float var5) {
      Vec3d var6 = MioRender.project(var1);
      if (var6 != null && !(var6.z > 1.0)) {
         int var7 = FontDraw.width(mc.textRenderer, var2);
         var0.getMatrices().pushMatrix();
         var0.getMatrices().translate((float)var6.x, (float)var6.y);
         var0.getMatrices().scale(var5, var5);
         var0.fill(-var7 / 2 - 2, -2, var7 / 2 + 2, 10, var4.getRGB());
         FontDraw.drawTextWithShadow(var0, mc.textRenderer, var2, -var7 / 2, 0, var3.getRGB());
         var0.getMatrices().popMatrix();
      }
   }

   private static void nameTags(MioConfiguredModule var0, DrawContext var1, float var2) {
      for (PlayerEntity var4 : mc.world.getPlayers()) {
         if (var4 != mc.player && !(mc.player.distanceTo(var4) > var0.n("range")) && (var4.isAlive() || var0.b("dead"))) {
            String var5 = var0.b("name") ? var4.getName().getString() : "";
            PlayerListEntry var6 = mc.getNetworkHandler().getPlayerListEntry(var4.getUuid());
            if (var0.b("health")) {
               float var7 = var4.getHealth() + var4.getAbsorptionAmount();
               var5 = var5
                  + " "
                  + (var0.b("healthColor") ? (var7 < 6.0F ? "§c" : (var7 < 12.0F ? "§6" : (var7 < 18.0F ? "§e" : "§a"))) : "")
                  + (int)Math.ceil(var7)
                  + "§r";
            }

            if (var0.b("ping") && var6 != null) {
               var5 = var5 + " " + var6.getLatency() + "ms";
            }

            if (var0.b("gamemode") && var6 != null) {
               var5 = var5 + " [" + var6.getGameMode().asString().charAt(0) + "]";
            }

            if (var0.b("totemPops")) {
               var5 = var5 + " -" + MioState.pops.getOrDefault(var4.getUuid(), 0);
            }

            Color var23 = friend(var4) && var0.b("friend")
               ? Color.CYAN
               : (var4.isInvisible() ? var0.c("invisibles") : (var4.isSneaking() ? var0.c("sneak") : (var4.isUsingItem() ? var0.c("eating") : var0.c("text"))));
            Vec3d var8 = MioRender.lerp(var4, var2).add(0.0, var4.getHeight() + 0.25, 0.0);
            label(var1, var8, var5, var23, var0.c("fill"), var0.f("scale"));
            Vec3d var9 = MioRender.project(var8);
            if (var9 != null && !(var9.z > 1.0)) {
               Color var10 = var0.c("outline");
               if (var0.b("smart")) {
                  if (mc.world.getBlockCollisions(var4, var4.getBoundingBox()).iterator().hasNext()) {
                     var10 = var0.c("phase");
                  } else {
                     BlockPos var11 = var4.getBlockPos();
                     boolean var12 = mc.world.getBlockState(var11.down()).getBlock().getBlastResistance() >= 600.0F;

                     for (Direction var16 : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                        var12 &= mc.world.getBlockState(var11.offset(var16)).getBlock().getBlastResistance() >= 600.0F;
                     }

                     if (var12) {
                        var10 = var0.c("hole");
                     }
                  }
               }

                ArrayList<ItemStack> var24 = new ArrayList<>();
               if (var0.b("items")) {
                  var24.add(var4.getMainHandStack());
               }

               if (var0.b("armor")) {
                  for (EquipmentSlot var32 : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                     var24.add(var4.getEquippedStack(var32));
                  }
               }

               if (var0.b("items")) {
                  var24.add(var4.getOffHandStack());
               }

               var24.removeIf(ItemStack::isEmpty);
               var1.getMatrices().pushMatrix();
               var1.getMatrices().translate((float)var9.x, (float)var9.y);
               var1.getMatrices().scale(var0.f("scale"), var0.f("scale"));
               int var26 = -var24.size() * 9;
               int var28 = FontDraw.width(mc.textRenderer, var5);
               if (var10.getAlpha() > 0) {
                  var1.drawStrokedRectangle(-var28 / 2 - 3, -3, var28 + 6, 14, var10.getRGB());
               }

               for (ItemStack var33 : var24) {
                  if (!var33.isEmpty()) {
                     if (var0.b("showEating") && var4.isUsingItem() && var4.getActiveItem() == var33 && var33.contains(DataComponentTypes.FOOD)) {
                        var1.fill(
                           var26,
                           -22,
                           var26 + (int)(16.0F * MathHelper.clamp((float)var4.getItemUseTime() / var33.getMaxUseTime(var4), 0.0F, 1.0F)),
                           -4,
                           MioRender.alpha(var0.c("eating"), 55).getRGB()
                        );
                     }

                     if (!var0.choice("durability", "ONLY")) {
                        var1.drawItem(var33, var26, -21);
                        var1.drawStackOverlay(mc.textRenderer, var33, var26, -21);
                     }

                     if (var33.isDamageable() && !var0.choice("durability", "HIDE")) {
                        int var34 = (int)((var33.getMaxDamage() - var33.getDamage()) * 100.0F / var33.getMaxDamage());
                        smallText(var1, var34 + "%", var26, -31, Color.HSBtoRGB(var34 / 300.0F, 1.0F, 1.0F));
                     }

                     if (var0.b("enchants") && !var0.choice("durability", "ONLY")) {
                         Set<it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<RegistryEntry<Enchantment>>> var35 = var33.getEnchantments().getEnchantmentEntries();
                        boolean var17 = !var35.isEmpty()
                           && var35.stream().allMatch(var0x -> var0x.getIntValue() >= ((Enchantment)((RegistryEntry)var0x.getKey()).value()).getMaxLevel());
                        byte var18 = -39;
                        if (var0.b("hideMax") && var17) {
                           smallText(var1, "Max", var26, var18, -43691);
                        } else {
                           for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry var20 : var35) {
                              String var21 = Enchantment.getName((RegistryEntry)var20.getKey(), var20.getIntValue()).getString();
                               String var22 = var21.substring(
                                 0, Math.min(var21.length(), ((Enchantment)((RegistryEntry)var20.getKey()).value()).getMaxLevel() == 1 ? 3 : 2)
                              );
                              smallText(
                                 var1,
                                 var22 + (((Enchantment)((RegistryEntry)var20.getKey()).value()).getMaxLevel() > 1 ? var20.getIntValue() : ""),
                                 var26,
                                 var18,
                                 -1
                              );
                              var18 -= 6;
                           }
                        }
                     }
                  }

                  var26 += 18;
               }

               if (var0.b("itemName") && !var4.getMainHandStack().isEmpty()) {
                  String var31 = var4.getMainHandStack().getName().getString();
                  FontDraw.drawTextWithShadow(var1, mc.textRenderer, var31, -FontDraw.width(mc.textRenderer, var31) / 2, 12, var23.getRGB());
               }

               var1.getMatrices().popMatrix();
            }
         }
      }
   }

   private static void smallText(DrawContext var0, String var1, int var2, int var3, int var4) {
      var0.getMatrices().pushMatrix();
      var0.getMatrices().translate(var2, var3);
      var0.getMatrices().scale(0.6F, 0.6F);
      FontDraw.drawTextWithShadow(var0, mc.textRenderer, var1, 0, 0, var4);
      var0.getMatrices().popMatrix();
   }

   private record Trail(Vec3d pos, long time) {
   }
}
