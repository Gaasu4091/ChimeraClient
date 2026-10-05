package me.alpha432.chimeraclient.features.gui.font;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.NativeImage.Format;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

public class GlyphMap {
   private static final AtomicInteger ID_COUNTER = new AtomicInteger();
   private final Font[] fonts;
   private final char include;
   private final char exclude;
   private final int padding;
   private final Char2ObjectArrayMap<GlyphMap.Glyph> glyphs = new Char2ObjectArrayMap();
   private boolean generated;
   private NativeImageBackedTexture texture;
   private Identifier textureId;
   private int width;
   private int height;

   public GlyphMap(Font[] var1, char var2, char var3, int var4) {
      this.fonts = var1;
      this.include = var2;
      this.exclude = var3;
      this.padding = var4;
   }

   public void generate() {
      synchronized (this) {
         this.generateInternal();
      }
   }

   public GlyphMap.Glyph getGlyph(char var1) {
      synchronized (this) {
         if (!this.generated) {
            this.generateInternal();
         }

         return (GlyphMap.Glyph)this.glyphs.get(var1);
      }
   }

   public boolean contains(char var1) {
      return var1 >= this.include && var1 < this.exclude;
   }

   public void destroy() {
      synchronized (this) {
         this.generated = false;
         if (this.texture != null) {
            MinecraftClient var3 = MinecraftClient.getInstance();
            if (var3 != null && this.textureId != null) {
               var3.getTextureManager().destroyTexture(this.textureId);
            }

            this.texture.close();
            this.texture = null;
            this.textureId = null;
         }

         this.glyphs.clear();
         this.width = -1;
         this.height = -1;
      }
   }

   public Identifier getTextureId() {
      return this.textureId;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   private void generateInternal() {
      if (!this.generated) {
         int var1 = this.exclude - this.include;
         if (var1 <= 0) {
            this.width = 1;
            this.height = 1;
            this.generated = true;
         } else {
            FontRenderContext var2 = new FontRenderContext(new AffineTransform(), true, true);
            int[] var3 = new int[var1];
            int[] var4 = new int[var1];
            float[] var5 = new float[var1];
            char[] var6 = new char[var1];

            for (int var7 = 0; var7 < var1; var7++) {
               char var8;
               var6[var7] = var8 = (char)(this.include + var7);
               Font var9 = this.getFontForGlyph(var8);
               Rectangle2D var10 = var9.getStringBounds(String.valueOf(var8), var2);
               var5[var7] = (float)var10.getWidth();
               var3[var7] = (int)Math.ceil(var5[var7]);
               var4[var7] = (int)Math.ceil(var10.getHeight());
            }

            int var28 = (int)Math.ceil(Math.sqrt(var1) * 1.5);
            int[] var29 = new int[var1];
            int[] var30 = new int[var1];
            int var31 = 0;
            int var11 = 0;
            int var12 = 0;
            int var13 = 0;
            int var14 = 0;

            for (int var15 = 0; var15 < var1; var15++) {
               int var16 = Math.max(var3[var15], 1);
               int var17 = Math.max(var4[var15], 1);
               if (var14 >= var28) {
                  var31 = 0;
                  var11 += var12 + this.padding;
                  var12 = 0;
                  var14 = 0;
               }

               var29[var15] = var31;
               var30[var15] = var11;
               if ((var31 += var16 + this.padding) > var13) {
                  var13 = var31;
               }

               if (var17 > var12) {
                  var12 = var17;
               }

               var14++;
            }

            int var32 = Math.max(var13 + this.padding, 1);
            int var33 = Math.max(var11 + var12 + this.padding, 1);
            BufferedImage var34 = new BufferedImage(var32, var33, 2);
            Graphics2D var18 = var34.createGraphics();
            var18.setComposite(AlphaComposite.Src);
            var18.setColor(new Color(0, 0, 0, 0));
            var18.fillRect(0, 0, var32, var33);
            var18.setComposite(AlphaComposite.SrcOver);
            var18.setColor(Color.WHITE);
            var18.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            var18.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            var18.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            for (int var19 = 0; var19 < var1; var19++) {
               if (var3[var19] > 0 && var4[var19] > 0) {
                  Font var20 = this.getFontForGlyph(var6[var19]);
                  var18.setFont(var20);
                  FontMetrics var21 = var18.getFontMetrics();
                  var18.drawString(String.valueOf(var6[var19]), var29[var19], var30[var19] + var21.getAscent());
                  this.glyphs.put(var6[var19], new GlyphMap.Glyph(this, var29[var19], var30[var19], var3[var19], var4[var19], var5[var19], var6[var19]));
               } else {
                  this.glyphs.put(var6[var19], new GlyphMap.Glyph(this, 0, 0, 0, 0, var5[var19], var6[var19]));
               }
            }

            var18.dispose();
            this.width = var32;
            this.height = var33;
            int[] var35 = new int[var32 * var33];
            var34.getRGB(0, 0, var32, var33, var35, 0, var32);
            NativeImage var36 = new NativeImage(Format.RGBA, var32, var33, true);
            IntBuffer var37 = MemoryUtil.memIntBuffer(var36.imageId(), var32 * var33);

            for (int var22 = 0; var22 < var35.length; var22++) {
               int var23 = var35[var22];
               int var24 = var23 >>> 24 & 0xFF;
               int var25 = var23 >>> 16 & 0xFF;
               int var26 = var23 >>> 8 & 0xFF;
               int var27 = var23 & 0xFF;
               var37.put(var24 << 24 | var27 << 16 | var26 << 8 | var25);
            }

            int var38 = ID_COUNTER.getAndIncrement();
            this.texture = new GlyphMap.LinearDynamicTexture(() -> "ChimeraFontPage" + var38, var36);
            this.texture.upload();
            this.textureId = Identifier.of("chimeraclient", "font/page_" + var38);
            MinecraftClient.getInstance().getTextureManager().registerTexture(this.textureId, this.texture);
            this.generated = true;
         }
      }
   }

   private Font getFontForGlyph(char var1) {
      for (Font var5 : this.fonts) {
         if (var5.canDisplay(var1)) {
            return var5;
         }
      }

      return this.fonts[0];
   }

   public record Glyph(GlyphMap parent, int u, int v, int width, int height, float advance, char value) {
   }

   private static final class LinearDynamicTexture extends NativeImageBackedTexture {
      LinearDynamicTexture(Supplier<String> var1, NativeImage var2) {
         super(var1, var2);
         this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
      }
   }
}
