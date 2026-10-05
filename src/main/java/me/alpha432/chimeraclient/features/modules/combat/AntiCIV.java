package me.alpha432.chimeraclient.features.modules.combat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import me.alpha432.chimeraclient.event.impl.network.PacketEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.util.InteractionUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AntiCIV extends Module {
   private final Setting<Boolean> silent = this.bool("Silent", true);
   private final Setting<Boolean> placeInAir = this.bool("PlaceInAir", true);
   private final Setting<Double> attackDelay = this.num("AttackDelay", 1.0, 0.0, 20.0);
   private final Setting<Double> placeDelay = this.num("PlaceDelay", 1.0, 0.0, 20.0);
   private final Setting<Integer> blocksPerTick = this.num("BlocksPerTick", 2, 1, 10);
   private final Setting<Double> detectRange = this.num("DetectRange", 4.0, 1.0, 7.0);
   private int attackTicks = 0;
   private int placeTicks = 0;
   private final Queue<BlockPos> placeQueue = new LinkedList<>();
   private final Set<BlockPos> ownCrystalPos = new HashSet<>();

   public AntiCIV() {
      super("AntiCIV", "Destroys enemy crystals near your hole, seals with obsidian", Module.Category.COMBAT);
   }

   @Override
   public void onDisable() {
      this.placeQueue.clear();
      this.ownCrystalPos.clear();
      this.attackTicks = 0;
      this.placeTicks = 0;
   }

   @Subscribe
   private void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck()) {
         if (event.getPacket() instanceof PlayerInteractBlockC2SPacket pkt) {
            boolean holding = mc.player.getMainHandStack().isOf(Items.END_CRYSTAL) || mc.player.getOffHandStack().isOf(Items.END_CRYSTAL);
            if (holding) {
               BlockPos base = pkt.getBlockHitResult().getBlockPos();
               this.ownCrystalPos.add(base.up());
            }
         }
      }
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         this.attackTicks++;
         this.placeTicks++;
         if (!mc.player.isUsingItem() || !mc.player.getActiveItem().contains(DataComponentTypes.FOOD)) {
            if (this.isInHole(mc.player)) {
               List<AntiCIV.HoleData> activeHoles = new ArrayList<>();

               for (PlayerEntity player : mc.world.getPlayers()) {
                  if (this.isInHole(player)) {
                     activeHoles.add(new AntiCIV.HoleData(player, player.getBlockPos()));
                  }
               }

               if (this.attackTicks >= this.attackDelay.getValue().intValue()) {
                  Box scanBox = new Box(mc.player.getBlockPos()).expand(this.detectRange.getValue());

                  for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(EndCrystalEntity.class, scanBox, e -> true)) {
                     BlockPos crystalBase = BlockPos.ofFloored(crystal.getX(), crystal.getY() - 1.0, crystal.getZ());
                     if (!this.ownCrystalPos.contains(crystalBase) && !this.ownCrystalPos.contains(crystal.getBlockPos())) {
                        boolean threatensMyHole = false;
                        BlockPos cPos = crystal.getBlockPos();

                        for (AntiCIV.HoleData hole : activeHoles) {
                           if (hole.isThreat(cPos) && hole.owner.equals(mc.player)) {
                              threatensMyHole = true;
                              break;
                           }
                        }

                        if (threatensMyHole) {
                           mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
                           mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                           this.attackTicks = 0;
                           this.enqueueObsidianPatch(crystal.getBlockPos());
                           break;
                        }
                     }
                  }
               }

               if (!this.placeQueue.isEmpty() && this.placeTicks >= this.placeDelay.getValue().intValue()) {
                  int blocksPlaced = 0;

                  while (!this.placeQueue.isEmpty() && blocksPlaced < this.blocksPerTick.getValue()) {
                     BlockPos pos = this.placeQueue.poll();
                     if (pos != null) {
                        BlockPos feet = mc.player.getBlockPos();
                        if (!pos.equals(feet) && !pos.equals(feet.up()) && mc.world.getBlockState(pos).isReplaceable()) {
                           this.placeObsidian(pos);
                           blocksPlaced++;
                        }
                     }
                  }

                  if (blocksPlaced > 0) {
                     this.placeTicks = 0;
                  }
               }
            }
         }
      }
   }

   private void enqueueObsidianPatch(BlockPos crystalPos) {
      BlockPos feet = mc.player.getBlockPos();
      if (!crystalPos.equals(feet) && !crystalPos.equals(feet.up())) {
         if (!this.placeInAir.getValue() && !this.hasSolidNeighbor(crystalPos)) {
            BlockPos support = crystalPos.down();
            if (mc.world.getBlockState(support).isReplaceable()) {
               this.enqueue(support);
            }
         }

         this.enqueue(crystalPos);
      }

      BlockPos above = crystalPos.up();
      if (!above.equals(feet) && !above.equals(feet.up()) && mc.world.getBlockState(above).isReplaceable()) {
         if (!this.placeInAir.getValue() && !this.hasSolidNeighbor(above)) {
            BlockPos support = above.down();
            if (mc.world.getBlockState(support).isReplaceable()) {
               this.enqueue(support);
            }
         }

         this.enqueue(above);
      }
   }

   private void enqueue(BlockPos pos) {
      if (!this.placeQueue.contains(pos)) {
         this.placeQueue.add(pos);
      }
   }

   private boolean hasSolidNeighbor(BlockPos pos) {
      for (Direction dir : Direction.values()) {
         if (!mc.world.getBlockState(pos.offset(dir)).isReplaceable()) {
            return true;
         }
      }

      return false;
   }

   private void placeObsidian(BlockPos pos) {
      int slot = this.findObsidianInHotbar();
      if (slot != -1) {
         int original = mc.player.getInventory().selectedSlot;
         mc.player.getInventory().selectedSlot = slot;
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         float preYaw = mc.player.getYaw();
         float prePitch = mc.player.getPitch();
         float[] look = this.calcLookAt(Vec3d.ofCenter(pos));
         this.sendLookPacket(look[0], look[1]);
         InteractionUtil.place(pos, true);
         this.sendLookPacket(preYaw, prePitch);
         mc.player.getInventory().selectedSlot = original;
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(original));
      }
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

   private boolean isInHole(PlayerEntity player) {
      if (player != null && mc.world != null) {
         BlockPos feet = player.getBlockPos();
         if (!this.isHoleSolid(feet.down())) {
            return false;
         } else {
            for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
               if (!this.isHoleSolid(feet.offset(dir))) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private boolean isHoleSolid(BlockPos pos) {
      Block b = mc.world.getBlockState(pos).getBlock();
      return b == Blocks.OBSIDIAN || b == Blocks.BEDROCK || b == Blocks.CRYING_OBSIDIAN;
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
      return this.isInHole(mc.player) ? "§aIn Hole §7Q:" + this.placeQueue.size() : "§7--";
   }

   private static class HoleData {
      public final PlayerEntity owner;
      public final BlockPos center;

      public HoleData(PlayerEntity owner, BlockPos center) {
         this.owner = owner;
         this.center = center;
      }

      public boolean isThreat(BlockPos crystalPos) {
         int dx = Math.abs(crystalPos.getX() - this.center.getX());
         int dy = crystalPos.getY() - this.center.getY();
         int dz = Math.abs(crystalPos.getZ() - this.center.getZ());
         return dx <= 1 && dz <= 1 && dy >= -1 && dy <= 2;
      }
   }
}
