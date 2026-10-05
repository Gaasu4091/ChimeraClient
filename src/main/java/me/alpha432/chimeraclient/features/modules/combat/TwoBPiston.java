package me.alpha432.chimeraclient.features.modules.combat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import me.alpha432.chimeraclient.event.impl.network.PacketEvent;
import me.alpha432.chimeraclient.event.impl.render.Render3DEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.mio.MioRender;
import me.alpha432.chimeraclient.shoreline.PortSupport;
import me.alpha432.chimeraclient.shoreline.mixin.PendingUpdatesAccess;
import me.alpha432.chimeraclient.shoreline.piston.AlienPlaceAnimation;
import me.alpha432.chimeraclient.shoreline.piston.PistonCycle;
import me.alpha432.chimeraclient.shoreline.piston.PistonInteraction;
import me.alpha432.chimeraclient.shoreline.piston.PistonMotion;
import me.alpha432.chimeraclient.shoreline.piston.PistonPatterns;
import me.alpha432.chimeraclient.shoreline.piston.PistonTools;
import me.alpha432.chimeraclient.shoreline.util.Globals;
import me.alpha432.chimeraclient.shoreline.util.player.RotationUtil;
import me.alpha432.chimeraclient.shoreline.util.world.ExplosionUtil;
import me.alpha432.chimeraclient.util.player.ChatUtil;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

public final class TwoBPiston extends PortSupport.CombatModule {
   private static TwoBPiston INSTANCE;
   private static final boolean CHIMERA_PATTERNS = false;
   private static final boolean MERGED_PATTERNS = true;
   private static final Direction[] HORIZONTAL = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
   private static final int[] TARGET_HEIGHTS = new int[]{1, 0, 2, -1};
   private static final int[] MINE_HEIGHTS = new int[]{0, 1, -1, 2};
   Setting<Boolean> rotateConfig = this.register(new PortSupport.BooleanConfig("Rotate", "Silently rotate server-side before piston placement", true));
   Setting<CrystalAura.Rotate> yawStepConfig = this.register(
      new PortSupport.EnumConfig<>(
         "YawStep", "AutoCrystal-style stepped silent rotation", CrystalAura.Rotate.FULL, CrystalAura.Rotate.values(), () -> this.rotateConfig.getValue()
      )
   );
   Setting<Integer> yawStepLimitConfig = this.register(
      new PortSupport.NumberConfig<>(
         "YawStep-Limit",
         "Maximum silent yaw rotation per tick",
         1,
         180,
         180,
         PortSupport.NumberDisplay.DEGREES,
         () -> this.rotateConfig.getValue() && this.yawStepConfig.getValue() != CrystalAura.Rotate.OFF
      )
   );
   Setting<Boolean> pistonPacketConfig = this.register(new PortSupport.BooleanConfig("PistonPacket", "Use packet placement for pistons", false));
   Setting<Boolean> airPlaceConfig = this.register(
      new PortSupport.BooleanConfig("AirPlace", "Place the piston without requiring a neighboring support block", true)
   );
   Setting<Boolean> basePlaceConfig = this.register(new PortSupport.BooleanConfig("BasePlace", "Air-place an obsidian crystal base when one is missing", true));
   Setting<Boolean> throughWallsConfig = this.register(new PortSupport.BooleanConfig("ThroughWalls", "Allow direct placement and attacks through walls", true));
   Setting<Boolean> avoidSelfPathConfig = this.register(
      new PortSupport.BooleanConfig("AvoidSelfPath", "Reject plans whose piston line passes through you", true)
   );
   Setting<Boolean> lowCeilingPatternConfig = this.register(
      new PortSupport.BooleanConfig("CrawlOrLowPattern", "Air-place a head-level crystal setup when the enemy is crawling or has a solid block overhead", true)
   );
   Setting<Boolean> minePatternConfig = this.register(
      new PortSupport.BooleanConfig("MinePattern", "Push crystals into blocks an enemy is repeatedly mining", true)
   );
   Setting<Integer> mineMemoryConfig = this.register(
      new PortSupport.NumberConfig<>("MineMemory", "Milliseconds to remember an enemy mining position", 100, 900, 2500)
   );
   Setting<Boolean> noEatingConfig = this.register(new PortSupport.BooleanConfig("NoEating", "Pause while using an item", true));
   Setting<Boolean> eatingBreakConfig = this.register(new PortSupport.BooleanConfig("EatingBreak", "Allow crystal attacks while eating", false));
   Setting<Float> placeRangeConfig = this.register(new PortSupport.NumberConfig<>("PlaceRange", "Maximum block placement range", 1.0F, 5.0F, 8.0F));
   Setting<Float> rangeConfig = this.register(new PortSupport.NumberConfig<>("Range", "Maximum target range", 1.0F, 4.0F, 8.0F));
   Setting<Boolean> fireConfig = this.register(new PortSupport.BooleanConfig("Fire", "Attempt the Alien fire stage when flint and steel is available", true));
   Setting<Boolean> switchPosConfig = this.register(new PortSupport.BooleanConfig("Switch", "Keep the current valid piston position", false));
   Setting<Boolean> onlyGroundConfig = this.register(new PortSupport.BooleanConfig("SelfGround", "Pause while airborne", true));
   Setting<Boolean> onlyStaticConfig = this.register(new PortSupport.BooleanConfig("MovingPause", "Pause while moving", true));
   Setting<Boolean> maxSpeedConfig = this.register(
      new PortSupport.BooleanConfig("MaxSpeed", "Use confirmation-gated actions without an additional fixed placement delay", true)
   );
   Setting<Integer> cycleDelayConfig = this.register(
      new PortSupport.NumberConfig<>("CycleDelay", "Extra ticks between attacks when MaxSpeed is disabled", 0, 0, 20)
   );
   Setting<Integer> updateDelayConfig = this.register(
      new PortSupport.NumberConfig<>("PlaceDelay", "Milliseconds between stage actions when MaxSpeed is disabled", 0, 0, 500)
   );
   Setting<Integer> posUpdateDelayConfig = this.register(new PortSupport.NumberConfig<>("PosUpdateDelay", "Milliseconds between position scans", 0, 500, 1000));
   Setting<Integer> stageConfig = this.register(new PortSupport.NumberConfig<>("Stage", "Number of stages in one cycle", 1, 4, 10));
   Setting<Integer> pistonStageConfig = this.register(new PortSupport.NumberConfig<>("PistonStage", "First piston stage", 1, 1, 10));
   Setting<Integer> pistonMaxStageConfig = this.register(new PortSupport.NumberConfig<>("PistonMaxStage", "Last piston stage", 1, 1, 10));
   Setting<Integer> powerStageConfig = this.register(new PortSupport.NumberConfig<>("PowerStage", "First redstone stage", 1, 3, 10));
   Setting<Integer> powerMaxStageConfig = this.register(new PortSupport.NumberConfig<>("PowerMaxStage", "Last redstone stage", 1, 3, 10));
   Setting<Integer> crystalStageConfig = this.register(new PortSupport.NumberConfig<>("CrystalStage", "First crystal stage", 1, 4, 10));
   Setting<Integer> crystalMaxStageConfig = this.register(new PortSupport.NumberConfig<>("CrystalMaxStage", "Last crystal stage", 1, 4, 10));
   Setting<Integer> fireStageConfig = this.register(new PortSupport.NumberConfig<>("FireStage", "First fire stage", 1, 2, 10));
   Setting<Integer> fireMaxStageConfig = this.register(new PortSupport.NumberConfig<>("FireMaxStage", "Last fire stage", 1, 2, 10));
   Setting<Boolean> confirmPushConfig = this.register(
      new PortSupport.BooleanConfig("ConfirmPush", "Only break after the placed crystal moves toward the target", true)
   );
   Setting<Boolean> instantBreakConfig = this.register(
      new PortSupport.BooleanConfig("InstantBreak", "Attack on the first tick where a pushed crystal is confirmed", true)
   );
   Setting<Integer> breakDelayConfig = this.register(
      new PortSupport.NumberConfig<>("BreakDelay", "Minimum milliseconds after crystal placement before breaking", 0, 250, 1500)
   );
   Setting<Integer> stuckTimeoutConfig = this.register(
      new PortSupport.NumberConfig<>("StuckTimeout", "Automatically reset a stalled sequence after this many milliseconds", 100, 650, 3000)
   );
   Setting<Integer> restartDelayConfig = this.register(new PortSupport.NumberConfig<>("RestartDelay", "Delay before retrying a stalled sequence", 0, 50, 1000));
   Setting<Boolean> powerCleanupConfig = this.register(
      new PortSupport.BooleanConfig("PowerCleanup", "Remove this module's redstone block after attacking", true)
   );
   Setting<Boolean> twoBTwoTModeConfig = this.register(
      new PortSupport.BooleanConfig("2b2t-Mode", "Leave the powered setup and immediately find a new piston route", false)
   );
   Setting<Integer> routeMemoryConfig = this.register(
      new PortSupport.NumberConfig<>(
         "2b2t-RouteMemory", "Milliseconds before a used piston route can be selected again", 1000, 10000, 30000, () -> this.twoBTwoTModeConfig.getValue()
      )
   );
   Setting<Boolean> inventoryConfig = this.register(new PortSupport.BooleanConfig("InventorySwap", "Use Shoreline silent hotbar swaps", true));
   Setting<Boolean> debugConfig = this.register(new PortSupport.BooleanConfig("Debug", "Print selected plan details", false));
   Setting<Boolean> wallPatternsConfig = this.register(
      new PortSupport.BooleanConfig("WallPatterns", "Search collision-tested embedded and corner crystal routes", true)
   );
   Setting<Boolean> verticalPatternsConfig = this.register(
      new PortSupport.BooleanConfig("VerticalPatterns", "Search feasible upward and downward piston orientations", true, () -> true)
   );
   Setting<Boolean> blockPushPatternsConfig = this.register(
      new PortSupport.BooleanConfig("BlockPushPatterns", "Search existing movable block chains with vanilla piston push limits", true, () -> true)
   );
   Setting<Boolean> renderConfig = this.register(new Setting<>("Render", true));
   Setting<Integer> fadeTimeConfig = this.register(new Setting<>("FadeTime", 500, 0, 3000, var1 -> this.renderConfig.getValue()));
   Setting<Integer> renderTimeoutConfig = this.register(new Setting<>("TimeOut", 500, 0, 3000, var1 -> this.renderConfig.getValue()));
   Setting<AlienPlaceAnimation.Mode> renderModeConfig = this.register(
      new Setting<>("RenderMode", AlienPlaceAnimation.Mode.All, var1 -> this.renderConfig.getValue())
   );
   Setting<AlienPlaceAnimation.Ease> renderEaseConfig = this.register(
      new Setting<>("Ease", AlienPlaceAnimation.Ease.CubicInOut, var1 -> this.renderConfig.getValue())
   );
   Setting<Boolean> renderNoFailConfig = this.register(new Setting<>("NoFail", false, var1 -> this.renderConfig.getValue()));
   Setting<Boolean> renderBoxConfig = this.register(new Setting<>("Box", true, var1 -> this.renderConfig.getValue()));
   Setting<Color> renderBoxColorConfig = this.register(
      new Setting<>("BoxColor", new Color(255, 255, 255, 255), var1 -> this.renderConfig.getValue() && this.renderBoxConfig.getValue())
   );
   Setting<Boolean> renderFillConfig = this.register(new Setting<>("Fill", true, var1 -> this.renderConfig.getValue()));
   Setting<Color> renderFillColorConfig = this.register(
      new Setting<>("FillColor", new Color(255, 255, 255, 100), var1 -> this.renderConfig.getValue() && this.renderFillConfig.getValue())
   );
   Setting<Boolean> renderTryBoxConfig = this.register(
      new Setting<>("TryPlaceBox", true, var1 -> this.renderConfig.getValue() && !this.renderNoFailConfig.getValue())
   );
   Setting<Color> renderTryBoxColorConfig = this.register(
      new Setting<>(
         "TryPlaceBoxColor",
         new Color(178, 178, 178, 255),
         var1 -> this.renderConfig.getValue() && !this.renderNoFailConfig.getValue() && this.renderTryBoxConfig.getValue()
      )
   );
   Setting<Boolean> renderTryFillConfig = this.register(
      new Setting<>("TryPlaceFill", true, var1 -> this.renderConfig.getValue() && !this.renderNoFailConfig.getValue())
   );
   Setting<Color> renderTryFillColorConfig = this.register(
      new Setting<>(
         "TryPlaceFillColor",
         new Color(255, 119, 119, 157),
         var1 -> this.renderConfig.getValue() && !this.renderNoFailConfig.getValue() && this.renderTryFillConfig.getValue()
      )
   );
   private int attackedCrystalId = -1;
   private long attackedAt;
   private long lastAttackTick = -1L;
   private long cleanupTick = -1L;
   private Vec3d scanTarget;
   private long lastAction;
   private long moduleTick;
   private long nextCycleTick;
   private long lastScan;
   private BlockPos rotatingPiston;
   private Direction rotatingLookDirection;
   private int sequencePhase;
   private long crystalPlacedAt;
   private long lastProgressAt;
   private long lastResetAt;
   private long cleanupSentAt;
   private int cleanupAttempts;
   private long cleanupLastUpdate;
   private float cleanupDamage;
   private BlockPos cleanupBlock;
   private TwoBPiston.Plan routeCleanupPlan;
   private int routeCleanupStage;
   private long lastRouteCleanupAt;
   private long lastDebugAt;
   private TwoBPiston.Plan plan;
   private final PistonCycle cycle = new PistonCycle();
   private Object cycleWorld;
   private Object renderWorld;
   private final Map<BlockPos, AlienPlaceAnimation> placementRender = new LinkedHashMap<>();
   private final Map<BlockPos, BlockState> acknowledgedBlocks = new HashMap<>();
   private final Map<BlockPos, Long> acknowledgedAt = new HashMap<>();
   private long lastWorldTimePacket;
   private float observedTps = 20.0F;
   private final Map<TwoBPiston.MiningKey, TwoBPiston.MiningInfo> mining = new ConcurrentHashMap<>();
   private final Map<BlockPos, Long> usedRoutes = new ConcurrentHashMap<>();
   private final Map<BlockPos, TwoBPiston.Plan> usedPlans = new ConcurrentHashMap<>();

