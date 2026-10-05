package me.alpha432.chimeraclient.mio.render;

import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.mio.support.ViewModelSupport;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.util.Hand;

public class ViewModel extends Module {
   public static ViewModel INSTANCE;
   public final Setting<Boolean> adjust;
   public final Setting<Boolean> shadow = this.bool("Shadow", true);
   public final Setting<Boolean> noSway = this.bool("NoSway", false);
   public final Setting<Boolean> instantSwap = this.bool("InstantSwap", false);
   public final Setting<Boolean> eating = this.bool("Eating", true);
   public final Setting<Boolean> mainHand2 = this.bool("MainHand", true);
   public final Setting<Boolean> arm = this.bool("Arm", false);
   public final Setting<Float> mainX = this.num("MainX", 0.0F, -1.0F, 1.0F);
   public final Setting<Float> mainY = this.num("MainY", 0.0F, -1.0F, 1.0F);
   public final Setting<Float> mainZ = this.num("MainZ", 0.0F, -1.0F, 1.0F);
   public final Setting<Float> mainScaleX = this.num("MainScaleX", 1.0F, 0.0F, 2.0F);
   public final Setting<Float> mainScaleY = this.num("MainScaleY", 1.0F, 0.0F, 2.0F);
   public final Setting<Float> mainScaleZ = this.num("MainScaleZ", 1.0F, 0.0F, 2.0F);
   public final Setting<Float> mainRotateX = this.num("MainRotateX", 0.0F, 0.0F, 360.0F);
   public final Setting<Float> mainRotateY = this.num("MainRotateY", 0.0F, 0.0F, 360.0F);
   public final Setting<Float> mainRotateZ = this.num("MainRotateZ", 0.0F, 0.0F, 360.0F);
   public final Setting<Boolean> offHand = this.bool("OffHand", true);
   public final Setting<Float> offX = this.num("OffX", 0.0F, -1.0F, 1.0F);
   public final Setting<Float> offY = this.num("OffY", 0.0F, -1.0F, 1.0F);
   public final Setting<Float> offZ = this.num("OffZ", 0.0F, -1.0F, 1.0F);
   public final Setting<Float> offScaleX = this.num("OffScaleX", 1.0F, 0.0F, 2.0F);
   public final Setting<Float> offScaleY = this.num("OffScaleY", 1.0F, 0.0F, 2.0F);
   public final Setting<Float> offScaleZ = this.num("OffScaleZ", 1.0F, 0.0F, 2.0F);
   public final Setting<Float> offRotateX = this.num("OffRotateX", 0.0F, 0.0F, 360.0F);
   public final Setting<Float> offRotateY = this.num("OffRotateY", 0.0F, 0.0F, 360.0F);
   public final Setting<Float> offRotateZ = this.num("OffRotateZ", 0.0F, 0.0F, 360.0F);
   public final Setting<Boolean> misc = this.bool("Misc", false);
   public final Setting<Float> eatMultiplier = this.num("EatMultiplier", 1.0F, 0.0F, 1.0F);
   public final Setting<Boolean> noTridentAnim = this.bool("NoTridentAnim", false);
   public final Setting<Boolean> swingProgress = this.bool("SwingProgress", false);
   public final Setting<Boolean> static_ = this.bool("Static", true);
   public final Setting<Boolean> mainHand = this.bool("SwingMainHand", false);
   public final Setting<Float> swingProgressAmount2 = this.num("SwingProgressAmount", 1.0F, 0.0F, 1.0F);
   public final Setting<Boolean> offHand2 = this.bool("SwingOffHand", false);
   public final Setting<Float> swingProgressAmount = this.num("SwingProgressAmountOffHand", 1.0F, 0.0F, 1.0F);
   public final Setting<Boolean> viewModelFov = this.bool("ViewModelFov", false);
   public final Setting<Integer> fovAmount = this.num("FovAmount", 90, 70, 180);

   public ViewModel() {
      super("ViewModel", "Transforms your 1st person view model.", Module.Category.RENDER);
      INSTANCE = this;
      Setting var1 = new Setting<Boolean>("Adjust", false) {
         public void setValue(Boolean var1) {
            if (Boolean.TRUE.equals(var1)) {
               Util.mc.setScreen(new ViewModelSupport.AdjustScreen(ViewModel.this));
               var1 = false;
            }

            super.setValue(var1);
         }
      };
      this.adjust = this.register(var1);
      this.settings.remove(var1);
      this.settings.add(this.settings.indexOf(this.shadow), var1);
      this.arm.setVisibility(var1x -> this.mainHand2.getValue());
      this.mainX.setVisibility(var1x -> this.mainHand2.getValue());
      this.mainY.setVisibility(var1x -> this.mainHand2.getValue());
      this.mainZ.setVisibility(var1x -> this.mainHand2.getValue());
      this.mainScaleX.setVisibility(var1x -> this.mainHand2.getValue());
      this.mainScaleY.setVisibility(var1x -> this.mainHand2.getValue());
      this.mainScaleZ.setVisibility(var1x -> this.mainHand2.getValue());
      this.mainRotateX.setVisibility(var1x -> this.mainHand2.getValue());
      this.mainRotateY.setVisibility(var1x -> this.mainHand2.getValue());
      this.mainRotateZ.setVisibility(var1x -> this.mainHand2.getValue());
      this.offX.setVisibility(var1x -> this.offHand.getValue());
      this.offY.setVisibility(var1x -> this.offHand.getValue());
      this.offZ.setVisibility(var1x -> this.offHand.getValue());
      this.offScaleX.setVisibility(var1x -> this.offHand.getValue());
      this.offScaleY.setVisibility(var1x -> this.offHand.getValue());
      this.offScaleZ.setVisibility(var1x -> this.offHand.getValue());
      this.offRotateX.setVisibility(var1x -> this.offHand.getValue());
      this.offRotateY.setVisibility(var1x -> this.offHand.getValue());
      this.offRotateZ.setVisibility(var1x -> this.offHand.getValue());
      this.eatMultiplier.setVisibility(var1x -> this.misc.getValue());
      this.noTridentAnim.setVisibility(var1x -> this.misc.getValue());
      this.swingProgress.setVisibility(var1x -> this.misc.getValue());
      this.static_.setVisibility(var1x -> this.misc.getValue() && this.swingProgress.getValue());
      this.mainHand.setVisibility(var1x -> this.misc.getValue() && this.swingProgress.getValue());
      this.swingProgressAmount2.setVisibility(var1x -> this.misc.getValue() && this.swingProgress.getValue() && this.mainHand.getValue());
      this.offHand2.setVisibility(var1x -> this.misc.getValue() && this.swingProgress.getValue());
      this.swingProgressAmount.setVisibility(var1x -> this.misc.getValue() && this.swingProgress.getValue() && this.offHand2.getValue());
      this.viewModelFov.setVisibility(var1x -> this.misc.getValue());
      this.fovAmount.setVisibility(var1x -> this.misc.getValue() && this.viewModelFov.getValue());
   }

   public static boolean active() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }

   public float swingProgress(Hand var1, float var2) {
      if (!this.isEnabled() || !this.swingProgress.getValue()) {
         return var2;
      } else if (var1 == Hand.MAIN_HAND && this.mainHand.getValue()) {
         return Math.max(this.static_.getValue() ? 0.0F : var2, this.swingProgressAmount2.getValue());
      } else {
         return var1 == Hand.OFF_HAND && this.offHand2.getValue() ? Math.max(this.static_.getValue() ? 0.0F : var2, this.swingProgressAmount.getValue()) : var2;
      }
   }
}
