package me.alpha432.chimeraclient.features.modules.combat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.event.impl.render.Render3DEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.util.render.RenderUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.Type;

public class PistonCrystal extends Module {
   private final Setting<Double> range = this.num("Range", 5.0, 1.0, 7.0);
   private final Setting<Double> baseDelay = this.num("BaseDelay", 0.0, 0.0, 20.0);
   private final Setting<Double> pushDelay = this.num("PushDelay", 0.0, 0.0, 20.0);
   private final Setting<Double> crystalDelay = this.num("CrystalDelay", 0.0, 0.0, 20.0);
   private final Setting<Double> restartDelay = this.num("RestartDelay", 0.0, 0.0, 20.0);
   private final Setting<Double> pushDistance = this.num("PushDistance", 0.25, 0.05, 1.0);
   private final Setting<Double> longPushDistance = this.num("LongPushDist", 0.9, 0.1, 1.0);
   private final Setting<Boolean> breakRedstoneOnRestart = this.bool("BreakRedstoneOnRestart", true);
   private final Setting<Integer> longPatternRedstoneThreshold = this.num("LongRedstoneThresh", 16, 1, 64);
   private final Setting<Integer> longSideObsidianThreshold = this.num("LongSideObsThresh", 16, 1, 64);
   private final Setting<Boolean> silent = this.bool("Silent", true);
   private final Setting<Boolean> autoRestart = this.bool("AutoRestart", false);
   private final Setting<Boolean> pauseWhileEating = this.bool("PauseWhileEating", true);
   private final Setting<Boolean> autoReplenish = this.bool("AutoReplenish", true);
   private final Setting<Integer> replenishThreshold = this.num("ReplenishThreshold", 16, 1, 63);
   private final Setting<Boolean> useTorch = this.bool("UseTorch", false);
   private final Setting<Boolean> blockSupport = this.bool("BlockSupport", false);
   private final Setting<Boolean> allowInAir = this.bool("AllowInAir", false);
   private final Setting<Boolean> strictPlace = this.bool("StrictPlace", true);
   private final Setting<Boolean> rotate = this.bool("Rotate", false);
   private final Setting<Boolean> render = this.bool("Render", true);
   private final Setting<Float> lineWidth = this.num("LineWidth", 1.0F, 0.1F, 5.0F);
   private final Setting<Color> crystalColor = this.color("CrystalColor", 255, 0, 255, 150);
   private final Setting<Color> pistonColor = this.color("PistonColor", 255, 255, 0, 150);
   private final Setting<Color> redstoneColor = this.color("RedstoneColor", 255, 0, 0, 150);
   private final Setting<Color> obsidianColor = this.color("ObsidianColor", 0, 0, 255, 150);
   private PistonCrystal.Phase phase = PistonCrystal.Phase.IDLE;
   private LivingEntity target = null;
   private BlockPos obsidianPos = null;
   private BlockPos pistonPos = null;
   private BlockPos redstonePos = null;
   private BlockPos crystalPos = null;
   private BlockPos pistonSupportPos = null;
   private BlockPos torchSupportPos = null;
   private BlockPos obstaclePos = null;
   private Direction pistonDirection = null;
   private EndCrystalEntity targetCrystal = null;
   private int actionTicks = 0;
   private boolean isMining = false;
   private int oldSlotBeforeMining = -1;
   private int pistonTickCounter = 0;
   private boolean retryAfterFailedPush = false;
   private boolean redstonePlacedSimultaneously = false;
   private boolean isLongPattern = false;
   private boolean isLongSidePattern = false;
   private boolean isAgainstWall = false;
   private Float lockedYaw = null;
   private Float lockedPitch = null;
   private float prePistonYaw = 0.0F;
   private float prePistonPitch = 0.0F;

   public PistonCrystal() {
      super("PistonCrystal", "Automatically places pistons and crystals to attack", Module.Category.COMBAT);
   }

   @Override
   public void onDisable() {
      this.resetState();
   }

