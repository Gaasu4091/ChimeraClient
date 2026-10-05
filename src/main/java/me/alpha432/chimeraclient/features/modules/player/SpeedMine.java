package me.alpha432.chimeraclient.features.modules.player;

import java.awt.Color;
import me.alpha432.chimeraclient.event.impl.entity.player.TickEvent;
import me.alpha432.chimeraclient.event.impl.network.PacketEvent;
import me.alpha432.chimeraclient.event.impl.render.Render3DEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.util.EnchantmentUtil;
import me.alpha432.chimeraclient.util.render.RenderUtil;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public class SpeedMine extends Module {
   private final Setting<Double> speed = this.register(new Setting<>("Speed", 1.0, 0.7, 1.0));
   private final Setting<Double> range = this.register(new Setting<>("Range", 5.0, 1.0, 7.0));
   private final Setting<Boolean> doubleMine = this.register(new Setting<>("DoubleMine", false));
   private final Setting<SpeedMine.SwapMode> swap = this.register(new Setting<>("Swap", SpeedMine.SwapMode.NORMAL));
   private final Setting<Boolean> swing = this.register(new Setting<>("Swing", true));
   private final Setting<Boolean> instant = this.register(new Setting<>("Instant", true));
   private final Setting<Integer> instantDelay = this.register(new Setting<>("InstantDelay", 0, 0, 1000));
   private final Setting<Boolean> grim = this.register(new Setting<>("Grim", false));
   private final Setting<Boolean> simulate = this.register(new Setting<>("Simulate", true));
   private SpeedMine.BlockBreakingTask currentTask = null;
   private SpeedMine.BlockBreakingTask doubleMineTask = null;
   private long instantRemineTimer = -1L;
   private boolean wasAttacking = false;
   private long instantRemineResetTimer = -1L;
   private int doubleMineOriginalSlot = -1;

   public SpeedMine() {
      super("SpeedMine", "Packet-based fast block breaking.", Module.Category.PLAYER);
   }

   @Override
   public void onDisable() {
      if (this.currentTask != null && this.currentTask.isStarted()) {
         this.abortMining(this.currentTask);
      }

      this.currentTask = null;
      this.doubleMineTask = null;
      this.instantRemineTimer = -1L;
      this.instantRemineResetTimer = -1L;
      this.wasAttacking = false;
      this.doubleMineOriginalSlot = -1;
   }

   @Subscribe
   public void onTick(TickEvent event) {
      if (!nullCheck()) {
         if (!mc.player.isUsingItem() || !mc.player.getActiveItem().contains(DataComponentTypes.FOOD)) {
            this.handleStartBreaking();
            if (this.currentTask != null) {
               this.handleMiningTick(this.currentTask);
            }

            if (this.doubleMineTask != null) {
               this.handleDoubleMine(this.doubleMineTask);
            }
         }
      }
   }

   private void handleStartBreaking() {
      boolean attacking = mc.options.attackKey.isPressed();
      boolean justPressed = attacking && !this.wasAttacking;
      this.wasAttacking = attacking;
      if (attacking) {
         if (mc.crosshairTarget instanceof BlockHitResult bhr) {
            if (mc.world != null) {
               BlockPos pos = bhr.getBlockPos();
               Direction face = bhr.getSide();
               Vec3d eyePos = mc.player.getEyePos();
               Vec3d closest = this.clampToBox(eyePos, new Box(pos));
               if (!(eyePos.distanceTo(closest) > this.range.getValue())) {
                  BlockState state = mc.world.getBlockState(pos);
                  if (!state.isAir() && !(state.getHardness(mc.world, pos) < 0.0F)) {
                     if (this.doubleMineTask == null || !this.doubleMineTask.getBlockPos().equals(pos)) {
                        if (this.currentTask == null || !this.currentTask.getBlockPos().equals(pos)) {
                           if (justPressed || this.currentTask != null) {
                              if (this.swing.getValue()) {
                                 mc.player.swingHand(Hand.MAIN_HAND);
                              }

                              if (this.currentTask != null) {
                                 if (this.currentTask.getBlockPos().equals(pos)) {
                                    return;
                                 }

                                 if (this.doubleMine.getValue() && this.doubleMineTask == null) {
                                    this.doubleMineTask = new SpeedMine.BlockBreakingTask(
                                       this.currentTask.getBlockPos(), this.currentTask.getFacing(), this.currentTask.getTargetSpeed()
                                    );
                                    this.doubleMineTask.setProgress(this.currentTask.getProgress());
                                    this.doubleMineTask.markStarted();
                                 }

                                 this.abortMining(this.currentTask);
                              }

                              this.currentTask = new SpeedMine.BlockBreakingTask(pos, face, this.speed.getValue().floatValue());
                              this.instantRemineTimer = System.currentTimeMillis();
                              this.instantRemineResetTimer = System.currentTimeMillis();
                              this.startMining(this.currentTask);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Subscribe
   @Override
   public void onRender3D(Render3DEvent event) {
      if (this.currentTask != null) {
         this.renderProgress(event, this.currentTask);
      }

      if (this.doubleMine.getValue()) {
         if (this.doubleMineTask != null) {
            this.renderProgress(event, this.doubleMineTask);
         }
      }
   }

   @Subscribe
   public void onPacketReceive(PacketEvent.Receive event) {
      if (this.currentTask != null) {
         if (event.getPacket() instanceof BlockUpdateS2CPacket blockPacket) {
            BlockPos pos = blockPacket.getPos();
            if (pos.equals(this.currentTask.getBlockPos())) {
               this.currentTask.markBroken();
            }
         }
      }
   }

   private void handleMiningTick(SpeedMine.BlockBreakingTask task) {
      Vec3d eyePos = mc.player.getEyePos();
      Box blockBox = new Box(task.getBlockPos());
      Vec3d closest = this.clampToBox(eyePos, blockBox);
      if (eyePos.distanceTo(closest) > this.range.getValue()) {
         this.abortMining(task);
         this.currentTask = null;
      } else {
         if (task.getBlockState().isAir()) {
            if (this.instant.getValue()) {
               task.markInstantRemine();
               task.setProgress(1.0F);
            } else {
               task.resetProgress();
            }
         }

         if (this.swing.getValue()) {
            mc.player.swingHand(Hand.MAIN_HAND);
         }

         float damageDelta = this.calculateBlockDamage(task.getStartState(), task.getBlockPos());
         if (task.incrementProgress(damageDelta) >= task.getTargetSpeed() || task.isInstantRemine()) {
            this.finishMining(task);
         }
      }
   }

   private void handleDoubleMine(SpeedMine.BlockBreakingTask task) {
      if (!this.doubleMine.getValue()) {
         this.doubleMineTask = null;
      } else if (task.doublemineHoldTicks >= 3) {
         this.doubleMineTask = null;
         if (this.doubleMineOriginalSlot != -1) {
            mc.player.getInventory().selectedSlot = this.doubleMineOriginalSlot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(this.doubleMineOriginalSlot));
            this.doubleMineOriginalSlot = -1;
         }
      } else {
         Vec3d eyePos = mc.player.getEyePos();
         Box blockBox = new Box(task.getBlockPos());
         if (!(eyePos.distanceTo(this.clampToBox(eyePos, blockBox)) > this.range.getValue()) && !task.getBlockState().isAir()) {
            float damageDelta = this.calculateBlockDamage(task.getBlockState(), task.getBlockPos());
            if (task.incrementProgress(damageDelta) >= task.getTargetSpeed()) {
               if (mc.player.isUsingItem()) {
                  return;
               }

               if (this.doubleMineOriginalSlot == -1) {
                  this.doubleMineOriginalSlot = mc.player.getInventory().selectedSlot;
               }

               int bestSlot = this.getBestToolSlot(task.getStartState());
               if (mc.player.getInventory().selectedSlot != bestSlot) {
                  mc.player.getInventory().selectedSlot = bestSlot;
                  mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(bestSlot));
               }

               if (task.doublemineHoldTicks == 0) {
                  task.markStarted();
                  if (this.grim.getValue()) {
                     this.sendPacket(Action.STOP_DESTROY_BLOCK, task);
                  }

                  if (this.swing.getValue()) {
                     mc.player.swingHand(Hand.MAIN_HAND);
                  }

                  this.sendPacket(Action.STOP_DESTROY_BLOCK, task);
                  if (this.simulate.getValue()) {
                     mc.world.breakBlock(task.getBlockPos(), false, mc.player, 512);
                  }
               }

               task.doublemineHoldTicks++;
            }
         } else {
            this.doubleMineTask = null;
         }
      }
   }

   private void startMining(SpeedMine.BlockBreakingTask task) {
      if (!task.getBlockState().isAir()) {
         if (this.grim.getValue()) {
            this.sendPacket(Action.STOP_DESTROY_BLOCK, task);
         }

         this.sendPacket(Action.START_DESTROY_BLOCK, task);
         this.sendPacket(Action.ABORT_DESTROY_BLOCK, task);
         task.markStarted();
      }
   }

   private void abortMining(SpeedMine.BlockBreakingTask task) {
      if (task.isStarted()) {
         if (!task.getBlockState().isAir() && !task.isInstantRemine() && !(task.getProgress() >= 1.0F)) {
            if (this.grim.getValue()) {
               this.sendPacket(Action.STOP_DESTROY_BLOCK, task);
            }

            if (this.swing.getValue()) {
               mc.player.swingHand(Hand.MAIN_HAND);
            }

            this.sendPacket(Action.ABORT_DESTROY_BLOCK, task);
         }
      }
   }

   private void finishMining(SpeedMine.BlockBreakingTask task) {
      if (task.isStarted()) {
         if (task.brokenCount != task.lastBrokenCount) {
            this.instantRemineResetTimer = System.currentTimeMillis();
         }

         if (!task.isInstantRemine() || this.elapsedMs(this.instantRemineTimer, this.instantDelay.getValue().intValue())) {
            if (!task.isInstantRemine() || !task.getBlockState().isAir() || !this.elapsedMs(this.instantRemineResetTimer, 250L)) {
               int orig = mc.player.getInventory().selectedSlot;
               int best = this.getBestToolSlot(task.getStartState());
               boolean needSwap = this.swap.getValue() != SpeedMine.SwapMode.NONE && best != orig;
               if (needSwap) {
                  if (this.swap.getValue() == SpeedMine.SwapMode.NORMAL) {
                     mc.player.getInventory().selectedSlot = best;
                  }

                  mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(best));
               }

               if (this.grim.getValue()) {
                  this.sendPacket(Action.STOP_DESTROY_BLOCK, task);
               }

               if (this.swing.getValue()) {
                  mc.player.swingHand(Hand.MAIN_HAND);
               }

               this.sendPacket(Action.STOP_DESTROY_BLOCK, task);
               if (this.simulate.getValue()) {
                  mc.world.breakBlock(task.getBlockPos(), false, mc.player, 512);
               }

               task.markLastBroken();
               if (needSwap && this.swap.getValue() == SpeedMine.SwapMode.SILENT) {
                  mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(orig));
               }

               if (task.isInstantRemine()) {
                  this.instantRemineTimer = System.currentTimeMillis();
               }
            }
         }
      }
   }

   private float calculateBlockDamage(BlockState state, BlockPos pos) {
      if (mc.world != null && mc.player != null) {
         float hardness = state.getHardness(mc.world, pos);
         if (hardness < 0.0F) {
            return 0.0F;
         } else {
            int divisor = this.canHarvest(state) ? 30 : 100;
            return this.getMiningSpeed(state) / hardness / divisor;
         }
      } else {
         return 0.0F;
      }
   }

   private boolean canHarvest(BlockState state) {
      if (!state.isToolRequired()) {
         return true;
      } else {
         ItemStack held = this.swap.getValue() != SpeedMine.SwapMode.NONE
            ? mc.player.getInventory().getStack(this.getBestToolSlot(state))
            : mc.player.getMainHandStack();
         return held.isSuitableFor(state);
      }
   }

   private float getMiningSpeed(BlockState state) {
      int slot = this.swap.getValue() != SpeedMine.SwapMode.NONE ? this.getBestToolSlot(state) : mc.player.getInventory().selectedSlot;
      ItemStack stack = mc.player.getInventory().getStack(slot);
      float sp = stack.getMiningSpeedMultiplier(state);
      if (sp > 1.0F) {
         int eff = EnchantmentUtil.getLevel(Enchantments.EFFICIENCY, stack);
         if (eff > 0 && !stack.isEmpty()) {
            sp += eff * eff + 1;
         }
      }

      if (StatusEffectUtil.hasHaste(mc.player)) {
         int amp = StatusEffectUtil.getHasteAmplifier(mc.player) + 1;
         sp *= 1.0F + amp * 0.2F;
      }

      if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
         float mult = switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
            case 0 -> 0.3F;
            case 1 -> 0.09F;
            case 2 -> 0.0027F;
            default -> 8.1E-4F;
         };
         sp *= mult;
      }

      boolean noAqua = EnchantmentUtil.getLevel(Enchantments.AQUA_AFFINITY, mc.player.getEquippedStack(EquipmentSlot.HEAD)) == 0;
      if (mc.player.isSubmergedIn(FluidTags.WATER) && noAqua) {
         sp /= 5.0F;
      }

      if (!mc.player.isOnGround()) {
         sp /= 5.0F;
      }

      return sp;
   }

   private int getBestToolSlot(BlockState state) {
      int best = mc.player.getInventory().selectedSlot;
      float bestSpeed = 1.0F;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty()) {
            float sp = this.getToolSpeed(stack, state);
            if (sp > bestSpeed) {
               bestSpeed = sp;
               best = i;
            }
         }
      }

      return best;
   }

   private float getToolSpeed(ItemStack stack, BlockState state) {
      if (!stack.isSuitableFor(state)) {
         return 1.0F;
      } else {
         int eff = EnchantmentUtil.getLevel(Enchantments.EFFICIENCY, stack);
         return stack.getMiningSpeedMultiplier(state) * (1.0F + eff * 0.2F);
      }
   }

   private void renderProgress(Render3DEvent event, SpeedMine.BlockBreakingTask task) {
      BlockPos pos = task.getBlockPos();
      if (mc.world != null && !mc.world.getBlockState(pos).isAir()) {
         VoxelShape shape = task.isInstantRemine() ? VoxelShapes.fullCube() : task.getStartState().getOutlineShape(mc.world, pos);
         if (shape.isEmpty()) {
            shape = VoxelShapes.fullCube();
         }

         Box bb = shape.getBoundingBox();
         Box worldBox = new Box(
            pos.getX() + bb.minX, pos.getY() + bb.minY, pos.getZ() + bb.minZ, pos.getX() + bb.maxX, pos.getY() + bb.maxY, pos.getZ() + bb.maxZ
         );
         Vec3d center = worldBox.getCenter();
         float delta = event.getDelta();
         float cur = task.getProgress();
         float prev = task.getPreviousProgress();
         float interp = prev + (cur - prev) * delta;
         float scale = MathHelper.clamp(interp / task.getTargetSpeed(), 0.0F, 1.0F);
         if (!(scale <= 0.0F)) {
            double dx = (bb.maxX - bb.minX) / 2.0;
            double dy = (bb.maxY - bb.minY) / 2.0;
            double dz = (bb.maxZ - bb.minZ) / 2.0;
            Box box = new Box(center, center).expand(dx * scale, dy * scale, dz * scale);
            float t = MathHelper.clamp((scale - 0.5F) * 2.0F, 0.0F, 1.0F);
            int r = (int)(200.0F * (1.0F - t));
            int g = (int)(200.0F * t);
            this.drawBox(event.getMatrix(), box, new Color(r, g, 0, 200), 1.5F);
         }
      }
   }

   private void drawBox(MatrixStack stack, Box box, Color color, float width) {
      RenderUtil.drawBox(stack, box, color, width);
   }

   private void sendPacket(Action action, SpeedMine.BlockBreakingTask task) {
      mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(action, task.getBlockPos(), task.getFacing()));
   }

   private boolean elapsedMs(long startMs, long ms) {
      return startMs < 0L || System.currentTimeMillis() - startMs >= ms;
   }

   private Vec3d clampToBox(Vec3d point, Box box) {
      return new Vec3d(
         MathHelper.clamp(point.x, box.minX, box.maxX), MathHelper.clamp(point.y, box.minY, box.maxY), MathHelper.clamp(point.z, box.minZ, box.maxZ)
      );
   }

   public static class BlockBreakingTask {
      private final BlockPos blockPos;
      private final Direction facing;
      private final float targetSpeed;
      private BlockState startState;
      private float progress;
      private float previousProgress;
      private boolean instantRemine;
      private boolean started;
      public int doublemineHoldTicks = 0;
      int brokenCount = 0;
      int lastBrokenCount = -1;
      private int ticksMining = 0;

      public BlockBreakingTask(BlockPos pos, Direction face, float speed) {
         this.blockPos = pos;
         this.facing = face;
         this.targetSpeed = speed;
         this.startState = Util.mc.world.getBlockState(pos);
      }

      public BlockPos getBlockPos() {
         return this.blockPos;
      }

      public Direction getFacing() {
         return this.facing;
      }

      public float getTargetSpeed() {
         return this.targetSpeed;
      }

      public BlockState getBlockState() {
         return Util.mc.world.getBlockState(this.blockPos);
      }

      public BlockState getStartState() {
         BlockState cur = this.getBlockState();
         if (!cur.isAir() && cur.getBlock() != this.startState.getBlock()) {
            this.startState = cur;
         }

         return this.startState;
      }

      public boolean isStarted() {
         return this.started;
      }

      public void markStarted() {
         this.started = true;
      }

      public float getProgress() {
         return this.progress;
      }

      public float getPreviousProgress() {
         return this.previousProgress;
      }

      public float incrementProgress(float delta) {
         this.previousProgress = this.progress;
         return this.progress += delta;
      }

      public void setProgress(float value) {
         this.previousProgress = this.progress;
         this.progress = value;
      }

      public void resetProgress() {
         this.progress = 0.0F;
         this.previousProgress = 0.0F;
         this.instantRemine = false;
      }

      public boolean isInstantRemine() {
         return this.instantRemine;
      }

      public void markInstantRemine() {
         this.instantRemine = true;
      }

      public void markBroken() {
         this.brokenCount++;
      }

      public void markLastBroken() {
         this.lastBrokenCount = this.brokenCount;
      }

      public void incrementTicksMining() {
         this.ticksMining++;
      }

      public boolean hasMinedFor(int ticks) {
         return this.ticksMining >= ticks;
      }
   }

   public static enum SwapMode {
      NONE,
      NORMAL,
      SILENT;
   }
}