   public static TwoBPiston getInstance() {
      return INSTANCE;
   }

   public TwoBPiston() {
      super("2bPiston", "Feedback-driven crystal-before-power cycles with reusable piston routes", Module.Category.COMBAT, 950);
      INSTANCE = this;
   }

   @Override
   public void onDisable() {
      this.resetCleanupMining();
      this.cycle.reset();
      this.acknowledgedBlocks.clear();
      this.acknowledgedAt.clear();
      this.cycleWorld = null;
      this.renderWorld = null;
      this.placementRender.clear();
      this.lastAction = this.lastScan = this.attackedAt = 0L;
      this.lastWorldTimePacket = 0L;
      this.observedTps = 20.0F;
      this.attackedCrystalId = -1;
      this.cleanupTick = -1L;
      this.lastAttackTick = -1L;
      this.scanTarget = null;
      this.plan = null;
      this.sequencePhase = 0;
      this.moduleTick = 0L;
      this.nextCycleTick = 0L;
      this.crystalPlacedAt = 0L;
      this.lastProgressAt = 0L;
      this.lastResetAt = 0L;
      this.cleanupSentAt = 0L;
      this.cleanupAttempts = 0;
      this.cleanupLastUpdate = 0L;
      this.cleanupDamage = 0.0F;
      this.cleanupBlock = null;
      this.lastRouteCleanupAt = 0L;
      this.lastDebugAt = 0L;
      if (this.routeCleanupPlan != null) {
         this.usedPlans.remove(this.routeCleanupPlan.piston);
         this.routeCleanupPlan = null;
         this.routeCleanupStage = 0;
         this.resetCleanupMining();
      }

      this.rotatingPiston = null;
      this.rotatingLookDirection = null;
      this.mining.clear();
      this.usedRoutes.clear();
      this.usedPlans.clear();
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
                     this.handleServerPacket(var5);
                  }
               } else {
                  this.handleServerPacket(var3);
               }
            }
         });
      }
   }

   private void handleServerPacket(Packet<?> var1) {
      if (var1 instanceof WorldTimeUpdateS2CPacket) {
         long var2 = System.currentTimeMillis();
         long var4 = var2 - this.lastWorldTimePacket;
         if (this.lastWorldTimePacket != 0L && var4 > 0L) {
            this.observedTps = this.observedTps * 0.5F + Math.min(20.0F, 20000.0F / (float)var4) * 0.5F;
         }

         this.lastWorldTimePacket = var2;
      }

      if (var1 instanceof BlockUpdateS2CPacket var6) {
         this.recordBlock(var6.getPos(), var6.getState());
      } else if (var1 instanceof ChunkDeltaUpdateS2CPacket var3) {
         var3.visitUpdates(this::recordBlock);
      }

      if (var1 instanceof BlockBreakingProgressS2CPacket var7) {
         long var8 = System.currentTimeMillis();
         TwoBPiston.MiningKey var5 = new TwoBPiston.MiningKey(var7.getEntityId(), var7.getPos());
         this.mining.compute(var5, (var4x, var5x) -> {
            int var6x = var5x != null && var8 - var5x.time <= this.mineMemoryConfig.getValue().intValue() ? var5x.repeats + 1 : 1;
            return new TwoBPiston.MiningInfo(var7.getPos(), var8, var6x, var7.getProgress() == 255 || var5x != null && var5x.instant);
         });
         this.mining.entrySet().removeIf(var3x -> var8 - var3x.getValue().time > this.mineMemoryConfig.getValue().intValue());
      }
   }

   private void recordBlock(BlockPos var1, BlockState var2) {
      if (this.plan != null && var1.equals(this.plan.piston)) {
         this.acknowledgedBlocks.put(var1.toImmutable(), var2);
         this.acknowledgedAt.put(var1.toImmutable(), System.currentTimeMillis());
      }
   }

   @Override
   public String getDisplayInfo() {
      return this.plan == null ? "Scan" : this.cycle.phase().name();
   }

   @Override
   public void onTick() {
   }

   private void renderPlacement(BlockPos var1) {
      if (this.renderConfig.getValue()) {
         if (this.renderWorld != mc.world) {
            this.placementRender.clear();
            this.renderWorld = mc.world;
         }

         this.placementRender.put(var1.toImmutable(), new AlienPlaceAnimation(System.currentTimeMillis(), this.fadeTimeConfig.getValue().intValue()));

         while (this.placementRender.size() > 128) {
            this.placementRender.remove(this.placementRender.keySet().iterator().next());
         }
      }
   }

   @Override
   public void onRender3D(Render3DEvent var1) {
      if (mc.world != null && mc.player != null && this.renderConfig.getValue() && this.renderWorld == mc.world) {
         long var2 = System.currentTimeMillis();
         Iterator var4 = this.placementRender.entrySet().iterator();

         while (var4.hasNext()) {
            Entry var5 = (Entry)var4.next();
            AlienPlaceAnimation.Frame var6 = ((AlienPlaceAnimation)var5.getValue())
               .frame(
                  var2,
                  mc.world.isAir((BlockPos)var5.getKey()),
                  this.renderTimeoutConfig.getValue().intValue(),
                  this.renderNoFailConfig.getValue(),
                  this.renderModeConfig.getValue(),
                  this.renderEaseConfig.getValue()
               );
            if (var6.expired()) {
               var4.remove();
            } else {
               Box var7 = new Box((BlockPos)var5.getKey()).expand(-var6.inset());
               if (var6.pending()) {
                  if (this.renderTryFillConfig.getValue()) {
                     MioRender.fill(var1.getMatrix(), var7, this.renderTryFillColorConfig.getValue());
                  }

                  if (this.renderTryBoxConfig.getValue()) {
                     MioRender.outline(var1.getMatrix(), var7, this.renderTryBoxColorConfig.getValue(), 1.5F);
                  }
               } else {
                  if (this.renderFillConfig.getValue()) {
                     Color var8 = this.renderFillColorConfig.getValue();
                     MioRender.fill(var1.getMatrix(), var7, MioRender.alpha(var8, (int)(var8.getAlpha() * var6.alpha())));
                  }

                  if (this.renderBoxConfig.getValue()) {
                     Color var9 = this.renderBoxColorConfig.getValue();
                     MioRender.outline(var1.getMatrix(), var7, MioRender.alpha(var9, (int)(var9.getAlpha() * var6.alpha())), 1.5F);
                  }
               }
            }
         }
      } else {
         this.placementRender.clear();
      }
   }

   public void shorelinePlayerTick() {
      this.onPlayerTick(new PortSupport.PlayerTickEvent());
   }

   public void onPlayerTick(PortSupport.PlayerTickEvent var1) {
      if (mc.player != null && mc.world != null) {
         if (this.cycleWorld != mc.world) {
            this.onDisable();
            this.cycleWorld = mc.world;
         }

         this.moduleTick++;
         final long var2 = System.currentTimeMillis();
         final boolean var4 = this.onlyGroundConfig.getValue() && !mc.player.isOnGround()
            || this.onlyStaticConfig.getValue() && mc.player.getVelocity().horizontalLengthSquared() > 0.003;
         if (var4 && !this.cycle.finishing(this.plan != null && this.plan.hasAdjacentPower())) {
            this.debug("Paused: SelfGround/MovingPause");
         } else {
            boolean var5 = this.noEatingConfig.getValue() && this.checkMultitask();
            final PlayerEntity var6 = this.getClosestPlayer(this.rangeConfig.getValue().floatValue());
            this.usedRoutes
               .entrySet()
               .removeIf(var3 -> var2 - var3.getValue() >= (this.twoBTwoTModeConfig.getValue() ? this.routeMemoryConfig.getValue().intValue() : 250L));
            this.usedPlans.entrySet().removeIf(var1x -> this.isAir(var1x.getKey()) && (var1x.getValue().power == null || this.isAir(var1x.getValue().power)));
            if (this.plan == null) {
               if (var6 == null || var5 || this.moduleTick < this.nextCycleTick) {
                  return;
               }

               if (this.scanTarget != null
                  && this.scanTarget.squaredDistanceTo(var6.getEntityPos()) < 0.0025
                  && var2 - this.lastScan < (this.maxSpeedConfig.getValue() ? 50 : this.posUpdateDelayConfig.getValue())) {
                  return;
               }

               this.plan = this.findPlan(var6);
               this.scanTarget = var6.getEntityPos();
               this.lastScan = var2;
               this.cycle.reset();
               if (this.plan == null) {
                  this.debug("No usable route");
                  if (!this.usedPlans.isEmpty()) {
                     this.cleanupBlockedRoute(var2);
                  } else {
                     this.cleanupUntrackedBlocker(var6, var2);
                  }

                  return;
               }

               this.debug("Route: " + this.plan.piston + " / " + this.cycle.phase());
            }

            if (!var5 || this.cycle.attacked() && this.eatingBreakConfig.getValue()) {
               final TwoBPiston.Plan var7 = this.plan;
               if (!this.inRange(var7.piston) || !this.inRange(var7.base)) {
                  this.debug("Outside placement range");
                  if (this.cycle.attacked() && this.powerCleanupConfig.getValue()) {
                     this.removePower(var7, var2);
                  } else {
                     this.plan = null;
                     this.cycle.reset();
                     this.scanTarget = null;
                     this.resetCleanupMining();
                  }
               } else if (var7.pistonPlacedByUs
                  && !this.isAir(var7.piston)
                  && !var7.pistonPresentAndFacing()
                  && this.acknowledgedAt.getOrDefault(var7.piston, 0L) >= var7.pistonRequestedAt) {
                  this.debug("Wrong piston facing: removing rejected setup");
                  if (this.breakBlockVanilla(var7.piston, var2)) {
                     var7.pistonRequestedAt = 0L;
                     this.cycle.reset();
                  }
               } else {
                  PistonCycle.Result var8 = this.cycle
                     .tick(
                        var2,
                        Math.max(50, Math.min(250, 50 + PortSupport.Managers.NETWORK.getClientLatency() / 2)),
                        this.effectiveStuckTimeout(),
                        new PistonCycle.Environment() {
                           @Override
                           public PistonCycle.View inspect() {
                              EndCrystalEntity var1x = TwoBPiston.this.cycleCrystal(var7);
                              return new PistonCycle.View(
                                 TwoBPiston.this.isCrystalBase(var7.base),
                                 var7.pistonConfirmed(),
                                 var7.hasAdjacentPower(),
                                 var7.retracted(),
                                 var1x != null,
                                 var1x != null && TwoBPiston.this.pushed(var7, var1x),
                                 !TwoBPiston.this.getCrystalBox(var7.crystal).intersects(new Box(var7.piston)),
                                 TwoBPiston.this.powerCleanupConfig.getValue() || !TwoBPiston.this.twoBTwoTModeConfig.getValue()
                              );
                           }

                           @Override
                           public boolean perform(PistonCycle.Action var1) {
                              if (var4 && var1 != PistonCycle.Action.ATTACK && var1 != PistonCycle.Action.MINE_POWER) {
                                 return false;
                              } else if (var1 != PistonCycle.Action.ATTACK
                                 && var1 != PistonCycle.Action.MINE_POWER
                                 && var2 - TwoBPiston.this.lastAction < TwoBPiston.this.actionDelay()) {
                                 return false;
                              } else {
                                 boolean var2x = switch (var1) {
                                    case BASE -> TwoBPiston.this.placeBase(var7);
                                    case PISTON -> TwoBPiston.this.placePiston(var7, true);
                                    case CRYSTAL -> TwoBPiston.this.placeCrystal(var7);
                                    case POWER -> {
                                       if (!var7.pistonConfirmed() || TwoBPiston.this.cycleCrystal(var7) == null) {
                                          yield false;
                                       } else if (!TwoBPiston.this.refreshCrystalMotion(var7)) {
                                          TwoBPiston.this.debug("Crystal path blocked");
                                          yield false;
                                       } else {
                                          yield TwoBPiston.this.placePower(var7);
                                       }
                                    }
                                    case ATTACK -> {
                                       if (TwoBPiston.this.moduleTick != TwoBPiston.this.lastAttackTick
                                          && TwoBPiston.this.moduleTick >= TwoBPiston.this.nextCycleTick) {
                                          boolean var3 = TwoBPiston.this.breakPushedCrystal(var7, var6, var2);
                                          if (var3) {
                                             TwoBPiston.this.lastAttackTick = TwoBPiston.this.moduleTick;
                                          }

                                          yield var3;
                                       } else {
                                          yield false;
                                       }
                                    }
                                    case MINE_POWER -> {
                                       TwoBPiston.this.removePower(var7, var2);
                                       yield true;
                                    }
                                 };
                                 if (var2x && var1 != PistonCycle.Action.MINE_POWER) {
                                    TwoBPiston.this.lastAction = var2;
                                 }

                                 TwoBPiston.this.debug(TwoBPiston.this.cycle.phase() + (var2x ? " sent " : " waiting ") + var1);
                                 return var2x;
                              }
                           }
                        }
                     );
                  if (var8 == PistonCycle.Result.COMPLETE) {
                     this.nextCycleTick = this.moduleTick + this.effectiveCycleDelayTicks();
                     this.attackedCrystalId = -1;
                     this.resetCleanupMining();
                     this.usedRoutes.remove(var7.piston);
                     if (!this.twoBTwoTModeConfig.getValue()
                        && var7.pistonPresentAndFacing()
                        && var7.retracted()
                        && !var7.blockPushPattern
                        && var6 != null
                        && this.scanTarget != null
                        && this.scanTarget.squaredDistanceTo(var6.getEntityPos()) <= 0.25
                        && (!var7.exactTargetAxis || var7.targetStillOnAxis(var6))) {
                        var7.prepareNextCycle();
                        this.cycle.reset();
                        if (this.moduleTick >= this.nextCycleTick && !var4 && !var5) {
                           this.onCycleReady(var7, var2);
                        }
                     } else {
                        if (this.twoBTwoTModeConfig.getValue() && var7.hasAdjacentPower()) {
                           this.rememberUsedPlan(var7, var2);
                        }

                        this.plan = null;
                        this.cycle.reset();
                        this.scanTarget = null;
                        this.lastResetAt = 0L;
                     }
                  } else if (var8 == PistonCycle.Result.FAILED) {
                     this.debug("No server confirmation: re-evaluate route");
                     if (var7.hasAdjacentPower()) {
                        this.removePower(var7, var2);
                        return;
                     }

                     EndCrystalEntity var9 = this.cycleCrystal(var7);
                     if (var9 != null && this.withinCrystalRange(var9)) {
                        if (this.lastAttackTick != this.moduleTick && var2 - this.attackedAt >= this.crystalConfirmDelay()) {
                           this.attackCrystal(var9);
                           this.lastAttackTick = this.moduleTick;
                           this.attackedAt = var2;
                        }

                        return;
                     }

                     this.usedRoutes.put(var7.piston, var2);
                     this.plan = null;
                     this.cycle.reset();
                     this.scanTarget = null;
                     this.resetCleanupMining();
                  }
               }
            } else {
               this.debug("Paused: NoEating");
            }
         }
      }
   }

   private void onCycleReady(final TwoBPiston.Plan var1, final long var2) {
      this.cycle
         .tick(
            var2,
            50L,
            this.effectiveStuckTimeout(),
            new PistonCycle.Environment() {
               @Override
               public PistonCycle.View inspect() {
                  return new PistonCycle.View(
                     TwoBPiston.this.isCrystalBase(var1.base),
                     var1.pistonConfirmed(),
                     var1.hasAdjacentPower(),
                     var1.retracted(),
                     TwoBPiston.this.cycleCrystal(var1) != null,
                     false,
                     false,
                     true
                  );
               }

               @Override
               public boolean perform(PistonCycle.Action var1x) {
                  if (var1x == PistonCycle.Action.CRYSTAL && var2 - TwoBPiston.this.lastAction >= TwoBPiston.this.actionDelay()) {
                     boolean var2x = TwoBPiston.this.placeCrystal(var1);
                     if (var2x) {
                        TwoBPiston.this.lastAction = var2;
                     }

                     return var2x;
                  } else {
                     return false;
                  }
               }
            }
         );
   }

   private EndCrystalEntity cycleCrystal(TwoBPiston.Plan var1) {
      if (var1.crystalEntityId < 0) {
         return this.findCrystalAtStart(var1);
      } else {
         return mc.world.getEntityById(var1.crystalEntityId) instanceof EndCrystalEntity var3 && !var3.isRemoved() ? var3 : null;
      }
   }

   private boolean pushed(TwoBPiston.Plan var1, EndCrystalEntity var2) {
      Vec3d var3 = var1.initialCrystalPosition;
      if (var3 == null) {
         return false;
      } else {
         Vec3d var4 = var2.getEntityPos().subtract(var3);
         double var5 = var4.dotProduct(Vec3d.of(var1.outward.getOpposite().getVector()));
         double var7 = Math.max(0.0, var4.lengthSquared() - var5 * var5);
         return var5 >= (this.confirmPushConfig.getValue() ? 0.1 : -0.05) && var7 <= 1.0 && var4.lengthSquared() <= 9.0;
      }
   }

   private TwoBPiston.Plan findPlan(PlayerEntity var1) {
      HashMap<Vec3d, Double> var2 = new HashMap<>();
      Set var3 = PistonPatterns.union(PistonPatterns.chimera(var1.getBlockPos()), List.of());
      TwoBPiston.Plan var4 = this.findChimeraPlan(var1, var3, var2);
      double var5 = var4 == null ? Double.MAX_VALUE : this.routeScore(var4, var1, (Double)var2.get(var4.predicted));
      Box var7 = var1.getBoundingBox();
      int var8 = (int)Math.floor(var7.minX) - 2;
      int var9 = (int)Math.floor(var7.maxX - 1.0E-7) + 2;
      int var10 = (int)Math.floor(var7.minZ) - 2;
      int var11 = (int)Math.floor(var7.maxZ - 1.0E-7) + 2;
      int var12 = (int)Math.floor(var7.minY) - 1;
      int var13 = (int)Math.floor(var7.maxY) + 2;
      Direction[] var14 = this.verticalPatternsConfig.getValue() ? Direction.values() : HORIZONTAL;

      for (int var15 = var12; var15 <= var13; var15++) {
         for (int var16 = var8; var16 <= var9; var16++) {
            for (int var17 = var10; var17 <= var11; var17++) {
               BlockPos var19 = new BlockPos(var16, var15, var17);
               BlockPos var20 = var19.down();
               if (this.inRange(var20)
                  && this.canUseBase(var20)
                  && mc.world.isAir(var19)
                  && mc.world.getOtherEntities(null, new Box(var16, var15, var17, var16 + 1, var15 + 2, var17 + 1)).isEmpty()) {
                  Box var21 = this.getCrystalBox(var19);

                  for (Direction var25 : var14) {
                     Direction var30 = var25.getOpposite();
                     Vec3d var31 = this.simulateStroke(var21, var30);
                     double var32 = var31.x * var30.getOffsetX() + var31.y * var30.getOffsetY() + var31.z * var30.getOffsetZ();
                     Vec3d var29;
                     if (!(var32 < 0.2)
                        && !((var29 = new Vec3d(var16 + 0.5 + var31.x, var15 + var31.y, var17 + 0.5 + var31.z)).squaredDistanceTo(var1.getEntityPos()) > 12.25)
                        )
                      {
                        BlockPos var34 = new BlockPos((int)Math.floor(var29.x), (int)Math.floor(var29.y), (int)Math.floor(var29.z));
                        boolean var28 = !this.isAir(var34);
                        double var26;
                        if ((!var28 || this.wallPatternsConfig.getValue())
                           && !((var26 = var2.computeIfAbsent(var29, var1x -> ExplosionUtil.getDamageTo(var1, var1x, false, false))) < 1.0)) {
                           for (int var36 = -1; var36 <= 1; var36++) {
                              for (int var37 = -1; var37 <= 1; var37++) {
                                 for (int var38 = 0; var38 <= 1; var38++) {
                                    BlockPos var41 = new BlockPos(var16 + var36, var15 + var38, var17 + var37).offset(var25);
                                    PistonPatterns.Route var42 = new PistonPatterns.Route(var19, var41, var25);
                                    if (PistonPatterns.geometric(var42) && this.inRange(var41) && var3.add(var42)) {
                                       TwoBPiston.Plan var43 = new TwoBPiston.Plan(
                                          var20,
                                          var19,
                                          var41,
                                          var34,
                                          var25,
                                          false,
                                          var1.isCrawling() || !this.isAir(new BlockPos(var16, var15 + 1, var17)),
                                          var28,
                                          false
                                       );
                                       var43.predicted = var29;
                                       var43.expectedPush = var32;
                                       boolean var44 = var43.wallPattern = var28 || mc.world.getBlockCollisions(null, var21).iterator().hasNext();
                                       double var39;
                                       if ((!var43.wallPattern || this.wallPatternsConfig.getValue())
                                          && this.routeAllowed(var43)
                                          && var43.selectionValid()
                                          && (var39 = this.routeScore(var43, var1, var26) + 0.0) < var5) {
                                          var5 = var39;
                                          var4 = var43;
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      if (var4 == null && this.blockPushPatternsConfig.getValue()) {
         var4 = this.findBlockPushPlan(var1, var8, var9, var12, var13, var10, var11);
      }

      return var4;
   }

   private double miningScore(TwoBPiston.Plan var1, PlayerEntity var2) {
      if (!this.minePatternConfig.getValue()) {
         return 0.0;
      } else {
         for (Entry var4 : this.mining.entrySet()) {
            TwoBPiston.MiningInfo var5 = (TwoBPiston.MiningInfo)var4.getValue();
            if (((TwoBPiston.MiningKey)var4.getKey()).entityId == var2.getId()
               && System.currentTimeMillis() - var5.time <= this.mineMemoryConfig.getValue().intValue()
               && (var5.repeats >= 2 || var5.instant)
               && var5.pos.equals(var1.impact)) {
               return -4.0;
            }
         }

         return 0.0;
      }
   }

   private TwoBPiston.Plan findChimeraPlan(PlayerEntity var1) {
      return this.findChimeraPlan(var1, PistonPatterns.chimera(var1.getBlockPos()), new HashMap<>());
   }

   private double routeScore(TwoBPiston.Plan var1, PlayerEntity var2, double var3) {
      return var1.score() + var1.predicted.squaredDistanceTo(var2.getEntityPos()) * 4.0 - Math.min(20.0, var3) * 2.0 + this.miningScore(var1, var2);
   }

   private TwoBPiston.Plan findChimeraPlan(PlayerEntity var1, Collection<PistonPatterns.Route> var2, Map<Vec3d, Double> var3) {
      TwoBPiston.Plan var4 = null;
      double var5 = Double.MAX_VALUE;

      for (PistonPatterns.Route var8 : var2) {
         if (PistonPatterns.geometric(var8)) {
            BlockPos var16 = var8.crystal();
            BlockPos var17 = var16.down();
            BlockPos var18 = var8.piston();
            if (this.inRange(var17)
               && this.inRange(var18)
               && this.canUseBase(var17)
               && mc.world.isAir(var16)
               && mc.world
                  .getOtherEntities(null, new Box(var16.getX(), var16.getY(), var16.getZ(), var16.getX() + 1, var16.getY() + 2, var16.getZ() + 1))
                  .isEmpty()) {
               Vec3d var19 = this.simulateStroke(this.getCrystalBox(var16), var8.outward().getOpposite());
               double var20 = -(var19.x * var8.outward().getOffsetX() + var19.z * var8.outward().getOffsetZ());
               Vec3d var14;
               if (!(var20 < 0.2)
                  && !(
                     (var14 = new Vec3d(var16.getX() + 0.5 + var19.x, var16.getY() + var19.y, var16.getZ() + 0.5 + var19.z))
                           .squaredDistanceTo(var1.getEntityPos())
                        > 12.25
                  )) {
                  BlockPos var22 = new BlockPos((int)Math.floor(var14.x), (int)Math.floor(var14.y), (int)Math.floor(var14.z));
                  boolean var13 = !this.isAir(var22) || mc.world.getBlockCollisions(null, this.getCrystalBox(var16)).iterator().hasNext();
                  double var11;
                  if ((!var13 || this.wallPatternsConfig.getValue())
                     && !((var11 = var3.computeIfAbsent(var14, var1x -> ExplosionUtil.getDamageTo(var1, var1x, false, false))) < 1.0)) {
                     TwoBPiston.Plan var24 = new TwoBPiston.Plan(var17, var16, var18, var22, var8.outward(), false, !this.isAir(var16.up()), var13, false);
                     var24.predicted = var14;
                     var24.expectedPush = var20;
                     var24.wallPattern = var13;
                     double var9;
                     if (this.routeAllowed(var24) && var24.selectionValid() && (var9 = this.routeScore(var24, var1, var11)) < var5) {
                        var4 = var24;
                        var5 = var9;
                     }
                  }
               }
            }
         }
      }

      return var4;
   }

   private Vec3d simulateStroke(Box var1, Direction var2) {
      return PistonMotion.stroke(var1, var2, var0 -> mc.world.getBlockCollisions(null, var0));
   }

   private TwoBPiston.Plan findBlockPushPlan(PlayerEntity var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      TwoBPiston.Plan var8 = null;
      double var9 = Double.MAX_VALUE;
      int var11 = Math.min(12, (int)Math.ceil(this.placeRangeConfig.getValue().floatValue()) + 2);
      Direction[] var12 = this.verticalPatternsConfig.getValue() ? Direction.values() : HORIZONTAL;

      for (int var13 = var4; var13 <= var5; var13++) {
         for (int var14 = var2; var14 <= var3; var14++) {
            for (int var15 = var6; var15 <= var7; var15++) {
               BlockPos var17 = new BlockPos(var14, var13, var15);
               BlockPos var18 = var17.down();
               Box var16;
               if (this.inRange(var18)
                  && this.canUseBase(var18)
                  && mc.world.isAir(var17)
                  && mc.world.getOtherEntities(null, var16 = new Box(var14, var13, var15, var14 + 1, var13 + 2, var15 + 1)).isEmpty()) {
                  for (Direction var22 : var12) {
                     for (int var23 = -1; var23 <= 1; var23++) {
                        for (int var24 = 0; var24 <= 1; var24++) {
                           BlockPos var25 = new BlockPos(var14 - var22.getOffsetZ() * var23, var13 + var24, var15 + var22.getOffsetX() * var23);

                           for (int var26 = 2; var26 <= var11; var26++) {
                              BlockPos var32 = var25.offset(var22, var26);
                              PistonHandler var31;
                              if (this.inRange(var32)
                                 && !var16.intersects(new Box(var32))
                                 && !this.isAir(var32.offset(var22.getOpposite()))
                                 && (var31 = new PistonHandler(mc.world, var32, var22.getOpposite(), true)).calculatePush()
                                 && !var31.getMovedBlocks().isEmpty()) {
                                 TwoBPiston.Plan var33 = new TwoBPiston.Plan(var18, var17, var32, var17, var22, false, false, true, false);
                                 var33.blockPushPattern = true;
                                 var33.movedBlocks = List.copyOf(var31.getMovedBlocks());
                                 if (this.routeAllowed(var33) && var33.selectionValid()) {
                                    HashSet var34 = new HashSet(var31.getMovedBlocks());
                                    var34.addAll(var31.getBrokenBlocks());
                                    Vec3d var35 = PistonMotion.stroke(
                                       this.getCrystalBox(var17), var22.getOpposite(), var2x -> this.collisionShapesExcept(var2x, var34)
                                    );
                                    double var36 = -(var35.x * var22.getOffsetX() + var35.y * var22.getOffsetY() + var35.z * var22.getOffsetZ());
                                    if (!(var36 < 0.2)) {
                                       var33.predicted = new Vec3d(var14 + 0.5 + var35.x, var13 + var35.y, var15 + 0.5 + var35.z);
                                       var33.expectedPush = var36;
                                       boolean var38 = var33.wallPattern = !this.isAir(
                                          new BlockPos(
                                             (int)Math.floor(var33.predicted.x), (int)Math.floor(var33.predicted.y), (int)Math.floor(var33.predicted.z)
                                          )
                                       );
                                       if (!var33.wallPattern || this.wallPatternsConfig.getValue()) {
                                          var33.powerCandidate = null;
                                          double var27;
                                          double var29;
                                          if (var33.reservePower() != null
                                             && !((var29 = ExplosionUtil.getDamageTo(var1, var33.predicted, false, false)) < 1.0)
                                             && (
                                                   var27 = var33.score()
                                                      + var33.predicted.squaredDistanceTo(var1.getEntityPos()) * 4.0
                                                      + var26
                                                      - Math.min(20.0, var29) * 2.0
                                                )
                                                < var9) {
                                             var9 = var27;
                                             var8 = var33;
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return var8;
   }

   private Iterable<VoxelShape> collisionShapesExcept(Box var1, Set<BlockPos> var2) {
      ArrayList var3 = new ArrayList();

      for (int var4 = (int)Math.floor(var1.minX) - 1; var4 <= (int)Math.floor(var1.maxX) + 1; var4++) {
         for (int var5 = (int)Math.floor(var1.minY) - 1; var5 <= (int)Math.floor(var1.maxY) + 1; var5++) {
            for (int var6 = (int)Math.floor(var1.minZ) - 1; var6 <= (int)Math.floor(var1.maxZ) + 1; var6++) {
               BlockPos var8 = new BlockPos(var4, var5, var6);
               VoxelShape var7;
               if (!var2.contains(var8) && !(var7 = mc.world.getBlockState(var8).getCollisionShape(mc.world, var8)).isEmpty()) {
                  var3.add(var7.offset(var4, var5, var6));
               }
            }
         }
      }

      return var3;
   }

   private TwoBPiston.Plan findFacePlacePlan(PlayerEntity var1, BlockPos var2) {
      double var3 = Math.abs(var1.getX() - (var2.getX() + 0.5));
      double var5 = Math.abs(var1.getZ() - (var2.getZ() + 0.5));
      if (!(var3 > 0.35) && !(var5 > 0.35)) {
         for (Direction var10 : HORIZONTAL) {
            if (!this.isCrystalBase(var2.offset(var10))) {
               return null;
            }
         }

         BlockPos var23 = var2.up();
         TwoBPiston.Plan var24 = null;
         double var25 = Double.MAX_VALUE;

         for (Direction var14 : HORIZONTAL) {
            BlockPos var21 = var2.offset(var14);
            BlockPos var20;
            TwoBPiston.Plan var22 = new TwoBPiston.Plan(var21, var20 = var21.up(), var20.offset(var14), var23, var14, false, false, false, true);
            double var15;
            double var17;
            if (this.routeAllowed(var22)
               && var22.selectionValid()
               && !((var17 = this.predictedDamage(var22, var1, null)) < 1.0)
               && (var15 = var22.score() - Math.min(12.0, var17)) < var25) {
               var24 = var22;
               var25 = var15;
            }
         }

         return var24;
      } else {
         return null;
      }
   }

   private TwoBPiston.Plan findLowCeilingPlan(PlayerEntity var1) {
      BlockPos var2 = new BlockPos((int)Math.floor(var1.getX()), (int)Math.floor(var1.getY()), (int)Math.floor(var1.getZ()));

      for (boolean var6 : new boolean[]{true, false}) {
         TwoBPiston.Plan var9 = null;
         double var10 = Double.MAX_VALUE;
         HashMap<BlockPos, Double> var12 = new HashMap<>();
         int[] var7;
         if (var6) {
            int[] var8;
            var7 = var8 = new int[1];
            var8[0] = 0;
         } else {
            int[] var49 = new int[]{-1, 0};
            var7 = var49;
            var49[1] = 1;
         }

         int[] var50 = var7;

         for (int var16 : TARGET_HEIGHTS) {
            BlockPos var17 = var2.offset(Direction.UP, var16);
            if (var16 < 2 || this.isAir(var17.down())) {
               for (Direction var21 : HORIZONTAL) {
                  int var22 = -var21.getOffsetZ();
                  int var23 = var21.getOffsetX();

                  for (int var27 : var50) {
                     BlockPos var30 = new BlockPos(var17.getX() + var22 * var27, var17.getY(), var17.getZ() + var23 * var27);
                     BlockPos var31 = var30.offset(var21);
                     BlockPos var32 = var31.down();
                     int[] var28;
                     if (var27 == 0) {
                        int[] var29;
                        var28 = var29 = new int[1];
                        var29[0] = 0;
                     } else {
                        int[] var51 = new int[]{0, 0};
                        var28 = var51;
                        var51[1] = var27;
                     }

                     for (int var38 : var28) {
                        BlockPos var39 = new BlockPos(
                           var17.getX() + var22 * var38 + var21.getOffsetX() * 2, var17.getY(), var17.getZ() + var23 * var38 + var21.getOffsetZ() * 2
                        );
                        boolean var37;
                        TwoBPiston.Plan var40 = new TwoBPiston.Plan(
                           var32, var31, var39, var30, var21, false, true, var37 = var27 == 0 && (var16 == 1 || var16 == 2) && !this.isAir(var30), var27 == 0
                        );
                        if (this.routeAllowed(var40) && var40.selectionValid()) {
                           double var41 = var12.computeIfAbsent(var30, var3 -> this.predictedDamage(var40, var1, null));
                           if (!(var41 < 1.0)) {
                              double var43 = var27 == 0 ? 0.0 : (var38 == 0 ? 1.5 : 2.0);
                              double var45 = Math.abs(var16 - 1) * 3.0 + var43 + (var37 ? -30.0 : 0.0);
                              double var47 = var40.score() + this.impactDistanceSq(var40, var1) * 8.0 + var45 - Math.min(12.0, var41);
                              if (var47 < var10) {
                                 var9 = var40;
                                 var10 = var47;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (var9 != null) {
            return var9;
         }
      }

      return null;
   }

   private TwoBPiston.Plan findMiningPlan(BlockPos var1, PlayerEntity var2) {
      for (boolean var6 : new boolean[]{true, false}) {
         TwoBPiston.Plan var9 = null;
         double var10 = Double.MAX_VALUE;
         HashMap<BlockPos, Double> var12 = new HashMap<>();
         int[] var7;
         if (var6) {
            int[] var8;
            var7 = var8 = new int[1];
            var8[0] = 0;
         } else {
            int[] var49 = new int[]{-1, 0};
            var7 = var49;
            var49[1] = 1;
         }

         int[] var50 = var7;

         for (int var16 : MINE_HEIGHTS) {
            BlockPos var17 = var1.offset(Direction.UP, var16);

            for (Direction var21 : HORIZONTAL) {
               int var22 = -var21.getOffsetZ();
               int var23 = var21.getOffsetX();

               for (int var27 : var50) {
                  BlockPos var30 = new BlockPos(var17.getX() + var22 * var27, var17.getY(), var17.getZ() + var23 * var27);
                  BlockPos var31 = var30.offset(var21);
                  BlockPos var32 = var31.down();
                  int[] var28;
                  if (var27 == 0) {
                     int[] var29;
                     var28 = var29 = new int[1];
                     var29[0] = 0;
                  } else {
                     int[] var51 = new int[]{0, 0};
                     var28 = var51;
                     var51[1] = var27;
                  }

                  for (int var38 : var28) {
                     BlockPos var39 = new BlockPos(
                        var17.getX() + var22 * var38 + var21.getOffsetX() * 2, var17.getY(), var17.getZ() + var23 * var38 + var21.getOffsetZ() * 2
                     );
                     TwoBPiston.Plan var40 = new TwoBPiston.Plan(
                        var32, var31, var39, var30, var21, true, false, var27 == 0 && (var16 == 0 || var16 == 1), false
                     );
                     if (this.routeAllowed(var40) && var40.selectionValid()) {
                        double var41 = var12.computeIfAbsent(var30, var4 -> this.predictedDamage(var40, var2, var1));
                        if (!(var41 < 1.0)) {
                           double var43 = var27 == 0 ? 0.0 : (var38 == 0 ? 1.5 : 2.0);
                           double var45 = Math.abs(var16) * 3.0 + var43;
                           double var47 = var40.score() + this.impactDistanceSq(var40, var2) * 8.0 + var45 - Math.min(12.0, var41);
                           if (var47 < var10) {
                              var9 = var40;
                              var10 = var47;
                           }
                        }
                     }
                  }
               }
            }
         }

         if (var9 != null) {
            return var9;
         }
      }

      return null;
   }

   private double impactDistanceSq(TwoBPiston.Plan var1, PlayerEntity var2) {
      Box var3 = var2.getBoundingBox();
      double var4 = var1.impact.getX() + 0.5 - var2.getX();
      double var6 = var1.impact.getZ() + 0.5 - var2.getZ();
      double var8 = var1.impact.getY();
      double var10 = Math.max(var3.minY, Math.min(var3.maxY, var8));
      double var12 = var8 - var10;
      return var4 * var4 + var12 * var12 + var6 * var6;
   }

   private double predictedDamage(TwoBPiston.Plan var1, PlayerEntity var2, BlockPos var3) {
      if (this.impactDistanceSq(var1, var2) > 6.25) {
         return 0.0;
      } else {
         Vec3d var4 = new Vec3d(var1.impact.getX() + 0.5, var1.impact.getY(), var1.impact.getZ() + 0.5);
         boolean var5 = var1.allowSolidImpact && !this.isAir(var1.impact);
         if (var5 && var3 != null && !var1.impact.equals(var3)) {
            return ExplosionUtil.getDamageTo(var2, var4, false, Set.of(var1.impact, var3), false);
         } else {
            BlockPos var7 = var5 ? var1.impact : var3;
            return var7 == null ? ExplosionUtil.getDamageTo(var2, var4, false, false) : ExplosionUtil.getDamageTo(var2, var4, false, Set.of(var7), false);
         }
      }
   }

   private void debug(String var1) {
      if (this.debugConfig.getValue()) {
         long var2 = System.currentTimeMillis();
         if (var2 - this.lastDebugAt >= 500L) {
            this.lastDebugAt = var2;
            ChatUtil.sendMessage(Text.literal("[2bPiston] " + var1), "2bpiston-source-debug");
         }
      }
   }

   private long effectiveStuckTimeout() {
      long var1 = Math.max(0L, Math.min(2500L, (long)PortSupport.Managers.NETWORK.getClientLatency()));
      return Math.max((long)this.stuckTimeoutConfig.getValue().intValue(), Math.min(6000L, 600L + var1 * 2L));
   }

   private long actionDelay() {
      return this.maxSpeedConfig.getValue() ? 0L : this.updateDelayConfig.getValue().intValue();
   }

   private int effectiveCycleDelayTicks() {
      int var1 = this.cycleDelayConfig.getValue();
      return this.maxSpeedConfig.getValue() ? 0 : Math.max(0, var1);
   }

   private long crystalConfirmDelay() {
      long var1 = Math.max(0L, Math.min(2500L, (long)PortSupport.Managers.NETWORK.getClientLatency()));
      return Math.max(150L, 100L + var1);
   }

   private boolean routeAllowed(TwoBPiston.Plan var1) {
      Long var2 = this.usedRoutes.get(var1.piston);
      long var3 = this.twoBTwoTModeConfig.getValue() ? this.routeMemoryConfig.getValue().intValue() : 250L;
      return var2 == null || System.currentTimeMillis() - var2 >= var3;
   }

   private void rememberUsedPlan(TwoBPiston.Plan var1, long var2) {
      this.trackOwnedPlan(var1);
      this.usedRoutes.put(var1.piston, var2);
   }

   private void trackOwnedPlan(TwoBPiston.Plan var1) {
      TwoBPiston.Plan var2 = this.usedPlans.get(var1.piston);
      if (var2 != null) {
         var1.pistonPlacedByUs = var1.pistonPlacedByUs | var2.pistonPlacedByUs;
      }

      if (var1.pistonPlacedByUs || var1.powerPlacedByUs || var2 != null) {
         this.usedPlans.put(var1.piston, var1);
      }
   }

   private int activeOwnedPistons() {
      int var1 = 0;

      for (BlockPos var3 : this.usedPlans.keySet()) {
         if (mc.world.getBlockState(var3).getBlock() instanceof PistonBlock) {
            var1++;
         }
      }

      return var1;
   }

   private void cleanupBlockedRoute(long var1) {
      if (this.routeCleanupPlan == null) {
         if (var1 - this.lastRouteCleanupAt < (this.maxSpeedConfig.getValue() ? 0L : 1000L)) {
            return;
         }

         Entry var4 = this.usedPlans
            .entrySet()
            .stream()
            .min(Comparator.comparingLong(var1x -> this.usedRoutes.getOrDefault(var1x.getKey(), Long.MAX_VALUE)))
            .orElse(null);
         if (var4 == null) {
            return;
         }

         this.routeCleanupPlan = (TwoBPiston.Plan)var4.getValue();
         this.routeCleanupStage = 0;
         this.resetCleanupMining();
      }

      TwoBPiston.Plan var3 = this.routeCleanupPlan;
      if (this.routeCleanupStage == 0) {
         EndCrystalEntity var5;
         if (var3.crystalPlacedByUs && (var5 = this.findCrystalNear(var3.impact, 1.6)) != null) {
            PortSupport.Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(var5, mc.player.isSneaking()));
            PortSupport.Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
         }

         this.routeCleanupStage = 1;
      } else if (this.routeCleanupStage == 1) {
         if (!var3.pistonPlacedByUs || !(mc.world.getBlockState(var3.piston).getBlock() instanceof PistonBlock) || this.breakBlockVanilla(var3.piston, var1)) {
            this.resetCleanupMining();
            this.usedRoutes.remove(var3.piston);
            this.routeCleanupStage = 2;
         }
      } else if (!var3.powerPlacedByUs
         || var3.power == null
         || this.powerSharedWithOtherPlan(var3)
         || mc.world.getBlockState(var3.power).getBlock() != Blocks.REDSTONE_BLOCK
         || this.breakBlockVanilla(var3.power, var1)) {
         this.resetCleanupMining();
         this.usedRoutes.remove(var3.piston);
         this.usedPlans.remove(var3.piston);
         this.routeCleanupPlan = null;
         this.routeCleanupStage = 0;
         this.lastRouteCleanupAt = var1;
      }
   }

   private boolean powerSharedWithOtherPlan(TwoBPiston.Plan var1) {
      if (var1.power == null) {
         return false;
      } else {
         for (TwoBPiston.Plan var3 : this.usedPlans.values()) {
            if (var3 != var1
               && mc.world.getBlockState(var3.piston).getBlock() instanceof PistonBlock
               && (
                  var1.power.equals(var3.power)
                     || this.isPowerClearOfCrystalPath(var3, var1.power)
                        && Math.abs(var3.piston.getX() - var1.power.getX())
                              + Math.abs(var3.piston.getY() - var1.power.getY())
                              + Math.abs(var3.piston.getZ() - var1.power.getZ())
                           == 1
               )) {
               return true;
            }
         }

         return false;
      }
   }

   private void cleanupUntrackedBlocker(PlayerEntity var1, long var2) {
      if (var2 - this.lastRouteCleanupAt >= (this.maxSpeedConfig.getValue() ? 0L : 1000L)) {
         BlockPos var4 = new BlockPos((int)Math.floor(var1.getX()), (int)Math.floor(var1.getY()), (int)Math.floor(var1.getZ()));

         for (int var8 : new int[]{0, 1}) {
            for (Direction var12 : HORIZONTAL) {
               BlockPos var13 = var4.offset(Direction.UP, var8);
               BlockPos var14 = var13.offset(var12);
               BlockPos var15 = var14.offset(var12);
               if (this.isAir(var13) && this.isAir(var14) && this.isCrystalBase(var14.down()) && this.inRange(var15)) {
                  BlockState var16 = mc.world.getBlockState(var15);
                  boolean var17 = var16.getBlock() == Blocks.REDSTONE_BLOCK;
                  boolean var18 = var16.getBlock() instanceof PistonBlock && var16.get(FacingBlock.FACING) != var12.getOpposite();
                  if (var17 || var18) {
                     if (this.breakBlockVanilla(var15, var2)) {
                        this.lastRouteCleanupAt = var2;
                     }

                     return;
                  }

                  if (var16.isReplaceable()) {
                     for (Direction var23 : Direction.values()) {
                        BlockPos var24 = var15.offset(var23);
                        if (mc.world.getBlockState(var24).getBlock() == Blocks.REDSTONE_BLOCK) {
                           if (this.breakBlockVanilla(var24, var2)) {
                              this.lastRouteCleanupAt = var2;
                           }

                           return;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean placeBase(TwoBPiston.Plan var1) {
      if (this.isCrystalBase(var1.base)) {
         return true;
      } else if (this.basePlaceConfig.getValue() && this.isAir(var1.base)) {
         int var2 = this.findSlot(Blocks.OBSIDIAN);
         if (var2 >= 0 && PistonInteraction.INSTANCE.canPlace(var1.base, Blocks.OBSIDIAN)) {
            this.renderPlacement(var1.base);
            return PistonInteraction.INSTANCE.placeBlock(var1.base, Blocks.OBSIDIAN, var2, false, false, true, true, (var1x, var2x) -> {
               if (this.rotateConfig.getValue()) {
                  if (var1x) {
                     PortSupport.Managers.ROTATION.setRotationSilent(var2x[0], var2x[1]);
                  } else {
                     PortSupport.Managers.ROTATION.setRotationSilentSync();
                  }
               }
            });
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean placePiston(TwoBPiston.Plan var1, boolean var2) {
      if (var1.reservePower() == null) {
         return false;
      } else if (!var1.canExtendPiston() || !var1.pistonUsable() || !var1.impactUsable()) {
         return false;
      } else if (!this.isAir(var1.piston)) {
         return true;
      } else {
         int var3 = this.pistonSlot();
         if (var3 < 0) {
            return false;
         } else {
            if (!var1.piston.equals(this.rotatingPiston) || var1.outward != this.rotatingLookDirection) {
               this.rotatingPiston = var1.piston;
               this.rotatingLookDirection = var1.outward;
            }

            float var4 = var1.outward.getOffsetY() == 0 ? var1.outward.getPositiveHorizontalDegrees() : PortSupport.Managers.ROTATION.getWrappedYaw();
            float var5 = PortSupport.Managers.ROTATION.getWrappedYaw();
            float var6 = this.wrapDegrees(var4 - var5);
            float var7 = var4;
            boolean var8 = true;
            if (this.rotateConfig.getValue() && !this.maxSpeedConfig.getValue() && this.yawStepConfig.getValue() != CrystalAura.Rotate.OFF) {
               float var9 = this.yawStepLimitConfig.getValue().intValue();
               if (Math.abs(var6) > var9) {
                  var7 = var5 + Math.copySign(var9, var6);
                  var8 = false;
               }
            }

            if (this.rotateConfig.getValue()) {
               this.setRotationSilent(var7, var1.outward.getOffsetY() > 0 ? -90.0F : (var1.outward.getOffsetY() < 0 ? 90.0F : 5.0F));
            }

            if (this.rotateConfig.getValue() && !var8) {
               return false;
            } else if (!var2) {
               return false;
            } else {
               boolean var13 = this.airPlaceConfig.getValue() || this.throughWallsConfig.getValue();
               Block var10 = ((BlockItem)mc.player.getInventory().getStack(var3).getItem()).getBlock();
               if (!PistonInteraction.INSTANCE.canPlace(var1.piston, var10)) {
                  return false;
               } else {
                  BlockHitResult var11 = this.pistonSupport(var1);
                  var1.pistonRequestedAt = System.currentTimeMillis();
                  this.renderPlacement(var1.piston);
                  boolean var12 = var11 != null
                     ? PistonInteraction.INSTANCE.placeBlock(var11, var3, false, false, this.pistonPacketConfig.getValue(), (var0, var1x) -> {})
                     : PistonInteraction.INSTANCE
                        .placeBlock(var1.piston, var10, var3, false, false, this.pistonPacketConfig.getValue(), var13, (var0, var1x) -> {});
                  if (var12) {
                     var1.pistonPlacedByUs = true;
                     this.trackOwnedPlan(var1);
                  }

                  this.rotatingPiston = null;
                  this.rotatingLookDirection = null;
                  return var12;
               }
            }
         }
      }
   }

   private BlockHitResult pistonSupport(TwoBPiston.Plan var1) {
      Direction[] var2 = new Direction[]{Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP};

      for (Direction var6 : var2) {
         BlockPos var7 = var1.piston.offset(var6);
         if (!mc.world.getBlockState(var7).isReplaceable()) {
            Direction var8 = var6.getOpposite();
            return new BlockHitResult(var7.toCenterPos().add(Vec3d.of(var8.getVector()).multiply(0.5)), var8, var7, false);
         }
      }

      return null;
   }

   private boolean placePower(TwoBPiston.Plan var1) {
      if (this.isAir(var1.piston) || !var1.pistonUsable()) {
         return false;
      } else if (var1.canExtendPiston() && var1.impactUsable()) {
         BlockPos var2 = var1.reservePower();
         if (var2 == null) {
            return false;
         } else if (mc.world.getBlockState(var2).getBlock() == Blocks.REDSTONE_BLOCK) {
            if (var1.power == null || !var1.power.equals(var2)) {
               var1.powerPlacedByUs = false;
            }

            var1.power = var2;
            return true;
         } else {
            int var3 = this.findSlot(Blocks.REDSTONE_BLOCK);
            if (var3 < 0) {
               return false;
            } else {
               boolean var4 = this.airPlaceConfig.getValue() || this.throughWallsConfig.getValue();
               this.renderPlacement(var2);
               boolean var5 = PistonInteraction.INSTANCE.placeBlock(var2, Blocks.REDSTONE_BLOCK, var3, false, false, true, var4, (var1x, var2x) -> {
                  if (this.rotateConfig.getValue() && var1x) {
                     PortSupport.Managers.ROTATION.setRotationSilent(var2x[0], var2x[1]);
                  }
               });
               if (this.rotateConfig.getValue()) {
                  PortSupport.Managers.ROTATION.setRotationSilentSync();
               }

               if (var5) {
                  var1.power = var2;
                  var1.powerPlacedByUs = true;
                  var1.powerPlacedAt = System.currentTimeMillis();
                  this.trackOwnedPlan(var1);
               }

               return var5;
            }
         }
      } else {
         return false;
      }
   }

   private BlockPos findPowerPosition(TwoBPiston.Plan var1) {
      for (Direction var6 : Direction.values()) {
         BlockPos var2 = var1.piston.offset(var6);
         if (this.isPowerClearOfCrystalPath(var1, var2) && mc.world.getBlockState(var2).getBlock() == Blocks.REDSTONE_BLOCK) {
            return var2;
         }
      }

      if (this.findSlot(Blocks.REDSTONE_BLOCK) < 0) {
         return null;
      } else {
         for (Direction var11 : Direction.values()) {
            BlockPos var7 = var1.piston.offset(var11);
            if (!var7.equals(var1.crystal)
               && !var7.equals(var1.impact)
               && this.isPowerClearOfCrystalPath(var1, var7)
               && this.isAir(var7)
               && this.inRange(var7)
               && PistonInteraction.INSTANCE.canPlace(var7, Blocks.REDSTONE_BLOCK)) {
               return var7;
            }
         }

         return null;
      }
   }

   private boolean isPowerClearOfCrystalPath(TwoBPiston.Plan var1, BlockPos var2) {
      Box var3 = this.getCrystalBox(var1);
      Vec3d var4 = var1.predicted == null ? new Vec3d(var1.impact.getX() + 0.5, var1.impact.getY(), var1.impact.getZ() + 0.5) : var1.predicted;
      Box var5 = var3.offset(var4.x - (var3.minX + var3.maxX) * 0.5, var4.y - var3.minY, var4.z - (var3.minZ + var3.maxZ) * 0.5);
      return !var3.union(var5).intersects(new Box(var2));
   }

   private Box getCrystalBox(BlockPos var1) {
      return EntityType.END_CRYSTAL.getDimensions().getBoxAt(var1.getX() + 0.5, var1.getY(), var1.getZ() + 0.5);
   }

   private Box getCrystalBox(TwoBPiston.Plan var1) {
      Entity var2 = var1.crystalEntityId < 0 ? null : mc.world.getEntityById(var1.crystalEntityId);
      return var2 instanceof EndCrystalEntity ? var2.getBoundingBox() : this.getCrystalBox(var1.crystal);
   }

   private boolean refreshCrystalMotion(TwoBPiston.Plan var1) {
      Entity var3 = var1.crystalEntityId < 0 ? null : mc.world.getEntityById(var1.crystalEntityId);
      if (!(var3 instanceof EndCrystalEntity)) {
         return false;
      } else {
         Direction var5 = var1.outward.getOpposite();
         Vec3d var2;
         if (var1.blockPushPattern) {
            PistonHandler var6 = new PistonHandler(mc.world, var1.piston, var5, true);
            if (!var6.calculatePush()) {
               return false;
            }

            var1.movedBlocks = List.copyOf(var6.getMovedBlocks());
            HashSet var7 = new HashSet<>(var1.movedBlocks);
            var7.addAll(var6.getBrokenBlocks());
            var2 = PistonMotion.stroke(var3.getBoundingBox(), var5, var2x -> this.collisionShapesExcept(var2x, var7));
         } else {
            var2 = this.simulateStroke(var3.getBoundingBox(), var5);
         }

         double var8 = var2.x * var5.getOffsetX() + var2.y * var5.getOffsetY() + var2.z * var5.getOffsetZ();
         if (!(var8 < 0.2) && var1.pistonHeadTouchesCrystal()) {
            var1.predicted = var3.getEntityPos().add(var2);
            var1.expectedPush = var8;
            var1.powerCandidate = null;
            return var1.reservePower() != null;
         } else {
            return false;
         }
      }
   }

   private boolean placeCrystal(TwoBPiston.Plan var1) {
      if (!this.isCrystalBase(var1.base) || !this.isAir(var1.crystal)) {
         return false;
      } else if (this.findCrystalAtStart(var1) != null) {
         return true;
      } else {
         int var2 = this.findSlot(Items.END_CRYSTAL);
         if (var2 < 0) {
            return false;
         } else {
            if (this.rotateConfig.getValue()) {
               float[] var3 = RotationUtil.getRotationsTo(mc.player.getEyePos(), var1.base.toCenterPos());
               PortSupport.Managers.ROTATION.setRotationSilent(var3[0], var3[1]);
            }

            boolean var6 = var2 != PortSupport.Managers.INVENTORY.getServerSlot();
            if (var6) {
               PortSupport.Managers.INVENTORY.setSlot(var2);
            }

            BlockHitResult var5 = new BlockHitResult(var1.base.toCenterPos(), Direction.UP, var1.base, false);
            PortSupport.Managers.NETWORK.sendSequencedPacket(var1x -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, var5, var1x));
            this.crystalPlacedAt = System.currentTimeMillis();
            var1.crystalAttempts++;
            var1.crystalPlacedByUs = true;
            mc.player.swingHand(Hand.MAIN_HAND);
            if (var6) {
               PortSupport.Managers.INVENTORY.syncToClient();
            }

            if (this.rotateConfig.getValue()) {
               PortSupport.Managers.ROTATION.setRotationSilentSync();
            }

            return true;
         }
      }
   }

   private EndCrystalEntity findCrystalAtStart(TwoBPiston.Plan var1) {
      double var2 = var1.crystal.getX() + 0.5;
      double var4 = var1.crystal.getY();
      double var6 = var1.crystal.getZ() + 0.5;
      EndCrystalEntity var8 = mc.world
         .getOtherEntities(null, new Box(var1.crystal).expand(0.75))
         .stream()
         .filter(var0 -> var0 instanceof EndCrystalEntity)
         .map(var0 -> (EndCrystalEntity)var0)
         .filter(var6x -> Math.abs(var6x.getX() - var2) < 0.4 && Math.abs(var6x.getY() - var4) < 0.6 && Math.abs(var6x.getZ() - var6) < 0.4)
         .findFirst()
         .orElse(null);
      if (var8 != null) {
         var1.crystalEntityId = var8.getId();
         if (var1.initialCrystalPosition == null) {
            var1.initialCrystalPosition = var8.getEntityPos();
         }
      }

      return var8;
   }

   private boolean breakPushedCrystal(TwoBPiston.Plan var1, PlayerEntity var2, long var3) {
      EndCrystalEntity var9 = this.findPushedCrystal(var1);
      if (var9 == null) {
         return false;
      } else if (this.moduleTick < this.nextCycleTick) {
         return false;
      } else if (!this.instantBreakConfig.getValue() && var3 - this.crystalPlacedAt < this.breakDelayConfig.getValue().intValue()) {
         return false;
      } else {
         Box var10 = var9.getBoundingBox();
         Vec3d var11 = mc.player.getEyePos();
         double var12 = mc.player.getAttributeValue(EntityAttributes.ENTITY_INTERACTION_RANGE);
         double var14 = Math.max(var10.minX, Math.min(var10.maxX, var11.x)) - var11.x;
         double var5;
         double var7;
         if (var14 * var14
               + (var7 = Math.max(var10.minY, Math.min(var10.maxY, var11.y)) - var11.y) * var7
               + (var5 = Math.max(var10.minZ, Math.min(var10.maxZ, var11.z)) - var11.z) * var5
            > var12 * var12) {
            return false;
         } else {
            this.attackedCrystalId = var9.getId();
            this.attackedAt = var3;
            this.attackCrystal(var9);
            return true;
         }
      }
   }

   private void attackCrystal(EndCrystalEntity var1) {
      if (this.rotateConfig.getValue()) {
         float[] var2 = RotationUtil.getRotationsTo(mc.player.getEyePos(), var1.getBoundingBox().getCenter());
         PortSupport.Managers.ROTATION.setRotationSilent(var2[0], var2[1]);
      }

      try {
         PortSupport.Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(var1, mc.player.isSneaking()));
         PortSupport.Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
      } finally {
         if (this.rotateConfig.getValue()) {
            PortSupport.Managers.ROTATION.setRotationSilentSync();
         }
      }
   }

   private boolean withinCrystalRange(EndCrystalEntity var1) {
      Box var2 = var1.getBoundingBox();
      Vec3d var3 = mc.player.getEyePos();
      double var4 = MathHelper.clamp(var3.x, var2.minX, var2.maxX) - var3.x;
      double var6 = MathHelper.clamp(var3.y, var2.minY, var2.maxY) - var3.y;
      double var8 = MathHelper.clamp(var3.z, var2.minZ, var2.maxZ) - var3.z;
      double var10 = mc.player.getAttributeValue(EntityAttributes.ENTITY_INTERACTION_RANGE);
      return var4 * var4 + var6 * var6 + var8 * var8 <= var10 * var10;
   }

   private EndCrystalEntity findPushedCrystal(TwoBPiston.Plan var1) {
      EndCrystalEntity var2 = this.cycleCrystal(var1);
      return var2 != null && this.pushed(var1, var2) ? var2 : null;
   }

   private boolean removePower(TwoBPiston.Plan var1, long var2) {
      if (this.cleanupBlock != null
         && mc.world.getBlockState(this.cleanupBlock).isOf(Blocks.REDSTONE_BLOCK)
         && !this.breakBlockVanilla(this.cleanupBlock, var2)) {
         return false;
      } else {
         for (Direction var7 : Direction.values()) {
            BlockPos var8 = var1.piston.offset(var7);
            if (mc.world.getBlockState(var8).isOf(Blocks.REDSTONE_BLOCK)) {
               if (!this.inRange(var8)) {
                  this.debug("Power outside mining range: " + var8);
                  return false;
               }

               var1.power = var8;
               return this.breakBlockVanilla(var8, var2);
            }
         }

         this.resetCleanupMining();
         return true;
      }
   }

   private boolean breakBlockVanilla(BlockPos var1, long var2) {
      if (this.isAir(var1)) {
         this.resetCleanupMining();
         return true;
      } else if (this.cleanupTick == this.moduleTick) {
         return false;
      } else {
         this.cleanupTick = this.moduleTick;
         if (!this.inRange(var1)) {
            this.debug("Mining out of range");
            return false;
         } else {
            if (this.cleanupBlock == null || !this.cleanupBlock.equals(var1)) {
               this.resetCleanupMining();
               this.cleanupBlock = var1.toImmutable();
            }

            BlockState var4 = mc.world.getBlockState(var1);
            int var5 = this.getCleanupTool(var4);
            if (var5 >= 0) {
               PortSupport.Managers.INVENTORY.setSlot(var5);
            }

            if (this.cleanupSentAt == 0L) {
               this.sendMining(Action.START_DESTROY_BLOCK, var1);
               this.cleanupSentAt = this.cleanupLastUpdate = var2;
               this.cleanupDamage = 0.0F;
               this.cleanupAttempts = 0;
            }

            if (this.cleanupAttempts == 0) {
               int var6 = mc.player.getInventory().getSelectedSlot();

               try {
                  if (var5 >= 0) {
                     mc.player.getInventory().setSelectedSlot(var5);
                  }

                  float var7 = var4.calcBlockBreakingDelta(mc.player, mc.world, var1);
                  if (var7 <= 0.0F) {
                     this.debug("Cannot mine this block with current tools");
                     return false;
                  }

                  this.cleanupDamage = this.cleanupDamage + PistonCycle.miningCredit(var7, this.observedTps);
               } finally {
                  mc.player.getInventory().setSelectedSlot(var6);
               }

               if (this.cleanupDamage >= 1.0F || mc.player.isCreative()) {
                  this.sendMining(Action.STOP_DESTROY_BLOCK, var1);
                  PortSupport.Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                  this.cleanupAttempts = 1;
                  this.cleanupLastUpdate = var2;
                  PortSupport.Managers.INVENTORY.syncToClient();
               }

               return false;
            } else if (var2 - this.cleanupLastUpdate >= Math.max(150L, 100L + PortSupport.Managers.NETWORK.getClientLatency() * 2L)) {
               this.sendMining(Action.ABORT_DESTROY_BLOCK, var1);
               this.cleanupSentAt = 0L;
               this.cleanupDamage = 0.0F;
               this.cleanupAttempts = 0;
               this.debug("Mining confirmation pending: retry");
            }

            return false;
         }
      }
   }

   private void sendMining(Action var1, BlockPos var2) {
      PendingUpdateManager var3 = ((PendingUpdatesAccess)mc.world).chimera$getPendingUpdates().incrementSequence();

      try {
         PortSupport.Managers.NETWORK.sendQuietPacket(new PlayerActionC2SPacket(var1, var2, Direction.UP, var3.getSequence()));
      } catch (Throwable var7) {
         if (var3 != null) {
            try {
               var3.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (var3 != null) {
         var3.close();
      }
   }

   private int getCleanupTool(BlockState var1) {
      return PistonTools.getBestToolNoFallback(var1);
   }

   private void resetCleanupMining() {
      if (this.cleanupBlock != null && mc.player != null) {
         if (mc.world != null && this.cycleWorld == mc.world && this.cleanupSentAt != 0L && !this.isAir(this.cleanupBlock)) {
            this.sendMining(Action.ABORT_DESTROY_BLOCK, this.cleanupBlock);
         }

         PortSupport.Managers.INVENTORY.syncToClient();
      }

      this.cleanupSentAt = 0L;
      this.cleanupLastUpdate = 0L;
      this.cleanupDamage = 0.0F;
      this.cleanupAttempts = 0;
      this.cleanupBlock = null;
   }

   private EndCrystalEntity findCrystalNear(BlockPos var1, double var2) {
      return mc.world
         .getOtherEntities(null, new Box(var1).expand(var2))
         .stream()
         .filter(var0 -> var0 instanceof EndCrystalEntity)
         .map(var0 -> (EndCrystalEntity)var0)
         .min(Comparator.comparingDouble(var1x -> var1x.squaredDistanceTo(var1.toCenterPos())))
         .orElse(null);
   }

   private void clearStalledSequence() {
      EndCrystalEntity var1;
      if (this.plan != null && (var1 = this.findCrystalNear(this.plan.impact, 1.6)) != null) {
         PortSupport.Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(var1, mc.player.isSneaking()));
         PortSupport.Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
      }

      this.rotatingPiston = null;
      this.rotatingLookDirection = null;
      this.crystalPlacedAt = 0L;
   }

   private float wrapDegrees(float var1) {
      var1 %= 360.0F;
      if (var1 >= 180.0F) {
         var1 -= 360.0F;
      }

      if (var1 < -180.0F) {
         var1 += 360.0F;
      }

      return var1;
   }

   private boolean isCrystalBase(BlockPos var1) {
      Block var2 = mc.world.getBlockState(var1).getBlock();
      return var2 == Blocks.OBSIDIAN || var2 == Blocks.BEDROCK;
   }

   private boolean canUseBase(BlockPos var1) {
      return this.isCrystalBase(var1)
         ? true
         : this.basePlaceConfig.getValue()
            && this.isAir(var1)
            && this.findSlot(Blocks.OBSIDIAN) >= 0
            && PistonInteraction.INSTANCE.canPlace(var1, Blocks.OBSIDIAN);
   }

   private boolean passesThroughSelf(TwoBPiston.Plan var1) {
      double var12 = mc.player.getX();
      double var14 = mc.player.getZ();
      double var16 = var1.piston.getX() + 0.5;
      double var18 = var1.piston.getZ() + 0.5;
      double var20 = var1.impact.getX() + 0.5;
      double var22 = var20 - var16;
      double var10;
      double var24 = var22 * var22 + (var10 = var1.impact.getZ() + 0.5 - var18) * var10;
      if (var24 < 1.0E-4) {
         return false;
      } else {
         double var26 = Math.max(0.0, Math.min(1.0, ((var12 - var16) * var22 + (var14 - var18) * var10) / var24));
         double var28 = var16 + var22 * var26;
         double var30 = var12 - var28;
         double var4 = var14 - (var18 + var10 * var26);
         return var30 * var30 + var4 * var4 < 0.81 && Math.abs(mc.player.getY() - var1.crystal.getY()) < 2.25;
      }
   }

   private boolean isAir(BlockPos var1) {
      return mc.world.getBlockState(var1).isReplaceable();
   }

   private boolean inRange(BlockPos var1) {
      return mc.player.squaredDistanceTo(var1.toCenterPos()) <= this.placeRangeConfig.getValue() * this.placeRangeConfig.getValue();
   }

   private int pistonSlot() {
      int var1 = this.findSlot(Blocks.PISTON);
      return var1 >= 0 ? var1 : this.findSlot(Blocks.STICKY_PISTON);
   }

   private int findSlot(Block var1) {
      for (int var2 = 0; var2 < 9; var2++) {
         ItemStack var5 = mc.player.getInventory().getStack(var2);
         BlockItem var3;
         Item var4;
         if (!var5.isEmpty() && (var4 = var5.getItem()) instanceof BlockItem && (var3 = (BlockItem)var4).getBlock() == var1) {
            return var2;
         }
      }

      return -1;
   }

   private int findSlot(Item var1) {
      for (int var2 = 0; var2 < 9; var2++) {
         ItemStack var3 = mc.player.getInventory().getStack(var2);
         if (!var3.isEmpty() && var3.getItem() == var1) {
            return var2;
         }
      }

      return -1;
   }

   private static final class MiningInfo {
      final BlockPos pos;
      final long time;
      final int repeats;
      final boolean instant;

      MiningInfo(BlockPos var1, long var2, int var4, boolean var5) {
         this.pos = var1;
         this.time = var2;
         this.repeats = var4;
         this.instant = var5;
      }
   }

   private static final class MiningKey {
      final int entityId;
      final BlockPos pos;

      MiningKey(int var1, BlockPos var2) {
         this.entityId = var1;
         this.pos = var2;
      }

      @Override
      public boolean equals(Object var1) {
         if (!(var1 instanceof TwoBPiston.MiningKey var2)) {
            return false;
         } else {
            return this.entityId != var2.entityId ? false : this.pos.equals(var2.pos);
         }
      }

      @Override
      public int hashCode() {
         return 31 * this.entityId + this.pos.hashCode();
      }
   }

   private final class Plan {
      final BlockPos base;
      final BlockPos crystal;
      final BlockPos piston;
      final BlockPos impact;
      final Direction outward;
      final boolean miningPattern;
      final boolean lowCeilingPattern;
      final boolean allowSolidImpact;
      final boolean exactTargetAxis;
      Vec3d predicted;
      double expectedPush = 1.0;
      boolean wallPattern;
      boolean blockPushPattern;
      List<BlockPos> movedBlocks = List.of();
      int crystalEntityId = -1;
      Vec3d initialCrystalPosition;
      BlockPos power;
      BlockPos powerCandidate;
      boolean powerPlacedByUs;
      boolean pistonPlacedByUs;
      boolean crystalPlacedByUs;
      boolean noDamageAtPush;
      long pistonRequestedAt;
      int crystalAttempts;
      long powerPlacedAt;

      Plan(
         BlockPos nullx,
         BlockPos nullxx,
         BlockPos nullxxx,
         BlockPos nullxxxx,
         Direction nullxxxxx,
         boolean nullxxxxxx,
         boolean nullxxxxxxx,
         boolean nullxxxxxxxx,
         boolean nullxxxxxxxxx
      ) {
         this.base = nullx;
         this.crystal = nullxx;
         this.piston = nullxxx;
         this.impact = nullxxxx;
         this.outward = nullxxxxx;
         this.miningPattern = nullxxxxxx;
         this.lowCeilingPattern = nullxxxxxxx;
         this.allowSolidImpact = nullxxxxxxxx;
         this.exactTargetAxis = nullxxxxxxxxx;
      }

      boolean targetStillOnAxis(PlayerEntity var1) {
         return (int)Math.floor(var1.getX()) == this.impact.getX() && (int)Math.floor(var1.getZ()) == this.impact.getZ();
      }

      boolean selectionValid() {
         return TwoBPiston.this.inRange(this.piston)
            && TwoBPiston.this.inRange(this.base)
            && TwoBPiston.this.findSlot(Items.END_CRYSTAL) >= 0
            && TwoBPiston.this.canUseBase(this.base)
            && TwoBPiston.this.isAir(this.crystal)
            && this.impactUsable()
            && this.pistonUsable()
            && this.canExtendPiston()
            && this.pistonHeadTouchesCrystal()
            && this.reservePower() != null
            && (!TwoBPiston.this.avoidSelfPathConfig.getValue() || !TwoBPiston.this.passesThroughSelf(this));
      }

      boolean activeValid() {
         if (TwoBPiston.this.inRange(this.piston)
            && TwoBPiston.this.inRange(this.base)
            && (!TwoBPiston.this.avoidSelfPathConfig.getValue() || !TwoBPiston.this.passesThroughSelf(this))) {
            if (TwoBPiston.this.sequencePhase == 3) {
               return this.pistonUsable() && this.impactUsable() && this.canExtendPiston();
            } else {
               return TwoBPiston.this.sequencePhase >= 4 ? this.pistonUsable() : this.selectionValid();
            }
         } else {
            return false;
         }
      }

      double distance() {
         return Globals.mc.player.squaredDistanceTo(this.piston.toCenterPos());
      }

      double score() {
         return (Globals.mc.world.getBlockState(this.piston).getBlock() == Blocks.PISTON ? -8.0 : 0.0)
            + (TwoBPiston.this.isCrystalBase(this.base) ? 0.0 : 25.0)
            + this.distance();
      }

      boolean hasAdjacentPower() {
         for (Direction var4 : Direction.values()) {
            if (Globals.mc.world.getBlockState(this.piston.offset(var4)).getBlock() == Blocks.REDSTONE_BLOCK) {
               return true;
            }
         }

         return false;
      }

      boolean impactUsable() {
         return this.allowSolidImpact || TwoBPiston.this.isAir(this.impact);
      }

      boolean pistonUsable() {
         BlockState var1 = Globals.mc.world.getBlockState(this.piston);
         return var1.isReplaceable() && TwoBPiston.this.pistonSlot() >= 0
            || var1.getBlock() instanceof PistonBlock && var1.get(FacingBlock.FACING) == this.outward.getOpposite();
      }

      boolean pistonPresentAndFacing() {
         BlockState var1 = Globals.mc.world.getBlockState(this.piston);
         return var1.getBlock() instanceof PistonBlock && var1.get(FacingBlock.FACING) == this.outward.getOpposite();
      }

      boolean pistonConfirmed() {
         return this.pistonPresentAndFacing()
            && (
               this.pistonRequestedAt == 0L
                  || TwoBPiston.this.acknowledgedAt.getOrDefault(this.piston, 0L) >= this.pistonRequestedAt
                     && TwoBPiston.this.acknowledgedBlocks.get(this.piston).getBlock() instanceof PistonBlock
            );
      }

      boolean retracted() {
         BlockState var1 = Util.mc.world.getBlockState(this.piston);
         return !(var1.getBlock() instanceof PistonBlock) ? true : !(Boolean)var1.get(PistonBlock.EXTENDED) && this.canExtendPiston();
      }

      boolean canExtendPiston() {
         BlockPos var1 = this.piston.offset(this.outward.getOpposite());
         if (this.blockPushPattern) {
            return new PistonHandler(Globals.mc.world, this.piston, this.outward.getOpposite(), true).calculatePush();
         } else {
            BlockState var2 = Util.mc.world.getBlockState(var1);
            return TwoBPiston.this.isAir(var1) || this.pistonPresentAndFacing() && (var2.isOf(Blocks.PISTON_HEAD) || var2.isOf(Blocks.MOVING_PISTON));
         }
      }

      boolean pistonHeadTouchesCrystal() {
         if (!this.blockPushPattern) {
            BlockPos var9 = this.piston.offset(this.outward.getOpposite());
            return TwoBPiston.this.getCrystalBox(this).intersects(new Box(var9));
         } else {
            Box var1 = TwoBPiston.this.getCrystalBox(this);
            Direction var2 = this.outward.getOpposite();

            for (BlockPos var4 : this.movedBlocks) {
               VoxelShape var5 = Globals.mc.world.getBlockState(var4).getCollisionShape(Globals.mc.world, var4);

               for (Box var7 : var5.getBoundingBoxes()) {
                  Box var8 = var7.offset(var4);
                  if (var8.union(var8.offset(var2.getOffsetX(), var2.getOffsetY(), var2.getOffsetZ())).intersects(var1)) {
                     return true;
                  }
               }
            }

            return false;
         }
      }

      boolean usesPreCrystalPower() {
         return this.powerCandidate != null && !TwoBPiston.this.isPowerClearOfCrystalPath(this, this.powerCandidate);
      }

      void prepareNextCycle() {
         this.crystalEntityId = -1;
         this.initialCrystalPosition = null;
         this.power = null;
         this.powerPlacedByUs = false;
         this.crystalPlacedByUs = false;
         this.noDamageAtPush = false;
         this.crystalAttempts = 0;
         this.powerPlacedAt = 0L;
      }

      BlockPos reservePower() {
         if (this.powerCandidate != null) {
            BlockState var1 = Globals.mc.world.getBlockState(this.powerCandidate);
            if (var1.getBlock() == Blocks.REDSTONE_BLOCK) {
               return this.powerCandidate;
            }

            if (var1.isReplaceable()
               && TwoBPiston.this.inRange(this.powerCandidate)
               && PistonInteraction.INSTANCE.canPlace(this.powerCandidate, Blocks.REDSTONE_BLOCK)) {
               return this.powerCandidate;
            }
         }

         this.powerCandidate = TwoBPiston.this.findPowerPosition(this);
         return this.powerCandidate;
      }
   }
}
