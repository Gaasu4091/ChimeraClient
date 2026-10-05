package me.alpha432.chimeraclient.mio;

public final class MioChoices {
   public static enum Ambience_AmbiencePredicateMode {
      CLEAR("Clear"),
      SNOW("Snow"),
      RAIN("Rain"),
      DUSTY("Dusty");

      final String label;

      private Ambience_AmbiencePredicateMode(String nullxx) {
         this.label = nullxx;
      }

      @Override
      public String toString() {
         return this.label;
      }
   }

   public static enum Ambience_MixinEntityRendererMode {
      SCREEN("Screen"),
      GAMMA("Gamma"),
      SKY("Sky"),
      POTION("Potion"),
      NONE("None");

      final String label;

      private Ambience_MixinEntityRendererMode(String nullxx) {
         this.label = nullxx;
      }

      @Override
      public String toString() {
         return this.label;
      }
   }

   public static enum ChamsMode {
      BOTH("Both"),
      FILL("Fill"),
      LINE("Line"),
      PLAIN("Plain"),
      OFF("Off");

      final String label;

      private ChamsMode(String nullxx) {
         this.label = nullxx;
      }

      @Override
      public String toString() {
         return this.label;
      }
   }

   public static enum ChamsMode_2 {
      BOTH("Both"),
      FILL("Fill"),
      LINE("Line");

      final String label;

      private ChamsMode_2(String nullxx) {
         this.label = nullxx;
      }

      @Override
      public String toString() {
         return this.label;
      }
   }

   public static enum ESPPredicateMode {
      BOX("Box"),
      TEXT("Text"),
      BOTH("Both");

      final String label;

      private ESPPredicateMode(String nullxx) {
         this.label = nullxx;
      }

      @Override
      public String toString() {
         return this.label;
      }
   }

   public static enum HoleESP_HoleESPMode {
      NONE("None"),
      SOLID("Solid"),
      GRADIENT("Gradient");

      final String label;

      private HoleESP_HoleESPMode(String nullxx) {
         this.label = nullxx;
      }

      @Override
      public String toString() {
         return this.label;
      }
   }

   public static enum LogoutSpots_LogoutSpotsMode {
      NONE("None"),
      COORDINATES("Coordinates"),
      DISTANCE("Distance");

      final String label;

      private LogoutSpots_LogoutSpotsMode(String nullxx) {
         this.label = nullxx;
      }

      @Override
      public String toString() {
         return this.label;
      }
   }

   public static enum LogoutSpots_LogoutSpotsMode_2 {
      NONE("None"),
      SIMPLE("Simple"),
      COMPLEX("Complex"),
      BOTH("Both");

      final String label;

      private LogoutSpots_LogoutSpotsMode_2(String nullxx) {
         this.label = nullxx;
      }

      @Override
      public String toString() {
         return this.label;
      }
   }

   public static enum NameTags_NameTagsMode {
      SHOW("Show"),
      ONLY("Only"),
      HIDE("Hide");

      final String label;

      private NameTags_NameTagsMode(String nullxx) {
         this.label = nullxx;
      }

      @Override
      public String toString() {
         return this.label;
      }
   }

   public static enum NoRender_NoRenderMode {
      PARTIAL("Partial"),
      FULL("Full");

      final String label;

      private NoRender_NoRenderMode(String nullxx) {
         this.label = nullxx;
      }

      @Override
      public String toString() {
         return this.label;
      }
   }

   public static enum ScaffoldMode_2 {
      ANY("Any"),
      WHITELIST("WhiteList"),
      BLACKLIST("BlackList");

      final String label;

      private ScaffoldMode_2(String nullxx) {
         this.label = nullxx;
      }

      @Override
      public String toString() {
         return this.label;
      }
   }

   public static enum Shader_MixinHeldItemRendererMode {
      SOLID("Solid"),
      RAINBOW("Rainbow"),
      GRADIENT("Gradient"),
      BLOOM("Bloom");

      final String label;

      private Shader_MixinHeldItemRendererMode(String nullxx) {
         this.label = nullxx;
      }

      @Override
      public String toString() {
         return this.label;
      }
   }

   public static enum Shader_Mode {
      NONE("None"),
      DOTS("Dots"),
      GRID("Grid");

      final String label;

      private Shader_Mode(String nullxx) {
         this.label = nullxx;
      }

      @Override
      public String toString() {
         return this.label;
      }
   }

   public static enum SkyColor_MixinClientWorldMode {
      NONE("None"),
      FLAT("Flat"),
      END("End");

      final String label;

      private SkyColor_MixinClientWorldMode(String nullxx) {
         this.label = nullxx;
      }

      @Override
      public String toString() {
         return this.label;
      }
   }

   public static enum Tracers_TracersMode {
      HEAD("Head"),
      BODY("Body"),
      LEGS("Legs");

      final String label;

      private Tracers_TracersMode(String nullxx) {
         this.label = nullxx;
      }

      @Override
      public String toString() {
         return this.label;
      }
   }

   public static enum Waypoints_WaypointsMode {
      NONE("None"),
      waypointsMode("Coords"),
      waypointsMode2("Distance");

      final String label;

      private Waypoints_WaypointsMode(String nullxx) {
         this.label = nullxx;
      }

      @Override
      public String toString() {
         return this.label;
      }
   }
}
