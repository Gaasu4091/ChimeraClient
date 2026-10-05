package me.alpha432.chimeraclient.mio.hud;

import java.util.ArrayList;
import java.util.List;
import me.alpha432.chimeraclient.manager.ModuleManager;

public final class MioHudRegistry {
   public static final List<MioHud> ELEMENTS = new ArrayList<>();

   public static void register(ModuleManager var0) {
      var0.register(new MioHudConfig());

      for (MioHud.Type var4 : MioHud.Type.values()) {
         MioHud var5 = new MioHud(var4);
         ELEMENTS.add(var5);
         var0.register(var5);
      }
   }

   public static MioHud active(MioHud.Type var0) {
      return ELEMENTS.stream().filter(var1 -> var1.type == var0 && var1.isEnabled()).findFirst().orElse(null);
   }
}