   private double getBoxDistance(Vec3d pos1, Vec3d pos2) {
      double dx = Math.abs(pos1.x - pos2.x);
      double dy = Math.abs(pos1.y - pos2.y);
      double dz = Math.abs(pos1.z - pos2.z);
      return Math.max(dx, Math.max(dy, dz));
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         this.actionTicks++;
         if (this.pauseWhileEating.getValue() && mc.player.isUsingItem()) {
            this.resetState();
         } else {
            if (this.autoReplenish.getValue()) {
               this.replenishItem(Items.OBSIDIAN);
               this.replenishItem(Items.END_CRYSTAL);
               this.replenishItem(Items.PISTON);
               this.replenishItem(Items.STICKY_PISTON);
               if (this.useTorch.getValue()) {
                  this.replenishItem(Items.REDSTONE_TORCH);
               } else {
                  this.replenishItem(Items.REDSTONE_BLOCK);
               }
            }

            if (this.phase == PistonCrystal.Phase.DONE) {
               if (this.autoRestart.getValue()) {
                  this.softRestart();
               } else {
                  this.disable();
               }
            } else {
               if (this.target == null
                  || this.target.isDead()
                  || this.getBoxDistance(mc.player.getEyePos(), this.target.getEntityPos()) > this.range.getValue()) {
                  this.target = this.findNearestTarget();
                  if (this.target == null) {
                     this.resetState();
                     return;
                  }
               }

               int obsSlot = this.findInHotbar(Items.OBSIDIAN);
               int pistonSlot = this.findInHotbar(Items.PISTON);
               if (pistonSlot == -1) {
                  pistonSlot = this.findInHotbar(Items.STICKY_PISTON);
               }

               int crystalSlot = this.findInHotbar(Items.END_CRYSTAL);
               int redstoneSlot = this.useTorch.getValue() ? this.findInHotbar(Items.REDSTONE_TORCH) : this.findInHotbar(Items.REDSTONE_BLOCK);
               if (pistonSlot != -1 && crystalSlot != -1 && redstoneSlot != -1 && obsSlot != -1) {
                  if (this.phase.ordinal() > PistonCrystal.Phase.PLACING_PISTON.ordinal()
                     && this.phase.ordinal() <= PistonCrystal.Phase.WAITING_FOR_PUSH.ordinal()
                     && this.pistonPos != null) {
                     BlockState state = mc.world.getBlockState(this.pistonPos);
                     boolean isPiston = state.isOf(Blocks.PISTON) || state.isOf(Blocks.STICKY_PISTON) || state.isOf(Blocks.MOVING_PISTON);
                     if (!isPiston) {
                        this.resetState();
                        return;
                     }
                  }

                  if (this.phase.ordinal() <= PistonCrystal.Phase.CLEARING_ENTITIES.ordinal()
                     || this.phase.ordinal() >= PistonCrystal.Phase.PLACING_PISTON.ordinal()
                     || (this.pistonPos == null || this.isWithinRange(this.pistonPos)) && (this.obsidianPos == null || this.isWithinRange(this.obsidianPos))) {
                     if (!this.isLongPattern && !this.isLongSidePattern) {
                        if (this.redstonePos != null
                           && this.phase.ordinal() > PistonCrystal.Phase.CLEARING_ENTITIES.ordinal()
                           && this.phase.ordinal() <= PistonCrystal.Phase.PLACING_REDSTONE.ordinal()
                           && !this.isWithinRange(this.redstonePos)) {
                           this.resetState();
                           return;
                        }
                     } else if (this.redstonePos != null
                        && this.phase.ordinal() > PistonCrystal.Phase.CLEARING_ENTITIES.ordinal()
                        && this.phase.ordinal() <= PistonCrystal.Phase.PLACING_LONG_REDSTONE.ordinal()
                        && !this.isWithinRange(this.redstonePos)) {
                        this.resetState();
                        return;
                     }

                     if (this.crystalPos != null) {
                        if (this.phase.ordinal() > PistonCrystal.Phase.CLEARING_ENTITIES.ordinal()
                           && this.phase.ordinal() <= PistonCrystal.Phase.WAITING_FOR_PUSH.ordinal()
                           && (!this.isWithinRange(this.crystalPos) || this.hasBlockingEntityAt(this.crystalPos))) {
                           this.resetState();
                           return;
                        }

                        if (this.phase.ordinal() > PistonCrystal.Phase.BREAKING_OBSTACLE.ordinal()
                           && this.phase.ordinal() <= PistonCrystal.Phase.WAITING_FOR_PUSH.ordinal()
                           && !this.isAirOrReplaceable(this.crystalPos)) {
                           BlockState crystalPosState = mc.world.getBlockState(this.crystalPos);
                           boolean isOwnExtendingPiston = crystalPosState.isOf(Blocks.MOVING_PISTON) || crystalPosState.isOf(Blocks.PISTON_HEAD);
                           boolean isExpectedDuringPush = this.phase == PistonCrystal.Phase.WAITING_FOR_PUSH && isOwnExtendingPiston;
                           if (!isExpectedDuringPush) {
                              this.resetState();
                              return;
                           }
                        }
                     }

                     this.handlePhases(obsSlot, pistonSlot, crystalSlot, redstoneSlot);
                  } else {
                     this.resetState();
                  }
               }
            }
         }
      }
   }

   private void attemptSimultaneousRedstone(int obsSlot, int redstoneSlot) {
      if (this.isAirOrReplaceable(this.redstonePos)) {
         BlockPos support = this.getOrCreateSupport(this.redstonePos);
         if (support != null && !support.equals(this.redstonePos)) {
            this.executeAction(obsSlot, () -> this.placeBlockAt(support, false));
            return;
         }

         if (this.useTorch.getValue()) {
            BlockPos actualTorchSupport = this.torchSupportPos != null ? this.torchSupportPos : this.redstonePos.down();
            if (actualTorchSupport.equals(this.redstonePos.down())) {
               this.executeAction(redstoneSlot, () -> this.placeBlockOnTop(actualTorchSupport));
            } else {
               this.executeAction(redstoneSlot, () -> this.placeBlockAgainst(this.redstonePos, actualTorchSupport));
            }
         } else if (this.blockSupport.getValue()) {
            BlockPos actualSupport = this.torchSupportPos != null ? this.torchSupportPos : this.redstonePos.down();
            if (actualSupport.equals(this.redstonePos.down())) {
               this.executeAction(redstoneSlot, () -> this.placeBlockOnTop(actualSupport));
            } else {
               this.executeAction(redstoneSlot, () -> this.placeBlockAgainst(this.redstonePos, actualSupport));
            }
         } else {
            this.executeAction(redstoneSlot, () -> this.placeBlockAgainst(this.redstonePos, this.pistonPos));
         }

         this.redstonePlacedSimultaneously = true;
      }
   }

   private void handlePhases(int obsSlot, int pistonSlot, int crystalSlot, int redstoneSlot) {
      switch (this.phase) {
         case IDLE:
            int currentRedstoneCount = this.getInventoryItemCount(this.useTorch.getValue() ? Items.REDSTONE_TORCH : Items.REDSTONE_BLOCK);
            int currentObsidianHotbarCount = this.getHotbarItemCount(Items.OBSIDIAN);
            if (currentObsidianHotbarCount <= this.longSideObsidianThreshold.getValue() && this.findLongSidePatternPositions(this.target, true)) {
               this.setPhase(PistonCrystal.Phase.CLEARING_ENTITIES);
               this.pistonTickCounter = 0;
            } else if (this.findLongPatternPositions(this.target, true)) {
               this.setPhase(PistonCrystal.Phase.CLEARING_ENTITIES);
               this.pistonTickCounter = 0;
            } else if (currentRedstoneCount <= this.longPatternRedstoneThreshold.getValue() && this.findLongPatternPositions(this.target, false)) {
               this.setPhase(PistonCrystal.Phase.CLEARING_ENTITIES);
               this.pistonTickCounter = 0;
            } else if (this.findPistonCrystalPositions(this.target)) {
               this.setPhase(PistonCrystal.Phase.CLEARING_ENTITIES);
               this.pistonTickCounter = 0;
            } else if (this.findLongSidePatternPositions(this.target, false)) {
               this.setPhase(PistonCrystal.Phase.CLEARING_ENTITIES);
               this.pistonTickCounter = 0;
            }
            break;
         case CLEARING_ENTITIES:
            boolean cleared = this.clearEntities(this.pistonPos)
               || this.clearEntities(this.redstonePos)
               || this.clearEntities(this.obsidianPos)
               || this.clearEntities(this.crystalPos);
            if (this.torchSupportPos != null) {
               cleared |= this.clearEntities(this.torchSupportPos);
            }

            if (this.pistonSupportPos != null) {
               cleared |= this.clearEntities(this.pistonSupportPos);
            }

            if ((this.isLongPattern || this.isLongSidePattern) && this.crystalPos != null && this.pistonDirection != null) {
               BlockPos gapPos = this.crystalPos.offset(this.pistonDirection.getOpposite());
               cleared |= this.clearEntities(gapPos);
            }

            if (cleared) {
               this.actionTicks = 0;
            } else if (this.obstaclePos != null) {
               this.setPhase(PistonCrystal.Phase.BREAKING_OBSTACLE);
            } else {
               this.setPhase(PistonCrystal.Phase.PLACING_SUPPORT);
            }
            break;
         case BREAKING_OBSTACLE:
            this.clearEntities(this.obstaclePos);

            for (Direction sweepDir : Direction.values()) {
               this.clearEntities(this.obstaclePos.offset(sweepDir));
            }

            if (this.isAirOrReplaceable(this.obstaclePos)) {
               this.isMining = false;
               this.restoreMiningSlot();
               BlockPos nextObstacle = null;

               for (Direction sweepDir : Direction.values()) {
                  BlockPos nb = this.obstaclePos.offset(sweepDir);
                  if (this.isManageableObstacle(nb)) {
                     nextObstacle = nb;
                     break;
                  }
               }

               if (nextObstacle != null) {
                  this.obstaclePos = nextObstacle;
               } else {
                  this.setPhase(PistonCrystal.Phase.PLACING_SUPPORT);
               }
            } else {
               this.breakBlock(this.obstaclePos);
            }
            break;
         case PLACING_SUPPORT:
            if (this.pistonSupportPos != null && !this.pistonSupportPos.equals(this.pistonPos) && this.isAirOrReplaceable(this.pistonSupportPos)) {
               if (this.actionTicks > this.baseDelay.getValue().intValue()) {
                  BlockPos support = this.getOrCreateSupport(this.pistonSupportPos);
                  if (support != null && !support.equals(this.pistonSupportPos)) {
                     this.executeAction(obsSlot, () -> this.placeBlockAt(support, false));
                  } else {
                     this.executeAction(obsSlot, () -> this.placeBlockAt(this.pistonSupportPos, false));
                  }

                  this.actionTicks = 0;
               }

               return;
            }

            this.setPhase(PistonCrystal.Phase.PLACING_TORCH_SUPPORT);
            break;
         case PLACING_TORCH_SUPPORT:
            if (this.useTorch.getValue() || !this.useTorch.getValue() && this.blockSupport.getValue()) {
               BlockPos targetSupport = this.torchSupportPos != null ? this.torchSupportPos : this.redstonePos.down();
               if (!targetSupport.equals(this.pistonPos) && this.isAirOrReplaceable(targetSupport)) {
                  if (this.actionTicks > this.baseDelay.getValue().intValue()) {
                      BlockPos support = this.getOrCreateSupport(targetSupport);
                      if (support != null && !support.equals(targetSupport)) {
                         this.executeAction(obsSlot, () -> this.placeBlockAt(support, false));
                     } else {
                        this.executeAction(obsSlot, () -> this.placeBlockAt(targetSupport, false));
                     }

                     this.actionTicks = 0;
                  }

                  return;
               }
            } else if (this.torchSupportPos != null && !this.torchSupportPos.equals(this.pistonPos) && this.isAirOrReplaceable(this.torchSupportPos)) {
               if (this.actionTicks > this.baseDelay.getValue().intValue()) {
                   BlockPos support = this.getOrCreateSupport(this.torchSupportPos);
                   if (support != null && !support.equals(this.torchSupportPos)) {
                      this.executeAction(obsSlot, () -> this.placeBlockAt(support, false));
                  } else {
                     this.executeAction(obsSlot, () -> this.placeBlockAt(this.torchSupportPos, false));
                  }

                  this.actionTicks = 0;
               }

               return;
            }

            this.setPhase(PistonCrystal.Phase.PLACING_BASE);
            break;
         case PLACING_BASE:
            if (this.isAirOrReplaceable(this.obsidianPos)) {
               if (this.actionTicks > this.baseDelay.getValue().intValue()) {
                   BlockPos support = this.getOrCreateSupport(this.obsidianPos);
                   if (support != null && !support.equals(this.obsidianPos)) {
                      this.executeAction(obsSlot, () -> this.placeBlockAt(support, false));
                  } else {
                     this.executeAction(obsSlot, () -> this.placeBlockAt(this.obsidianPos, false));
                  }

                  this.actionTicks = 0;
               }

               return;
            }

            if (!this.isLongPattern && !this.isLongSidePattern) {
               this.setPhase(PistonCrystal.Phase.TURNING_FOR_PISTON);
            } else {
               this.setPhase(PistonCrystal.Phase.PLACING_LONG_REDSTONE);
            }
            break;
         case PLACING_LONG_REDSTONE:
            if (this.isLongPattern || this.isLongSidePattern) {
               if (this.useTorch.getValue()) {
                  BlockState rState = mc.world.getBlockState(this.redstonePos);
                  if (!rState.isOf(Blocks.REDSTONE_TORCH) && !rState.isOf(Blocks.REDSTONE_WALL_TORCH)) {
                     if (this.actionTicks > this.baseDelay.getValue().intValue()) {
                         BlockPos support = this.torchSupportPos != null ? this.torchSupportPos : this.redstonePos.down();
                         if (support.equals(this.redstonePos.down())) {
                           BlockPos extraSupport = this.getOrCreateSupport(this.redstonePos);
                           if (extraSupport != null && !extraSupport.equals(this.redstonePos)) {
                              this.executeAction(obsSlot, () -> this.placeBlockAt(extraSupport, false));
                              this.actionTicks = 0;
                              return;
                           }

                           this.executeAction(redstoneSlot, () -> this.placeBlockOnTop(support));
                        } else {
                           this.executeAction(redstoneSlot, () -> this.placeBlockAgainst(this.redstonePos, support));
                        }

                        this.actionTicks = 0;
                     }

                     return;
                  }
               } else if (!mc.world.getBlockState(this.redstonePos).isOf(Blocks.REDSTONE_BLOCK)) {
                  if (this.actionTicks > this.baseDelay.getValue().intValue()) {
                     BlockPos extraSupport = this.getOrCreateSupport(this.redstonePos);
                     if (extraSupport != null && !extraSupport.equals(this.redstonePos)) {
                        this.executeAction(obsSlot, () -> this.placeBlockAt(extraSupport, false));
                        this.actionTicks = 0;
                        return;
                     }

                     if (this.blockSupport.getValue()) {
                         BlockPos support = this.torchSupportPos != null ? this.torchSupportPos : this.redstonePos.down();
                         if (support.equals(this.redstonePos.down())) {
                           this.executeAction(redstoneSlot, () -> this.placeBlockOnTop(support));
                        } else {
                           this.executeAction(redstoneSlot, () -> this.placeBlockAgainst(this.redstonePos, support));
                        }
                     } else {
                        this.executeAction(redstoneSlot, () -> this.placeBlockAt(this.redstonePos, false));
                     }

                     this.actionTicks = 0;
                  }

                  return;
               }

               this.setPhase(PistonCrystal.Phase.PLACING_LONG_CRYSTAL);
            }
            break;
         case PLACING_LONG_CRYSTAL:
            if (this.hasCrystalAt(this.crystalPos)) {
               this.setPhase(PistonCrystal.Phase.TURNING_FOR_PISTON);
            } else if (this.actionTicks > this.crystalDelay.getValue().intValue()) {
               this.executeAction(crystalSlot, () -> this.placeCrystalAt(this.obsidianPos));
               this.actionTicks = 0;
            }
            break;
         case TURNING_FOR_PISTON:
            if (this.actionTicks > this.pushDelay.getValue().intValue() / 2) {
               if (this.lockedYaw != null && this.lockedPitch != null) {
                  this.prePistonYaw = mc.player.getYaw();
                  this.prePistonPitch = mc.player.getPitch();
                  this.sendLookPacket(this.lockedYaw, this.lockedPitch);
               }

               this.setPhase(PistonCrystal.Phase.PLACING_PISTON);
               this.pistonTickCounter = 0;
            }
            break;
         case PLACING_PISTON:
            this.pistonTickCounter++;
            if (this.pistonTickCounter >= 1) {
               if ((this.isLongPattern || this.isLongSidePattern) && !this.hasCrystalAt(this.crystalPos)) {
                  this.setPhase(PistonCrystal.Phase.PLACING_LONG_REDSTONE);
                  return;
               }

               if (this.isAirOrReplaceable(this.pistonPos)) {
                  BlockPos support = this.getOrCreateSupport(this.pistonPos);
                  if (support != null && !support.equals(this.pistonPos)) {
                     this.executeAction(obsSlot, () -> this.placeBlockAt(support, false));
                     this.pistonTickCounter = 0;
                     return;
                  }

                  this.executeAction(pistonSlot, () -> this.placeBlockAtDirectional(this.pistonPos, this.pistonDirection));
               }

               if (!this.isLongPattern && !this.isLongSidePattern && this.crystalDelay.getValue() == 0.0) {
                  this.executeAction(crystalSlot, () -> this.placeCrystalAt(this.obsidianPos));
                  if (this.pushDelay.getValue() == 0.0) {
                     this.attemptSimultaneousRedstone(obsSlot, redstoneSlot);
                  }
               }

               this.setPhase(PistonCrystal.Phase.RESETTING_VIEW);
               this.pistonTickCounter = 0;
            }
            break;
         case RESETTING_VIEW:
            this.pistonTickCounter++;
            if (this.pistonTickCounter >= 1) {
               this.sendLookPacket(this.prePistonYaw, this.prePistonPitch);
               if (!this.isLongPattern && !this.isLongSidePattern) {
                  this.setPhase(PistonCrystal.Phase.WAITING_FOR_CRYSTAL);
               } else {
                  this.setPhase(PistonCrystal.Phase.WAITING_FOR_PUSH);
               }
            }
            break;
         case WAITING_FOR_CRYSTAL:
            if (this.hasCrystalAt(this.crystalPos)) {
               this.setPhase(PistonCrystal.Phase.PLACING_REDSTONE);
            } else if (this.actionTicks > this.crystalDelay.getValue().intValue()) {
               if (this.isAirOrReplaceable(this.obsidianPos.up())) {
                  this.executeAction(crystalSlot, () -> this.placeCrystalAt(this.obsidianPos));
                  if (this.pushDelay.getValue() == 0.0) {
                     this.attemptSimultaneousRedstone(obsSlot, redstoneSlot);
                  }
               }

               this.actionTicks = 0;
            }
            break;
         case PLACING_REDSTONE:
            if (this.redstonePlacedSimultaneously) {
               this.redstonePlacedSimultaneously = false;
               this.setPhase(PistonCrystal.Phase.WAITING_FOR_PUSH);
               return;
            }

            if (this.useTorch.getValue() || !this.useTorch.getValue() && this.blockSupport.getValue()) {
               BlockPos actualTorchSupport = this.torchSupportPos != null ? this.torchSupportPos : this.redstonePos.down();
               if (this.isAirOrReplaceable(actualTorchSupport)) {
                  this.setPhase(PistonCrystal.Phase.PLACING_TORCH_SUPPORT);
                  return;
               }
            }

            if (this.isBlockedByLivingEntity(this.redstonePos)) {
               this.resetState();
               return;
            }

            if (this.actionTicks >= this.pushDelay.getValue().intValue()) {
               if (this.isAirOrReplaceable(this.redstonePos)) {
                  BlockPos support = this.getOrCreateSupport(this.redstonePos);
                  if (support != null && !support.equals(this.redstonePos)) {
                     this.executeAction(obsSlot, () -> this.placeBlockAt(support, false));
                     this.actionTicks = 0;
                     return;
                  }

                  if (this.useTorch.getValue()) {
                     BlockPos actualTorchSupport = this.torchSupportPos != null ? this.torchSupportPos : this.redstonePos.down();
                     if (actualTorchSupport.equals(this.redstonePos.down())) {
                        this.executeAction(redstoneSlot, () -> this.placeBlockOnTop(actualTorchSupport));
                     } else {
                        this.executeAction(redstoneSlot, () -> this.placeBlockAgainst(this.redstonePos, actualTorchSupport));
                     }
                  } else if (this.blockSupport.getValue()) {
                     BlockPos actualSupport = this.torchSupportPos != null ? this.torchSupportPos : this.redstonePos.down();
                     if (actualSupport.equals(this.redstonePos.down())) {
                        this.executeAction(redstoneSlot, () -> this.placeBlockOnTop(actualSupport));
                     } else {
                        this.executeAction(redstoneSlot, () -> this.placeBlockAgainst(this.redstonePos, actualSupport));
                     }
                  } else {
                     this.executeAction(redstoneSlot, () -> this.placeBlockAgainst(this.redstonePos, this.pistonPos));
                  }
               }

               this.setPhase(PistonCrystal.Phase.WAITING_FOR_PUSH);
            }
            break;
         case WAITING_FOR_PUSH:
            if (this.isCrystalPushed()) {
               boolean exploded = this.explodeCrystal();
               if (exploded) {
                  this.setPhase(PistonCrystal.Phase.CONFIRMING_EXPLOSION);
               } else {
                  this.setPhase(PistonCrystal.Phase.BREAKING_REDSTONE);
               }
            } else if (this.actionTicks > this.pushDelay.getValue().intValue()) {
               this.retryAfterFailedPush = true;
               this.setPhase(PistonCrystal.Phase.BREAKING_REDSTONE);
            }
            break;
         case EXPLODING:
            if (this.actionTicks > this.crystalDelay.getValue().intValue()) {
               boolean exploded = this.explodeCrystal();
               if (exploded) {
                  this.setPhase(PistonCrystal.Phase.CONFIRMING_EXPLOSION);
               } else {
                  this.setPhase(PistonCrystal.Phase.BREAKING_REDSTONE);
               }
            }
            break;
         case CONFIRMING_EXPLOSION:
            int confirmWait = this.autoRestart.getValue() ? 0 : this.restartDelay.getValue().intValue();
            if (this.actionTicks > confirmWait) {
               if (this.redstonePos != null && !this.isAirOrReplaceable(this.redstonePos) && this.breakRedstoneOnRestart.getValue()) {
                  this.retryAfterFailedPush = true;
                  this.setPhase(PistonCrystal.Phase.BREAKING_REDSTONE);
               } else {
                  this.setPhase(PistonCrystal.Phase.DONE);
               }
            }
            break;
         case BREAKING_REDSTONE:
            if (this.isLongPattern || this.isLongSidePattern) {
               if (!this.isAgainstWall) {
                  this.isMining = false;
                  this.restoreMiningSlot();
                  this.explodeCrystal();
                  if (this.retryAfterFailedPush) {
                     this.retryAfterFailedPush = false;
                     this.setPhase(PistonCrystal.Phase.PLACING_LONG_REDSTONE);
                  } else {
                     this.setPhase(PistonCrystal.Phase.DONE);
                  }

                  return;
               }

               boolean attacked = this.explodeCrystal();
               boolean stillHasCrystal = false;
               if (this.crystalPos != null) {
                  stillHasCrystal = this.hasCrystalAt(this.crystalPos)
                     || this.pistonDirection != null && this.hasCrystalAt(this.crystalPos.offset(this.pistonDirection));
               }

               if (attacked || stillHasCrystal) {
                  return;
               }
            }

            if (this.isAirOrReplaceable(this.redstonePos)) {
               this.isMining = false;
               this.restoreMiningSlot();
               if (this.crystalPos != null) {
                  EndCrystalEntity oldCrystal = this.findNearestCrystal(Vec3d.ofCenter(this.crystalPos), 10.0);
                  if (oldCrystal != null) {
                     mc.interactionManager.attackEntity(mc.player, oldCrystal);
                     mc.player.swingHand(Hand.MAIN_HAND);
                  }
               }

               if (this.retryAfterFailedPush) {
                  this.retryAfterFailedPush = false;
                  if (!this.isLongPattern && !this.isLongSidePattern) {
                     this.setPhase(PistonCrystal.Phase.TURNING_FOR_PISTON);
                  } else {
                     this.setPhase(PistonCrystal.Phase.PLACING_LONG_REDSTONE);
                  }
               } else {
                  this.setPhase(PistonCrystal.Phase.DONE);
               }
            } else {
               this.breakBlock(this.redstonePos);
            }
         case DONE:
      }
   }

   private void setPhase(PistonCrystal.Phase newPhase) {
      this.phase = newPhase;
      this.actionTicks = 0;
   }

   private void restoreMiningSlot() {
      if (this.oldSlotBeforeMining != -1) {
         mc.player.getInventory().selectedSlot = this.oldSlotBeforeMining;
         this.oldSlotBeforeMining = -1;
      }
   }

   private boolean isAirOrReplaceable(BlockPos pos) {
      if (pos == null) {
         return false;
      } else {
         BlockState state = mc.world.getBlockState(pos);
         return state.isAir() || state.isReplaceable();
      }
   }

   private void sendLookPacket(float yaw, float pitch) {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(new LookAndOnGround(yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision));
         if (this.rotate.getValue()) {
            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
         }
      }
   }

   private float[] calcLookAtPos(Vec3d target) {
      Vec3d eyes = mc.player.getEyePos();
      double dx = target.x - eyes.x;
      double dy = target.y - eyes.y;
      double dz = target.z - eyes.z;
      double dist = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)(MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
      float pitch = (float)(-(MathHelper.atan2(dy, dist) * (180.0 / Math.PI)));
      return new float[]{yaw, pitch};
   }

   private boolean isStrictDirectionValid(BlockPos blockPos, Direction face) {
      if (!this.strictPlace.getValue()) {
         return true;
      } else {
         Vec3d eyePos = mc.player.getEyePos();
         Vec3d faceCenter = Vec3d.ofCenter(blockPos).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
         Vec3d lookVec = faceCenter.subtract(eyePos);
         Vec3d normal = new Vec3d(face.getOffsetX(), face.getOffsetY(), face.getOffsetZ());
         return lookVec.dotProduct(normal) <= 0.0;
      }
   }

   private boolean isPosReservedOrBlocking(BlockPos pos) {
      if (pos.equals(this.crystalPos) || pos.equals(this.pistonPos) || pos.equals(this.redstonePos) || pos.equals(this.obsidianPos)) {
         return true;
      } else if (this.pistonPos != null && this.pistonDirection != null && pos.equals(this.pistonPos.offset(this.pistonDirection))) {
         return true;
      } else {
         return pos.equals(this.obstaclePos) ? true : this.target != null && this.target.getBoundingBox().intersects(new Box(pos));
      }
   }

   private BlockPos getOrCreateSupport(BlockPos pos) {
      Direction[] dirs = new Direction[]{Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP};

      for (Direction dir : dirs) {
         BlockPos nb = pos.offset(dir);
         if (!this.isAirOrReplaceable(nb)) {
            Direction face = dir.getOpposite();
            if (!this.strictPlace.getValue() || this.isStrictDirectionValid(nb, face)) {
               return pos;
            }
         }
      }

      if (this.strictPlace.getValue()) {
         for (Direction dirx : dirs) {
            BlockPos supportPos = pos.offset(dirx);
            if (this.isAirOrReplaceable(supportPos) && !this.isPosReservedOrBlocking(supportPos)) {
               Direction faceForTarget = dirx.getOpposite();
               if (this.isStrictDirectionValid(supportPos, faceForTarget)) {
                  for (Direction sDir : dirs) {
                     BlockPos sNb = supportPos.offset(sDir);
                     if (!sNb.equals(pos) && !this.isAirOrReplaceable(sNb)) {
                        Direction sFace = sDir.getOpposite();
                        if (this.isStrictDirectionValid(sNb, sFace)) {
                           return supportPos;
                        }
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   private void doPlace(BlockPos nb, Direction face) {
      Vec3d hitVec = Vec3d.ofCenter(nb).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
      BlockHitResult hitResult = new BlockHitResult(hitVec, face, nb, false);
      float preYaw = mc.player.getYaw();
      float prePitch = mc.player.getPitch();
      float[] look = this.calcLookAtPos(hitVec);
      this.sendLookPacket(look[0], look[1]);
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
      mc.player.swingHand(Hand.MAIN_HAND);
      if (!this.rotate.getValue()) {
         this.sendLookPacket(preYaw, prePitch);
      }
   }

   private boolean isBlockedByLivingEntity(BlockPos pos) {
      if (pos == null) {
         return false;
      } else {
         for (Entity e : mc.world.getOtherEntities((Entity)null, new Box(pos))) {
            if (e instanceof LivingEntity && !e.isSpectator()) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean hasBlockingEntityAt(BlockPos pos) {
      if (pos == null) {
         return false;
      } else {
         for (Entity e : mc.world.getOtherEntities((Entity)null, new Box(pos))) {
            if (!(e instanceof EndCrystalEntity)) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean hasAnyEntityAt(BlockPos pos) {
      return pos == null ? false : !mc.world.getOtherEntities((Entity)null, new Box(pos)).isEmpty();
   }

   private boolean hasCrystalAt(BlockPos pos) {
      if (pos == null) {
         return false;
      } else {
         for (Entity e : mc.world.getOtherEntities((Entity)null, new Box(pos))) {
            if (e instanceof EndCrystalEntity) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean clearEntities(BlockPos pos) {
      if (pos == null) {
         return false;
      } else {
         boolean hit = false;

         for (Entity e : mc.world.getOtherEntities((Entity)null, new Box(pos))) {
            if (e instanceof EndCrystalEntity) {
               this.attackCrystal(e);
               hit = true;
            }
         }

         return hit;
      }
   }

   private boolean isWithinRange(BlockPos pos) {
      if (pos == null) {
         return false;
      } else {
         double maxRange = this.range.getValue();
         return this.getBoxDistance(mc.player.getEyePos(), Vec3d.ofCenter(pos)) <= maxRange;
      }
   }

   private void breakBlock(BlockPos pos) {
      if (pos != null) {
         int pickaxeSlot = -1;

         for (int i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getStack(i).getItem();
            if (item == Items.WOODEN_PICKAXE
               || item == Items.STONE_PICKAXE
               || item == Items.IRON_PICKAXE
               || item == Items.GOLDEN_PICKAXE
               || item == Items.DIAMOND_PICKAXE
               || item == Items.NETHERITE_PICKAXE) {
               pickaxeSlot = i;
               break;
            }
         }

         if (!this.isMining) {
            if (this.oldSlotBeforeMining == -1) {
               this.oldSlotBeforeMining = mc.player.getInventory().selectedSlot;
            }

            if (pickaxeSlot != -1) {
               mc.player.getInventory().selectedSlot = pickaxeSlot;
            }

            mc.interactionManager.attackBlock(pos, Direction.UP);
            mc.player.swingHand(Hand.MAIN_HAND);
            this.isMining = true;
         } else {
            if (pickaxeSlot != -1) {
               mc.player.getInventory().selectedSlot = pickaxeSlot;
            }

            if (mc.interactionManager.updateBlockBreakingProgress(pos, Direction.UP)) {
               mc.player.swingHand(Hand.MAIN_HAND);
            }
         }
      }
   }

   private boolean isExistingValidPiston(BlockPos pos, Direction pushDir) {
      if (pos != null && pushDir != null) {
         BlockState state = mc.world.getBlockState(pos);
         if ((state.isOf(Blocks.PISTON) || state.isOf(Blocks.STICKY_PISTON)) && state.contains(Properties.FACING)) {
            boolean extended = state.contains(Properties.EXTENDED) && (Boolean)state.get(Properties.EXTENDED);
            return state.get(Properties.FACING) == pushDir && !extended;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean isManageableObstacle(BlockPos pos) {
      if (pos == null) {
         return false;
      } else {
         BlockState state = mc.world.getBlockState(pos);
         if (!state.isAir() && !state.isReplaceable() && !state.isOf(Blocks.OBSIDIAN)) {
            if (state.getBlock() == Blocks.BEDROCK || state.isOf(Blocks.END_PORTAL_FRAME)) {
               return false;
            } else {
               return pos.equals(this.pistonPos) && this.pistonDirection != null && this.isExistingValidPiston(pos, this.pistonDirection)
                  ? false
                  : !this.isLongPattern && !this.isLongSidePattern
                     || !pos.equals(this.redstonePos)
                     || !state.isOf(Blocks.REDSTONE_BLOCK) && !state.isOf(Blocks.REDSTONE_TORCH) && !state.isOf(Blocks.REDSTONE_WALL_TORCH);
            }
         } else {
            return false;
         }
      }
   }

   private boolean isCoveredByFlatBlocks(BlockPos tPos) {
      BlockPos headUp2 = tPos.up(2);
      if (this.isFullySurrounded(headUp2)) {
         return true;
      } else {
         BlockPos headUp3 = tPos.up(3);
         return this.isFullySurrounded(headUp3);
      }
   }

   private boolean isFullySurrounded(BlockPos pos) {
      Direction[] dirs = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

      for (Direction dir : dirs) {
         if (this.isAirOrReplaceable(pos.offset(dir))) {
            return false;
         }
      }

      return true;
   }

   private boolean willPistonHitCrystal(BlockPos pistonPos, Direction pushDir, BlockPos crystalPos) {
      if (pistonPos != null && pushDir != null && crystalPos != null) {
         Box pistonHeadBox = new Box(pistonPos.offset(pushDir));
         Box crystalBox = new Box(
               crystalPos.getX() - 0.5, crystalPos.getY(), crystalPos.getZ() - 0.5, crystalPos.getX() + 1.5, crystalPos.getY() + 2.0, crystalPos.getZ() + 1.5
            )
            .contract(0.001);
         return pistonHeadBox.intersects(crystalBox);
      } else {
         return false;
      }
   }

   private boolean isValidPushDestination(BlockPos crystalPos, Direction pushDir, BlockPos targetPos) {
      if (crystalPos != null && pushDir != null && targetPos != null) {
         BlockPos pushedPos = crystalPos.offset(pushDir);
         if (crystalPos.getY() == targetPos.getY() && pushedPos.equals(targetPos)) {
            BlockState state = mc.world.getBlockState(pushedPos);
            if (!state.isAir() && !state.isReplaceable()) {
               Box crystalBox = new Box(
                     pushedPos.getX() - 0.5, pushedPos.getY(), pushedPos.getZ() - 0.5, pushedPos.getX() + 1.5, pushedPos.getY() + 2.0, pushedPos.getZ() + 1.5
                  )
                  .contract(0.001);
               Box blockBox = new Box(pushedPos);
               return crystalBox.intersects(blockBox);
            } else {
               return true;
            }
         } else {
            int bodyY = targetPos.getY() + 1;
            int checkFrom = pushedPos.getY() - 1;
            int checkTo = bodyY;
            if (checkFrom < bodyY) {
               return true;
            } else {
               for (int y = checkFrom; y >= checkTo; y--) {
                  BlockPos checkPos = new BlockPos(pushedPos.getX(), y, pushedPos.getZ());
                  if (!this.isAirOrReplaceable(checkPos)) {
                     return false;
                  }
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }

   private boolean willCrystalFloatAfterPush(BlockPos crystalPos, Direction pushDir) {
      if (crystalPos != null && pushDir != null) {
         BlockPos pushedPos = crystalPos.offset(pushDir);
         BlockPos belowPushed = pushedPos.down();
         BlockState belowState = mc.world.getBlockState(belowPushed);
         return belowState.isAir() || belowState.isReplaceable();
      } else {
         return false;
      }
   }

   private boolean findLongPatternPositions(LivingEntity targetEntity, boolean requireExisting) {
      BlockPos tPos = targetEntity.getBlockPos();
      if (this.isCoveredByFlatBlocks(tPos)) {
         return false;
      } else {
         boolean isHeadClear = this.isAirOrReplaceable(tPos.up(1));
         Direction[] dirs = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
         int[] yOffsets = new int[]{0, 1, 2, -1, -2};

         for (int yOffset : yOffsets) {
            if (isHeadClear || yOffset < 1) {
               for (Direction dir : dirs) {
                  Direction pushDir = dir.getOpposite();
                  BlockPos basePos = tPos.up(yOffset).offset(dir);
                  if (!this.allowInAir.getValue()) {
                     BlockPos underBase = basePos.down();
                     BlockState underState = mc.world.getBlockState(underBase);
                     if (underState.isAir() || underState.isReplaceable()) {
                        continue;
                     }
                  }

                  BlockPos cPos = basePos.up();
                  if (cPos.getY() >= tPos.getY()) {
                     BlockPos gapPos = cPos.offset(dir);
                     BlockPos pPos = gapPos.offset(dir);
                     BlockPos rPos = pPos.down();
                     if (requireExisting) {
                        boolean rExists = false;
                        if (!this.useTorch.getValue()) {
                           rExists = mc.world.getBlockState(rPos).isOf(Blocks.REDSTONE_BLOCK);
                        } else {
                           BlockState rState = mc.world.getBlockState(rPos);
                           rExists = rState.isOf(Blocks.REDSTONE_TORCH) || rState.isOf(Blocks.REDSTONE_WALL_TORCH);
                        }

                        if (!rExists) {
                           continue;
                        }
                     }

                     if (this.isWithinRange(basePos) && this.isWithinRange(cPos) && this.isWithinRange(pPos) && this.isWithinRange(rPos)) {
                        boolean feetBypass = cPos.getY() == tPos.getY() && gapPos.equals(tPos);
                        if ((feetBypass || this.isAirOrReplaceable(gapPos) && !this.hasAnyEntityAt(gapPos))
                           && !this.hasBlockingEntityAt(cPos)
                           && !this.isBlockedByLivingEntity(basePos)
                           && !this.isBlockedByLivingEntity(pPos)
                           && !this.isBlockedByLivingEntity(rPos)) {
                           boolean obsValid = this.isAirOrReplaceable(basePos)
                              || mc.world.getBlockState(basePos).isOf(Blocks.OBSIDIAN)
                              || mc.world.getBlockState(basePos).isOf(Blocks.BEDROCK)
                              || this.isManageableObstacle(basePos);
                           boolean cValid = this.isAirOrReplaceable(cPos) || this.isManageableObstacle(cPos);
                           boolean pValid = this.isAirOrReplaceable(pPos) || this.isManageableObstacle(pPos) || this.isExistingValidPiston(pPos, pushDir);
                           boolean rValid = false;
                           BlockPos foundTorchSupport = null;
                           if (this.useTorch.getValue()) {
                              BlockState rState = mc.world.getBlockState(rPos);
                              if (rState.isOf(Blocks.REDSTONE_TORCH) || rState.isOf(Blocks.REDSTONE_WALL_TORCH)) {
                                 rValid = true;
                              } else if (this.isAirOrReplaceable(rPos) || this.isManageableObstacle(rPos)) {
                                 for (Direction d : Type.HORIZONTAL) {
                                    BlockPos adj = rPos.offset(d);
                                    if (mc.world.getBlockState(adj).isSolid()) {
                                       foundTorchSupport = adj;
                                       rValid = true;
                                       break;
                                    }
                                 }

                                 if (!rValid) {
                                    BlockPos underR = rPos.down();
                                    if (this.isAirOrReplaceable(underR) || mc.world.getBlockState(underR).isSolid()) {
                                       foundTorchSupport = underR;
                                       rValid = true;
                                    } else if (this.isManageableObstacle(underR)) {
                                       foundTorchSupport = underR;
                                       rValid = true;
                                    }
                                 }
                              }
                           } else {
                              BlockState rState = mc.world.getBlockState(rPos);
                              if (rState.isOf(Blocks.REDSTONE_BLOCK)) {
                                 rValid = true;
                              } else if (this.isAirOrReplaceable(rPos) || this.isManageableObstacle(rPos)) {
                                 rValid = true;
                                 if (this.blockSupport.getValue()) {
                                    BlockPos rSupport = rPos.down();
                                    if (rSupport.equals(basePos) || rSupport.equals(cPos)) {
                                       rValid = false;
                                    } else if (this.isAirOrReplaceable(rSupport)) {
                                       if (this.isWithinRange(rSupport) && !this.isBlockedByLivingEntity(rSupport)) {
                                          foundTorchSupport = rSupport;
                                       } else {
                                          rValid = false;
                                       }
                                    }
                                 } else if (!this.allowInAir.getValue()
                                    && this.isAirOrReplaceable(rPos.down())
                                    && !rPos.down().equals(basePos)
                                    && !rPos.down().equals(cPos)) {
                                    BlockPos rSupport = rPos.down();
                                    if (this.isWithinRange(rSupport) && !this.isBlockedByLivingEntity(rSupport)) {
                                       foundTorchSupport = rSupport;
                                    } else {
                                       rValid = false;
                                    }
                                 }
                              }
                           }

                           if (obsValid && cValid && pValid && rValid) {
                              int blockedCount = 0;
                              if (this.isManageableObstacle(basePos)) {
                                 blockedCount++;
                              }

                              if (this.isManageableObstacle(cPos)) {
                                 blockedCount++;
                              }

                              if (!this.isExistingValidPiston(pPos, pushDir) && this.isManageableObstacle(pPos)) {
                                 blockedCount++;
                              }

                              boolean isRBlockObstacle = this.isManageableObstacle(rPos);
                              if (this.useTorch.getValue()) {
                                 if (isRBlockObstacle
                                    && !mc.world.getBlockState(rPos).isOf(Blocks.REDSTONE_TORCH)
                                    && !mc.world.getBlockState(rPos).isOf(Blocks.REDSTONE_WALL_TORCH)) {
                                    blockedCount++;
                                 }
                              } else if (isRBlockObstacle && !mc.world.getBlockState(rPos).isOf(Blocks.REDSTONE_BLOCK)) {
                                 blockedCount++;
                              }

                              if (blockedCount < 2
                                 && this.isValidPushDestination(cPos, pushDir, tPos)
                                 && (cPos.getY() <= tPos.getY() || this.willCrystalFloatAfterPush(cPos, pushDir))) {
                                 this.isLongPattern = true;
                                 this.obsidianPos = basePos;
                                 this.crystalPos = cPos;
                                 this.pistonPos = pPos;
                                 this.redstonePos = rPos;
                                 this.pistonDirection = pushDir;
                                 this.pistonSupportPos = null;
                                 this.torchSupportPos = foundTorchSupport;
                                 BlockPos pushedPos = cPos.offset(pushDir);
                                 this.isAgainstWall = !this.isAirOrReplaceable(pushedPos.offset(pushDir));
                                 this.obstaclePos = null;
                                 if (this.isManageableObstacle(basePos)) {
                                    this.obstaclePos = basePos;
                                 } else if (this.isManageableObstacle(cPos)) {
                                    this.obstaclePos = cPos;
                                 } else if (!this.isExistingValidPiston(pPos, pushDir) && this.isManageableObstacle(pPos)) {
                                    this.obstaclePos = pPos;
                                 } else if (isRBlockObstacle
                                    && !mc.world.getBlockState(rPos).isOf(Blocks.REDSTONE_BLOCK)
                                    && !mc.world.getBlockState(rPos).isOf(Blocks.REDSTONE_TORCH)
                                    && !mc.world.getBlockState(rPos).isOf(Blocks.REDSTONE_WALL_TORCH)) {
                                    this.obstaclePos = rPos;
                                 }

                                 Direction lookDir = pushDir.getOpposite();
                                 switch (lookDir) {
                                    case SOUTH:
                                       this.lockedYaw = 0.0F;
                                       break;
                                    case WEST:
                                       this.lockedYaw = 90.0F;
                                       break;
                                    case NORTH:
                                       this.lockedYaw = 180.0F;
                                       break;
                                    case EAST:
                                       this.lockedYaw = -90.0F;
                                       break;
                                    default:
                                       this.lockedYaw = mc.player.getYaw();
                                 }

                                 this.lockedPitch = 0.0F;
                                 return true;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         return false;
      }
   }

   private boolean findLongSidePatternPositions(LivingEntity targetEntity, boolean requireExisting) {
      BlockPos tPos = targetEntity.getBlockPos();
      if (this.isCoveredByFlatBlocks(tPos)) {
         return false;
      } else {
         boolean isHeadClear = this.isAirOrReplaceable(tPos.up(1));
         Direction[] dirs = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
         int[] yOffsets = new int[]{0, 1, 2, -1, -2};
         int[] lateralOffsets = new int[]{0, 1, -1};

         for (int yOffset : yOffsets) {
            if (isHeadClear || yOffset < 1) {
               for (Direction dir : dirs) {
                  Direction pushDir = dir.getOpposite();
                  Direction sideDir = dir.rotateYClockwise();

                  for (int lat : lateralOffsets) {
                     BlockPos basePos = tPos.up(yOffset).offset(dir);
                     if (lat != 0) {
                        basePos = basePos.offset(sideDir, lat);
                     }

                     BlockPos cPos = basePos.up();
                     if (cPos.getY() >= tPos.getY()) {
                        BlockPos obsPos = basePos;
                        BlockPos gapPos = cPos.offset(dir);
                        BlockPos pPos = gapPos.offset(dir);
                        if (requireExisting) {
                           boolean hasBaseBlock = mc.world.getBlockState(basePos).isOf(Blocks.OBSIDIAN) || mc.world.getBlockState(basePos).isOf(Blocks.BEDROCK);
                           boolean hasCrystal = this.hasCrystalAt(cPos);
                           if (!hasBaseBlock && !hasCrystal) {
                              continue;
                           }
                        }

                        if (!this.allowInAir.getValue()) {
                           BlockPos underBase = basePos.down();
                           BlockState underState = mc.world.getBlockState(underBase);
                           if (underState.isAir() || underState.isReplaceable()) {
                              continue;
                           }
                        }

                        if (this.isWithinRange(basePos) && this.isWithinRange(cPos) && this.isWithinRange(pPos)) {
                           boolean feetBypass = cPos.getY() == tPos.getY() && gapPos.equals(tPos);
                           if ((feetBypass || this.isAirOrReplaceable(gapPos) && !this.hasAnyEntityAt(gapPos))
                              && !this.hasBlockingEntityAt(cPos)
                              && !this.isBlockedByLivingEntity(basePos)
                              && !this.isBlockedByLivingEntity(pPos)) {
                              boolean obsValid = this.isAirOrReplaceable(basePos)
                                 || mc.world.getBlockState(basePos).isOf(Blocks.OBSIDIAN)
                                 || mc.world.getBlockState(basePos).isOf(Blocks.BEDROCK)
                                 || this.isManageableObstacle(basePos);
                              boolean cValid = this.isAirOrReplaceable(cPos) || this.isManageableObstacle(cPos);
                              boolean pValid = this.isAirOrReplaceable(pPos) || this.isManageableObstacle(pPos) || this.isExistingValidPiston(pPos, pushDir);
                              if (obsValid && cValid && pValid) {
                                 BlockPos pSupport = null;
                                 if (!this.allowInAir.getValue()
                                    && this.isAirOrReplaceable(pPos.down())
                                    && !pPos.down().equals(cPos)
                                    && !pPos.down().equals(basePos)) {
                                    pSupport = pPos.down();
                                    if (!this.isWithinRange(pSupport) || this.isBlockedByLivingEntity(pSupport)) {
                                       continue;
                                    }

                                    if (requireExisting) {
                                       boolean pSupportExists = mc.world.getBlockState(pSupport).isOf(Blocks.OBSIDIAN)
                                          || mc.world.getBlockState(pSupport).isOf(Blocks.BEDROCK);
                                       if (!pSupportExists) {
                                          continue;
                                       }
                                    }
                                 }

                                 Direction[] redstoneDirs;
                                 if (this.useTorch.getValue()) {
                                    redstoneDirs = new Direction[]{dir.rotateYClockwise(), dir.rotateYCounterclockwise(), dir, Direction.UP};
                                 } else {
                                    redstoneDirs = new Direction[]{dir.rotateYClockwise(), dir.rotateYCounterclockwise(), dir, Direction.UP};
                                 }

                                 BlockPos foundRedstonePos = null;
                                 BlockPos foundTorchSupport = null;
                                 Direction[] blockedCount = redstoneDirs;
                                 int pushedPos = redstoneDirs.length;
                                 int lookDir = 0;

                                 while (lookDir < pushedPos) {
                                    BlockPos rPos;
                                    label411: {
                                       Direction rDir = blockedCount[lookDir];
                                       rPos = pPos.offset(rDir);
                                       if (!rPos.equals(cPos)
                                          && !rPos.equals(obsPos)
                                          && !rPos.equals(gapPos)
                                          && this.isWithinRange(rPos)
                                          && !this.isBlockedByLivingEntity(rPos)) {
                                          if (this.useTorch.getValue()) {
                                             BlockPos tSupport;
                                             if (rDir == Direction.UP) {
                                                tSupport = rPos.offset(dir);
                                             } else {
                                                tSupport = rPos.down();
                                             }

                                             if (!tSupport.equals(cPos)
                                                && !tSupport.equals(obsPos)
                                                && !tSupport.equals(pPos)
                                                && !tSupport.equals(gapPos)
                                                && this.isWithinRange(tSupport)
                                                && !this.isBlockedByLivingEntity(tSupport)
                                                && (this.isAirOrReplaceable(rPos) || this.isManageableObstacle(rPos))) {
                                                foundRedstonePos = rPos;
                                                foundTorchSupport = tSupport;
                                                break;
                                             }
                                          } else if (this.isAirOrReplaceable(rPos) || this.isManageableObstacle(rPos)) {
                                             if (this.blockSupport.getValue()) {
                                                BlockPos rSupport = rPos.down();
                                                if (!rSupport.equals(cPos) && !rSupport.equals(obsPos) && !rSupport.equals(pPos) && !rSupport.equals(gapPos)) {
                                                   if (!this.isAirOrReplaceable(rSupport)) {
                                                      break label411;
                                                   }

                                                   if (this.isWithinRange(rSupport) && !this.isBlockedByLivingEntity(rSupport)) {
                                                      foundTorchSupport = rSupport;
                                                      break label411;
                                                   }
                                                }
                                             } else {
                                                if (this.allowInAir.getValue()
                                                   || !this.isAirOrReplaceable(rPos.down())
                                                   || rPos.down().equals(cPos)
                                                   || rPos.down().equals(obsPos)
                                                   || rPos.down().equals(pPos)
                                                   || rPos.down().equals(gapPos)) {
                                                   break label411;
                                                }

                                                BlockPos rSupport = rPos.down();
                                                if (this.isWithinRange(rSupport) && !this.isBlockedByLivingEntity(rSupport)) {
                                                   foundTorchSupport = rSupport;
                                                   break label411;
                                                }
                                             }
                                          }
                                       }

                                       lookDir++;
                                       continue;
                                    }

                                    foundRedstonePos = rPos;
                                    break;
                                 }

                                 if (foundRedstonePos != null) {
                                    int blockedCountx = 0;
                                    if (this.isManageableObstacle(obsPos)) {
                                       blockedCountx++;
                                    }

                                    if (this.isManageableObstacle(cPos)) {
                                       blockedCountx++;
                                    }

                                    if (!this.isExistingValidPiston(pPos, pushDir) && this.isManageableObstacle(pPos)) {
                                       blockedCountx++;
                                    }

                                    if (this.isManageableObstacle(foundRedstonePos)) {
                                       blockedCountx++;
                                    }

                                    if (blockedCountx < 2
                                       && this.isValidPushDestination(cPos, pushDir, tPos)
                                       && (cPos.getY() <= tPos.getY() || this.willCrystalFloatAfterPush(cPos, pushDir))) {
                                       this.isLongSidePattern = true;
                                       this.obsidianPos = obsPos;
                                       this.crystalPos = cPos;
                                       this.pistonPos = pPos;
                                       this.redstonePos = foundRedstonePos;
                                       this.pistonDirection = pushDir;
                                       this.pistonSupportPos = pSupport;
                                       this.torchSupportPos = foundTorchSupport;
                                       BlockPos pushedPosx = cPos.offset(pushDir);
                                       this.isAgainstWall = !this.isAirOrReplaceable(pushedPosx.offset(pushDir));
                                       this.obstaclePos = null;
                                       if (this.isManageableObstacle(obsPos)) {
                                          this.obstaclePos = obsPos;
                                       } else if (this.isManageableObstacle(cPos)) {
                                          this.obstaclePos = cPos;
                                       } else if (!this.isExistingValidPiston(pPos, pushDir) && this.isManageableObstacle(pPos)) {
                                          this.obstaclePos = pPos;
                                       } else if (this.isManageableObstacle(foundRedstonePos)) {
                                          this.obstaclePos = foundRedstonePos;
                                       } else if (this.torchSupportPos != null && this.isManageableObstacle(this.torchSupportPos)) {
                                          this.obstaclePos = this.torchSupportPos;
                                       }

                                       Direction lookDirx = pushDir.getOpposite();
                                       switch (lookDirx) {
                                          case SOUTH:
                                             this.lockedYaw = 0.0F;
                                             break;
                                          case WEST:
                                             this.lockedYaw = 90.0F;
                                             break;
                                          case NORTH:
                                             this.lockedYaw = 180.0F;
                                             break;
                                          case EAST:
                                             this.lockedYaw = -90.0F;
                                             break;
                                          default:
                                             this.lockedYaw = mc.player.getYaw();
                                       }

                                       this.lockedPitch = 0.0F;
                                       return true;
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

         return false;
      }
   }

   private boolean findPistonCrystalPositions(LivingEntity targetEntity) {
      BlockPos tPos = targetEntity.getBlockPos();
      if (this.isCoveredByFlatBlocks(tPos)) {
         return false;
      } else {
         boolean isHeadClear = this.isAirOrReplaceable(tPos.up(1));
         Direction[] dirs = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
         int[] yOffsets = new int[]{0, 1, 2, -1, -2};
         int[] pistonLevels = new int[]{0, 1};
         int[] strategyPasses = new int[]{1, 0, 2};
         boolean[] requireExistingPistonPasses = new boolean[]{true, false};

         for (boolean requireExisting : requireExistingPistonPasses) {
            for (int pLevel : pistonLevels) {
               for (int pass : strategyPasses) {
                  boolean isSide = pass == 2;
                  boolean isNewStrat = pass == 1;

                  for (int yOffset : yOffsets) {
                     if (isHeadClear || yOffset < 1) {
                        for (Direction dir : dirs) {
                           Direction[] sideDirs = isSide ? new Direction[]{dir.rotateYClockwise(), dir.rotateYCounterclockwise()} : new Direction[]{null};

                           label768:
                           for (Direction sideDir : sideDirs) {
                              if (isNewStrat) {
                                 Direction pushDir = dir.getOpposite();

                                 for (int dist = 1; dist <= 1; dist++) {
                                    BlockPos pPos = tPos.up(yOffset);

                                    for (int d = 0; d < dist; d++) {
                                       pPos = pPos.offset(dir);
                                    }

                                    if (sideDir != null) {
                                       pPos = pPos.offset(sideDir);
                                    }

                                    if (this.isWithinRange(pPos) && !this.isBlockedByLivingEntity(pPos)) {
                                       boolean isExistingPiston = this.isExistingValidPiston(pPos, pushDir);
                                       if (!requireExisting || isExistingPiston) {
                                          BlockPos behindPiston = pPos.offset(dir);
                                          BlockState behindState = mc.world.getBlockState(behindPiston);
                                          if (behindState.isOf(Blocks.OBSIDIAN) || behindState.isOf(Blocks.BEDROCK)) {
                                             BlockPos pistonHeadPos = pPos.offset(pushDir);
                                             if (this.isAirOrReplaceable(pistonHeadPos) || this.isManageableObstacle(pistonHeadPos)) {
                                                boolean isFarFromPlayer = this.getBoxDistance(mc.player.getEyePos(), Vec3d.ofCenter(pPos)) > 2.5;
                                                BlockPos[] cPosOptions = isFarFromPlayer
                                                   ? new BlockPos[]{pPos.down().offset(pushDir), pPos.down()}
                                                   : new BlockPos[]{pPos.down(), pPos.down().offset(pushDir)};

                                                for (BlockPos candidateCPos : cPosOptions) {
                                                   if (this.willPistonHitCrystal(pPos, pushDir, candidateCPos)
                                                      && candidateCPos.getY() >= tPos.getY()
                                                      && this.isValidPushDestination(candidateCPos, pushDir, tPos)
                                                      && (candidateCPos.getY() <= tPos.getY() || this.willCrystalFloatAfterPush(candidateCPos, pushDir))) {
                                                      BlockPos candidateObsPos = candidateCPos.down();
                                                      if (this.isWithinRange(candidateObsPos)
                                                         && this.isWithinRange(candidateCPos)
                                                         && !this.hasBlockingEntityAt(candidateCPos)
                                                         && !this.isBlockedByLivingEntity(candidateObsPos)) {
                                                         if (!this.allowInAir.getValue()) {
                                                            BlockPos underBase = candidateObsPos.down();
                                                            BlockState underState = mc.world.getBlockState(underBase);
                                                            if (underState.isAir() || underState.isReplaceable()) {
                                                               continue;
                                                            }
                                                         }

                                                         boolean obsValid = this.isAirOrReplaceable(candidateObsPos)
                                                            || mc.world.getBlockState(candidateObsPos).isOf(Blocks.OBSIDIAN)
                                                            || this.isManageableObstacle(candidateObsPos);
                                                         boolean cValid = this.isAirOrReplaceable(candidateCPos) || this.isManageableObstacle(candidateCPos);
                                                         boolean pValid = this.isAirOrReplaceable(pPos) || this.isManageableObstacle(pPos) || isExistingPiston;
                                                         if (obsValid && cValid && pValid) {
                                                            List<BlockPos> rPosOptions = new ArrayList<>();
                                                            rPosOptions.add(pPos.up());
                                                            if (this.useTorch.getValue()) {
                                                               rPosOptions.add(pPos.offset(dir.rotateYClockwise()));
                                                               rPosOptions.add(pPos.offset(dir.rotateYCounterclockwise()));
                                                            }

                                                            BlockPos foundRedstonePos = null;
                                                            BlockPos foundTorchSupport = null;

                                                            for (BlockPos candidateRPos : rPosOptions) {
                                                               if (!candidateRPos.equals(candidateCPos)
                                                                  && !candidateRPos.equals(candidateObsPos)
                                                                  && !candidateRPos.equals(pPos)
                                                                  && this.isWithinRange(candidateRPos)
                                                                  && !this.isBlockedByLivingEntity(candidateRPos)) {
                                                                  if (this.useTorch.getValue()) {
                                                                     BlockPos tSupport;
                                                                     if (candidateRPos.equals(pPos.up())) {
                                                                        tSupport = candidateRPos.offset(dir);
                                                                     } else {
                                                                        tSupport = candidateRPos.down();
                                                                     }

                                                                     if (!tSupport.equals(candidateCPos)
                                                                        && !tSupport.equals(candidateObsPos)
                                                                        && !tSupport.equals(pPos)
                                                                        && this.isWithinRange(tSupport)
                                                                        && !this.isBlockedByLivingEntity(tSupport)
                                                                        && (this.isAirOrReplaceable(candidateRPos) || this.isManageableObstacle(candidateRPos))
                                                                        )
                                                                      {
                                                                        foundRedstonePos = candidateRPos;
                                                                        foundTorchSupport = tSupport;
                                                                        break;
                                                                     }
                                                                  } else if (this.isAirOrReplaceable(candidateRPos) || this.isManageableObstacle(candidateRPos)
                                                                     )
                                                                   {
                                                                     if (this.blockSupport.getValue()) {
                                                                        BlockPos rSupport = candidateRPos.down();
                                                                        if (rSupport.equals(candidateCPos)
                                                                           || rSupport.equals(candidateObsPos)
                                                                           || rSupport.equals(pPos)) {
                                                                           continue;
                                                                        }

                                                                        if (this.isAirOrReplaceable(rSupport)) {
                                                                           if (!this.isWithinRange(rSupport) || this.isBlockedByLivingEntity(rSupport)) {
                                                                              continue;
                                                                           }

                                                                           foundTorchSupport = rSupport;
                                                                        }
                                                                     }

                                                                     foundRedstonePos = candidateRPos;
                                                                     if (this.useTorch.getValue()) {
                                                                        foundTorchSupport = pPos;
                                                                     }
                                                                     break;
                                                                  }
                                                               }
                                                            }

                                                            if (foundRedstonePos != null) {
                                                               int blockedCount = 0;
                                                               if (this.isManageableObstacle(candidateObsPos)) {
                                                                  blockedCount++;
                                                               }

                                                               if (this.isManageableObstacle(candidateCPos)) {
                                                                  blockedCount++;
                                                               }

                                                               if (!isExistingPiston && this.isManageableObstacle(pPos)) {
                                                                  blockedCount++;
                                                               }

                                                               if (this.isManageableObstacle(pistonHeadPos)) {
                                                                  blockedCount++;
                                                               }

                                                               if (blockedCount < 2) {
                                                                  BlockPos pSupport = null;
                                                                  this.obsidianPos = candidateObsPos;
                                                                  this.crystalPos = candidateCPos;
                                                                  this.pistonPos = pPos;
                                                                  this.redstonePos = foundRedstonePos;
                                                                  this.pistonDirection = pushDir;
                                                                  this.pistonSupportPos = pSupport;
                                                                  this.torchSupportPos = foundTorchSupport;
                                                                  this.obstaclePos = null;
                                                                  if (this.isManageableObstacle(this.obsidianPos)) {
                                                                     this.obstaclePos = this.obsidianPos;
                                                                  } else if (this.isManageableObstacle(this.crystalPos)) {
                                                                     this.obstaclePos = this.crystalPos;
                                                                  } else if (!isExistingPiston && this.isManageableObstacle(this.pistonPos)) {
                                                                     this.obstaclePos = this.pistonPos;
                                                                  } else if (this.isManageableObstacle(pistonHeadPos)) {
                                                                     this.obstaclePos = pistonHeadPos;
                                                                  } else if (this.isManageableObstacle(this.redstonePos)) {
                                                                     this.obstaclePos = this.redstonePos;
                                                                  } else if (this.torchSupportPos != null
                                                                     && !this.torchSupportPos.equals(pPos)
                                                                     && this.isManageableObstacle(this.torchSupportPos)) {
                                                                     this.obstaclePos = this.torchSupportPos;
                                                                  }

                                                                  Direction lookDir = pushDir.getOpposite();
                                                                  switch (lookDir) {
                                                                     case SOUTH:
                                                                        this.lockedYaw = 0.0F;
                                                                        break;
                                                                     case WEST:
                                                                        this.lockedYaw = 90.0F;
                                                                        break;
                                                                     case NORTH:
                                                                        this.lockedYaw = 180.0F;
                                                                        break;
                                                                     case EAST:
                                                                        this.lockedYaw = -90.0F;
                                                                        break;
                                                                     default:
                                                                        this.lockedYaw = mc.player.getYaw();
                                                                  }

                                                                  this.lockedPitch = 0.0F;
                                                                  return true;
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
                              } else {
                                 BlockPos basePos = tPos.up(yOffset).offset(dir);
                                 if (sideDir != null) {
                                    basePos = basePos.offset(sideDir);
                                 }

                                 if (!this.allowInAir.getValue()) {
                                    BlockPos underBase = basePos.down();
                                    BlockState underState = mc.world.getBlockState(underBase);
                                    if (underState.isAir() || underState.isReplaceable()) {
                                       continue;
                                    }
                                 }

                                 BlockPos cPos = basePos.up();
                                 if (cPos.getY() >= tPos.getY()
                                    && this.isWithinRange(basePos)
                                    && this.isWithinRange(cPos)
                                    && !this.hasBlockingEntityAt(cPos)
                                    && !this.isBlockedByLivingEntity(basePos)) {
                                    boolean baseValid = this.isAirOrReplaceable(basePos)
                                       || mc.world.getBlockState(basePos).isOf(Blocks.OBSIDIAN)
                                       || this.isManageableObstacle(basePos);
                                    boolean cValid = this.isAirOrReplaceable(cPos) || this.isManageableObstacle(cPos);
                                    Direction pushDir = dir.getOpposite();
                                    List<BlockPos> headPositions = new ArrayList<>();
                                    if (pLevel == 0) {
                                       headPositions.add(cPos);
                                    }

                                    int[] dxs = new int[]{0, 1, -1};
                                    int[] dys = new int[]{pLevel};
                                    int[] dzs = new int[]{0, 1, -1};

                                    for (int dy : dys) {
                                       for (int dx : dxs) {
                                          for (int dz : dzs) {
                                             BlockPos h = cPos.add(dx, dy, dz);
                                             if (!headPositions.contains(h)) {
                                                headPositions.add(h);
                                             }
                                          }
                                       }
                                    }

                                    Iterator var76 = headPositions.iterator();

                                    while (true) {
                                       BlockPos H;
                                       BlockPos pPos;
                                       boolean isExistingPiston;
                                       boolean pValid;
                                       BlockPos pSupport;
                                       while (true) {
                                          if (!var76.hasNext()) {
                                             continue label768;
                                          }

                                          H = (BlockPos)var76.next();
                                          if (H.equals(cPos) || this.isAirOrReplaceable(H)) {
                                             pPos = H.offset(dir);
                                             if (!pPos.equals(cPos) && !pPos.equals(basePos) && this.isWithinRange(pPos) && !this.isBlockedByLivingEntity(pPos)
                                                )
                                              {
                                                isExistingPiston = this.isExistingValidPiston(pPos, pushDir);
                                                if ((!requireExisting || isExistingPiston)
                                                   && this.willPistonHitCrystal(pPos, pushDir, cPos)
                                                   && this.isValidPushDestination(cPos, pushDir, tPos)
                                                   && (cPos.getY() <= tPos.getY() || this.willCrystalFloatAfterPush(cPos, pushDir))) {
                                                   pValid = this.isAirOrReplaceable(pPos) || this.isManageableObstacle(pPos) || isExistingPiston;
                                                   pSupport = null;
                                                   if (!this.isAirOrReplaceable(pPos.down()) || pPos.down().equals(cPos) || pPos.down().equals(basePos)) {
                                                      break;
                                                   }

                                                   pSupport = pPos.down();
                                                   if (this.isWithinRange(pSupport) && !this.isBlockedByLivingEntity(pSupport)) {
                                                      break;
                                                   }
                                                }
                                             }
                                          }
                                       }

                                       boolean isPistonAbove = H.getY() > cPos.getY();
                                       boolean isPistonSameHeight = H.getY() == cPos.getY();
                                       boolean isPistonBeside = false;
                                       if (dir.getAxis() == Axis.Z) {
                                          isPistonBeside = pPos.getX() != cPos.getX();
                                       } else if (dir.getAxis() == Axis.X) {
                                          isPistonBeside = pPos.getZ() != cPos.getZ();
                                       }

                                       Direction[] redstoneDirs;
                                       if (this.useTorch.getValue()) {
                                          if (isPistonAbove) {
                                             redstoneDirs = new Direction[]{Direction.UP, dir.rotateYClockwise(), dir.rotateYCounterclockwise(), dir};
                                          } else {
                                             redstoneDirs = new Direction[]{dir.rotateYClockwise(), dir.rotateYCounterclockwise(), dir, Direction.UP};
                                          }
                                       } else if (!isPistonAbove && !isPistonBeside) {
                                          if (isPistonSameHeight) {
                                             redstoneDirs = new Direction[]{dir};
                                          } else {
                                             redstoneDirs = new Direction[]{dir, Direction.UP};
                                          }
                                       } else if (isPistonSameHeight) {
                                          redstoneDirs = new Direction[]{dir.rotateYClockwise(), dir.rotateYCounterclockwise(), dir};
                                       } else {
                                          redstoneDirs = new Direction[]{dir.rotateYClockwise(), dir.rotateYCounterclockwise(), dir, Direction.UP};
                                       }

                                       BlockPos foundRedstonePos = null;
                                       BlockPos foundTorchSupport = null;
                                       Direction[] var103 = redstoneDirs;
                                       int lookDir = redstoneDirs.length;
                                       int var59 = 0;

                                       while (var59 < lookDir) {
                                          BlockPos rPos;
                                          label862: {
                                             Direction rDir = var103[var59];
                                             rPos = pPos.offset(rDir);
                                             label722:
                                             if (!rPos.equals(cPos)
                                                && !rPos.equals(basePos)
                                                && !rPos.equals(H)
                                                && this.isWithinRange(rPos)
                                                && !this.isBlockedByLivingEntity(rPos)) {
                                                if (!this.useTorch.getValue() && rPos.getY() == cPos.getY()) {
                                                   int dx = Math.abs(rPos.getX() - cPos.getX());
                                                   int dzx = Math.abs(rPos.getZ() - cPos.getZ());
                                                   if (dx <= 1 && dzx <= 1) {
                                                      break label722;
                                                   }
                                                }

                                                if (this.useTorch.getValue()) {
                                                   BlockPos tSupportx;
                                                   if (rDir == Direction.UP) {
                                                      tSupportx = rPos.offset(dir);
                                                   } else {
                                                      tSupportx = rPos.down();
                                                   }

                                                   if (!tSupportx.equals(cPos)
                                                      && !tSupportx.equals(basePos)
                                                      && !tSupportx.equals(pPos)
                                                      && !tSupportx.equals(H)
                                                      && this.isWithinRange(tSupportx)
                                                      && !this.isBlockedByLivingEntity(tSupportx)
                                                      && (this.isAirOrReplaceable(rPos) || this.isManageableObstacle(rPos))) {
                                                      foundRedstonePos = rPos;
                                                      foundTorchSupport = tSupportx;
                                                      break;
                                                   }
                                                } else if (this.isAirOrReplaceable(rPos) || this.isManageableObstacle(rPos)) {
                                                   if (!this.blockSupport.getValue()) {
                                                      break label862;
                                                   }

                                                   BlockPos rSupportx = rPos.down();
                                                   if (!rSupportx.equals(cPos) && !rSupportx.equals(basePos) && !rSupportx.equals(pPos) && !rSupportx.equals(H)
                                                      )
                                                    {
                                                      if (!this.isAirOrReplaceable(rSupportx)) {
                                                         break label862;
                                                      }

                                                      if (this.isWithinRange(rSupportx) && !this.isBlockedByLivingEntity(rSupportx)) {
                                                         foundTorchSupport = rSupportx;
                                                         break label862;
                                                      }
                                                   }
                                                }
                                             }

                                             var59++;
                                             continue;
                                          }

                                          foundRedstonePos = rPos;
                                          break;
                                       }

                                       if (foundRedstonePos != null) {
                                          int blockedCountx = 0;
                                          if (this.isManageableObstacle(basePos)) {
                                             blockedCountx++;
                                          }

                                          if (this.isManageableObstacle(cPos)) {
                                             blockedCountx++;
                                          }

                                          if (!isExistingPiston && this.isManageableObstacle(pPos)) {
                                             blockedCountx++;
                                          }

                                          if (blockedCountx < 2 && baseValid && cValid && pValid) {
                                             this.obsidianPos = basePos;
                                             this.crystalPos = cPos;
                                             this.pistonPos = pPos;
                                             this.redstonePos = foundRedstonePos;
                                             this.pistonDirection = pushDir;
                                             this.pistonSupportPos = pSupport;
                                             this.torchSupportPos = foundTorchSupport;
                                             this.obstaclePos = null;
                                             if (this.isManageableObstacle(basePos)) {
                                                this.obstaclePos = basePos;
                                             } else if (this.isManageableObstacle(cPos)) {
                                                this.obstaclePos = cPos;
                                             } else if (!isExistingPiston && this.isManageableObstacle(this.pistonPos)) {
                                                this.obstaclePos = this.pistonPos;
                                             } else if (this.isManageableObstacle(this.redstonePos)) {
                                                this.obstaclePos = this.redstonePos;
                                             } else if (this.torchSupportPos != null && this.isManageableObstacle(this.torchSupportPos)) {
                                                this.obstaclePos = this.torchSupportPos;
                                             }

                                             Direction lookDirx = pushDir.getOpposite();
                                             switch (lookDirx) {
                                                case SOUTH:
                                                   this.lockedYaw = 0.0F;
                                                   break;
                                                case WEST:
                                                   this.lockedYaw = 90.0F;
                                                   break;
                                                case NORTH:
                                                   this.lockedYaw = 180.0F;
                                                   break;
                                                case EAST:
                                                   this.lockedYaw = -90.0F;
                                                   break;
                                                default:
                                                   this.lockedYaw = mc.player.getYaw();
                                             }

                                             this.lockedPitch = 0.0F;
                                             return true;
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

         return false;
      }
   }

   private void executeAction(int slot, Runnable action) {
      if (this.silent.getValue()) {
         this.withSilent(slot, action);
      } else {
         int original = mc.player.getInventory().selectedSlot;
         if (slot != original) {
            mc.player.getInventory().selectedSlot = slot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         }

         action.run();
      }
   }

   private void withSilent(int slot, Runnable action) {
      int original = mc.player.getInventory().selectedSlot;
      if (slot != original) {
         mc.player.getInventory().selectedSlot = slot;
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      }

      action.run();
      if (slot != original) {
         mc.player.getInventory().selectedSlot = original;
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(original));
      }
   }

   private void placeBlockAt(BlockPos pos, boolean packetOnly) {
      Direction[] dirs = new Direction[]{Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP};

      for (Direction dir : dirs) {
         BlockPos nb = pos.offset(dir);
         if (!this.isAirOrReplaceable(nb)) {
            Direction face = dir.getOpposite();
            if (!this.strictPlace.getValue() || this.isStrictDirectionValid(nb, face)) {
               this.doPlace(nb, face);
               return;
            }
         }
      }

      if (this.strictPlace.getValue()) {
         for (Direction dirx : dirs) {
            BlockPos nb = pos.offset(dirx);
            if (!this.isAirOrReplaceable(nb)) {
               Direction face = dirx.getOpposite();
               this.doPlace(nb, face);
               return;
            }
         }
      }

      this.doPlace(pos, Direction.UP);
   }

   private void placeBlockOnTop(BlockPos supportPos) {
      Direction face = Direction.UP;
      if (this.strictPlace.getValue() && !this.isStrictDirectionValid(supportPos, face)) {
         Direction[] dirs = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN};

         for (Direction d : dirs) {
            if (this.isStrictDirectionValid(supportPos, d)) {
               face = d;
               break;
            }
         }
      }

      this.doPlace(supportPos, face);
   }

   private void placeBlockAgainst(BlockPos pos, BlockPos support) {
      Direction face = null;

      for (Direction d : Direction.values()) {
         if (support.offset(d).equals(pos)) {
            face = d;
            break;
         }
      }

      if (face != null) {
         if (this.strictPlace.getValue() && !this.isStrictDirectionValid(support, face)) {
            this.placeBlockAt(pos, false);
            return;
         }

         this.doPlace(support, face);
      } else {
         this.placeBlockAt(pos, false);
      }
   }

   private void placeBlockAtDirectional(BlockPos pos, Direction pushDir) {
      BlockPos neighbor = null;
      Direction hitDirection = null;
      Direction[] dirs = new Direction[]{Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP};

      for (Direction dir : dirs) {
         BlockPos nb = pos.offset(dir);
         if (!this.isAirOrReplaceable(nb)) {
            Direction face = dir.getOpposite();
            if (!this.strictPlace.getValue() || this.isStrictDirectionValid(nb, face)) {
               neighbor = nb;
               hitDirection = face;
               break;
            }
         }
      }

      if (neighbor == null) {
         for (Direction dirx : dirs) {
            BlockPos nb = pos.offset(dirx);
            if (!this.isAirOrReplaceable(nb)) {
               neighbor = nb;
               hitDirection = dirx.getOpposite();
               break;
            }
         }
      }

      if (neighbor == null) {
         neighbor = pos;
         hitDirection = Direction.UP;
      }

      Vec3d hitVec = Vec3d.ofCenter(neighbor).add(hitDirection.getOffsetX() * 0.5, hitDirection.getOffsetY() * 0.5, hitDirection.getOffsetZ() * 0.5);
      BlockHitResult hitResult = new BlockHitResult(hitVec, hitDirection, neighbor, false);
      float preYaw = mc.player.getYaw();
      float prePitch = mc.player.getPitch();
      float[] look = this.calcLookAtPos(hitVec);
      float finalYaw = this.lockedYaw != null ? this.lockedYaw : look[0];
      float finalPitch = look[1];
      if (finalPitch > 40.0F) {
         finalPitch = 40.0F;
      }

      if (finalPitch < -40.0F) {
         finalPitch = -40.0F;
      }

      this.sendLookPacket(finalYaw, finalPitch);
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
      mc.player.swingHand(Hand.MAIN_HAND);
      if (!this.rotate.getValue()) {
         this.sendLookPacket(preYaw, prePitch);
      }
   }

   private void placeCrystalAt(BlockPos pos) {
      float preYaw = mc.player.getYaw();
      float prePitch = mc.player.getPitch();
      Vec3d hitVec = Vec3d.ofCenter(pos).add(0.0, 0.5, 0.0);
      float[] look = this.calcLookAtPos(hitVec);
      this.sendLookPacket(look[0], look[1]);
      BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, false);
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
      mc.player.swingHand(Hand.MAIN_HAND);
      if (!this.rotate.getValue()) {
         this.sendLookPacket(preYaw, prePitch);
      }
   }

   private boolean explodeCrystal() {
      if (this.targetCrystal != null) {
         this.attackCrystal(this.targetCrystal);
         this.targetCrystal = null;
         return true;
      } else if (this.crystalPos != null && this.pistonDirection != null) {
         Vec3d targetCenter = Vec3d.ofCenter(this.crystalPos).add(this.pistonDirection.getOffsetX(), 0.0, this.pistonDirection.getOffsetZ());
         EndCrystalEntity crystal = this.findNearestCrystal(targetCenter, 2.0);
         if (crystal == null) {
            return false;
         } else {
            this.attackCrystal(crystal);
            return true;
         }
      } else {
         return false;
      }
   }

   private void attackCrystal(Entity crystal) {
      if (crystal != null) {
         float preYaw = mc.player.getYaw();
         float prePitch = mc.player.getPitch();
         float[] look = this.calcLookAtPos(crystal.getEntityPos().add(0.0, 0.5, 0.0));
         this.sendLookPacket(look[0], look[1]);
         mc.interactionManager.attackEntity(mc.player, crystal);
         mc.player.swingHand(Hand.MAIN_HAND);
         if (!this.rotate.getValue()) {
            this.sendLookPacket(preYaw, prePitch);
         }
      }
   }

   private boolean isCrystalPushed() {
      if (this.crystalPos != null && this.pistonDirection != null) {
         Vec3d startPos = new Vec3d(this.crystalPos.getX() + 0.5, this.crystalPos.getY(), this.crystalPos.getZ() + 0.5);
         EndCrystalEntity crystal = this.findNearestCrystal(startPos, 1.5);
         if (crystal == null) {
            return false;
         } else {
            double movedX = (crystal.getEntityPos().x - startPos.x) * this.pistonDirection.getOffsetX();
            double movedY = (crystal.getEntityPos().y - startPos.y) * this.pistonDirection.getOffsetY();
            double movedZ = (crystal.getEntityPos().z - startPos.z) * this.pistonDirection.getOffsetZ();
            double movedAmount = Math.max(movedX, Math.max(movedY, movedZ));
            if (!this.isLongPattern && !this.isLongSidePattern) {
               if (movedAmount >= this.pushDistance.getValue()) {
                  this.targetCrystal = crystal;
                  return true;
               } else {
                  return false;
               }
            } else if (movedAmount >= this.longPushDistance.getValue()) {
               this.targetCrystal = crystal;
               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private void replenishItem(Item item) {
      int hotbarSlot = this.findInHotbar(item);
      if (hotbarSlot == -1 || mc.player.getInventory().getStack(hotbarSlot).getCount() <= this.replenishThreshold.getValue()) {
         int invSlot = this.findInInventory(item);
         if (invSlot != -1) {
            int targetHotbar = hotbarSlot != -1 ? hotbarSlot : this.getEmptyHotbarSlot();
            if (targetHotbar != -1) {
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, invSlot, targetHotbar, SlotActionType.SWAP, mc.player);
            }
         }
      }
   }

   private int findInInventory(Item item) {
      for (int i = 9; i < 36; i++) {
         if (mc.player.getInventory().getStack(i).getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   private int getEmptyHotbarSlot() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   private int findInHotbar(Item item) {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   private int getHotbarItemCount(Item item) {
      int count = 0;
      if (mc.player == null) {
         return 0;
      } else {
         for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
               count += stack.getCount();
            }
         }

         return count;
      }
   }

   private int getInventoryItemCount(Item item) {
      int count = 0;
      if (mc.player == null) {
         return 0;
      } else {
         for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
               count += stack.getCount();
            }
         }

         if (mc.player.getOffHandStack().getItem() == item) {
            count += mc.player.getOffHandStack().getCount();
         }

         return count;
      }
   }

   private LivingEntity findNearestTarget() {
      LivingEntity nearest = null;
      double nearestDist = this.range.getValue();

      for (PlayerEntity e : mc.world.getPlayers()) {
         if (e != mc.player && !e.isDead() && !ChimeraClient.friendManager.isFriend(e)) {
            double dist = this.getBoxDistance(mc.player.getEyePos(), e.getEntityPos());
            if (dist < nearestDist) {
               nearestDist = dist;
               nearest = e;
            }
         }
      }

      return nearest;
   }

   private EndCrystalEntity findNearestCrystal(Vec3d center, double maxDist) {
      EndCrystalEntity nearest = null;
      double best = maxDist;
      Box box = new Box(center.x - maxDist, center.y - maxDist, center.z - maxDist, center.x + maxDist, center.y + maxDist, center.z + maxDist);

      for (EndCrystalEntity c : mc.world.getNonSpectatingEntities(EndCrystalEntity.class, box)) {
         double dist = c.getEntityPos().distanceTo(center);
         if (dist < best) {
            best = dist;
            nearest = c;
         }
      }

      return nearest;
   }

   private void softRestart() {
      this.restoreMiningSlot();
      this.isMining = false;
      this.retryAfterFailedPush = false;
      this.redstonePlacedSimultaneously = false;
      this.targetCrystal = null;
      this.pistonTickCounter = 0;
      if (!this.isLongPattern && !this.isLongSidePattern) {
         if (this.pistonPos != null) {
            BlockState state = mc.world.getBlockState(this.pistonPos);
            boolean pistonExists = state.isOf(Blocks.PISTON) || state.isOf(Blocks.STICKY_PISTON) || state.isOf(Blocks.MOVING_PISTON);
            if (pistonExists) {
               this.setPhase(PistonCrystal.Phase.WAITING_FOR_CRYSTAL);
               return;
            }
         }

         this.setPhase(PistonCrystal.Phase.TURNING_FOR_PISTON);
      } else {
         this.setPhase(PistonCrystal.Phase.PLACING_LONG_REDSTONE);
      }
   }

   private void resetState() {
      this.restoreMiningSlot();
      this.target = null;
      this.obsidianPos = null;
      this.pistonPos = null;
      this.redstonePos = null;
      this.crystalPos = null;
      this.pistonSupportPos = null;
      this.torchSupportPos = null;
      this.obstaclePos = null;
      this.isMining = false;
      this.isLongPattern = false;
      this.isLongSidePattern = false;
      this.isAgainstWall = false;
      this.phase = PistonCrystal.Phase.IDLE;
      this.lockedYaw = null;
      this.lockedPitch = null;
      this.pistonTickCounter = 0;
      this.retryAfterFailedPush = false;
      this.redstonePlacedSimultaneously = false;
      this.targetCrystal = null;
      this.actionTicks = 0;
   }

   @Subscribe
   @Override
   public void onRender3D(Render3DEvent event) {
      if (this.render.getValue() && mc.player != null) {
         if (this.obsidianPos != null) {
            RenderUtil.drawBox(event.getMatrix(), new Box(this.obsidianPos), this.obsidianColor.getValue(), this.lineWidth.getValue());
         }

         if (this.crystalPos != null) {
            RenderUtil.drawBox(event.getMatrix(), new Box(this.crystalPos), this.crystalColor.getValue(), this.lineWidth.getValue());
         }

         if (this.pistonPos != null) {
            RenderUtil.drawBox(event.getMatrix(), new Box(this.pistonPos), this.pistonColor.getValue(), this.lineWidth.getValue());
         }

         if (this.redstonePos != null) {
            RenderUtil.drawBox(event.getMatrix(), new Box(this.redstonePos), this.redstoneColor.getValue(), this.lineWidth.getValue());
         }
      }
   }

   @Override
   public String getDisplayInfo() {
      if (this.target == null) {
         return "§7None";
      } else {
         if (mc.player != null && mc.player.isUsingItem()) {
            ItemStack activeItem = mc.player.getActiveItem();
            if (activeItem.contains(DataComponentTypes.FOOD) || activeItem.getItem() == Items.POTION) {
               return "§ePaused";
            }
         }

         return "§a"
            + this.target.getName().getString()
            + " "
            + this.phase.name()
            + (this.isLongPattern ? " (Long)" : (this.isLongSidePattern ? " (LongSide)" : ""));
      }
   }

   private static enum Phase {
      IDLE,
      CLEARING_ENTITIES,
      BREAKING_OBSTACLE,
      PLACING_SUPPORT,
      PLACING_TORCH_SUPPORT,
      PLACING_BASE,
      PLACING_LONG_REDSTONE,
      PLACING_LONG_CRYSTAL,
      TURNING_FOR_PISTON,
      PLACING_PISTON,
      RESETTING_VIEW,
      WAITING_FOR_CRYSTAL,
      PLACING_REDSTONE,
      WAITING_FOR_PUSH,
      EXPLODING,
      CONFIRMING_EXPLOSION,
      BREAKING_REDSTONE,
      DONE;
   }
}
