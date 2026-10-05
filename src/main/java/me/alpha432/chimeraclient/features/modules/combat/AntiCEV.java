package me.alpha432.chimeraclient.features.modules.combat;

import java.util.HashSet;
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
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AntiCEV extends Module {
   private final Setting<Double> delay = this.num("Delay", 2.0, 0.0, 40.0);
   private final Setting<Boolean> silent = this.bool("Silent", true);
   private final Setting<Boolean> placeInAir = this.bool("PlaceInAir", true);
   private final Setting<Integer> blocksPerTick = this.num("BlocksPerTick", 2, 1, 2);
   private int placeTicks = 0;
   private int attackTicks = 0;
   private final Set<BlockPos> ownCrystalPos = new HashSet<>();
   private BlockPos placedPos0 = null;
   private BlockPos placedPos = null;

   public AntiCEV() {
      super("AntiCEV", "Places obsidian on top of crystal-cage roof (2 layers), replaces if broken", Module.Category.COMBAT);
   }

   @Override
   public void onDisable() {
      this.resetAll();
      this.ownCrystalPos.clear();
      this.placeTicks = 0;
      this.attackTicks = 0;
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
         }
      }
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         this.placeTicks++;
         this.attackTicks++;
         if (!mc.player.isUsingItem() || !mc.player.getActiveItem().contains(DataComponentTypes.FOOD)) {
            if (this.attackTicks >= 1 && this.tryBreakCrystalOnLayers()) {
               this.attackTicks = 0;
            }

            if (this.placeTicks >= this.delay.getValue().intValue()) {
               BlockPos feet = mc.player.getBlockPos();
               if (!this.isInsideHole(feet)) {
                  if (this.placedPos0 != null) {
                     this.resetAll();
                  }
               } else {
                  if (this.placedPos0 == null) {
                     BlockPos roofPos = feet.up(2);
                     Block roofBlock = mc.world.getBlockState(roofPos).getBlock();
                     boolean isRoof = roofBlock == Blocks.OBSIDIAN || roofBlock == Blocks.BEDROCK || roofBlock == Blocks.CRYING_OBSIDIAN;
                     if (!isRoof) {
                        return;
                     }

                     this.placedPos0 = roofPos;
                  }

                  int blocksPlaced = 0;
                  int maxBlocks = this.blocksPerTick.getValue();
                  if (!mc.world.getBlockState(this.placedPos0).isOf(Blocks.OBSIDIAN) && mc.world.getBlockState(this.placedPos0).isReplaceable()) {
                     if (!this.placeInAir.getValue() && !this.hasSolidNeighbor(this.placedPos0)) {
                        BlockPos support = this.placedPos0.down();
                        if (mc.world.getBlockState(support).isReplaceable()) {
                           this.placeObsidian(support);
                           blocksPlaced++;
                        }
                     }

                     if (blocksPlaced < maxBlocks && mc.world.getBlockState(this.placedPos0).isReplaceable()) {
                        this.placeObsidian(this.placedPos0);
                        blocksPlaced++;
                     }
                  }

                  BlockPos target = this.placedPos0.up();
                  if (blocksPlaced < maxBlocks) {
                     if (!mc.world.getBlockState(target).isOf(Blocks.OBSIDIAN)) {
                        if (mc.world.getBlockState(target).isReplaceable()) {
                           if (!this.placeInAir.getValue() && !this.hasSolidNeighbor(target)) {
                              BlockPos support = target.down();
                              if (mc.world.getBlockState(support).isReplaceable()) {
                                 this.placeObsidian(support);
                                 blocksPlaced++;
                              }
                           }

                           if (blocksPlaced < maxBlocks && mc.world.getBlockState(target).isReplaceable()) {
                              this.placeObsidian(target);
                              this.placedPos = target;
                              blocksPlaced++;
                           }
                        }
                     } else {
                        this.placedPos = target;
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

   private boolean tryBreakCrystalOnLayers() {
      if (mc.player != null && mc.world != null) {
         BlockPos[] checkPositions = new BlockPos[]{this.placedPos0 != null ? this.placedPos0.up() : null, this.placedPos != null ? this.placedPos.up() : null};

         for (BlockPos crystalPos : checkPositions) {
            if (crystalPos != null) {
               for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(crystalPos).expand(0.5), e -> true)) {
                  BlockPos base = BlockPos.ofFloored(crystal.getX(), crystal.getY() - 1.0, crystal.getZ());
                  if (!this.ownCrystalPos.contains(base) && !this.ownCrystalPos.contains(crystal.getBlockPos())) {
                     mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
                     mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                     this.placeTicks = this.delay.getValue().intValue();
                     if (mc.world.getBlockState(crystalPos).isReplaceable()) {
                        if (!this.placeInAir.getValue() && !this.hasSolidNeighbor(crystalPos)) {
                           BlockPos support = crystalPos.down();
                           if (mc.world.getBlockState(support).isReplaceable()) {
                              this.placeObsidian(support);
                           }
                        }

                        this.placeObsidian(crystalPos);
                     }

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

   @Subscribe
   private void onPacketReceive(PacketEvent.Receive event) {
      if (!nullCheck()) {
         if (event.getPacket() instanceof BlockUpdateS2CPacket packet) {
            this.handleBlockBreak(packet.getPos(), packet.getState().isAir());
         }

         if (event.getPacket() instanceof ChunkDeltaUpdateS2CPacket packet) {
            packet.visitUpdates((pos, state) -> this.handleBlockBreak(pos, state.isAir()));
         }
      }
   }

   private void handleBlockBreak(BlockPos pos, boolean isAir) {
      if (isAir) {
         if (this.placedPos0 != null && pos.equals(this.placedPos0)) {
            this.placedPos = null;
            this.placeTicks = 0;
         } else if (this.placedPos != null && pos.equals(this.placedPos)) {
            this.placedPos = null;
            this.placeTicks = 0;
         }
      }
   }

   private boolean isInsideHole(BlockPos feet) {
      for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
         BlockPos neighbor = feet.offset(dir);
         Block block = mc.world.getBlockState(neighbor).getBlock();
         boolean isSolid = block == Blocks.OBSIDIAN || block == Blocks.BEDROCK || block == Blocks.CRYING_OBSIDIAN;
         if (!isSolid) {
            return false;
         }
      }

      return true;
   }

   private boolean hasSolidNeighbor(BlockPos pos) {
      for (Direction dir : Direction.values()) {
         if (!mc.world.getBlockState(pos.offset(dir)).isReplaceable()) {
            return true;
         }
      }

      return false;
   }

   private void resetAll() {
      this.placedPos0 = null;
      this.placedPos = null;
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
      if (this.placedPos0 == null) {
         return "§7--";
      } else {
         return this.placedPos != null ? "§a2L" : "§61L";
      }
   }
}
