package me.alpha432.chimeraclient.features.modules.combat;

import me.alpha432.chimeraclient.event.impl.network.PacketEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.modules.Module;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Type;

public class AutoHitboxDesync extends Module {
   private static final double MAGIC_OFFSET = 0.20000996883537;
   private BlockPos lastBrokenPos = null;
   private BlockPos lastSelfBrokenPos = null;
   private int cooldown = 0;

   public AutoHitboxDesync() {
      super("AutoHitboxDesync", "Automatically desyncs hitbox towards the most recently broken hole block", Module.Category.COMBAT);
   }

   @Override
   public void onDisable() {
      this.lastBrokenPos = null;
      this.lastSelfBrokenPos = null;
      this.cooldown = 0;
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         if (!this.isInHole()) {
            this.lastBrokenPos = null;
         } else {
            if (this.cooldown > 0) {
               this.cooldown--;
            }

            if (this.lastBrokenPos != null && this.cooldown == 0) {
               this.doHitboxDesync(this.lastBrokenPos);
               this.lastBrokenPos = null;
               this.cooldown = 10;
            }
         }
      }
   }

   @Subscribe
   private void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck()) {
         if (event.getPacket() instanceof PlayerActionC2SPacket pkt && pkt.getAction() == Action.STOP_DESTROY_BLOCK) {
            this.lastSelfBrokenPos = pkt.getPos();
         }
      }
   }

   @Subscribe
   private void onPacketReceive(PacketEvent.Receive event) {
      if (!nullCheck()) {
         if (event.getPacket() instanceof BlockUpdateS2CPacket pkt) {
            this.handleBlockUpdate(pkt.getPos(), pkt.getState());
         } else if (event.getPacket() instanceof ChunkDeltaUpdateS2CPacket pkt) {
            pkt.visitUpdates((pos, state) -> this.handleBlockUpdate(pos, state));
         }
      }
   }

   private void handleBlockUpdate(BlockPos pos, BlockState state) {
      if (mc.player != null) {
         if (!pos.equals(this.lastSelfBrokenPos)) {
            if ((state.isAir() || state.isReplaceable()) && this.isHoleBlock(pos)) {
               this.lastBrokenPos = pos;
            }
         }
      }
   }

   private boolean isInHole() {
      if (mc.player != null && mc.world != null) {
         BlockPos playerPos = BlockPos.ofFloored(mc.player.getEntityPos());
         BlockPos[] offsets = new BlockPos[]{playerPos.down(), playerPos.north(), playerPos.south(), playerPos.east(), playerPos.west()};

         for (BlockPos offset : offsets) {
            BlockState state = mc.world.getBlockState(offset);
            if (!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean isHoleBlock(BlockPos pos) {
      BlockPos playerPos = BlockPos.ofFloored(mc.player.getEntityPos());

      for (Direction dir : Type.HORIZONTAL) {
         if (playerPos.offset(dir).equals(pos) || playerPos.offset(dir).up().equals(pos)) {
            return true;
         }
      }

      return false;
   }

   private void doHitboxDesync(BlockPos targetPos) {
      BlockPos playerPos = BlockPos.ofFloored(mc.player.getEntityPos());
      Direction facing = Direction.NORTH;
      int dx = targetPos.getX() - playerPos.getX();
      int dz = targetPos.getZ() - playerPos.getZ();
      if (Math.abs(dx) > Math.abs(dz)) {
         facing = dx > 0 ? Direction.EAST : Direction.WEST;
      } else if (Math.abs(dz) > 0) {
         facing = dz > 0 ? Direction.SOUTH : Direction.NORTH;
      } else {
         facing = mc.player.getHorizontalFacing();
      }

      Box bb = mc.player.getBoundingBox();
      Vec3d center = bb.getCenter();
      Vec3d offset = new Vec3d(facing.getOffsetX(), facing.getOffsetY(), facing.getOffsetZ());
      Vec3d basePos = Vec3d.ofBottomCenter(BlockPos.ofFloored(center));
      Vec3d targetCalc = basePos.add(offset.multiply(0.20000996883537));
      Vec3d fin = this.merge(targetCalc, facing);
      double newX = fin.x == 0.0 ? mc.player.getX() : fin.x;
      double newY = mc.player.getY();
      double newZ = fin.z == 0.0 ? mc.player.getZ() : fin.z;
      double originalX = mc.player.getX();
      double originalY = mc.player.getY();
      double originalZ = mc.player.getZ();
      mc.player.setPosition(newX, newY, newZ);
      if (mc.world != null) {
         BlockPos desyncBlockPos = BlockPos.ofFloored(newX, newY, newZ);
         BlockState state = mc.world.getBlockState(desyncBlockPos);
         if (!state.isAir() && !state.isReplaceable()) {
            mc.player.setPosition(originalX, originalY, originalZ);
         }
      }
   }

   private Vec3d merge(Vec3d a, Direction facing) {
      return new Vec3d(a.x * Math.abs(facing.getOffsetX()), a.y * Math.abs(facing.getOffsetY()), a.z * Math.abs(facing.getOffsetZ()));
   }
}
