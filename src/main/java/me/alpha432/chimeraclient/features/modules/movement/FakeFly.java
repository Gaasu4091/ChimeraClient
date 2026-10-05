package me.alpha432.chimeraclient.features.modules.movement;

import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.util.inventory.InventoryUtil;
import me.alpha432.chimeraclient.util.inventory.Result;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

public class FakeFly extends Module {
   private int state = 0;
   private int waitTicks = 0;
   private double maxSpeed = 0.0;
   private boolean isFirstEquip = true;
   private double lastGroundY = 0.0;
   private Double lockedY = null;
   private final Setting<Double> fireworkThreshold = this.num("Threshold", 0.8, 0.1, 1.0);

   public FakeFly() {
      super("FakeFly", "Custom Elytra flight sequence", Module.Category.MOVEMENT);
   }

   @Override
   public void onEnable() {
      this.state = 0;
      this.waitTicks = 0;
      this.maxSpeed = 0.0;
      this.isFirstEquip = true;
      this.lockedY = null;
      if (mc.player != null) {
         this.lastGroundY = mc.player.getY();
      }
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         double topY = mc.player.getY() + mc.player.getHeight();
         BlockPos headPos = new BlockPos((int)Math.floor(mc.player.getX()), (int)Math.floor(topY), (int)Math.floor(mc.player.getZ()));
         BlockPos nearRoofPos = new BlockPos((int)Math.floor(mc.player.getX()), (int)Math.floor(topY + 0.5), (int)Math.floor(mc.player.getZ()));
         boolean isNearRoof = !mc.world.getBlockState(headPos).isAir() || !mc.world.getBlockState(nearRoofPos).isAir();
         if (!isNearRoof) {
            boolean isNearWall = false;
            Box expandedBox = mc.player.getBoundingBox().expand(0.2, 0.0, 0.2);

            for (VoxelShape shape : mc.world.getBlockCollisions(mc.player, expandedBox)) {
               if (!shape.isEmpty()) {
                  isNearWall = true;
                  break;
               }
            }

            if (!mc.player.horizontalCollision && !isNearWall) {
               if (mc.player.isOnGround()) {
                  this.isFirstEquip = true;
                  this.lastGroundY = mc.player.getY();
                  this.lockedY = null;
               } else {
                  BlockPos playerPos = mc.player.getBlockPos();
                  boolean hasRoof = !mc.world.getBlockState(playerPos.up(2)).isAir() || !mc.world.getBlockState(playerPos.up(3)).isAir();
                  if (hasRoof && mc.player.getY() - this.lastGroundY >= 1.0) {
                     if (this.lockedY == null) {
                        this.lockedY = mc.player.getY();
                     }

                     mc.player.setPosition(mc.player.getX(), this.lockedY, mc.player.getZ());
                     Vec3d currentDelta = mc.player.getVelocity();
                     mc.player.setVelocity(currentDelta.x, 0.0, currentDelta.z);
                  } else {
                     this.lockedY = null;
                  }

                  Vec3d delta = mc.player.getVelocity();
                  double currentSpeed = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
                  if (currentSpeed > this.maxSpeed) {
                     this.maxSpeed = currentSpeed;
                  }

                  switch (this.state) {
                     case 0:
                        this.equipItem(Items.ELYTRA);
                        this.startGlide();
                        if (this.isFirstEquip) {
                           this.handleFireworkUsage();
                           this.isFirstEquip = false;
                           this.maxSpeed = currentSpeed;
                        } else if (currentSpeed <= this.maxSpeed * this.fireworkThreshold.getValue() && this.maxSpeed > 0.1) {
                           this.handleFireworkUsage();
                           this.maxSpeed = currentSpeed;
                        }

                        this.equipChestplate();
                        this.waitTicks = 1;
                        this.state = 1;
                        break;
                     case 1:
                        if (this.waitTicks > 0) {
                           this.waitTicks--;
                        }

                        if (this.waitTicks <= 0) {
                           this.state = 0;
                        }
                  }
               }
            }
         }
      }
   }

   private void handleFireworkUsage() {
      this.useFireworkFromInventory();
   }

   private void useFireworkFromInventory() {
      Result fireworkResult = InventoryUtil.find(Items.FIREWORK_ROCKET, InventoryUtil.HOTBAR_SCOPE);
      if (!fireworkResult.found()) {
         fireworkResult = InventoryUtil.find(Items.FIREWORK_ROCKET, InventoryUtil.FULL_SCOPE);
      }

      if (fireworkResult.found()) {
         InventoryUtil.withSwap(fireworkResult, () -> {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
         });
      }
   }

   private void equipItem(Item item) {
      Result result = InventoryUtil.find(item, InventoryUtil.HOTBAR_SCOPE);
      if (!result.found()) {
         result = InventoryUtil.find(item, InventoryUtil.FULL_SCOPE);
      }

      if (result.found()) {
         int slot = result.slot();
         if (slot < 9) {
            slot += 36;
         }

         mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(0, 6, 0, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
      }
   }

   private void equipChestplate() {
      if (InventoryUtil.find(Items.NETHERITE_CHESTPLATE, InventoryUtil.FULL_SCOPE).found()) {
         this.equipItem(Items.NETHERITE_CHESTPLATE);
      } else if (InventoryUtil.find(Items.DIAMOND_CHESTPLATE, InventoryUtil.FULL_SCOPE).found()) {
         this.equipItem(Items.DIAMOND_CHESTPLATE);
      }
   }

   private void startGlide() {
      mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
   }
}
