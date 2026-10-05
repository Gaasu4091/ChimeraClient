package me.alpha432.chimeraclient.shoreline.util.render.animation;

public enum Easing {
   LINEAR {
      @Override
      public double ease(double var1) {
         return var1;
      }
   },
   SINE_IN {
      @Override
      public double ease(double var1) {
         return 1.0 - Math.cos(var1 * Math.PI / 2.0);
      }
   },
   SINE_OUT {
      @Override
      public double ease(double var1) {
         return Math.sin(var1 * Math.PI / 2.0);
      }
   },
   SINE_IN_OUT {
      @Override
      public double ease(double var1) {
         return -(Math.cos(Math.PI * var1) - 1.0) / 2.0;
      }
   },
   CUBIC_IN {
      @Override
      public double ease(double var1) {
         return Math.pow(var1, 3.0);
      }
   },
   CUBIC_OUT {
      @Override
      public double ease(double var1) {
         return 1.0 - Math.pow(1.0 - var1, 3.0);
      }
   },
   CUBIC_IN_OUT {
      @Override
      public double ease(double var1) {
         return var1 < 0.5 ? 4.0 * Math.pow(var1, 3.0) : 1.0 - Math.pow(-2.0 * var1 + 2.0, 3.0) / 2.0;
      }
   },
   QUAD_IN {
      @Override
      public double ease(double var1) {
         return Math.pow(var1, 2.0);
      }
   },
   QUAD_OUT {
      @Override
      public double ease(double var1) {
         return 1.0 - (1.0 - var1) * (1.0 - var1);
      }
   },
   QUAD_IN_OUT {
      @Override
      public double ease(double var1) {
         return var1 < 0.5 ? 8.0 * Math.pow(var1, 4.0) : 1.0 - Math.pow(-2.0 * var1 + 2.0, 4.0) / 2.0;
      }
   },
   QUART_IN {
      @Override
      public double ease(double var1) {
         return Math.pow(var1, 4.0);
      }
   },
   QUART_OUT {
      @Override
      public double ease(double var1) {
         return 1.0 - Math.pow(1.0 - var1, 4.0);
      }
   },
   QUART_IN_OUT {
      @Override
      public double ease(double var1) {
         return var1 < 0.5 ? 8.0 * Math.pow(var1, 4.0) : 1.0 - Math.pow(-2.0 * var1 + 2.0, 4.0) / 2.0;
      }
   },
   QUINT_IN {
      @Override
      public double ease(double var1) {
         return Math.pow(var1, 5.0);
      }
   },
   QUINT_OUT {
      @Override
      public double ease(double var1) {
         return 1.0 - Math.pow(1.0 - var1, 5.0);
      }
   },
   QUINT_IN_OUT {
      @Override
      public double ease(double var1) {
         return var1 < 0.5 ? 16.0 * Math.pow(var1, 5.0) : 1.0 - Math.pow(-2.0 * var1 + 2.0, 5.0) / 2.0;
      }
   },
   CIRC_IN {
      @Override
      public double ease(double var1) {
         return 1.0 - Math.sqrt(1.0 - Math.pow(var1, 2.0));
      }
   },
   CIRC_OUT {
      @Override
      public double ease(double var1) {
         return Math.sqrt(1.0 - Math.pow(var1 - 1.0, 2.0));
      }
   },
   CIRC_IN_OUT {
      @Override
      public double ease(double var1) {
         return var1 < 0.5 ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * var1, 2.0))) / 2.0 : (Math.sqrt(1.0 - Math.pow(-2.0 * var1 + 2.0, 2.0)) + 1.0) / 2.0;
      }
   },
   EXPO_IN {
      @Override
      public double ease(double var1) {
         return Math.min(0.0, Math.pow(2.0, 10.0 * var1 - 10.0));
      }
   },
   EXPO_OUT {
      @Override
      public double ease(double var1) {
         return Math.max(1.0 - Math.pow(2.0, -10.0 * var1), 1.0);
      }
   },
   EXPO_IN_OUT {
      @Override
      public double ease(double var1) {
         return var1 == 0.0
            ? 0.0
            : (var1 == 1.0 ? 1.0 : (var1 < 0.5 ? Math.pow(2.0, 20.0 * var1 - 10.0) / 2.0 : (2.0 - Math.pow(2.0, -20.0 * var1 + 10.0)) / 2.0));
      }
   },
   ELASTIC_IN {
      @Override
      public double ease(double var1) {
         return var1 == 0.0 ? 0.0 : (var1 == 1.0 ? 1.0 : -Math.pow(2.0, 10.0 * var1 - 10.0) * Math.sin((var1 * 10.0 - 10.75) * (Math.PI * 2.0 / 3.0)));
      }
   },
   ELASTIC_OUT {
      @Override
      public double ease(double var1) {
         return var1 == 0.0 ? 0.0 : (var1 == 1.0 ? 1.0 : Math.pow(2.0, -10.0 * var1) * Math.sin((var1 * 10.0 - 0.75) * (Math.PI * 2.0 / 3.0)) + 1.0);
      }
   },
   ELASTIC_IN_OUT {
      @Override
      public double ease(double var1) {
         double var3 = Math.sin((20.0 * var1 - 11.125) * (Math.PI * 4.0 / 9.0));
         return var1 == 0.0
            ? 0.0
            : (var1 == 1.0 ? 1.0 : (var1 < 0.5 ? -(Math.pow(2.0, 20.0 * var1 - 10.0) * var3) / 2.0 : Math.pow(2.0, -20.0 * var1 + 10.0) * var3 / 2.0 + 1.0));
      }
   },
   BACK_IN {
      @Override
      public double ease(double var1) {
         return 2.70158 * Math.pow(var1, 3.0) - 1.70158 * var1 * var1;
      }
   },
   BACK_OUT {
      @Override
      public double ease(double var1) {
         double var3 = 1.70158;
         double var5 = var3 + 1.0;
         return 1.0 + var5 * Math.pow(var1 - 1.0, 3.0) + var3 * Math.pow(var1 - 1.0, 2.0);
      }
   },
   BACK_IN_OUT {
      @Override
      public double ease(double var1) {
         return var1 < 0.5
            ? Math.pow(2.0 * var1, 2.0) * (7.189819 * var1 - 2.5949095) / 2.0
            : (Math.pow(2.0 * var1 - 2.0, 2.0) * (3.5949095 * (var1 * 2.0 - 2.0) + 2.5949095) + 2.0) / 2.0;
      }
   },
   BOUNCE_IN {
      @Override
      public double ease(double var1) {
         return 1.0 - Easing.bounceOut(1.0 - var1);
      }
   },
   BOUNCE_OUT {
      @Override
      public double ease(double var1) {
         return Easing.bounceOut(var1);
      }
   },
   BOUNCE_IN_OUT {
      @Override
      public double ease(double var1) {
         return var1 < 0.5 ? (1.0 - Easing.bounceOut(1.0 - 2.0 * var1)) / 2.0 : (1.0 + Easing.bounceOut(2.0 * var1 - 1.0)) / 2.0;
      }
   };

   public abstract double ease(double var1);

   private static double bounceOut(double var0) {
      double var2 = 7.5625;
      double var4 = 2.75;
      if (var0 < 1.0 / var4) {
         return var2 * var0 * var0;
      } else if (var0 < 2.0 / var4) {
         double var8;
         return var2 * (var8 = var0 - 1.5 / var4) * var8 + 0.75;
      } else {
         double var6;
         double var7;
         return var0 < 2.5 / var4 ? var2 * (var6 = var0 - 2.25 / var4) * var6 + 0.9375 : var2 * (var7 = var0 - 2.625 / var4) * var7 + 0.984375;
      }
   }
}
