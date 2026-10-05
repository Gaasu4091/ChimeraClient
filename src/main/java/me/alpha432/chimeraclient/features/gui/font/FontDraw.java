package me.alpha432.chimeraclient.features.gui.font;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.alpha432.chimeraclient.features.modules.client.FontModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.text.StringVisitable.StyledVisitor;
import net.minecraft.text.StringVisitable.Visitor;
import net.minecraft.text.StyleSpriteSource.Font;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import net.minecraft.util.math.MathHelper;

public final class FontDraw {
   private static final StyleSpriteSource SMOOTH = new Font(Identifier.of("chimeraclient", "smooth"));
   private static final StyleSpriteSource BOLD = new Font(Identifier.of("chimeraclient", "bold"));
   private static final java.awt.Font BASE = new java.awt.Font("SansSerif", 0, 1);
   private static FontRenderer smooth;
   private static FontRenderer bold;
   private static final ThreadLocal<Boolean> FALLBACK = ThreadLocal.withInitial(() -> false);

   private FontDraw() {
   }

   public static boolean enabled() {
      return FontModule.INSTANCE != null && FontModule.INSTANCE.isEnabled();
   }

   public static boolean chatEnabled() {
      return enabled() && FontModule.INSTANCE.chat.getValue();
   }

   public static boolean isFallback() {
      return FALLBACK.get();
   }

   public static boolean marker(Style var0) {
      return var0.getFont().equals(SMOOTH) || var0.getFont().equals(BOLD);
   }

   public static boolean supported(Style var0, int var1) {
      return var1 >= 32 && var1 < 65280 && !var0.isObfuscated() && !var0.isItalic() && !var0.isUnderlined() && !var0.isStrikethrough() && BASE.canDisplay(var1);
   }

   private static boolean bold(Style var0) {
      return var0.isBold() || var0.getFont().equals(BOLD);
   }

   private static FontRenderer renderer(Style var0) {
      if (bold(var0)) {
         if (bold == null) {
            bold = new FontRenderer(new java.awt.Font[]{BASE.deriveFont(1)});
         }

         return bold;
      } else {
         if (smooth == null) {
            smooth = new FontRenderer(new java.awt.Font[]{BASE});
         }

         return smooth;
      }
   }

   private static Style mark(Style var0) {
      return var0.withFont(FontModule.INSTANCE.face.getValue() == FontModule.Face.Bold ? BOLD : SMOOTH);
   }

   private static Style vanilla(Style var0) {
      return var0.withFont(Style.EMPTY.getFont());
   }

   public static OrderedText mark(OrderedText var0) {
      return var1 -> var0.accept((var1x, var2, var3) -> var1.accept(var1x, mark(var2), var3));
   }

   public static OrderedText formatted(String var0) {
      return var1 -> TextVisitFactory.visitFormatted(var0, Style.EMPTY, var1);
   }

   public static boolean hasMarker(OrderedText var0) {
      boolean[] var1 = new boolean[]{false};
      var0.accept((var1x, var2, var3) -> {
         if (marker(var2)) {
            var1[0] = true;
            return false;
         } else {
            return true;
         }
      });
      return var1[0];
   }

   public static StringVisitable chatLines(final StringVisitable var0) {
      return chatEnabled() && chatHudScope() ? new StringVisitable() {
         public <T> Optional<T> visit(Visitor<T> visitor) {
            return var0.visit(visitor);
         }

         public <T> Optional<T> visit(StyledVisitor<T> styledVisitor, Style style) {
            return var0.visit((var1, var2) -> styledVisitor.accept(FontDraw.mark(var1), var2), style);
         }
      } : var0;
   }

   private static boolean chatHudScope() {
      return StackWalker.getInstance()
         .walk(var0 -> var0.limit(32L).anyMatch(var0x -> var0x.getClassName().equals("net.minecraft.class_338") || var0x.getClassName().endsWith("ChatHud")));
   }

   public static boolean chatInputScope() {
      return chatEnabled() && MinecraftClient.getInstance().currentScreen instanceof ChatScreen && !isFallback()
         ? StackWalker.getInstance()
            .walk(
               var0 -> var0.limit(32L)
                  .anyMatch(var0x -> var0x.getClassName().equals("net.minecraft.class_342") || var0x.getClassName().endsWith("TextFieldWidget"))
            )
         : false;
   }

