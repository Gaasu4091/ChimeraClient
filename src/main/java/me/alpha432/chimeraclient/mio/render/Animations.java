package me.alpha432.chimeraclient.mio.render;

import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;

public class Animations extends Module {
   public static Animations INSTANCE;
   public final Setting<Boolean> crystals = this.bool("Crystals", false);
   public final Setting<Float> floatFactor = this.num("FloatFactor", 1.0F, 0.0F, 1.0F);
   public final Setting<Float> rotationSpeed = this.num("RotationSpeed", 1.0F, 0.0F, 6.0F);
   public final Setting<Float> crystalScale = this.num("CrystalScale", 1.0F, 0.0F, 2.0F);
   public final Setting<Boolean> parts = this.bool("Parts", false);
   public final Setting<Boolean> inner = this.bool("Inner", true);
   public final Setting<Boolean> outer = this.bool("Outer", true);
   public final Setting<Boolean> core = this.bool("Core", true);
   public final Setting<Boolean> bottom = this.bool("Bottom", true);
   public final Setting<Boolean> players = this.bool("Players", false);
   public final Setting<Boolean> static_ = this.bool("Static", false);
   public final Setting<Boolean> sneak = this.bool("Sneak", false);
   public final Setting<Float> playerScale = this.num("PlayerScale", 1.0F, 0.0F, 2.0F);

   public Animations() {
      super("Animations", "Modifies entity animations.", Module.Category.RENDER);
      INSTANCE = this;
      this.floatFactor.setVisibility(var1 -> this.crystals.getValue());
      this.rotationSpeed.setVisibility(var1 -> this.crystals.getValue());
      this.crystalScale.setVisibility(var1 -> this.crystals.getValue());
      this.parts.setVisibility(var1 -> this.crystals.getValue());
      this.inner.setVisibility(var1 -> this.crystals.getValue() && this.parts.getValue());
      this.outer.setVisibility(var1 -> this.crystals.getValue() && this.parts.getValue());
      this.core.setVisibility(var1 -> this.crystals.getValue() && this.parts.getValue());
      this.bottom.setVisibility(var1 -> this.crystals.getValue() && this.parts.getValue());
      this.static_.setVisibility(var1 -> this.players.getValue());
      this.sneak.setVisibility(var1 -> this.players.getValue());
      this.playerScale.setVisibility(var1 -> this.players.getValue());
   }

   public static boolean active() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }

   public boolean isPlayerScaled() {
      return this.isEnabled() && this.players.getValue() && this.playerScale.getValue() != 1.0F;
   }

   public boolean isStatic() {
      return this.isEnabled() && this.players.getValue() && this.static_.getValue();
   }

   public boolean isSneak() {
      return this.isEnabled() && this.players.getValue() && this.sneak.getValue();
   }

   public boolean isCrystals() {
      return this.isEnabled() && this.crystals.getValue();
   }

   public boolean hideInner() {
      return this.isCrystals() && !this.inner.getValue();
   }

   public boolean hideOuter() {
      return this.isCrystals() && !this.outer.getValue();
   }

   public boolean hideCore() {
      return this.isCrystals() && !this.core.getValue();
   }

   public boolean hideBottom() {
      return this.isCrystals() && !this.bottom.getValue();
   }
}
