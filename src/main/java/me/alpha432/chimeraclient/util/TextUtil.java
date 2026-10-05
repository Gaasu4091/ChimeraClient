package me.alpha432.chimeraclient.util;

import dev.cattyn.catformat.CatFormat;
import dev.cattyn.catformat.fabric.FabricCatFormat;
import me.alpha432.chimeraclient.ChimeraClient;
import net.minecraft.text.MutableText;

public final class TextUtil {
   private static CatFormat<MutableText> formatter;

   private TextUtil() {
      throw new AssertionError("Can't create an instance of utility class");
   }

   public static void init() {
      if (formatter != null) {
         throw new IllegalStateException("Formatter is already initialized");
      } else {
         formatter = new FabricCatFormat(true);
         initColors();
      }
   }

   public static MutableText text(String content, Object... obj) {
      return obj != null && obj.length != 0 ? (MutableText)formatter.format(content, obj) : (MutableText)formatter.format(content);
   }

   public static CatFormat<MutableText> getFormatter() {
      return formatter;
   }

   private static void initColors() {
      formatter.add("global", () -> ChimeraClient.colorManager.getColorAsInt());
   }
}
