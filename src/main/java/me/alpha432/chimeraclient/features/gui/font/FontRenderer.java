package me.alpha432.chimeraclient.features.gui.font;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

public class FontRenderer implements Closeable {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final float REF_TEXT_HEIGHT = 8.0F;
   private static final float TARGET_ASCENT = 7.0F;
   private final Map<Identifier, ObjectList<FontRenderer.DrawEntry>> glyphPages = new HashMap<>();
   private final ObjectList<GlyphMap> maps = new ObjectArrayList();
   private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
   private final int charsPerPage;
   private final int padding;
   private Font[] baseFonts;
   private Font[] fittedFonts;
   private int multiplier;
   private int previousGameScale = -1;
   private int blockPx;
   private boolean initialized;

   public FontRenderer(Font[] var1) {
      this(var1, 256, 4);
   }

   public FontRenderer(Font[] var1, int var2, int var3) {
      if (var1.length == 0) {
         throw new IllegalArgumentException("fonts.length == 0");
      } else if (var2 <= 4) {
         throw new IllegalArgumentException("charactersPerPage too small");
      } else if (var3 < 0) {
         throw new IllegalArgumentException("padding < 0");
      } else {
         this.baseFonts = var1;
         this.charsPerPage = var2;
         this.padding = var3;
         this.init();
      }
   }

   public void drawString(DrawContext var1, String var2, float var3, float var4, int var5, boolean var6) {
      this.drawText(var1, parse(var2, var5).asOrderedText(), var3, var4, var5, var6);
   }

   public void drawText(DrawContext var1, OrderedText var2, float var3, float var4, int var5, boolean var6) {
      this.rebuildIfScaleChanged();
      if ((var5 & -67108864) == 0) {
         var5 |= -16777216;
      }

      int var7 = var5 >>> 24 & 0xFF;
      int var8 = var6 ? shadow(var5) : var5 & 16777215;
      float[] var9 = new float[]{0.0F};
      Map var10 = this.glyphPages;
      synchronized (var10) {
         var2.accept((var5x, var6x, var7x) -> {
            int var8x;
            if (var6x.getColor() != null) {
               int var9x = var6x.getColor().getRgb();
               var8x = var6 ? shadow(var9x) : var9x & 16777215;
            } else {
               var8x = var8;
            }

            int var16 = var7 << 24 | var8x;

            for (char var13x : Character.toChars(var7x)) {
               GlyphMap.Glyph var14x = this.locateGlyph(var13x);
               if (var14x != null) {
                  if (var14x.width() > 0 && var14x.height() > 0 && var13x != ' ') {
                     Identifier var15x = var14x.parent().getTextureId();
                     this.glyphPages.computeIfAbsent(var15x, var0 -> new ObjectArrayList()).add(new FontRenderer.DrawEntry(var9[0], 0.0F, var16, var14x));
                  }

                  var9[0] += var14x.advance();
               }
            }

            return true;
         });
         float var12 = (float)this.blockPx / this.multiplier;
         float var13 = var4 + (8.0F - var12) / 2.0F;
         Matrix3x2fStack var14 = var1.getMatrices();
         var14.pushMatrix();
         var14.translate(var3, var13);
         float var15 = 1.0F / this.multiplier;
         var14.scale(var15, var15);

          for (Entry<Identifier, ObjectList<FontRenderer.DrawEntry>> var17 : this.glyphPages.entrySet()) {
            Identifier var18 = (Identifier)var17.getKey();

             for (FontRenderer.DrawEntry var21 : var17.getValue()) {
               GlyphMap.Glyph var22 = var21.glyph;
               GlyphMap var23 = var22.parent();
               var1.drawTexture(
                  RenderPipelines.GUI_TEXTURED,
                  var18,
                  Math.round(var21.dx),
                  Math.round(var21.dy),
                  var22.u(),
                  var22.v(),
                  var22.width(),
                  var22.height(),
                  var23.getWidth(),
                  var23.getHeight(),
                  var21.argb
               );
            }
         }

         var14.popMatrix();
         this.glyphPages.clear();
      }
   }

   public static String stripControlCodes(String var0) {
      if (var0.indexOf(167) < 0) {
         return var0;
      } else {
         StringBuilder var1 = new StringBuilder(var0.length());

         for (int var2 = 0; var2 < var0.length(); var2++) {
            char var3 = var0.charAt(var2);
            if (var3 == 167 && var2 + 1 < var0.length()) {
               var2++;
            } else {
               var1.append(var3);
            }
         }

         return var1.toString();
      }
   }

   public float getTextWidth(String var1) {
      this.rebuildIfScaleChanged();
      String var2 = stripControlCodes(var1);
      float var3 = 0.0F;

      for (int var4 = 0; var4 < var2.length(); var4++) {
         GlyphMap.Glyph var5 = this.locateGlyph(var2.charAt(var4));
         if (var5 != null) {
            var3 += var5.advance();
         }
      }

      return var3 / this.multiplier;
   }

   public float getTextWidth(OrderedText var1) {
      this.rebuildIfScaleChanged();
      float[] var2 = new float[]{0.0F};
      var1.accept((var2x, var3, var4) -> {
         for (char var8 : Character.toChars(var4)) {
            GlyphMap.Glyph var9 = this.locateGlyph(var8);
            if (var9 != null) {
               var2[0] += var9.advance();
            }
         }

         return true;
      });
      return var2[0] / this.multiplier;
   }

