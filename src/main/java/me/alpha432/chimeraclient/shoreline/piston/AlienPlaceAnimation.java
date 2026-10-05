package me.alpha432.chimeraclient.shoreline.piston;

public final class AlienPlaceAnimation {
   private final long created;
   private final long duration;
   private long fadeStart;
   private boolean confirmed;

   public AlienPlaceAnimation(long var1, long var3) {
      this.created = this.fadeStart = var1;
      this.duration = Math.max(0L, var3);
   }

   public AlienPlaceAnimation.Frame frame(long var1, boolean var3, long var4, boolean var6, AlienPlaceAnimation.Mode var7, AlienPlaceAnimation.Ease var8) {
      if (!this.confirmed) {
         if (var3 && !var6) {
            this.fadeStart = var1;
            if (var1 - this.created >= Math.max(0L, var4)) {
               return new AlienPlaceAnimation.Frame(true, false, 0.0, 0.0);
            }

            return new AlienPlaceAnimation.Frame(false, true, 1.0, 0.0);
         }

         this.confirmed = true;
         if (!var6) {
            this.fadeStart = var1;
         }
      }

      double var9 = this.duration == 0L ? 1.0 : Math.max(0.0, Math.min(1.0, (double)(var1 - this.fadeStart) / this.duration));
      if (var9 >= 1.0) {
         return new AlienPlaceAnimation.Frame(true, false, 0.0, 0.5);
      } else {
         double var11 = Math.max(0.0, Math.min(1.0, var8.ease(var9)));
         return new AlienPlaceAnimation.Frame(
            false, false, var7 == AlienPlaceAnimation.Mode.Shrink ? 1.0 : 1.0 - var11, var7 == AlienPlaceAnimation.Mode.Fade ? 0.0 : var11 * 0.5
         );
      }
   }

   public static enum Ease {
      Linear,
      SineOut,
      SineInOut,
      CubicIn,
      CubicOut,
      CubicInOut,
      QuadIn,
      QuadOut,
      QuadInOut,
      QuartIn,
      QuartOut,
      QuartInOut,
      QuintIn,
      QuintOut,
      QuintInOut,
      CircIn,
      CircOut,
      CircInOut,
      Expo,
      BackOut,
      BackInOut,
      Bounce;

      public double ease(double var1) {
         return switch (this) {
            case Linear -> var1;
            case SineOut -> Math.sin(var1 * Math.PI / 2.0);
            case SineInOut -> -(Math.cos(Math.PI * var1) - 1.0) / 2.0;
            case CubicIn -> Math.pow(var1, 3.0);
            case CubicOut -> 1.0 - Math.pow(1.0 - var1, 3.0);
            case CubicInOut -> var1 < 0.5 ? 4.0 * Math.pow(var1, 3.0) : 1.0 - Math.pow(-2.0 * var1 + 2.0, 3.0) / 2.0;
            case QuadIn -> var1 * var1;
            case QuadOut -> 1.0 - (1.0 - var1) * (1.0 - var1);
            case QuadInOut, QuartInOut -> var1 < 0.5 ? 8.0 * Math.pow(var1, 4.0) : 1.0 - Math.pow(-2.0 * var1 + 2.0, 4.0) / 2.0;
            case QuartIn -> Math.pow(var1, 4.0);
            case QuartOut -> 1.0 - Math.pow(1.0 - var1, 4.0);
            case QuintIn -> Math.pow(var1, 5.0);
            case QuintOut -> 1.0 - Math.pow(1.0 - var1, 5.0);
            case QuintInOut -> var1 < 0.5 ? 16.0 * Math.pow(var1, 5.0) : 1.0 - Math.pow(-2.0 * var1 + 2.0, 5.0) / 2.0;
            case CircIn -> 1.0 - Math.sqrt(1.0 - var1 * var1);
            case CircOut -> Math.sqrt(1.0 - Math.pow(var1 - 1.0, 2.0));
            case CircInOut -> var1 < 0.5
               ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * var1, 2.0))) / 2.0
               : (Math.sqrt(1.0 - Math.pow(-2.0 * var1 + 2.0, 2.0)) + 1.0) / 2.0;
            case Expo -> var1 == 0.0
               ? 0.0
               : (var1 == 1.0 ? 1.0 : (var1 < 0.5 ? Math.pow(2.0, 20.0 * var1 - 10.0) / 2.0 : (2.0 - Math.pow(2.0, -20.0 * var1 + 10.0)) / 2.0));
            case BackOut -> 1.0 + 2.70158 * Math.pow(var1 - 1.0, 3.0) + 1.70158 * Math.pow(var1 - 1.0, 2.0);
            case BackInOut -> var1 < 0.5
               ? Math.pow(2.0 * var1, 2.0) * (7.189819 * var1 - 2.5949095) / 2.0
               : (Math.pow(2.0 * var1 - 2.0, 2.0) * (3.5949095 * (var1 * 2.0 - 2.0) + 2.5949095) + 2.0) / 2.0;
            case Bounce -> {
               if (var1 < 0.36363636363636365) {
                  yield 7.5625 * var1 * var1;
               } else if (var1 < 0.7272727272727273) {
                  var1 -= 0.5454545454545454;
                  yield 7.5625 * var1 * var1 + 0.75;
               } else if (var1 < 0.9090909090909091) {
                  var1 -= 0.8181818181818182;
                  yield 7.5625 * var1 * var1 + 0.9375;
               } else {
                  var1 -= 0.9545454545454546;
                  yield 7.5625 * var1 * var1 + 0.984375;
               }
            }
         };
      }
   }

   public record Frame(boolean expired, boolean pending, double alpha, double inset) {
   }

   public static enum Mode {
      Fade,
      Shrink,
      All;
   }
}