   public static float advance(Style var0, int var1) {
      if (supported(var0, var1)) {
         return renderer(var0).getTextWidth(new String(Character.toChars(var1)));
      } else {
         boolean var2 = FALLBACK.get();
         FALLBACK.set(true);

         float var3;
         try {
            var3 = MinecraftClient.getInstance()
               .textRenderer
               .getWidth(OrderedText.styledForwardsVisitedString(new String(Character.toChars(var1)), vanilla(var0)));
         } finally {
            FALLBACK.set(var2);
         }

         return var3;
      }
   }

   public static int width(TextRenderer var0, String var1) {
      return width(var0, formatted(var1));
   }

   public static int width(TextRenderer var0, StringVisitable var1) {
      return width(var0, Language.getInstance().reorder(var1));
   }

   public static int width(TextRenderer var0, OrderedText var1) {
      if (enabled() && !isFallback()) {
         float[] var2 = new float[]{0.0F};
         mark(var1).accept((var1x, var2x, var3) -> {
            var2[0] += advance(var2x, var3);
            return true;
         });
         return MathHelper.ceil(var2[0]);
      } else {
         return var0.getWidth(var1);
      }
   }

   public static void drawText(DrawContext var0, TextRenderer var1, String var2, int var3, int var4, int var5, boolean var6) {
      drawText(var0, var1, formatted(var2), var3, var4, var5, var6);
   }

   public static void drawText(DrawContext var0, TextRenderer var1, Text var2, int var3, int var4, int var5, boolean var6) {
      drawText(var0, var1, var2.asOrderedText(), var3, var4, var5, var6);
   }

   public static void drawText(DrawContext var0, TextRenderer var1, OrderedText var2, int var3, int var4, int var5, boolean var6) {
      if (enabled() && !isFallback()) {
         drawMarked(var0, var1, mark(var2), var3, var4, var5, var6);
      } else {
         var0.drawText(var1, var2, var3, var4, var5, var6);
      }
   }

   public static void drawTextWithShadow(DrawContext var0, TextRenderer var1, String var2, int var3, int var4, int var5) {
      drawText(var0, var1, var2, var3, var4, var5, true);
   }

   public static void drawTextWithShadow(DrawContext var0, TextRenderer var1, Text var2, int var3, int var4, int var5) {
      drawText(var0, var1, var2, var3, var4, var5, true);
   }

   public static void drawTextWithShadow(DrawContext var0, TextRenderer var1, OrderedText var2, int var3, int var4, int var5) {
      drawText(var0, var1, var2, var3, var4, var5, true);
   }

   public static void drawMarked(DrawContext var0, TextRenderer var1, OrderedText var2, int var3, int var4, int var5, boolean var6) {
      ArrayList var7 = new ArrayList();
      var2.accept((var1x, var2x, var3x) -> {
         var7.add(new FontDraw.Glyph(var1x, var2x, var3x, marker(var2x) && supported(var2x, var3x)));
         return true;
      });
      float var8 = var3;
      int var9 = 0;

      while (var9 < var7.size()) {
         int var10 = var9 + 1;
         FontDraw.Glyph var11 = (FontDraw.Glyph)var7.get(var9);

         while (var10 < var7.size() && ((FontDraw.Glyph)var7.get(var10)).custom == var11.custom && ((FontDraw.Glyph)var7.get(var10)).style.equals(var11.style)) {
            var10++;
         }

         List<FontDraw.Glyph> var12 = var7.subList(var9, var10);
         OrderedText var13 = var2x -> {
            for (FontDraw.Glyph var4x : var12) {
               if (!var2x.accept(var4x.index, var11.custom ? var4x.style : vanilla(var4x.style), var4x.cp)) {
                  return false;
               }
            }

            return true;
         };
         if (var11.custom) {
            FontRenderer var14 = renderer(var11.style);
            if (var6) {
               var14.drawText(var0, var13, var8 + 0.7F, var4 + 0.7F, var5, true);
            }

            var14.drawText(var0, var13, var8, var4, var5, false);
            var8 += var14.getTextWidth(var13);
         } else {
            boolean var18 = FALLBACK.get();
            FALLBACK.set(true);

            try {
               var0.drawText(var1, var13, Math.round(var8), var4, var5, var6);
               var8 += var1.getWidth(var13);
            } finally {
               FALLBACK.set(var18);
            }
         }

         var9 = var10;
      }
   }

   private record Glyph(int index, Style style, int cp, boolean custom) {
   }
}
