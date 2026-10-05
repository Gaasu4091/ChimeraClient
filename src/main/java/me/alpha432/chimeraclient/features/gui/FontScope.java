package me.alpha432.chimeraclient.features.gui;

import me.alpha432.chimeraclient.features.modules.client.FontModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.StyleSpriteSource.Font;
import net.minecraft.util.Identifier;

public final class FontScope {
   private static final StackWalker WALKER = StackWalker.getInstance();
   private static final StyleSpriteSource SMOOTH = new Font(Identifier.of("chimeraclient", "smooth"));
   private static final StyleSpriteSource BOLD = new Font(Identifier.of("chimeraclient", "bold"));

   private FontScope() {
   }

   public static Style apply(Style var0) {
      FontModule var1 = FontModule.INSTANCE;
      if (var1 != null && var1.isEnabled()) {
         boolean var2 = MinecraftClient.getInstance().currentScreen instanceof ChatScreen;
         int var3 = WALKER.walk(
            var1x -> var1x.limit(45L)
               .map(
                  var1xx -> {
                     String var2x = var1xx.getClassName();
                     if (var2x.equals("net.minecraft.class_338")
                        || var2x.contains("ChatHud")
                        || var2x.equals("net.minecraft.class_408")
                        || var2x.contains("ChatScreen")) {
                        return 2;
                     } else if (var2x.startsWith("me.alpha432.chimeraclient.features.gui.") && !var2x.equals(FontScope.class.getName())) {
                        return 1;
                     } else if (!var2x.startsWith("me.alpha432.chimeraclient.features.modules.hud.") && !var2x.endsWith(".NotificationsModule")) {
                        return !var2 || !var2x.equals("net.minecraft.class_342") && !var2x.contains("TextFieldWidget") ? 0 : 2;
                     } else {
                        return 1;
                     }
                  }
               )
               .reduce(0, Math::max)
         );
         return var3 != 0 && (var3 != 2 || var1.chat.getValue()) ? var0.withFont(var1.face.getValue() == FontModule.Face.Bold ? BOLD : SMOOTH) : var0;
      } else {
         return var0;
      }
   }
}
