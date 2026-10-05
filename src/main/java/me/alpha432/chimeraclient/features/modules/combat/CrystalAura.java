package me.alpha432.chimeraclient.features.modules.combat;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import me.alpha432.chimeraclient.event.impl.network.PacketEvent;
import me.alpha432.chimeraclient.event.impl.render.Render3DEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.modules.client.ClickGuiModule;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.shoreline.PortSupport;
import me.alpha432.chimeraclient.shoreline.util.collection.EvictingQueue;
import me.alpha432.chimeraclient.shoreline.util.entity.EntityUtil;
import me.alpha432.chimeraclient.shoreline.util.math.PerSecondCounter;
import me.alpha432.chimeraclient.shoreline.util.math.timer.CacheTimer;
import me.alpha432.chimeraclient.shoreline.util.math.timer.Timer;
import me.alpha432.chimeraclient.shoreline.util.player.InventoryUtil;
import me.alpha432.chimeraclient.shoreline.util.player.PlayerUtil;
import me.alpha432.chimeraclient.shoreline.util.player.RotationUtil;
import me.alpha432.chimeraclient.shoreline.util.render.animation.Animation;
import me.alpha432.chimeraclient.shoreline.util.world.BlastResistantBlocks;
import me.alpha432.chimeraclient.shoreline.util.world.ExplosionUtil;
import me.alpha432.chimeraclient.util.render.RenderUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class CrystalAura extends PortSupport.CombatModule {
   private static CrystalAura INSTANCE;
   Setting<Boolean> whileMiningConfig = this.register(new PortSupport.BooleanConfig("WhileMining", "Allows attacking while mining blocks", false));
   Setting<Float> targetRangeConfig = this.register(new PortSupport.NumberConfig<>("EnemyRange", "Range to search for potential enemies", 1.0F, 10.0F, 13.0F));
   Setting<Boolean> instantConfig = this.register(new PortSupport.BooleanConfig("Instant", "Instantly attacks crystals when they spawn", false));
   Setting<CrystalAura.Sequential> sequentialConfig = this.register(
      new PortSupport.EnumConfig<>("Sequential", "Places a crystal after spawn", CrystalAura.Sequential.NONE, CrystalAura.Sequential.values())
   );
   Setting<Boolean> idPredictConfig = this.register(new PortSupport.BooleanConfig("BreakPredict", "Attempts to predict crystal entity ids", false));
   Setting<Boolean> instantCalcConfig = this.register(
      new PortSupport.BooleanConfig(
         "Instant-Calc",
         "Calculates a crystal when it spawns and attacks if it meets MINIMUM requirements, this will result in non-ideal crystal attacks",
         false,
         () -> false
      )
   );
   Setting<Float> instantDamageConfig = this.register(
      new PortSupport.NumberConfig<>("InstantDamage", "Minimum damage to attack crystals instantly", 1.0F, 6.0F, 10.0F, () -> false)
   );
   Setting<Boolean> instantMaxConfig = this.register(
      new PortSupport.BooleanConfig(
         "InstantMax",
         "Attacks crystals instantly if they exceed the previous max attack damage (Note: This is still not a perfect check because the next tick could have better damages)",
         true,
         () -> false
      )
   );
   Setting<Boolean> raytraceConfig = this.register(new PortSupport.BooleanConfig("Raytrace", "Raytrace to crystal position", true));
   Setting<Boolean> swingConfig = this.register(new PortSupport.BooleanConfig("Swing", "Swing hand when placing and attacking crystals", true));
   Setting<Boolean> rotateConfig = this.register(new PortSupport.BooleanConfig("Rotate", "Rotate before placing and breaking", false));
   Setting<CrystalAura.Rotate> strictRotateConfig = this.register(
      new PortSupport.EnumConfig<>(
         "YawStep",
         "Rotates yaw over multiple ticks to prevent certain rotation flags in NCP",
         CrystalAura.Rotate.OFF,
         CrystalAura.Rotate.values(),
         () -> this.rotateConfig.getValue()
      )
   );
   Setting<Integer> rotateLimitConfig = this.register(
      new PortSupport.NumberConfig<>(
         "YawStep-Limit",
         "Maximum yaw rotation in degrees for one tick",
         1,
         180,
         180,
         PortSupport.NumberDisplay.DEGREES,
         () -> this.rotateConfig.getValue() && this.strictRotateConfig.getValue() != CrystalAura.Rotate.OFF
      )
   );
   Setting<Boolean> playersConfig = this.register(new PortSupport.BooleanConfig("Players", "Target players", true));
   Setting<Boolean> monstersConfig = this.register(new PortSupport.BooleanConfig("Monsters", "Target monsters", false));
   Setting<Boolean> neutralsConfig = this.register(new PortSupport.BooleanConfig("Neutrals", "Target neutrals", false));
   Setting<Boolean> animalsConfig = this.register(new PortSupport.BooleanConfig("Animals", "Target animals", false));
   Setting<Boolean> shulkersConfig = this.register(new PortSupport.BooleanConfig("Shulkers", "Target shulker boxes", false));
   Setting<Float> breakSpeedConfig = this.register(new PortSupport.NumberConfig<>("BreakSpeed", "Speed to break crystals", 0.1F, 18.0F, 20.0F));
   Setting<Float> attackDelayConfig = this.register(new PortSupport.NumberConfig<>("AttackDelay", "Added delays", 0.0F, 0.0F, 5.0F));
   Setting<Integer> attackFactorConfig = this.register(
      new PortSupport.NumberConfig<>("AttackFactor", "Factor of attack delay", 0, 0, 3, () -> this.attackDelayConfig.getValue().floatValue() > 0.0)
   );
   Setting<Float> attackLimitConfig = this.register(
      new PortSupport.NumberConfig<>("AttackLimit", "The number of attacks before considering a crystal unbreakable", 0.5F, 1.5F, 20.0F)
   );
   Setting<Boolean> breakDelayConfig = this.register(new PortSupport.BooleanConfig("BreakDelay", "Uses attack latency to calculate break delays", false));
   Setting<Float> breakTimeoutConfig = this.register(
      new PortSupport.NumberConfig<>(
         "BreakTimeout",
         "Time after waiting for the average break time before considering a crystal attack failed",
         0.0F,
         3.0F,
         10.0F,
         () -> this.breakDelayConfig.getValue()
      )
   );
   Setting<Float> minTimeoutConfig = this.register(
      new PortSupport.NumberConfig<>(
         "MinTimeout", "Minimum time before considering a crystal break/place failed", 0.0F, 5.0F, 20.0F, () -> this.breakDelayConfig.getValue()
      )
   );
   Setting<Integer> ticksExistedConfig = this.register(
      new PortSupport.NumberConfig<>("TicksExisted", "Minimum ticks alive to consider crystals for attack", 0, 0, 10)
   );
   Setting<Float> breakRangeConfig = this.register(new PortSupport.NumberConfig<>("BreakRange", "Range to break crystals", 0.1F, 4.0F, 6.0F));
   Setting<Float> maxYOffsetConfig = this.register(new PortSupport.NumberConfig<>("MaxYOffset", "Maximum crystal y-offset difference", 1.0F, 5.0F, 10.0F));
   Setting<Float> breakWallRangeConfig = this.register(
      new PortSupport.NumberConfig<>("BreakWallRange", "Range to break crystals through walls", 0.1F, 4.0F, 6.0F)
   );
   Setting<CrystalAura.Swap> antiWeaknessConfig = this.register(
      new PortSupport.EnumConfig<>("AntiWeakness", "Swap to tools before attacking crystals", CrystalAura.Swap.OFF, CrystalAura.Swap.values())
   );
   Setting<Float> swapDelayConfig = this.register(
      new PortSupport.NumberConfig<>("SwapPenalty", "Delay for attacking after swapping items which prevents NCP flags", 0.0F, 0.0F, 10.0F)
   );
   Setting<Boolean> inhibitConfig = this.register(new PortSupport.BooleanConfig("Inhibit", "Prevents excessive attacks", true));
   Setting<Boolean> placeConfig = this.register(
      new PortSupport.BooleanConfig("Place", "Places crystals to damage enemies. Place settings will only function if this setting is enabled.", true)
   );
   Setting<Float> placeSpeedConfig = this.register(
      new PortSupport.NumberConfig<>("PlaceSpeed", "Speed to place crystals", 0.1F, 18.0F, 20.0F, () -> this.placeConfig.getValue())
   );
   Setting<Float> placeLimitConfig = this.register(
      new PortSupport.NumberConfig<>("PlaceLimit", "Limit to place crystals", 0.1F, 10.0F, 100.0F, () -> this.placeConfig.getValue())
   );
   Setting<Float> placeRangeConfig = this.register(
      new PortSupport.NumberConfig<>("PlaceRange", "Range to place crystals", 0.1F, 4.0F, 6.0F, () -> this.placeConfig.getValue())
   );
   Setting<Float> placeWallRangeConfig = this.register(
      new PortSupport.NumberConfig<>("PlaceWallRange", "Range to place crystals through walls", 0.1F, 4.0F, 6.0F, () -> this.placeConfig.getValue())
   );
   Setting<Boolean> placeRangeEyeConfig = this.register(
      new PortSupport.BooleanConfig(
         "PlaceRangeEye", "Calculates place ranges starting from the eye position of the player", false, () -> this.placeConfig.getValue()
      )
   );
   Setting<Boolean> placeRangeCenterConfig = this.register(
      new PortSupport.BooleanConfig("PlaceRangeCenter", "Calculates place ranges to the center of the block", true, () -> this.placeConfig.getValue())
   );
   Setting<CrystalAura.Swap> autoSwapConfig = this.register(
      new PortSupport.EnumConfig<>(
         "Swap",
         "Swaps to an end crystal before placing if the player is not holding one",
         CrystalAura.Swap.OFF,
         CrystalAura.Swap.values(),
         () -> this.placeConfig.getValue()
      )
   );
   Setting<Boolean> antiSurroundConfig = this.register(
      new PortSupport.BooleanConfig(
         "AntiSurround",
         "Places on mining blocks that when broken, can be placed on to damage enemies. Instantly destroys items spawned from breaking block and allows faster placing",
         false,
         () -> this.placeConfig.getValue()
      )
   );
   Setting<CrystalAura.ForcePlace> forcePlaceConfig = this.register(
      new PortSupport.EnumConfig<>("PreventReplace", "Attempts to replace crystals in surrounds", CrystalAura.ForcePlace.NONE, CrystalAura.ForcePlace.values())
   );
   Setting<Boolean> breakValidConfig = this.register(
      new PortSupport.BooleanConfig("Strict", "Only places crystals that can be attacked", false, () -> this.placeConfig.getValue())
   );
   Setting<Boolean> strictDirectionConfig = this.register(
      new PortSupport.BooleanConfig("StrictDirection", "Interacts with only visible directions when placing crystals", false, () -> this.placeConfig.getValue())
   );
   Setting<CrystalAura.Placements> placementsConfig = this.register(
      new PortSupport.EnumConfig<>(
         "Placements",
         "Version standard for placing end crystals",
         CrystalAura.Placements.NATIVE,
         CrystalAura.Placements.values(),
         () -> this.placeConfig.getValue()
      )
   );
   Setting<Float> minDamageConfig = this.register(
      new PortSupport.NumberConfig<>("MinDamage", "Minimum damage required to consider attacking or placing an end crystal", 1.0F, 4.0F, 10.0F)
   );
   Setting<Float> maxLocalDamageConfig = this.register(new PortSupport.NumberConfig<>("MaxLocalDamage", "The maximum player damage", 4.0F, 12.0F, 20.0F));
   Setting<Boolean> assumeArmorConfig = this.register(new PortSupport.BooleanConfig("AssumeBestArmor", "Assumes Prot 0 armor is max armor", false));
   Setting<Boolean> armorBreakerConfig = this.register(new PortSupport.BooleanConfig("ArmorBreaker", "Attempts to break enemy armor with crystals", true));
   Setting<Float> armorScaleConfig = this.register(
      new PortSupport.NumberConfig<>(
         "ArmorScale",
         "Armor damage scale before attempting to break enemy armor with crystals",
         1.0F,
         5.0F,
         20.0F,
         PortSupport.NumberDisplay.PERCENT,
         () -> this.armorBreakerConfig.getValue()
      )
   );
   Setting<Float> lethalMultiplier = this.register(
      new PortSupport.NumberConfig<>("LethalMultiplier", "If we can kill an enemy with this many crystals, disregard damage values", 0.0F, 1.5F, 4.0F)
   );
   Setting<Boolean> antiTotemConfig = this.register(
      new PortSupport.BooleanConfig(
         "Lethal-Totem", "Predicts totems and places crystals to instantly double pop and kill the target", false, () -> this.placeConfig.getValue()
      )
   );
   Setting<Boolean> lethalDamageConfig = this.register(
      new PortSupport.BooleanConfig("Lethal-DamageTick", "Places lethal crystals only on ticks where they damage entities", false)
   );
   Setting<Boolean> safetyConfig = this.register(
      new PortSupport.BooleanConfig("Safety", "Accounts for total player safety when attacking and placing crystals", true)
   );
   Setting<Boolean> safetyOverride = this.register(
      new PortSupport.BooleanConfig("SafetyOverride", "Overrides the safety checks if the crystal will kill an enemy", false)
   );
   Setting<Boolean> blockDestructionConfig = this.register(
      new PortSupport.BooleanConfig("BlockDestruction", "Accounts for explosion block destruction when calculating damages", false)
   );
   Setting<Boolean> selfExtrapolateConfig = this.register(
      new PortSupport.BooleanConfig("SelfExtrapolate", "Accounts for motion when calculating self damage", false)
   );
   Setting<Integer> extrapolateTicksConfig = this.register(
      new PortSupport.NumberConfig<>("ExtrapolationTicks", "Accounts for motion when calculating enemy positions, not fully accurate.", 0, 0, 10)
   );
   Setting<Boolean> renderConfig = this.register(new PortSupport.BooleanConfig("Render", "Renders the current placement", true));
   Setting<Integer> fadeTimeConfig = this.register(new PortSupport.NumberConfig<>("Fade-Time", "Timer for the fade", 0, 250, 1000, () -> false));
   Setting<Boolean> disableDeathConfig = this.register(new PortSupport.BooleanConfig("DisableOnDeath", "Disables during disconnect/death", false));
   Setting<Boolean> debugConfig = new PortSupport.BooleanConfig("Debug", "Adds extra debug info to arraylist", false);
   Setting<Boolean> debugDamageConfig = new PortSupport.BooleanConfig("Debug-Damage", "Renders damage", false, () -> this.renderConfig.getValue());
   private CrystalAura.DamageData<EndCrystalEntity> attackCrystal;
   private CrystalAura.DamageData<BlockPos> placeCrystal;
   private BlockPos renderPos;
   private double renderDamage;
   private BlockPos renderSpawnPos;
   private Vec3d crystalRotation;
   private boolean attackRotate;
   private boolean rotated;
   private float[] silentRotations;
   private float calculatePlaceCrystalTime = 0.0F;
   private static final Box FULL_CRYSTAL_BB = new Box(0.0, 0.0, 0.0, 1.0, 2.0, 1.0);
   private static final Box HALF_CRYSTAL_BB = new Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
   private final CacheTimer lastAttackTimer = new CacheTimer();
   private final Timer lastPlaceTimer = new CacheTimer();
   private final Timer lastSwapTimer = new CacheTimer();
   private final Timer autoSwapTimer = new CacheTimer();
   private final Deque<Long> attackLatency = new EvictingQueue<>(20);
   private final Map<Integer, Long> attackPackets = Collections.synchronizedMap(new ConcurrentHashMap<>());
   private final Map<BlockPos, Long> placePackets = Collections.synchronizedMap(new ConcurrentHashMap<>());
   private final PerSecondCounter sequentialLimit = new PerSecondCounter();
   private final PerSecondCounter crystalCounter = new PerSecondCounter();
   private final Map<BlockPos, Animation> fadeList = new HashMap<>();
   private long predictId;
   private final Map<Integer, Integer> antiStuckCrystals = new HashMap<>();
   private final List<CrystalAura.AntiStuckData> stuckCrystals = new CopyOnWriteArrayList<>();
   private ClientWorld trackedWorld;

   @Override
   public void onTick() {
   }

   public void shorelinePlayerTick() {
      if (mc.world != this.trackedWorld) {
         this.onDisable();
         PortSupport.reset();
         this.trackedWorld = mc.world;
      }

      if (mc.player != null && mc.world != null) {
         if (mc.player.isDead()) {
            if (this.disableDeathConfig.getValue()) {
               this.disable();
            } else {
               this.onDisable();
            }
         } else {
            PortSupport.Managers.tick();
            this.onPlayerUpdate(new PortSupport.PlayerTickEvent());
         }
      } else {
         if (this.disableDeathConfig.getValue()) {
            this.disable();
         }
      }
   }

   public CrystalAura() {
      super("CrystalAura", "Attacks entities with end crystals", Module.Category.COMBAT, 750);
      INSTANCE = this;
      this.register(this.debugConfig);
      this.register(this.debugDamageConfig);
   }

   public static CrystalAura getInstance() {
      return INSTANCE;
   }

   @Override
   public String getDisplayInfo() {
      return this.debugConfig.getValue()
         ? String.format(
            "%sms, %.0f, %dms, %d"
               .formatted(
                  new DecimalFormat("0.00").format(this.calculatePlaceCrystalTime / 1000000.0),
                  this.placeCrystal == null ? 0.0 : this.lastAttackTimer.getLastResetTime() / 1000000.0,
                  this.lastAttackTimer.passed((20.0F - this.breakSpeedConfig.getValue()) * 50.0F + 2000.0F) ? 0 : this.getBreakMs(),
                  this.crystalCounter.getPerSecond()
               )
         )
         : String.format(
            "%dms, %d",
            this.lastAttackTimer.passed((20.0F - this.breakSpeedConfig.getValue()) * 50.0F + 2000.0F) ? 0 : this.getBreakMs(),
            this.crystalCounter.getPerSecond()
         );
   }

   @Override
   public void onDisable() {
      PortSupport.reset();
      this.predictId = 0L;
      this.renderSpawnPos = null;
      this.attackRotate = this.rotated = false;
      this.renderPos = null;
      this.attackCrystal = null;
      this.placeCrystal = null;
      this.crystalRotation = null;
      this.silentRotations = null;
      this.calculatePlaceCrystalTime = 0.0F;
      this.stuckCrystals.clear();
      this.attackPackets.clear();
      this.antiStuckCrystals.clear();
      this.placePackets.clear();
      this.attackLatency.clear();
      this.fadeList.clear();
      this.setStage("NONE");
   }

   public void onDisconnect(PortSupport.DisconnectEvent var1) {
      if (this.disableDeathConfig.getValue()) {
         this.disable();
      } else {
         this.onDisable();
      }
   }

   public void onPlayerUpdate(PortSupport.PlayerTickEvent var1) {
      if (!mc.player.isSpectator() && (!this.isSilentSwap(this.autoSwapConfig.getValue()) || !PortSupport.AutoMineModule.getInstance().isSilentSwapping())) {
         for (CrystalAura.AntiStuckData var3 : this.stuckCrystals) {
            double var4 = mc.player.squaredDistanceTo(var3.pos());
            double var6 = var3.stuckDist() - var4;
            if (var6 > 0.5) {
               this.stuckCrystals.remove(var3);
            }
         }

         if (mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.MAIN_HAND || mc.options.attackKey.isPressed() || PlayerUtil.isHotbarKeysPressed()) {
            this.autoSwapTimer.reset();
         }

         this.renderPos = null;
         ArrayList var15 = Lists.newArrayList(mc.world.getEntities());
         List var16 = this.getSphere(this.placeRangeEyeConfig.getValue() ? mc.player.getEyePos() : mc.player.getEntityPos());
         long var17 = System.nanoTime();
         if (this.placeConfig.getValue()) {
            this.placeCrystal = this.calculatePlaceCrystal(var16, var15);
         }

         this.attackCrystal = this.calculateAttackCrystal(var15);
         if (this.attackCrystal == null) {
            EndCrystalEntity var18;
            if (this.placeCrystal != null && (var18 = this.intersectingCrystalCheck(this.placeCrystal.getDamageData())) != null) {
               double var7 = ExplosionUtil.getDamageTo(
                  mc.player,
                  var18.getEntityPos(),
                  this.blockDestructionConfig.getValue(),
                  this.selfExtrapolateConfig.getValue() ? this.extrapolateTicksConfig.getValue() : 0,
                  false
               );
               if (!this.safetyConfig.getValue() || !this.playerDamageCheck(var7)) {
                  this.attackCrystal = new CrystalAura.DamageData<>(
                     var18, this.placeCrystal.getAttackTarget(), this.placeCrystal.getDamage(), var7, var18.getBlockPos().down(), false
                  );
               }
            }

            this.calculatePlaceCrystalTime = (float)(System.nanoTime() - var17);
         }

         if (this.inhibitConfig.getValue() && this.attackCrystal != null && this.attackPackets.containsKey(this.attackCrystal.getDamageData().getId())) {
            float var19;
            if (this.attackDelayConfig.getValue().floatValue() > 0.0) {
               float var21 = 50.0F / Math.max(1.0F, (float)this.attackFactorConfig.getValue().intValue());
               var19 = this.attackDelayConfig.getValue() * var21;
            } else {
               var19 = 1000.0F - this.breakSpeedConfig.getValue() * 50.0F;
            }

            this.lastAttackTimer.setDelay(var19 + 100.0F);
            this.attackPackets.remove(this.attackCrystal.getDamageData().getId());
         }

         float var20 = this.getBreakDelay();
         if (this.breakDelayConfig.getValue()) {
            var20 = Math.max(this.minTimeoutConfig.getValue() * 50.0F, this.getBreakMs() + this.breakTimeoutConfig.getValue() * 50.0F);
         }

         boolean var22 = this.attackRotate = this.attackCrystal != null
            && this.attackDelayConfig.getValue().floatValue() <= 0.0
            && this.lastAttackTimer.passed(var20);
         if (this.attackCrystal != null) {
            this.crystalRotation = this.attackCrystal.damageData.getEntityPos();
         } else if (this.placeCrystal != null) {
            this.crystalRotation = this.placeCrystal.damageData.toCenterPos().add(0.0, 0.5, 0.0);
         }

         if (this.rotateConfig.getValue() && this.crystalRotation != null && (this.placeCrystal == null || this.canHoldCrystal())) {
            float[] var8 = RotationUtil.getRotationsTo(mc.player.getEyePos(), this.crystalRotation);
            if (this.strictRotateConfig.getValue() == CrystalAura.Rotate.FULL
               || this.strictRotateConfig.getValue() == CrystalAura.Rotate.SEMI && this.attackRotate) {
               float var10 = PortSupport.Managers.ROTATION.getWrappedYaw();
               float var11 = var10 - var8[0];
               float var12 = Math.abs(var11);
               if (var12 > 180.0F) {
                  var11 += var11 > 0.0F ? -360.0F : 360.0F;
               }

               int var13 = var11 > 0.0F ? -1 : 1;
               float var14 = var13 * this.rotateLimitConfig.getValue();
               float var9;
               if (var12 > this.rotateLimitConfig.getValue().intValue()) {
                  var9 = var10 + var14;
                  this.rotated = false;
               } else {
                  var9 = var8[0];
                  this.rotated = true;
                  this.crystalRotation = null;
               }

               var8[0] = var9;
            } else {
               this.rotated = true;
               this.crystalRotation = null;
            }

            this.setRotation(var8[0], var8[1]);
         } else {
            this.silentRotations = null;
         }

         if (!this.isRotationBlocked() && (this.rotated || !this.rotateConfig.getValue())) {
            Hand var23 = this.getCrystalHand();
            if (this.attackCrystal != null && this.attackRotate) {
               this.attackCrystal(this.attackCrystal.getDamageData(), var23);
               this.setStage("ATTACKING");
               this.lastAttackTimer.reset();
            }

            boolean var24 = this.lastPlaceTimer.passed(1000.0F - this.placeSpeedConfig.getValue() * 50.0F);
            if (this.placeCrystal != null) {
               this.renderPos = this.placeCrystal.getDamageData();
               this.renderDamage = this.placeCrystal.getDamage();
               if (var24) {
                  this.placeCrystal(this.placeCrystal.getDamageData(), var23);
                  this.setStage("PLACING");
                  this.lastPlaceTimer.reset();
               }
            }
         }
      }
   }

   public void onRunTick(PortSupport.RunTickEvent var1) {
      if (mc.player != null) {
         Hand var2 = this.getCrystalHand();
         if (this.attackDelayConfig.getValue().floatValue() > 0.0) {
            float var3 = 50.0F / Math.max(1.0F, (float)this.attackFactorConfig.getValue().intValue());
            if (this.attackCrystal != null && this.lastAttackTimer.passed(this.attackDelayConfig.getValue() * var3)) {
               this.attackCrystal(this.attackCrystal.getDamageData(), var2);
               this.lastAttackTimer.reset();
            }
         }
      }
   }

   @Subscribe
   public void onRenderWorld(Render3DEvent var1) {
      if (mc.world != null && mc.player != null) {
         if (this.renderConfig.getValue()) {
            BlockPos var2 = null;
            double var3 = 0.0;

            for (Entry var6 : this.fadeList.entrySet()) {
               if (var6.getKey() != this.renderPos) {
                  if (((Animation)var6.getValue()).getFactor() > var3) {
                     var2 = (BlockPos)var6.getKey();
                     var3 = ((Animation)var6.getValue()).getFactor();
                  }

                  ((Animation)var6.getValue()).setState(false);
                  int var7 = (int)(40.0 * ((Animation)var6.getValue()).getFactor());
                  int var8 = (int)(100.0 * ((Animation)var6.getValue()).getFactor());
                  Color var9 = this.renderColor(var7);
                  Color var10 = this.renderColor(var8);
                  RenderUtil.drawBoxFilled(var1.getMatrix(), (BlockPos)var6.getKey(), var9);
                  RenderUtil.drawBox(var1.getMatrix(), (BlockPos)var6.getKey(), var10, 1.5F);
               }
            }

            if (this.debugDamageConfig.getValue() && var2 != null) {
               this.renderDamageSign(String.format("%.1f", this.renderDamage), var2.toCenterPos(), new Color(255, 255, 255, (int)(255.0 * var3)).getRGB());
            }

            this.fadeList.entrySet().removeIf(var0 -> var0.getValue().getFactor() == 0.0);
            if (this.renderPos != null && this.isHoldingCrystal()) {
               Animation var11 = new Animation(true, this.fadeTimeConfig.getValue().intValue());
               this.fadeList.put(this.renderPos, var11);
            }
         }
      }
   }

   private Color renderColor(int var1) {
      Color var2 = ClickGuiModule.getInstance().color.getValue();
      return new Color(var2.getRed(), var2.getGreen(), var2.getBlue(), var1);
   }

   private void renderDamageSign(String var1, Vec3d var2, int var3) {
      Camera var4 = mc.gameRenderer.getCamera();
      Vec3d var5 = var4.getCameraPos();
      double var6 = var5.distanceTo(var2);
      float var8 = var6 <= 8.0 ? 0.0245F : (float)(0.0018F + 0.003F * var6);
      MatrixStack var9 = new MatrixStack();
      var9.multiply(RotationAxis.POSITIVE_X.rotationDegrees(var4.getPitch()));
      var9.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var4.getYaw() + 180.0F));
      var9.translate(var2.x - var5.x, var2.y - var5.y, var2.z - var5.z);
      var9.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-var4.getYaw()));
      var9.multiply(RotationAxis.POSITIVE_X.rotationDegrees(var4.getPitch()));
      var9.scale(-var8, -var8, -1.0F);
      Immediate var10 = mc.getBufferBuilders().getEntityVertexConsumers();
      mc.textRenderer
         .draw(var1, -mc.textRenderer.getWidth(var1) / 2.0F, 0.0F, var3, true, var9.peek().getPositionMatrix(), var10, TextLayerType.SEE_THROUGH, 0, 15728880);
      var10.draw();
   }

   @Subscribe
   public void onPacketInbound(PacketEvent.Receive var1) {
      if (mc.player != null && mc.world != null) {
         ClientWorld var2 = mc.world;
         Packet var3 = var1.getPacket();
         mc.execute(() -> {
            if (this.isEnabled() && mc.world == var2 && mc.player != null) {
               if (var3 instanceof BundleS2CPacket var3x) {
                  for (Packet var5 : var3x.getPackets()) {
                     this.handleServerPackets(var5);
                  }
               } else {
                  this.handleServerPackets(var3);
               }
            }
         });
      }
   }

   private void handleServerPackets(Packet<?> var1) {
      PortSupport.observeInbound(var1);
      if (var1 instanceof ExplosionS2CPacket var3) {
         for (Entity var5 : Lists.newArrayList(mc.world.getEntities())) {
            if (var5 instanceof EndCrystalEntity && var5.squaredDistanceTo(var3.center().x, var3.center().y, var3.center().z) < 144.0) {
               mc.executeSync(() -> mc.world.removeEntity(var5.getId(), RemovalReason.DISCARDED));
               this.antiStuckCrystals.remove(var5.getId());
               Long var2 = this.attackPackets.remove(var5.getId());
               if (var2 != null) {
                  this.attackLatency.add(System.currentTimeMillis() - var2);
               }
            }
         }
      }

      if (var1 instanceof PlaySoundS2CPacket var8
         && var8.getSound().value() == SoundEvents.ENTITY_GENERIC_EXPLODE.value()
         && var8.getCategory() == SoundCategory.BLOCKS) {
         for (Entity var13 : Lists.newArrayList(mc.world.getEntities())) {
            if (var13 instanceof EndCrystalEntity && var13.squaredDistanceTo(var8.getX(), var8.getY(), var8.getZ()) < 144.0) {
               mc.executeSync(() -> mc.world.removeEntity(var13.getId(), RemovalReason.DISCARDED));
               this.antiStuckCrystals.remove(var13.getId());
               Long var6 = this.attackPackets.remove(var13.getId());
               if (var6 != null) {
                  this.attackLatency.add(System.currentTimeMillis() - var6);
               }
            }
         }
      }

      if (var1 instanceof EntitiesDestroyS2CPacket var9) {
         IntListIterator var12 = var9.getEntityIds().iterator();

         while (var12.hasNext()) {
            int var14 = var12.next();
            this.antiStuckCrystals.remove(var14);
            Long var7 = this.attackPackets.remove(var14);
            if (var7 != null) {
               this.attackLatency.add(System.currentTimeMillis() - var7);
            }
         }
      }

      if (var1 instanceof EntitySpawnS2CPacket var10 && var10.getEntityId() > this.predictId) {
         this.predictId = var10.getEntityId();
      }
   }

   public void onAddEntity(PortSupport.AddEntityEvent var1) {
      if (var1.getEntity() instanceof EndCrystalEntity var3) {
         Vec3d var2 = var3.getEntityPos();
         BlockPos var4;
         this.renderSpawnPos = var4 = BlockPos.ofFloored(var2.add(0.0, -1.0, 0.0));
         Long var6 = this.placePackets.remove(var4);
         boolean var7 = this.attackRotate = var6 != null;
         if (this.attackRotate) {
            this.crystalCounter.updateCounter();
         }

         if (this.instantConfig.getValue()) {
            if (!this.attackRotate) {
               if (this.instantCalcConfig.getValue()) {
                  if (this.attackRangeCheck(var2)) {
                     return;
                  }

                  double var18 = ExplosionUtil.getDamageTo(
                     mc.player,
                     var2,
                     this.blockDestructionConfig.getValue(),
                     this.selfExtrapolateConfig.getValue() ? this.extrapolateTicksConfig.getValue() : 0,
                     false
                  );
                  if (this.playerDamageCheck(var18)) {
                     return;
                  }

                  for (Entity var20 : mc.world.getEntities()) {
                     if (var20 != null
                        && var20.isAlive()
                        && var20 != mc.player
                        && this.isValidTarget(var20)
                        && !PortSupport.Managers.SOCIAL.isFriend(var20.getName())
                        && !(var2.squaredDistanceTo(var20.getEntityPos()) > 144.0)
                        && !(mc.player.squaredDistanceTo(var20) > this.targetRangeConfig.getValue() * this.targetRangeConfig.getValue())) {
                        double var13 = ExplosionUtil.getDamageTo(
                           var20, var2, this.blockDestructionConfig.getValue(), this.extrapolateTicksConfig.getValue(), this.assumeArmorConfig.getValue()
                        );
                        CrystalAura.DamageData var15 = new CrystalAura.DamageData<>(var3, var20, var13, var18, var3.getBlockPos().down(), false);
                        LivingEntity var8;
                        boolean var16 = this.attackRotate = var13 > this.instantDamageConfig.getValue().floatValue()
                           || this.attackCrystal != null && var13 >= this.attackCrystal.getDamage() && this.instantMaxConfig.getValue()
                           || var20 instanceof LivingEntity && this.isCrystalLethalTo(var15, var8 = (LivingEntity)var20);
                        if (this.attackRotate) {
                           Hand var17 = this.getCrystalHand();
                           this.attackInternal(var3, var17);
                           this.setStage("ATTACKING");
                           this.lastAttackTimer.reset();
                           if (this.sequentialConfig.getValue() == CrystalAura.Sequential.NORMAL) {
                              this.placeSequentialCrystal(var17);
                           }
                           break;
                        }
                     }
                  }
               }
            } else {
               Hand var21 = this.getCrystalHand();
               this.attackInternal(var3, var21);
               this.setStage("ATTACKING");
               this.lastAttackTimer.reset();
               if (this.sequentialConfig.getValue() == CrystalAura.Sequential.NORMAL) {
                  this.placeSequentialCrystal(var21);
               }
            }
         }
      }
   }

   @Subscribe
   public void onPacketOutbound(PacketEvent.Send var1) {
      PortSupport.observeOutbound(var1.getPacket());
      if (mc.player != null) {
         if (var1.getPacket() instanceof UpdateSelectedSlotC2SPacket) {
            this.lastSwapTimer.reset();
         }
      }
   }

   public boolean isAttacking() {
      return this.attackCrystal != null;
   }

   public boolean isPlacing() {
      return this.placeCrystal != null && this.isHoldingCrystal();
   }

   public void attackCrystal(EndCrystalEntity var1, Hand var2) {
      if (!this.attackCheckPre(var2)) {
         StatusEffectInstance var3 = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
         StatusEffectInstance var4 = mc.player.getStatusEffect(StatusEffects.STRENGTH);
         if (var3 != null && (var4 == null || var3.getAmplifier() > var4.getAmplifier())) {
            int var5 = -1;

            for (int var6 = 0; var6 < 9; var6++) {
               ItemStack var7 = mc.player.getInventory().getStack(var6);
               if (!var7.isEmpty() && (var7.isIn(ItemTags.SWORDS) || var7.getItem() instanceof AxeItem || var7.isIn(ItemTags.PICKAXES))) {
                  var5 = var6;
                  break;
               }
            }

            if (var5 != -1) {
               boolean var8 = var5 != PortSupport.Managers.INVENTORY.getServerSlot()
                  && (this.antiWeaknessConfig.getValue() != CrystalAura.Swap.NORMAL || this.autoSwapTimer.passed(500));
               if (this.antiWeaknessConfig.getValue() != CrystalAura.Swap.OFF && var8) {
                  if (this.antiWeaknessConfig.getValue() == CrystalAura.Swap.SILENT_ALT) {
                     mc.interactionManager
                        .clickSlot(mc.player.playerScreenHandler.syncId, var5 + 36, mc.player.getInventory().getSelectedSlot(), SlotActionType.SWAP, mc.player);
                  } else if (this.antiWeaknessConfig.getValue() == CrystalAura.Swap.SILENT) {
                     PortSupport.Managers.INVENTORY.setSlot(var5);
                  } else {
                     PortSupport.Managers.INVENTORY.setClientSlot(var5);
                  }
               }

               this.attackInternal(var1, Hand.MAIN_HAND);
               if (var8) {
                  if (this.antiWeaknessConfig.getValue() == CrystalAura.Swap.SILENT_ALT) {
                     mc.interactionManager
                        .clickSlot(mc.player.playerScreenHandler.syncId, var5 + 36, mc.player.getInventory().getSelectedSlot(), SlotActionType.SWAP, mc.player);
                  } else if (this.antiWeaknessConfig.getValue() == CrystalAura.Swap.SILENT) {
                     PortSupport.Managers.INVENTORY.syncToClient();
                  }
               }

               if (this.sequentialConfig.getValue() == CrystalAura.Sequential.STRICT) {
                  this.placeSequentialCrystal(var2);
               }
            }
         } else {
            this.attackInternal(var1, var2);
            if (this.sequentialConfig.getValue() == CrystalAura.Sequential.STRICT) {
               this.placeSequentialCrystal(var2);
            }
         }
      }
   }

   private void attackInternal(EndCrystalEntity var1, Hand var2) {
      this.attackInternal(var1.getId(), var2);
   }

   private void attackInternal(int var1, Hand var2) {
      var2 = var2 != null ? var2 : Hand.MAIN_HAND;
      EndCrystalEntity var3 = new EndCrystalEntity(mc.world, 0.0, 0.0, 0.0);
      var3.setId(var1);
      PlayerInteractEntityC2SPacket var4 = PlayerInteractEntityC2SPacket.attack(var3, mc.player.isSneaking());
      PortSupport.Managers.NETWORK.sendPacket(var4);
      if (this.swingConfig.getValue()) {
         mc.player.swingHand(var2);
      } else {
         PortSupport.Managers.NETWORK.sendPacket(new HandSwingC2SPacket(var2));
      }

      this.attackPackets.put(var1, System.currentTimeMillis());
      Integer var5 = this.antiStuckCrystals.get(var1);
      if (var5 != null) {
         this.antiStuckCrystals.replace(var1, var5 + 1);
      } else {
         this.antiStuckCrystals.put(var1, 1);
      }
   }

   private void placeSequentialCrystal(Hand var1) {
      if (!(this.sequentialLimit.getPerSecond() > this.placeLimitConfig.getValue() * 10.0F)) {
         if (this.placeCrystal != null) {
            this.placeCrystal(this.placeCrystal.getBlockPos(), var1);
            this.sequentialLimit.updateCounter();
         }
      }
   }

   private void placeCrystal(BlockPos var1, Hand var2) {
      if (!this.isRotationBlocked() && (this.rotated || !this.rotateConfig.getValue())) {
         this.placeCrystal(var1, var2, true);
      }
   }

   public void placeCrystal(BlockPos var1, Hand var2, boolean var3) {
      if (!var3 || !this.checkCanUseCrystal()) {
         Direction var4 = this.getPlaceDirection(var1);
         BlockHitResult var5 = new BlockHitResult(var1.toCenterPos(), var4, var1, false);
         if (this.autoSwapConfig.getValue() != CrystalAura.Swap.OFF && var2 != Hand.OFF_HAND && this.getCrystalHand() == null) {
            if (this.isSilentSwap(this.autoSwapConfig.getValue()) && InventoryUtil.count(Items.END_CRYSTAL) == 0) {
               return;
            }

            int var6 = this.getCrystalSlot();
            if (var6 != -1) {
               boolean var7 = var6 != PortSupport.Managers.INVENTORY.getServerSlot()
                  && (this.autoSwapConfig.getValue() != CrystalAura.Swap.NORMAL || this.autoSwapTimer.passed(500));
               if (var7) {
                  if (this.autoSwapConfig.getValue() == CrystalAura.Swap.SILENT_ALT) {
                     mc.interactionManager
                        .clickSlot(mc.player.playerScreenHandler.syncId, var6 + 36, mc.player.getInventory().getSelectedSlot(), SlotActionType.SWAP, mc.player);
                  } else if (this.autoSwapConfig.getValue() == CrystalAura.Swap.SILENT) {
                     PortSupport.Managers.INVENTORY.setSlot(var6);
                  } else {
                     PortSupport.Managers.INVENTORY.setClientSlot(var6);
                  }
               }

               this.placeInternal(var5, Hand.MAIN_HAND);
               this.placePackets.put(var1, System.currentTimeMillis());
               if (var7) {
                  if (this.autoSwapConfig.getValue() == CrystalAura.Swap.SILENT_ALT) {
                     mc.interactionManager
                        .clickSlot(mc.player.playerScreenHandler.syncId, var6 + 36, mc.player.getInventory().getSelectedSlot(), SlotActionType.SWAP, mc.player);
                  } else if (this.autoSwapConfig.getValue() == CrystalAura.Swap.SILENT) {
                     PortSupport.Managers.INVENTORY.syncToClient();
                  }
               }
            }
         } else if (this.isHoldingCrystal()) {
            this.placeInternal(var5, var2);
            this.placePackets.put(var1, System.currentTimeMillis());
         }
      }
   }

   private void placeInternal(BlockHitResult var1, Hand var2) {
      if (var2 != null) {
         PortSupport.Managers.NETWORK.sendSequencedPacket(var2x -> new PlayerInteractBlockC2SPacket(var2, var1, var2x));
         if (this.swingConfig.getValue()) {
            mc.player.swingHand(var2);
         } else {
            PortSupport.Managers.NETWORK.sendPacket(new HandSwingC2SPacket(var2));
         }

         if (this.idPredictConfig.getValue()) {
            boolean var3 = PortSupport.AutoXPModule.getInstance().isEnabled()
               || mc.player.isUsingItem() && mc.player.getStackInHand(mc.player.getActiveHand()).getItem() instanceof ExperienceBottleItem;
            int var4 = (int)(this.predictId + 1L);
            if (var3 || this.attackPackets.containsKey(var4)) {
               return;
            }

            Entity var5 = mc.world.getEntityById(var4);
            if (var5 != null && !(var5 instanceof EndCrystalEntity)) {
               return;
            }

            EndCrystalEntity var6 = new EndCrystalEntity(mc.world, 0.0, 0.0, 0.0);
            var6.setId(var4);
            PlayerInteractEntityC2SPacket var7 = PlayerInteractEntityC2SPacket.attack(var6, false);
            PortSupport.Managers.NETWORK.sendPacket(var7);
            PortSupport.Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            this.attackPackets.put(var4, System.currentTimeMillis());
         }
      }
   }

   private boolean isSilentSwap(CrystalAura.Swap var1) {
      return var1 == CrystalAura.Swap.SILENT || var1 == CrystalAura.Swap.SILENT_ALT;
   }

   private int getCrystalSlot() {
      int var1 = -1;

      for (int var2 = 0; var2 < 9; var2++) {
         ItemStack var3 = mc.player.getInventory().getStack(var2);
         if (var3.getItem() instanceof EndCrystalItem) {
            var1 = var2;
            break;
         }
      }

      return var1;
   }

   private Direction getPlaceDirection(BlockPos var1) {
      int var2 = var1.getX();
      int var3 = var1.getY();
      int var4 = var1.getZ();
      if (this.strictDirectionConfig.getValue()) {
         if (mc.player.getY() >= var1.getY()) {
            return Direction.UP;
         }

         BlockHitResult var5 = mc.world
            .raycast(new RaycastContext(mc.player.getEyePos(), new Vec3d(var2 + 0.5, var3 + 0.5, var4 + 0.5), ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
         if (var5 != null && var5.getType() == Type.BLOCK) {
            return var5.getSide();
         }
      } else {
         if (mc.world.isInBuildLimit(var1)) {
            return Direction.DOWN;
         }

         BlockHitResult var6 = mc.world
            .raycast(new RaycastContext(mc.player.getEyePos(), new Vec3d(var2 + 0.5, var3 + 0.5, var4 + 0.5), ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
         if (var6 != null && var6.getType() == Type.BLOCK) {
            return var6.getSide();
         }
      }

      return Direction.UP;
   }

   private CrystalAura.DamageData<EndCrystalEntity> calculateAttackCrystal(List<Entity> var1) {
      if (var1.isEmpty()) {
         return null;
      } else {
          ArrayList<CrystalAura.DamageData<EndCrystalEntity>> var2 = new ArrayList<>();
         CrystalAura.DamageData var3 = null;

         for (Entity var5 : var1) {
            if (var5 instanceof EndCrystalEntity var7 && var5.isAlive() && !this.stuckCrystals.stream().anyMatch(var1x -> var1x.id() == var5.getId())) {
               Long var8 = this.attackPackets.get(var5.getId());
               boolean var6 = var8 != null && var8 < this.getBreakMs();
               if ((var5.age >= this.ticksExistedConfig.getValue() && !var6 || !this.inhibitConfig.getValue()) && !this.attackRangeCheck(var7)) {
                  double var10 = ExplosionUtil.getDamageTo(
                     mc.player,
                     var5.getEntityPos(),
                     this.blockDestructionConfig.getValue(),
                     this.selfExtrapolateConfig.getValue() ? this.extrapolateTicksConfig.getValue() : 0,
                     false
                  );
                  boolean var12 = this.playerDamageCheck(var10);
                  if (!var12 || this.safetyOverride.getValue()) {
                     for (Entity var14 : var1) {
                        if (var14 != null
                           && var14.isAlive()
                           && var14 != mc.player
                           && this.isValidTarget(var14)
                           && !PortSupport.Managers.SOCIAL.isFriend(var14.getName())
                           && !(var5.squaredDistanceTo(var14) > 144.0)
                           && !(mc.player.squaredDistanceTo(var14) > this.targetRangeConfig.getValue() * this.targetRangeConfig.getValue())) {
                           boolean var22 = false;
                           PlayerEntity var17;
                           if (this.antiSurroundConfig.getValue()
                              && var14 instanceof PlayerEntity
                              && !BlastResistantBlocks.isUnbreakable((var17 = (PlayerEntity)var14).getBlockPos())) {
                               HashSet<BlockPos> var23 = new HashSet<>();
                              BlockPos var24 = PortSupport.AutoMineModule.getInstance().getMiningBlock();
                              if (PortSupport.AutoMineModule.getInstance().isEnabled() && var24 != null) {
                                 var23.add(var24);
                              }

                              if (PortSupport.Managers.BLOCK.getMines(0.75F).contains(var17.getBlockPos().up())) {
                                 var23.add(var17.getBlockPos().up());
                              }

                              for (BlockPos var26 : var23) {
                                 if (PortSupport.SurroundModule.getInstance().getSurroundNoDown(var17).contains(var26)) {
                                    for (Direction var30 : Direction.values()) {
                                       BlockPos var31 = var26.offset(var30);
                                       if (var5.getBlockPos().equals(var31.down())) {
                                          var22 = true;
                                       }
                                    }
                                 }
                              }
                           }

                           double var15;
                           if (!this.checkOverrideSafety(
                              var12,
                              var15 = ExplosionUtil.getDamageTo(
                                 var14,
                                 var5.getEntityPos(),
                                 this.blockDestructionConfig.getValue(),
                                 this.extrapolateTicksConfig.getValue(),
                                 this.assumeArmorConfig.getValue()
                              ),
                              var14
                           )) {
                              CrystalAura.DamageData var32 = new CrystalAura.DamageData<>(var7, var14, var15, var10, var7.getBlockPos().down(), var22);
                              var2.add(var32);
                              if (var3 == null || var15 > var3.getDamage()) {
                                 var3 = var32;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (var3 != null && !this.targetDamageCheck(var3)) {
            return var3;
         } else {
            return this.antiSurroundConfig.getValue()
               ? var2.stream()
                   .filter(data -> data.isAntiSurround())
                   .min(Comparator.comparingDouble((CrystalAura.DamageData<EndCrystalEntity> data) -> mc.player.squaredDistanceTo(data.getBlockPos().toCenterPos())))
                  .orElse(null)
               : null;
         }
      }
   }

   private boolean attackRangeCheck(EndCrystalEntity var1) {
      return this.attackRangeCheck(var1.getEntityPos());
   }

   private boolean attackRangeCheck(Vec3d var1) {
      double var2 = this.breakRangeConfig.getValue().floatValue();
      double var4 = this.breakWallRangeConfig.getValue().floatValue();
      Vec3d var6 = mc.player.getEyePos();
      double var7 = var6.squaredDistanceTo(var1);
      if (var7 > var2 * var2) {
         return true;
      } else {
         double var9 = Math.abs(var1.getY() - mc.player.getY());
         if (var9 > this.maxYOffsetConfig.getValue().floatValue()) {
            return true;
         } else {
            BlockHitResult var11 = mc.world.raycast(new RaycastContext(var6, var1, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
            return var11.getType() != Type.MISS && var7 > var4 * var4;
         }
      }
   }

   private CrystalAura.DamageData<BlockPos> calculatePlaceCrystal(List<BlockPos> var1, List<Entity> var2) {
      if (!var1.isEmpty() && !var2.isEmpty()) {
         ArrayList<CrystalAura.DamageData<BlockPos>> var3 = new ArrayList<>();
         CrystalAura.DamageData var4 = null;

         for (BlockPos var6 : var1) {
            if (this.canUseCrystalOnBlock(var6) && !this.placeRangeCheck(var6) && !this.intersectingAntiStuckCheck(var6)) {
               double var7 = ExplosionUtil.getDamageTo(
                  mc.player,
                  this.crystalDamageVec(var6),
                  this.blockDestructionConfig.getValue(),
                  this.selfExtrapolateConfig.getValue() ? this.extrapolateTicksConfig.getValue() : 0,
                  false
               );
               boolean var9 = this.playerDamageCheck(var7);
               if (!var9 || this.safetyOverride.getValue()) {
                  for (Entity var11 : var2) {
                     if (var11 != null
                        && var11.isAlive()
                        && var11 != mc.player
                        && this.isValidTarget(var11)
                        && !PortSupport.Managers.SOCIAL.isFriend(var11.getName())
                        && !(var6.getSquaredDistance(var11.getEntityPos()) > 144.0)
                        && !(mc.player.squaredDistanceTo(var11) > this.targetRangeConfig.getValue() * this.targetRangeConfig.getValue())) {
                        boolean var19 = false;
                        PlayerEntity var14;
                        if (this.antiSurroundConfig.getValue()
                           && var11 instanceof PlayerEntity
                           && !BlastResistantBlocks.isUnbreakable((var14 = (PlayerEntity)var11).getBlockPos())) {
                           HashSet<BlockPos> var20 = new HashSet<>();
                           BlockPos var21 = PortSupport.AutoMineModule.getInstance().getMiningBlock();
                           if (PortSupport.AutoMineModule.getInstance().isEnabled() && var21 != null) {
                              var20.add(var21);
                           }

                           if (PortSupport.Managers.BLOCK.getMines(0.75F).contains(var14.getBlockPos().up())) {
                              var20.add(var14.getBlockPos().up());
                           }

                           for (BlockPos var23 : var20) {
                              if (PortSupport.SurroundModule.getInstance().getSurroundNoDown(var14).contains(var23)) {
                                 for (Direction var27 : Direction.values()) {
                                    BlockPos var28 = var23.offset(var27);
                                    if (var6.equals(var28.down())) {
                                       var19 = true;
                                    }
                                 }
                              }
                           }
                        }

                        double var12;
                        if (!this.checkOverrideSafety(
                           var9,
                           var12 = ExplosionUtil.getDamageTo(
                              var11,
                              this.crystalDamageVec(var6),
                              this.blockDestructionConfig.getValue(),
                              this.extrapolateTicksConfig.getValue(),
                              this.assumeArmorConfig.getValue()
                           ),
                           var11
                        )) {
                           CrystalAura.DamageData var29 = new CrystalAura.DamageData(var6, var11, var12, var7, var19);
                           var3.add(var29);
                           if (var4 == null || var12 > var4.getDamage()) {
                              var4 = var29;
                           }
                        }
                     }
                  }
               }
            }
         }

         if (var4 != null && !this.targetDamageCheck(var4)) {
            return var4;
         } else {
            return this.antiSurroundConfig.getValue()
               ? var3.stream()
                  .filter(data -> data.isAntiSurround())
                  .min(Comparator.comparingDouble((CrystalAura.DamageData<BlockPos> data) -> mc.player.squaredDistanceTo(data.getBlockPos().toCenterPos())))
                  .orElse(null)
               : null;
         }
      } else {
         return null;
      }
   }

   private boolean placeRangeCheck(BlockPos var1) {
      double var4 = this.placeRangeConfig.getValue().floatValue();
      double var6 = this.placeWallRangeConfig.getValue().floatValue();
      Vec3d var8 = this.placeRangeEyeConfig.getValue() ? mc.player.getEyePos() : mc.player.getEntityPos();
      double var2 = this.placeRangeCenterConfig.getValue() ? var8.squaredDistanceTo(var1.toCenterPos()) : var1.getSquaredDistance(var8.x, var8.y, var8.z);
      if (var2 > var4 * var4) {
         return true;
      } else {
         Vec3d var11 = Vec3d.of(var1).add(0.5, 2.70000004768372, 0.5);
         BlockHitResult var12 = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), var11, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
         float var13 = this.breakRangeConfig.getValue() * this.breakRangeConfig.getValue();
         if (var12 != null && var12.getType() == Type.BLOCK && !var12.getBlockPos().equals(var1)) {
            var13 = this.breakWallRangeConfig.getValue() * this.breakWallRangeConfig.getValue();
            if (!this.raytraceConfig.getValue() || var2 > var6 * var6) {
               return true;
            }
         }

         return this.breakValidConfig.getValue() && var2 > var13;
      }
   }

   public void placeCrystalForTarget(PlayerEntity var1, BlockPos var2) {
      if (var1 != null && !var1.isDead() && !this.placeRangeCheck(var2) && this.canUseCrystalOnBlock(var2)) {
         double var3 = ExplosionUtil.getDamageTo(
            mc.player,
            this.crystalDamageVec(var2),
            this.blockDestructionConfig.getValue(),
            Set.of(var2),
            this.selfExtrapolateConfig.getValue() ? this.extrapolateTicksConfig.getValue() : 0,
            false
         );
         if (!this.playerDamageCheck(var3)) {
            double var5 = ExplosionUtil.getDamageTo(
               var1,
               this.crystalDamageVec(var2),
               this.blockDestructionConfig.getValue(),
               Set.of(var2),
               this.extrapolateTicksConfig.getValue(),
               this.assumeArmorConfig.getValue()
            );
            if ((!(var5 < this.minDamageConfig.getValue().floatValue()) || this.isCrystalLethalTo(var5, var1))
               && (this.placeCrystal == null || !(this.placeCrystal.getDamage() >= var5))) {
               float[] var7 = RotationUtil.getRotationsTo(mc.player.getEyePos(), var2.toCenterPos());
               this.setRotation(var7[0], var7[1]);
               this.placeCrystal(var2, Hand.MAIN_HAND, false);
               this.fadeList.put(var2, new Animation(true, this.fadeTimeConfig.getValue().intValue()));
            }
         }
      }
   }

   private boolean checkOverrideSafety(boolean var1, double var2, Entity var4) {
      return this.safetyOverride.getValue() && var1 && var2 < EntityUtil.getHealth(var4) + 0.5;
   }

   private boolean targetDamageCheck(CrystalAura.DamageData<?> var1) {
      double var3 = this.minDamageConfig.getValue().floatValue();
      Entity var5 = var1.getAttackTarget();
      LivingEntity var2;
      if (var5 instanceof LivingEntity && this.isCrystalLethalTo(var1, var2 = (LivingEntity)var5)) {
         var3 = 2.0;
      }

      return var1.getDamage() < var3;
   }

   private boolean playerDamageCheck(double var1) {
      if (!mc.player.isCreative()) {
         float var3 = mc.player.getHealth() + mc.player.getAbsorptionAmount();
         return this.safetyConfig.getValue() && var1 >= var3 + 0.5F ? true : var1 > this.maxLocalDamageConfig.getValue().floatValue();
      } else {
         return false;
      }
   }

   private boolean isFeetSurrounded(LivingEntity var1) {
      BlockPos var2 = var1.getBlockPos();
      if (!mc.world.getBlockState(var2).isReplaceable()) {
         return true;
      } else {
         for (Direction var6 : Direction.values()) {
            if (var6.getAxis().isHorizontal() && mc.world.getBlockState(var2.offset(var6)).isReplaceable()) {
               return false;
            }
         }

         return true;
      }
   }

   private boolean checkAntiTotem(double var1, LivingEntity var3) {
      long var4;
      PlayerEntity var6;
      float var7;
      return var3 instanceof PlayerEntity
            && (var7 = EntityUtil.getHealth(var6 = (PlayerEntity)var3)) <= 2.0F
            && var7 - var1 < 0.5
            && (var4 = PortSupport.Managers.TOTEM.getLastPopTime(var6)) != -1L
         ? System.currentTimeMillis() - var4 <= 500L
         : false;
   }

   private boolean isCrystalLethalTo(CrystalAura.DamageData<?> var1, LivingEntity var2) {
      return this.isCrystalLethalTo(var1.getDamage(), var2);
   }

   private boolean isCrystalLethalTo(double var1, LivingEntity var3) {
      if (this.lethalDamageConfig.getValue() && this.lastAttackTimer.passed(500)) {
         return true;
      } else if (this.antiTotemConfig.getValue() && this.checkAntiTotem(var1, var3)) {
         return true;
      } else {
         float var4 = var3.getHealth() + var3.getAbsorptionAmount();
         if (var1 * (1.0F + this.lethalMultiplier.getValue()) >= var4 + 0.5F) {
            return true;
         } else {
            if (this.armorBreakerConfig.getValue()) {
               for (ItemStack var6 : PortSupport.armor(var3)) {
                  int var7 = var6.getDamage();
                  int var8 = var6.getMaxDamage();
                  if (var8 > 0 && !var6.isEmpty()) {
                     float var9 = (float)(var8 - var7) / var8 * 100.0F;
                     if (var9 < this.armorScaleConfig.getValue()) {
                        return true;
                     }
                  }
               }
            }

            if (this.shulkersConfig.getValue() && var3 instanceof PlayerEntity) {
               for (BlockPos var11 : this.getSphere(3.0, var3.getEntityPos())) {
                  BlockState var12 = mc.world.getBlockState(var11);
                  if (var12.getBlock() instanceof ShulkerBoxBlock) {
                     return true;
                  }
               }
            }

            return false;
         }
      }
   }

   private boolean attackCheckPre(Hand var1) {
      if (!this.lastSwapTimer.passed(this.swapDelayConfig.getValue() * 25.0F)) {
         return true;
      } else {
         return var1 == Hand.MAIN_HAND ? this.checkCanUseCrystal() : false;
      }
   }

   private boolean checkCanUseCrystal() {
      return !this.multitaskConfig.getValue() && this.checkMultitask() || !this.whileMiningConfig.getValue() && mc.interactionManager.isBreakingBlock();
   }

   private boolean isHoldingCrystal() {
      return this.checkCanUseCrystal()
            || this.autoSwapConfig.getValue() != CrystalAura.Swap.SILENT && this.autoSwapConfig.getValue() != CrystalAura.Swap.SILENT_ALT
         ? this.getCrystalHand() != null
         : true;
   }

   private Vec3d crystalDamageVec(BlockPos var1) {
      return Vec3d.of(var1).add(0.5, 1.0, 0.5);
   }

   private boolean isValidTarget(Entity var1) {
      return var1 instanceof PlayerEntity && this.playersConfig.getValue()
         || EntityUtil.isMonster(var1) && this.monstersConfig.getValue()
         || EntityUtil.isNeutral(var1) && this.neutralsConfig.getValue()
         || EntityUtil.isPassive(var1) && this.animalsConfig.getValue();
   }

   public boolean canUseCrystalOnBlock(BlockPos var1) {
      BlockState var2 = mc.world.getBlockState(var1);
      return !var2.isOf(Blocks.OBSIDIAN) && !var2.isOf(Blocks.BEDROCK) ? false : this.isCrystalHitboxClear(var1);
   }

   public boolean isCrystalHitboxClear(BlockPos var1) {
      BlockPos var2 = var1.up();
      BlockState var3 = mc.world.getBlockState(var2);
      if (this.placementsConfig.getValue() == CrystalAura.Placements.PROTOCOL && !mc.world.isAir(var2.up())) {
         return false;
      } else if (!mc.world.isAir(var2) && !var3.isOf(Blocks.FIRE)) {
         return false;
      } else {
         Box var4 = PortSupport.Managers.NETWORK.isCrystalPvpCC() ? HALF_CRYSTAL_BB : FULL_CRYSTAL_BB;
         double var5 = var2.getX();
         double var7 = var2.getY();
         double var9 = var2.getZ();
          List<Entity> var11 = this.getEntitiesBlockingCrystal(new Box(var5, var7, var9, var5 + var4.maxX, var7 + var4.maxY, var9 + var4.maxZ));
         return var11.isEmpty();
      }
   }

   private List<Entity> getEntitiesBlockingCrystal(Box var1) {
       CopyOnWriteArrayList<Entity> var2 = new CopyOnWriteArrayList<>(mc.world.getOtherEntities(null, var1));

      for (Entity var4 : var2) {
         if (var4 != null
            && var4.isAlive()
            && !(var4 instanceof ExperienceOrbEntity)
            && (this.forcePlaceConfig.getValue() == CrystalAura.ForcePlace.NONE || !(var4 instanceof ItemEntity) || var4.age > 10)) {
            EndCrystalEntity var5;
            if (var4 instanceof EndCrystalEntity && (var5 = (EndCrystalEntity)var4).getBoundingBox().intersects(var1)) {
               Integer var6 = this.antiStuckCrystals.get(var5.getId());
               if (this.attackRangeCheck(var5) || var6 != null && !(var6.intValue() <= this.attackLimitConfig.getValue() * 10.0F)) {
                  double var7 = mc.player.squaredDistanceTo(var5);
                  this.stuckCrystals.add(new CrystalAura.AntiStuckData(var5.getId(), var5.getBlockPos(), var5.getEntityPos(), var7));
               } else {
                  var2.remove(var4);
               }
            }
         } else {
            var2.remove(var4);
         }
      }

      return var2;
   }

   private boolean intersectingAntiStuckCheck(BlockPos var1) {
      return this.stuckCrystals.isEmpty() ? false : this.stuckCrystals.stream().anyMatch(var1x -> var1x.blockPos().equals(var1.up()));
   }

   private EndCrystalEntity intersectingCrystalCheck(BlockPos var1) {
      return mc.world
         .getOtherEntities(null, new Box(var1))
         .stream()
         .filter(var0 -> var0 instanceof EndCrystalEntity)
         .map(var0 -> (EndCrystalEntity)var0)
         .min(Comparator.comparingDouble(var0 -> mc.player.distanceTo(var0)))
         .orElse(null);
   }

   private List<BlockPos> getSphere(Vec3d var1) {
      double var2 = Math.ceil(this.placeRangeConfig.getValue().floatValue());
      return this.getSphere(var2, var1);
   }

   private List<BlockPos> getSphere(double var1, Vec3d var3) {
      ArrayList var4 = new ArrayList();

      for (double var5 = -var1; var5 <= var1; var5++) {
         for (double var7 = -var1; var7 <= var1; var7++) {
            for (double var9 = -var1; var9 <= var1; var9++) {
               Vec3i var11 = new Vec3i((int)(var3.getX() + var5), (int)(var3.getY() + var7), (int)(var3.getZ() + var9));
               BlockPos var12 = new BlockPos(var11);
               var4.add(var12);
            }
         }
      }

      return var4;
   }

   private boolean canHoldCrystal() {
      return this.isHoldingCrystal() || this.autoSwapConfig.getValue() != CrystalAura.Swap.OFF && this.getCrystalSlot() != -1;
   }

   private Hand getCrystalHand() {
      ItemStack var1 = mc.player.getOffHandStack();
      ItemStack var2 = mc.player.getMainHandStack();
      if (var1.getItem() instanceof EndCrystalItem) {
         return Hand.OFF_HAND;
      } else {
         return var2.getItem() instanceof EndCrystalItem ? Hand.MAIN_HAND : null;
      }
   }

   public float getBreakDelay() {
      return 1000.0F - this.breakSpeedConfig.getValue() * 50.0F;
   }

   public void setStage(String var1) {
   }

   public int getBreakMs() {
      if (this.attackLatency.isEmpty()) {
         return 0;
      } else {
         float var1 = 0.0F;
         ArrayList var2 = Lists.newArrayList(this.attackLatency);
         if (!var2.isEmpty()) {
            Iterator var3 = var2.iterator();

            while (var3.hasNext()) {
               float var4 = (float)((Long)var3.next()).longValue();
               var1 += var4;
            }

            var1 /= var2.size();
         }

         return (int)var1;
      }
   }

   public boolean shouldPreForcePlace() {
      return this.forcePlaceConfig.getValue() == CrystalAura.ForcePlace.PRE;
   }

   public float getPlaceRange() {
      return this.placeRangeConfig.getValue();
   }

   private record AntiStuckData(int id, BlockPos blockPos, Vec3d pos, double stuckDist) {
   }

   private class AttackCrystalTask implements Callable<CrystalAura.DamageData<EndCrystalEntity>> {
      private final List<Entity> threadSafeEntities;

      public AttackCrystalTask(List<Entity> nullx) {
         this.threadSafeEntities = nullx;
      }

      public CrystalAura.DamageData<EndCrystalEntity> call() throws Exception {
         return CrystalAura.this.calculateAttackCrystal(this.threadSafeEntities);
      }
   }

   private static class DamageData<T> {
      private final List<String> tags = new ArrayList<>();
      private T damageData;
      private Entity attackTarget;
      private BlockPos blockPos;
      private double damage;
      private double selfDamage;
      private boolean antiSurround;

      public DamageData() {
      }

      public DamageData(BlockPos var1, Entity var2, double var3, double var5, boolean var7) {
         this.damageData = (T)var1;
         this.attackTarget = var2;
         this.damage = var3;
         this.selfDamage = var5;
         this.blockPos = var1;
         this.antiSurround = var7;
      }

      public DamageData(T var1, Entity var2, double var3, double var5, BlockPos var7, boolean var8) {
         this.damageData = (T)var1;
         this.attackTarget = var2;
         this.damage = var3;
         this.selfDamage = var5;
         this.blockPos = var7;
         this.antiSurround = var8;
      }

      public void setDamageData(T var1, Entity var2, double var3, double var5) {
         this.damageData = (T)var1;
         this.attackTarget = var2;
         this.damage = var3;
         this.selfDamage = var5;
      }

      public T getDamageData() {
         return this.damageData;
      }

      public Entity getAttackTarget() {
         return this.attackTarget;
      }

      public double getDamage() {
         return this.damage;
      }

      public double getSelfDamage() {
         return this.selfDamage;
      }

      public BlockPos getBlockPos() {
         return this.blockPos;
      }

      public boolean isAntiSurround() {
         return this.antiSurround;
      }
   }

   public static enum ForcePlace {
      PRE,
      POST,
      NONE;
   }

   private class PlaceCrystalTask implements Callable<CrystalAura.DamageData<BlockPos>> {
      private final List<BlockPos> threadSafeBlocks;
      private final List<Entity> threadSafeEntities;

      public PlaceCrystalTask(List<BlockPos> nullx, List<Entity> nullxx) {
         this.threadSafeBlocks = nullx;
         this.threadSafeEntities = nullxx;
      }

      public CrystalAura.DamageData<BlockPos> call() throws Exception {
         return CrystalAura.this.calculatePlaceCrystal(this.threadSafeBlocks, this.threadSafeEntities);
      }
   }

   public static enum Placements {
      NATIVE,
      PROTOCOL;
   }

   public static enum Rotate {
      FULL,
      SEMI,
      OFF;
   }

   public static enum Sequential {
      NORMAL,
      STRICT,
      NONE;
   }

   public static enum Swap {
      NORMAL,
      SILENT,
      SILENT_ALT,
      OFF;
   }
}
