package me.alpha432.chimeraclient.features.modules.misc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.event.impl.network.PacketEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AntiRegear extends Module {
   private final Setting<Boolean> silent = this.bool("Silent", true);
   private final Setting<Boolean> rotate = this.bool("Rotate", false);
   private final Setting<Boolean> autoSwitch = this.bool("AutoSwitch", true);
   private final Setting<Boolean> sneak = this.bool("Sneak", true);
   private final Setting<Double> delay = this.num("Delay", 0.0, 0.0, 20.0);
   private final Setting<Double> range = this.num("Range", 6.0, 1.0, 6.0);
   private final Setting<Double> timeoutTicks = this.num("TimeoutTicks", 20.0, 5.0, 40.0);
   private final List<AntiRegear.PlaceTask> placeTasks = new ArrayList<>();
   private final List<BlockPos> prevShulkers = new ArrayList<>();
   private final Set<BlockPos> ownShulkerPos = new HashSet<>();
   private int delayTicks = 0;
   private boolean isSneaking = false;

   public AntiRegear() {
      super("AntiRegear", "シュルカーボックスの開口部に黒曜石を自動設置します", Module.Category.MISC);
   }

   @Override
   public void onDisable() {
      this.placeTasks.clear();
      this.prevShulkers.clear();
      this.ownShulkerPos.clear();
      this.delayTicks = 0;
      this.stopSneak();
   }

   @Subscribe
   private void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck()) {
         if (event.getPacket() instanceof PlayerInteractBlockC2SPacket pkt) {
            boolean mainHandShulker = mc.player.getMainHandStack().getItem() instanceof BlockItem bi1 && bi1.getBlock() instanceof ShulkerBoxBlock;
            boolean offHandShulker = mc.player.getOffHandStack().getItem() instanceof BlockItem bi2 && bi2.getBlock() instanceof ShulkerBoxBlock;
            if (mainHandShulker || offHandShulker) {
               BlockPos placedPos = pkt.getBlockHitResult().getBlockPos().offset(pkt.getBlockHitResult().getSide());
               this.ownShulkerPos.add(placedPos);
            }
         }
      }
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         this.delayTicks++;
         this.detectNewShulkers();
         if (this.delayTicks > this.delay.getValue().intValue()) {
            if (this.placeTasks.isEmpty()) {
               this.stopSneak();
            } else {
               this.processPlaceTasks();
            }
         }
      }
   }

   private void detectNewShulkers() {
      if (mc.player != null && mc.world != null) {
         int r = (int)Math.ceil(this.range.getValue());
         BlockPos origin = mc.player.getBlockPos();
         List<BlockPos> currentShulkers = new ArrayList<>();

         for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
               for (int z = -r; z <= r; z++) {
                  BlockPos pos = origin.add(x, y, z);
                  if (this.isShulkerBox(mc.world.getBlockState(pos)) && !this.ownShulkerPos.contains(pos)) {
                     currentShulkers.add(pos);
                  }
               }
            }
         }

         for (BlockPos pos : currentShulkers) {
            if (!this.prevShulkers.contains(pos) && !this.isFriendShulker(pos)) {
               boolean alreadyQueued = this.placeTasks.stream().anyMatch(t -> t.shulkerPos.equals(pos));
               if (!alreadyQueued) {
                  this.placeTasks.add(new AntiRegear.PlaceTask(pos));
                  this.delayTicks = 0;
               }
            }
         }

         this.prevShulkers.clear();
         this.prevShulkers.addAll(currentShulkers);
      }
   }

   private boolean isFriendShulker(BlockPos pos) {
      if (mc.world == null) {
         return false;
      } else {
         PlayerEntity closestPlayer = null;
         double minDistance = Double.MAX_VALUE;

         for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && !player.isDead()) {
               double dist = player.squaredDistanceTo(Vec3d.ofCenter(pos));
               if (dist < minDistance) {
                  minDistance = dist;
                  closestPlayer = player;
               }
            }
         }

         return closestPlayer != null && minDistance <= 36.0 ? ChimeraClient.friendManager.isFriend(closestPlayer) : false;
      }
   }

   private void processPlaceTasks() {
      if (mc.player != null && mc.world != null) {
         if (this.sneak.getValue() && !this.isSneaking) {
            this.startSneak();
         }

         Iterator<AntiRegear.PlaceTask> it = this.placeTasks.iterator();

         while (it.hasNext()) {
            AntiRegear.PlaceTask task = it.next();
            task.ticksWaited++;
            BlockPos shulkerPos = task.shulkerPos;
            if (!this.isShulkerBox(mc.world.getBlockState(shulkerPos))) {
               it.remove();
            } else {
               Direction facing = this.getShulkerFacing(shulkerPos);
               BlockPos targetPos = shulkerPos.offset(facing);
               BlockState targetState = mc.world.getBlockState(targetPos);
               if (!targetState.isAir() && !targetState.isReplaceable()) {
                  it.remove();
               } else {
                  int obsSlot = this.findObsidianInHotbar();
                  if (obsSlot != -1) {
                     this.doPlace(obsSlot, shulkerPos, targetPos, facing);
                     it.remove();
                  } else if (task.ticksWaited >= this.timeoutTicks.getValue().intValue()) {
                     it.remove();
                  }
               }
            }
         }

         if (this.placeTasks.isEmpty()) {
            this.stopSneak();
         }
      }
   }

   private void startSneak() {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         this.isSneaking = true;
         mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, true, false)));
         mc.player.setPose(EntityPose.CROUCHING);
         mc.player.setSneaking(true);
      }
   }

   private void stopSneak() {
      if (this.isSneaking) {
         if (mc.player != null && mc.getNetworkHandler() != null) {
            this.isSneaking = false;
            mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));
            mc.player.setPose(EntityPose.STANDING);
            mc.player.setSneaking(false);
         } else {
            this.isSneaking = false;
         }
      }
   }

   private void doPlace(int obsSlot, BlockPos shulkerPos, BlockPos targetPos, Direction shulkerFacing) {
      if (mc.player != null && mc.world != null) {
         BlockPos clickTarget = shulkerPos;
         Direction clickFace = shulkerFacing;
         if (!this.canPlaceAgainst(shulkerPos, shulkerFacing)) {
            Direction[] dirs = new Direction[]{Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP};
            boolean found = false;

            for (Direction dir : dirs) {
               BlockPos nb = targetPos.offset(dir);
               if (!nb.equals(shulkerPos) && !mc.world.getBlockState(nb).isAir()) {
                  Direction face = dir.getOpposite();
                  if (this.canPlaceAgainst(nb, face)) {
                     clickTarget = nb;
                     clickFace = face;
                     found = true;
                     break;
                  }
               }
            }

            if (!found) {
               clickTarget = shulkerPos;
               clickFace = shulkerFacing;
            }
         }

         Vec3d hitVec = Vec3d.ofCenter(clickTarget).add(clickFace.getOffsetX() * 0.5, clickFace.getOffsetY() * 0.5, clickFace.getOffsetZ() * 0.5);
         BlockHitResult hitResult = new BlockHitResult(hitVec, clickFace, clickTarget, false);
         int original = mc.player.getInventory().selectedSlot;
         boolean needSwap = obsSlot != original;
         if (needSwap) {
            mc.player.getInventory().selectedSlot = obsSlot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(obsSlot));
         }

         float preYaw = mc.player.getYaw();
         float prePitch = mc.player.getPitch();
         float[] look = this.calcLookAtPos(hitVec);
         this.sendLookPacket(look[0], look[1]);
         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
         mc.player.swingHand(Hand.MAIN_HAND);
         if (!this.rotate.getValue()) {
            this.sendLookPacket(preYaw, prePitch);
         }

         if (needSwap && (this.silent.getValue() || this.autoSwitch.getValue())) {
            mc.player.getInventory().selectedSlot = original;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(original));
         }
      }
   }

   private boolean canPlaceAgainst(BlockPos pos, Direction face) {
      if (mc.world.getBlockState(pos).isAir()) {
         return false;
      } else {
         Vec3d eyePos = mc.player.getEyePos();
         Vec3d faceCenter = Vec3d.ofCenter(pos).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
         Vec3d lookVec = faceCenter.subtract(eyePos);
         Vec3d normal = new Vec3d(face.getOffsetX(), face.getOffsetY(), face.getOffsetZ());
         return lookVec.dotProduct(normal) <= 0.0;
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

   private int findObsidianInHotbar() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).isOf(Items.OBSIDIAN)) {
            return i;
         }
      }

      return -1;
   }

   private Direction getShulkerFacing(BlockPos pos) {
      BlockState state = mc.world.getBlockState(pos);
      return state.contains(Properties.FACING) ? (Direction)state.get(Properties.FACING) : Direction.UP;
   }

   private boolean isShulkerBox(BlockState state) {
      return state.getBlock() instanceof ShulkerBoxBlock;
   }

   @Override
   public String getDisplayInfo() {
      return "§7Q:" + this.placeTasks.size() + (this.isSneaking ? " §bSneak" : "");
   }

   private static class PlaceTask {
      final BlockPos shulkerPos;
      int ticksWaited;

      PlaceTask(BlockPos pos) {
         this.shulkerPos = pos;
         this.ticksWaited = 0;
      }
   }
}