   public float getHeight() {
      return 9.0F;
   }

   private void rebuildIfScaleChanged() {
      int var1 = Math.round((float)mc.getWindow().getScaleFactor());
      if (var1 != this.previousGameScale) {
         this.close();
         this.init();
      }
   }

   private void init() {
      if (this.initialized) {
         throw new IllegalStateException("Double call to init()");
      } else {
         this.lock.writeLock().lock();

         try {
            int var1 = Math.round((float)mc.getWindow().getScaleFactor());
            if (var1 < 1) {
               var1 = 1;
            }

            this.previousGameScale = var1;
            this.multiplier = Math.max(2, var1);
            BufferedImage var2 = new BufferedImage(1, 1, 2);
            Graphics2D var3 = var2.createGraphics();
            float var4 = 16.0F * this.multiplier;
            var3.setFont(this.baseFonts[0].deriveFont(var4));
            FontMetrics var5 = var3.getFontMetrics();
            float var6 = var5.getAscent() / var4;
            float var7 = 7.0F * this.multiplier / var6;
            Font[] var8 = new Font[this.baseFonts.length];

            for (int var9 = 0; var9 < this.baseFonts.length; var9++) {
               var8[var9] = this.baseFonts[var9].deriveFont(var7);
            }

            var3.setFont(var8[0]);
            FontMetrics var13 = var3.getFontMetrics();
            this.blockPx = var13.getAscent() + var13.getDescent();
            var3.dispose();
            this.fittedFonts = var8;
            this.initialized = true;
         } finally {
            this.lock.writeLock().unlock();
         }
      }
   }

   private GlyphMap.Glyph locateGlyph(char var1) {
      this.lock.readLock().lock();

      try {
         ObjectListIterator var2 = this.maps.iterator();

         while (var2.hasNext()) {
            GlyphMap var3 = (GlyphMap)var2.next();
            if (var3.contains(var1)) {
               return var3.getGlyph(var1);
            }
         }
      } finally {
         this.lock.readLock().unlock();
      }

      int var14 = this.charsPerPage * (var1 / this.charsPerPage);
      int var15 = Math.min(var14 + this.charsPerPage, 65536);
      GlyphMap var16 = new GlyphMap(this.fittedFonts, (char)var14, (char)var15, this.padding);
      this.lock.writeLock().lock();

      try {
         var16.generate();
         this.maps.add(var16);
      } finally {
         this.lock.writeLock().unlock();
      }

      return var16.getGlyph(var1);
   }

   @Override
   public void close() {
      this.lock.writeLock().lock();

      try {
         ObjectListIterator var1 = this.maps.iterator();

         while (var1.hasNext()) {
            GlyphMap var2 = (GlyphMap)var1.next();
            var2.destroy();
         }

         this.maps.clear();
         this.initialized = false;
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   private static int shadow(int var0) {
      return (var0 & 16579836) >> 2;
   }

   private static Text parse(String var0, int var1) {
      if (var0.indexOf(167) < 0) {
         return Text.literal(var0).fillStyle(Style.EMPTY.withColor(var1));
      } else {
         MutableText var2 = Text.literal("").fillStyle(Style.EMPTY.withColor(var1));
         Style var3 = Style.EMPTY.withColor(var1);
         StringBuilder var4 = new StringBuilder();

         for (int var5 = 0; var5 < var0.length(); var5++) {
            char var6 = var0.charAt(var5);
            if (var6 == 167 && var5 + 1 < var0.length()) {
               if (var4.length() > 0) {
                  var2.append(Text.literal(var4.toString()).fillStyle(var3));
                  var4.setLength(0);
               }

               char var7 = Character.toLowerCase(var0.charAt(++var5));
               var3 = applyCode(var3, var7, var1);
            } else {
               var4.append(var6);
            }
         }

         if (var4.length() > 0) {
            var2.append(Text.literal(var4.toString()).fillStyle(var3));
         }

         return var2;
      }
   }

   private static Style applyCode(Style var0, char var1, int var2) {
      return switch (var1) {
         case '0' -> var0.withColor(-16777216);
         case '1' -> var0.withColor(-16777046);
         case '2' -> var0.withColor(-16733696);
         case '3' -> var0.withColor(-16733526);
         case '4' -> var0.withColor(-5636096);
         case '5' -> var0.withColor(-5635926);
         case '6' -> var0.withColor(-22016);
         case '7' -> var0.withColor(-5592406);
         case '8' -> var0.withColor(-11184811);
         case '9' -> var0.withColor(-11184641);
         default -> var0;
         case 'a' -> var0.withColor(-11141291);
         case 'b' -> var0.withColor(-11141121);
         case 'c' -> var0.withColor(-43691);
         case 'd' -> var0.withColor(-43521);
         case 'e' -> var0.withColor(-171);
         case 'f' -> var0.withColor(-1);
         case 'r' -> Style.EMPTY.withColor(var2);
      };
   }

   private record DrawEntry(float dx, float dy, int argb, GlyphMap.Glyph glyph) {
   }
}
