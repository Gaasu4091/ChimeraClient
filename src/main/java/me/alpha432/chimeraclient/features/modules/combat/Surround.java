package me.alpha432.chimeraclient.features.modules.combat;

import java.util.ArrayList;
import java.util.List;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.ActionResult.SwingSource;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Surround extends Module {
   private final Setting<Integer> placesPerTick = this.register(new Setting<>("PlacesPerTick", 1, 1, 8));
   private final Setting<Integer> delay = this.register(new Setting<>("Delay", 0, 0, 20));
   private final Setting<Boolean> silent = this.bool("Silent", true);
   private final Setting<Boolean> autoCenter = this.bool("AutoCenter", true);
   private final Setting<Boolean> placeInAir = this.bool("PlaceInAir", true);
   private final Setting<Boolean> disableOnMove = this.bool("DisableOnMove", true);
   private int delayTimer = 0;
   private double baseX = Double.NaN;
   private double baseZ = Double.NaN;
   private double baseY = Double.NaN;
   private double lastGroundY = Double.NaN;
   private int maxHoleSize = 1;
   private static final double RISE_THRESHOLD = 1.0;
   private static final double XZ_THRESHOLD = 0.5;

   public Surround() {
      super("Surround", "Surrounds you with obsidian", Module.Category.COMBAT);
   }

   @Override
   public void onEnable() {
      if (!nullCheck()) {
         this.baseY = mc.player.getY();
         this.lastGroundY = mc.player.isOnGround() ? mc.player.getY() : Double.NaN;
         this.maxHoleSize = 1;
         if (this.autoCenter.getValue()) {
            this.centerPlayer();
         }

         this.baseX = mc.player.getX();
         this.baseZ = mc.player.getZ();
         this.delayTimer = this.delay.getValue();
      }
   }

   @Override
   public void onDisable() {
      this.baseX = Double.NaN;
      this.baseZ = Double.NaN;
      this.baseY = Double.NaN;
      this.lastGroundY = Double.NaN;
      this.delayTimer = 0;
      this.maxHoleSize = 1;
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         if (!mc.player.isUsingItem() || !mc.player.getActiveItem().contains(DataComponentTypes.FOOD)) {
            List<BlockPos> occupiedPositions = this.getOccupiedPositions();
            this.maxHoleSize = Math.max(this.maxHoleSize, occupiedPositions.size());
            if (this.disableOnMove.getValue()) {
               double currentX = mc.player.getX();
               double currentY = mc.player.getY();
               double currentZ = mc.player.getZ();
               if (mc.player.isOnGround()) {
                  this.lastGroundY = currentY;
               }

               boolean shouldDisable = false;
               if (!Double.isNaN(this.lastGroundY)) {
                  if (currentY - this.lastGroundY >= 1.0) {
                     shouldDisable = true;
                  }
               } else if (!Double.isNaN(this.baseY) && currentY - this.baseY >= 1.0) {
                  shouldDisable = true;
               }

               if (!Double.isNaN(this.baseX) && !Double.isNaN(this.baseZ)) {
                  double deltaX = currentX - this.baseX;
                  double deltaZ = currentZ - this.baseZ;
                  double distanceXZ = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                  double dynamicXZThreshold = 0.5;
                  if (!this.autoCenter.getValue()) {
                     dynamicXZThreshold = 0.5 * this.maxHoleSize;
                  }

                  if (distanceXZ >= dynamicXZThreshold) {
                     shouldDisable = true;
                  }
               }

               if (shouldDisable) {
                  this.disable();
                  return;
               }
            }

            int obbySlot = this.findObsidianInHotbar();
            if (obbySlot != -1) {
               List<BlockPos> allTargets = this.buildAllTargets(occupiedPositions);
               List<BlockPos> emptyTargets = this.filterEmpty(allTargets);
               if (!emptyTargets.isEmpty()) {
                  if (this.autoCenter.getValue()) {
                     this.centerPlayer();
                  }

                  if (this.delayTimer >= this.delay.getValue()) {
                     this.delayTimer = 0;
                     int count = Math.min(this.placesPerTick.getValue(), emptyTargets.size());

                     for (int i = 0; i < count; i++) {
                        this.placeObsidian(emptyTargets.get(i), obbySlot);
                     }
                  } else {
                     this.delayTimer++;
                  }
               } else {
                  this.delayTimer = this.delay.getValue();
               }
            }
         }
      }
   }

   private List<BlockPos> getOccupiedPositions() {
      List<BlockPos> positions = new ArrayList<>();
      int y = (int)Math.floor(Double.isNaN(this.baseY) ? mc.player.getY() : this.baseY);
      if (this.autoCenter.getValue()) {
         positions.add(new BlockPos((int)Math.floor(mc.player.getX()), y, (int)Math.floor(mc.player.getZ())));
         return positions;
      } else {
         double x = mc.player.getX();
         double z = mc.player.getZ();
         int minX = (int)Math.floor(x - 0.3);
         int maxX = (int)Math.floor(x + 0.3);
         int minZ = (int)Math.floor(z - 0.3);
         int maxZ = (int)Math.floor(z + 0.3);

         for (int i = minX; i <= maxX; i++) {
            for (int j = minZ; j <= maxZ; j++) {
               positions.add(new BlockPos(i, y, j));
            }
         }

         return positions;
      }
   }

   private List<BlockPos> buildAllTargets(List<BlockPos> occupiedPositions) {
      List<BlockPos> list = new ArrayList<>();

      for (BlockPos pos : occupiedPositions) {
         BlockPos[] sides = new BlockPos[]{pos.north(), pos.south(), pos.east(), pos.west()};

         for (BlockPos sidePos : sides) {
            if (!occupiedPositions.contains(sidePos) && !this.isBlockedByEntity(sidePos)) {
               if (!this.hasSolidNeighbor(sidePos) && !this.placeInAir.getValue()) {
                  BlockPos supportPos = sidePos.down();
                  if (mc.world.getBlockState(supportPos).isReplaceable() && !this.isBlockedByEntity(supportPos) && !list.contains(supportPos)) {
                     list.add(supportPos);
                  }
               }

               if (!list.contains(sidePos)) {
                  list.add(sidePos);
               }
            }
         }

         BlockPos floorPos = pos.down();
         Block floorBlock = mc.world.getBlockState(floorPos).getBlock();
         boolean isObsidian = floorBlock == Blocks.OBSIDIAN;
         boolean isBedrock = floorBlock == Blocks.BEDROCK;
         if (!isObsidian && !isBedrock && !this.isBlockedByEntity(floorPos) && !list.contains(floorPos)) {
            list.add(floorPos);
         }
      }

      return list;
   }

   private List<BlockPos> filterEmpty(List<BlockPos> targets) {
      List<BlockPos> empty = new ArrayList<>();

      for (BlockPos pos : targets) {
         if (mc.world.getBlockState(pos).isReplaceable()) {
            empty.add(pos);
         }
      }

      return empty;
   }

   private boolean hasSolidNeighbor(BlockPos pos) {
      for (Direction dir : Direction.values()) {
         if (!mc.world.getBlockState(pos.offset(dir)).isReplaceable()) {
            return true;
         }
      }

      return false;
   }

   private void centerPlayer() {
      if (mc.player != null) {
         double targetY = Double.isNaN(this.baseY) ? mc.player.getY() : this.baseY;
         double centerX = Math.floor(mc.player.getX()) + 0.5;
         double centerZ = Math.floor(mc.player.getZ()) + 0.5;
         if (mc.player.getX() != centerX || mc.player.getZ() != centerZ || mc.player.getY() != targetY) {
            mc.player.setPosition(centerX, targetY, centerZ);
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(centerX, targetY, centerZ, true, false));
         }
      }
   }

   private boolean placeObsidian(BlockPos pos, int slot) {
      if (mc.player != null && mc.world != null) {
         Direction clickSide = Direction.UP;
         BlockPos clickTarget = pos;

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

         float preYaw = mc.player.getYaw();
         float prePitch = mc.player.getPitch();
         Vec3d lookTarget = clickTarget.equals(pos) ? Vec3d.ofCenter(pos) : Vec3d.ofCenter(clickTarget);
         float[] look = this.calcLookAtPos(lookTarget);
         this.sendLookPacket(look[0], look[1]);
         if (mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit) instanceof Success success && success.swingSource() != SwingSource.NONE) {
            mc.player.swingHand(Hand.MAIN_HAND);
         }

         this.sendLookPacket(preYaw, prePitch);
         if (needSwap && this.silent.getValue()) {
            mc.player.getInventory().selectedSlot = original;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(original));
         }

         return true;
      } else {
         return false;
      }
   }

   private void sendLookPacket(float yaw, float pitch) {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(new LookAndOnGround(yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision));
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

   private boolean isBlockedByEntity(BlockPos pos) {
      return mc.player != null && mc.world != null
         ? !mc.world
            .getEntitiesByClass(Entity.class, new Box(pos), e -> e != mc.player && !(e instanceof ItemEntity) && !(e instanceof ExperienceOrbEntity))
            .isEmpty()
         : false;
   }

   private int findObsidianInHotbar() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).isOf(Items.OBSIDIAN)) {
            return i;
         }
      }

      return -1;
   }
}
