package me.alpha432.chimeraclient.features.modules.combat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import me.alpha432.chimeraclient.event.impl.network.PacketEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.ActionResult.SwingSource;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AntiPA extends Module {
   private final Setting<Double> range = this.num("Range", 5.0, 1.0, 7.0);
   private final Setting<Boolean> silent = this.bool("Silent", true);
   private final Setting<Boolean> breakCrystal = this.bool("BreakCrystal", true);
   private final Setting<Double> breakDelay = this.num("BreakDelay", 0.0, 0.0, 20.0);
   private final Setting<Double> placeDelay = this.num("PlaceDelay", 0.0, 0.0, 20.0);
   private final Setting<Boolean> spam = this.bool("Spam", true);
   private final Setting<Double> spamDelay = this.num("SpamDelay", 1.0, 1.0, 20.0);
   private final Setting<Double> blocksPerTick = this.num("BlocksPerTick", 1.0, 1.0, 10.0);
   private final Setting<Double> timeoutTicks = this.num("TimeoutTicks", 20.0, 5.0, 40.0);
   private final List<AntiPA.ReplaceTask> replaceTasks = new ArrayList<>();
   private final Set<BlockPos> ownCrystalPos = new HashSet<>();
   private final Set<BlockPos> confirmedOwnPistons = new HashSet<>();
   private static final long OWN_TIMEOUT_MS = 3000L;
   private final Map<BlockPos, AntiPA.OwnEntry> ownCandidates = new ConcurrentHashMap<>();
   private volatile boolean placingObsidian = false;
   private volatile int pistonSlotMask = 0;
   private int placeTicks = 0;
   private int breakTicks = 0;

   public AntiPA() {
      super("AntiPA", "Instantly breaks crystals touching any pistons and places obsidian", Module.Category.COMBAT);
   }

   @Override
   public void onDisable() {
      this.replaceTasks.clear();
      this.ownCrystalPos.clear();
      this.confirmedOwnPistons.clear();
      this.ownCandidates.clear();
      this.placingObsidian = false;
      this.pistonSlotMask = 0;
      this.placeTicks = 0;
      this.breakTicks = 0;
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         this.placeTicks++;
         this.breakTicks++;
         if (!mc.player.isUsingItem() || !mc.player.getActiveItem().contains(DataComponentTypes.FOOD)) {
            this.pistonSlotMask = this.buildPistonSlotMask();
            this.trackCrosshairCandidate();
            this.proactivelyRegisterNearbyPlacements();
            long now = System.currentTimeMillis();
            this.ownCandidates.entrySet().removeIf(e -> now - e.getValue().registeredAt() > 3000L);
            if (this.breakCrystal.getValue()) {
               int currentBreakDelay = this.spam.getValue() ? Math.max(0, this.spamDelay.getValue().intValue() - 1) : this.breakDelay.getValue().intValue();
               if (this.breakTicks > currentBreakDelay && this.handleCrystalBreaker()) {
                  this.breakTicks = 0;
               }
            }

            this.processReplaceTasks();
         }
      }
   }

   @Subscribe
   private void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck()) {
         if (event.getPacket() instanceof PlayerInteractBlockC2SPacket pkt) {
            boolean holdingCrystal = mc.player.getMainHandStack().isOf(Items.END_CRYSTAL) || mc.player.getOffHandStack().isOf(Items.END_CRYSTAL);
            if (holdingCrystal) {
               BlockPos base = pkt.getBlockHitResult().getBlockPos();
               this.ownCrystalPos.add(base.up());
            }

            if (!this.placingObsidian && this.pistonSlotMask != 0) {
               BlockPos placed = pkt.getBlockHitResult().getBlockPos().offset(pkt.getBlockHitResult().getSide());
               this.ownCandidates.putIfAbsent(placed, new AntiPA.OwnEntry(System.currentTimeMillis(), null));
            }
         }
      }
   }

   @Subscribe
   private void onPacketReceive(PacketEvent.Receive event) {
      if (!nullCheck()) {
         if (event.getPacket() instanceof BlockUpdateS2CPacket pkt) {
            this.handleBlockUpdate(pkt.getPos(), pkt.getState());
         } else if (event.getPacket() instanceof ChunkDeltaUpdateS2CPacket pkt) {
            pkt.visitUpdates(this::handleBlockUpdate);
         }
      }
   }

   private void handleBlockUpdate(BlockPos pos, BlockState state) {
      if (mc.player != null) {
         if (this.isPiston(state)) {
            if (this.confirmedOwnPistons.contains(pos)) {
               return;
            }

            boolean isOwn = false;
            long now = System.currentTimeMillis();
            List<BlockPos> checkTargets = new ArrayList<>();
            checkTargets.add(pos);

            for (Direction d : Direction.values()) {
               checkTargets.add(pos.offset(d));
            }

            for (BlockPos cp : checkTargets) {
               AntiPA.OwnEntry entry = this.ownCandidates.get(cp);
               if (entry != null && now - entry.registeredAt() <= 3000L) {
                  isOwn = true;
                  break;
               }
            }

            if (isOwn) {
               this.confirmedOwnPistons.add(pos);
            }
         } else {
            this.confirmedOwnPistons.remove(pos);
         }
      }
   }

   private boolean handleCrystalBreaker() {
      if (mc.player != null && mc.world != null) {
         double r = this.range.getValue();
         Box checkArea = mc.player.getBoundingBox().expand(r);

         for (EndCrystalEntity crystal : mc.world.getNonSpectatingEntities(EndCrystalEntity.class, checkArea)) {
            if (!(this.getBoxDistance(mc.player.getEyePos(), crystal.getEntityPos()) > r)) {
               BlockPos crystalBase = BlockPos.ofFloored(crystal.getX(), crystal.getY() - 1.0, crystal.getZ());
               if (!this.ownCrystalPos.contains(crystalBase) && !this.ownCrystalPos.contains(crystal.getBlockPos())) {
                  Box crystalBox = crystal.getBoundingBox();
                  BlockPos targetPiston = null;
                  BlockPos crystalPos = BlockPos.ofFloored(crystal.getEntityPos());

                  for (int x = -2; x <= 2; x++) {
                     for (int y = -2; y <= 2; y++) {
                        for (int z = -2; z <= 2; z++) {
                           BlockPos pos = crystalPos.add(x, y, z);
                           BlockState state = mc.world.getBlockState(pos);
                           if (this.isPiston(state) && !this.confirmedOwnPistons.contains(pos)) {
                              Box pistonBox = new Box(pos);
                              if (state.contains(Properties.FACING)) {
                                 Direction facing = (Direction)state.get(Properties.FACING);
                                 Box pistonHeadBox = new Box(pos.offset(facing));
                                 pistonBox = pistonBox.union(pistonHeadBox);
                              }

                              if (crystalBox.intersects(pistonBox)) {
                                 targetPiston = pos;
                                 break;
                              }
                           }
                        }

                        if (targetPiston != null) {
                           break;
                        }
                     }

                     if (targetPiston != null) {
                        break;
                     }
                  }

                  if (targetPiston != null) {
                     BlockPos feet = mc.player.getBlockPos();

                     for (Direction dir : Direction.values()) {
                        BlockPos nb = targetPiston.offset(dir);
                        if (!nb.equals(feet) && !nb.equals(feet.up())) {
                           boolean alreadyInQueue = this.replaceTasks.stream().anyMatch(t -> t.pos.equals(nb));
                           if (!alreadyInQueue) {
                              this.replaceTasks.add(new AntiPA.ReplaceTask(nb, true));
                           }
                        }
                     }

                     float preYaw = mc.player.getYaw();
                     float prePitch = mc.player.getPitch();
                     float[] look = this.calcLookAt(crystal.getEntityPos());
                     if (this.silent.getValue()) {
                        this.sendLookPacket(look[0], look[1]);
                     }

                     mc.interactionManager.attackEntity(mc.player, crystal);
                     mc.player.swingHand(Hand.MAIN_HAND);
                     if (this.silent.getValue()) {
                        this.sendLookPacket(preYaw, prePitch);
                     }

                     this.replaceTasks.add(new AntiPA.ReplaceTask(crystalPos, false));
                     return true;
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private void processReplaceTasks() {
      if (!this.replaceTasks.isEmpty()) {
         int currentPlaceDelay = this.spam.getValue() ? Math.max(0, this.spamDelay.getValue().intValue() - 1) : this.placeDelay.getValue().intValue();
         if (this.placeTicks > currentPlaceDelay) {
            int placementsThisTick = 0;
            int maxPlacements = this.blocksPerTick.getValue().intValue();
            Iterator<AntiPA.ReplaceTask> it = this.replaceTasks.iterator();

            while (it.hasNext() && placementsThisTick < maxPlacements) {
               AntiPA.ReplaceTask task = it.next();
               task.ticksWaited++;
               BlockState state = mc.world.getBlockState(task.pos);
               if (state.isReplaceable()) {
                  if (this.placeObsidian(task.pos)) {
                     placementsThisTick++;
                     it.remove();
                  }
               } else if (task.ticksWaited >= this.timeoutTicks.getValue().intValue()) {
                  it.remove();
               }
            }

            if (placementsThisTick > 0) {
               this.placeTicks = 0;
            }
         }
      }
   }

   private boolean placeObsidian(BlockPos pos) {
      if (mc.player != null && mc.world != null) {
         int slot = this.findObsidianInHotbar();
         if (slot == -1) {
            return false;
         } else {
            Direction clickSide = Direction.UP;
            BlockPos clickTarget = pos.down();

            for (Direction dir : Direction.values()) {
               BlockPos nb = pos.offset(dir);
               if (!mc.world.getBlockState(nb).isReplaceable()) {
                  clickSide = dir.getOpposite();
                  clickTarget = nb;
                  break;
               }
            }

            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), clickSide, clickTarget, false);
            int original = mc.player.getInventory().selectedSlot;
            boolean needSwap = slot != original;
            if (needSwap) {
               mc.player.getInventory().selectedSlot = slot;
               mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            }

            this.placingObsidian = true;
            float preYaw = mc.player.getYaw();
            float prePitch = mc.player.getPitch();
            float[] look = this.calcLookAt(Vec3d.ofCenter(pos));
            this.sendLookPacket(look[0], look[1]);
            if (mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit) instanceof Success success && success.swingSource() != SwingSource.NONE) {
               mc.player.swingHand(Hand.MAIN_HAND);
            }

            this.sendLookPacket(preYaw, prePitch);
            this.placingObsidian = false;
            if (needSwap && this.silent.getValue()) {
               mc.player.getInventory().selectedSlot = original;
               mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(original));
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private void trackCrosshairCandidate() {
      if (this.holdingPiston()) {
         if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult)mc.crosshairTarget;
            BlockPos candidate = bhr.getBlockPos().offset(bhr.getSide());
            this.ownCandidates.put(candidate, new AntiPA.OwnEntry(System.currentTimeMillis(), this.calcPistonFacing()));
         }
      }
   }

   private Direction calcPistonFacing() {
      if (mc.player == null) {
         return null;
      } else {
         float pitch = mc.player.getPitch();
         if (pitch < -55.0F) {
            return Direction.UP;
         } else {
            return pitch > 55.0F ? Direction.DOWN : mc.player.getHorizontalFacing();
         }
      }
   }

   private void proactivelyRegisterNearbyPlacements() {
      if (this.pistonSlotMask != 0 && mc.player != null && mc.world != null) {
         BlockPos feet = mc.player.getBlockPos();
         long now = System.currentTimeMillis();

         for (Direction dir : Direction.values()) {
            BlockPos nb = feet.offset(dir);
            if (mc.world.getBlockState(nb).isReplaceable()) {
               this.ownCandidates.putIfAbsent(nb, new AntiPA.OwnEntry(now, null));
            }
         }
      }
   }

   private int buildPistonSlotMask() {
      if (mc.player == null) {
         return 0;
      } else {
         int mask = 0;

         for (int i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getStack(i).getItem();
            if (item == Items.PISTON || item == Items.STICKY_PISTON) {
               mask |= 1 << i;
            }
         }

         Item off = mc.player.getOffHandStack().getItem();
         if (off == Items.PISTON || off == Items.STICKY_PISTON) {
            mask |= 512;
         }

         return mask;
      }
   }

   private boolean holdingPiston() {
      if (mc.player == null) {
         return false;
      } else {
         Item main = mc.player.getMainHandStack().getItem();
         Item off = mc.player.getOffHandStack().getItem();
         return main == Items.PISTON || main == Items.STICKY_PISTON || off == Items.PISTON || off == Items.STICKY_PISTON;
      }
   }

   private double getBoxDistance(Vec3d pos1, Vec3d pos2) {
      double dx = Math.abs(pos1.x - pos2.x);
      double dy = Math.abs(pos1.y - pos2.y);
      double dz = Math.abs(pos1.z - pos2.z);
      return Math.max(dx, Math.max(dy, dz));
   }

   private void sendLookPacket(float yaw, float pitch) {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(new LookAndOnGround(yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision));
      }
   }

   private float[] calcLookAt(Vec3d target) {
      Vec3d eyes = mc.player.getEyePos();
      double dx = target.x - eyes.x;
      double dy = target.y - eyes.y;
      double dz = target.z - eyes.z;
      double dist = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)(MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
      float pitch = (float)(-(MathHelper.atan2(dy, dist) * (180.0 / Math.PI)));
      return new float[]{yaw, pitch};
   }

   private boolean isPiston(BlockState state) {
      return state.isOf(Blocks.PISTON) || state.isOf(Blocks.STICKY_PISTON);
   }

   private int findObsidianInHotbar() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).isOf(Items.OBSIDIAN)) {
            return i;
         }
      }

      return -1;
   }

   @Override
   public String getDisplayInfo() {
      return "§7Q:" + this.replaceTasks.size();
   }

   private record OwnEntry(long registeredAt, Direction facing) {
   }

   private static class ReplaceTask {
      BlockPos pos;
      int ticksWaited;
      boolean isPistonPos;

      ReplaceTask(BlockPos pos, boolean isPistonPos) {
         this.pos = pos;
         this.isPistonPos = isPistonPos;
         this.ticksWaited = 0;
      }
   }
}
