package me.alpha432.chimeraclient.features.modules.combat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.event.impl.entity.player.TickEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.modules.player.SpeedMine;
import me.alpha432.chimeraclient.features.settings.Setting;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Type;

public class AutoMine extends Module {
   private final Setting<Double> range = this.register(new Setting<>("Range", 5.0, 1.0, 7.0));
   private final Setting<Boolean> burrow = this.register(new Setting<>("Burrow", true));
   private final Map<BlockPos, List<Long>> airBreakHistory = new ConcurrentHashMap<>();
   private final Map<BlockPos, Boolean> wasAirMap = new ConcurrentHashMap<>();
   private BlockPos target1 = null;
   private BlockPos target2 = null;
   private BlockPos lastSpoofedTarget = null;
   private boolean isSpoofingAttack = false;

   public AutoMine() {
      super("AutoMine", "Automatically mines enemy holes and burrows via SpeedMine", Module.Category.COMBAT);
   }

   @Override
   public void onDisable() {
      this.stopSpoofing();
      this.airBreakHistory.clear();
      this.wasAirMap.clear();
      this.target1 = null;
      this.target2 = null;
      this.lastSpoofedTarget = null;
   }

   @Subscribe
   public void onTick(TickEvent event) {
      if (!nullCheck()) {
         SpeedMine speedMine = ChimeraClient.moduleManager.getModuleByClass(SpeedMine.class);
         if (speedMine != null && speedMine.isEnabled()) {
            this.updateAirHistory();
            PlayerEntity targetPlayer = this.getClosestEnemy();
            if (targetPlayer == null) {
               this.stopSpoofing();
            } else {
               List<BlockPos> occupiedBlocks = this.getOccupiedPositions(targetPlayer);
               List<BlockPos> surroundBlocks = this.getSurroundingBlocks(occupiedBlocks);
               List<BlockPos> burrowBlocks = this.getBurrowBlocks(occupiedBlocks);
               List<BlockPos> validTargets = new ArrayList<>();
               if (this.burrow.getValue() && !burrowBlocks.isEmpty()) {
                  for (BlockPos pos : burrowBlocks) {
                     if (this.isValidTarget(pos, true)) {
                        validTargets.add(pos);
                     }
                  }
               } else {
                  for (BlockPos posx : surroundBlocks) {
                     if (this.isValidTarget(posx, false)) {
                        validTargets.add(posx);
                     }
                  }
               }

               if (validTargets.isEmpty()) {
                  this.stopSpoofing();
               } else {
                  validTargets.sort(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(Vec3d.ofCenter(p))));
                  boolean isDoubleMine = this.getSpeedMineDoubleSetting(speedMine);
                  this.target1 = validTargets.get(0);
                  this.target2 = isDoubleMine && validTargets.size() > 1 ? validTargets.get(1) : null;
                  this.executeViaSpeedMine();
               }
            }
         } else {
            this.stopSpoofing();
         }
      }
   }

   private void updateAirHistory() {
      long now = System.currentTimeMillis();

      for (Entry<BlockPos, List<Long>> entry : this.airBreakHistory.entrySet()) {
         entry.getValue().removeIf(time -> now - time > 3000L);
      }

      int r = (int)Math.ceil(this.range.getValue());
      BlockPos playerPos = mc.player.getBlockPos();

      for (int x = -r; x <= r; x++) {
         for (int y = -r; y <= r; y++) {
            for (int z = -r; z <= r; z++) {
               BlockPos pos = playerPos.add(x, y, z);
               boolean isAirNow = mc.world.getBlockState(pos).isAir();
               Boolean wasAir = this.wasAirMap.get(pos);
               if (wasAir != null && !wasAir && isAirNow) {
                  this.airBreakHistory.computeIfAbsent(pos, k -> new ArrayList<>()).add(now);
               }

               this.wasAirMap.put(pos, isAirNow);
            }
         }
      }
   }

   private boolean isValidTarget(BlockPos pos, boolean isBurrowBlock) {
      List<Long> history = this.airBreakHistory.get(pos);
      if (history != null && history.size() >= 2) {
         return false;
      } else {
         BlockState state = mc.world.getBlockState(pos);
         if (!state.isAir() && !(state.getHardness(mc.world, pos) < 0.0F)) {
            if (!isBurrowBlock) {
               List<PlayerEntity> intersectingPlayers = mc.world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos));
               if (!intersectingPlayers.isEmpty()) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      }
   }

   private void executeViaSpeedMine() {
      if (this.target2 != null) {
         if (!this.target1.equals(this.lastSpoofedTarget) && !this.target2.equals(this.lastSpoofedTarget)) {
            this.setSpoofedHitResult(this.target1);
            this.lastSpoofedTarget = this.target1;
         } else if (this.target1.equals(this.lastSpoofedTarget)) {
            this.setSpoofedHitResult(this.target2);
            this.lastSpoofedTarget = this.target2;
         } else {
            this.setSpoofedHitResult(this.target2);
         }
      } else {
         this.setSpoofedHitResult(this.target1);
         this.lastSpoofedTarget = this.target1;
      }

      mc.options.attackKey.setPressed(true);
      this.isSpoofingAttack = true;
   }

   private void setSpoofedHitResult(BlockPos pos) {
      mc.crosshairTarget = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
   }

   private void stopSpoofing() {
      if (this.isSpoofingAttack) {
         mc.options.attackKey.setPressed(false);
         this.isSpoofingAttack = false;
      }

      this.lastSpoofedTarget = null;
   }

   private PlayerEntity getClosestEnemy() {
      PlayerEntity closest = null;
      double minDist = this.range.getValue();

      for (PlayerEntity p : mc.world.getPlayers()) {
         if (p != mc.player && !p.isSpectator() && !p.isDead() && !ChimeraClient.friendManager.isFriend(p)) {
            double dist = mc.player.distanceTo(p);
            if (dist <= minDist) {
               minDist = dist;
               closest = p;
            }
         }
      }

      return closest;
   }

   private List<BlockPos> getOccupiedPositions(PlayerEntity player) {
      List<BlockPos> positions = new ArrayList<>();
      Box bb = player.getBoundingBox();
      int minX = MathHelper.floor(bb.minX);
      int maxX = MathHelper.floor(bb.maxX);
      int minZ = MathHelper.floor(bb.minZ);
      int maxZ = MathHelper.floor(bb.maxZ);
      int y = MathHelper.floor(player.getY());

      for (int x = minX; x <= maxX; x++) {
         for (int z = minZ; z <= maxZ; z++) {
            positions.add(new BlockPos(x, y, z));
         }
      }

      return positions;
   }

   private List<BlockPos> getSurroundingBlocks(List<BlockPos> occupied) {
      List<BlockPos> surround = new ArrayList<>();

      for (BlockPos pos : occupied) {
         for (Direction dir : Type.HORIZONTAL) {
            BlockPos adj = pos.offset(dir);
            if (!occupied.contains(adj) && !surround.contains(adj)) {
               surround.add(adj);
            }
         }
      }

      return surround;
   }

   private List<BlockPos> getBurrowBlocks(List<BlockPos> occupied) {
      List<BlockPos> burrows = new ArrayList<>();

      for (BlockPos pos : occupied) {
         BlockState state = mc.world.getBlockState(pos);
         if (!state.isAir() && state.getHardness(mc.world, pos) >= 0.0F) {
            burrows.add(pos);
         }
      }

      return burrows;
   }

   private boolean getSpeedMineDoubleSetting(SpeedMine speedMineInstance) {
      try {
         Field doubleMineField = SpeedMine.class.getDeclaredField("doubleMine");
         doubleMineField.setAccessible(true);
         Setting<Boolean> setting = (Setting<Boolean>)doubleMineField.get(speedMineInstance);
         return setting.getValue();
      } catch (Exception var4) {
         return false;
      }
   }
}
