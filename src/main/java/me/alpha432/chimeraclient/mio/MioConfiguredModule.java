package me.alpha432.chimeraclient.mio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import me.alpha432.chimeraclient.event.impl.render.Render2DEvent;
import me.alpha432.chimeraclient.event.impl.render.Render3DEvent;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;

public class MioConfiguredModule extends Module {
   public static final Map<String, MioConfiguredModule> MODULES = new LinkedHashMap<>();
   private static final JsonObject INVENTORY = readInventory();
   public final Map<String, Setting<?>> options = new LinkedHashMap<>();

   public MioConfiguredModule(String var1) {
      super(var1, "Mio " + var1 + " rendering", Module.Category.RENDER);
      JsonArray var2 = INVENTORY.getAsJsonArray(var1);
      HashSet var3 = new HashSet();

      for (JsonElement var5 : var2) {
         JsonObject var6 = var5.getAsJsonObject();
         String var7 = var6.get("field").getAsString();
         String var8 = var6.get("name").getAsString();
         if (!var3.add(var8)) {
            var8 = var8 + " (" + var7 + ")";
         }

         Setting var9 = create(var8, var6);
         this.register(var9);
         this.options.put(var7, var9);
      }

      for (JsonElement var11 : var2) {
         JsonObject var12 = var11.getAsJsonObject();
         if (var12.has("visible")) {
            String[] var13 = var12.get("visible").getAsString().split(", ");
            this.options.get(var12.get("field").getAsString()).setVisibility(var2x -> Arrays.stream(var13).allMatch(this::b));
         }
      }

      if (Set.of("Ambience", "Blur", "Crosshair", "FreeLook", "Glint", "Highlight", "NoBob", "SkyColor", "ViewClip", "Xray", "Zoom").contains(var1)) {
         this.setDrawn(false);
      }

      MODULES.put(var1, this);
   }

   private static JsonObject readInventory() {
      try {
         JsonObject var1;
         try (InputStream var0 = MioConfiguredModule.class.getResourceAsStream("/assets/chimeraclient/mio/settings.json")) {
            if (var0 == null) {
               throw new IllegalStateException("Mio settings resource missing");
            }

            var1 = JsonParser.parseReader(new InputStreamReader(var0, StandardCharsets.UTF_8)).getAsJsonObject();
         }

         return var1;
      } catch (IOException var5) {
         throw new UncheckedIOException(var5);
      }
   }

   private static Setting<?> create(String var0, JsonObject var1) {
      JsonElement var2 = var1.get("default");
      String var3 = var1.get("type").getAsString();

      return switch (var3) {
         case "boolean" -> {
            Setting var13 = new Setting<>(var0, var2.getAsBoolean());
            yield var13;
         }
         case "integer" -> {
            Setting var12 = new Setting<>(var0, var2.getAsInt(), var1.get("min").getAsInt(), var1.get("max").getAsInt());
            yield var12;
         }
         case "float" -> {
            Setting var11 = new Setting<>(var0, var2.getAsFloat(), var1.get("min").getAsFloat(), var1.get("max").getAsFloat());
            yield var11;
         }
         case "double" -> {
            Setting var10 = new Setting<>(var0, var2.getAsDouble(), var1.get("min").getAsDouble(), var1.get("max").getAsDouble());
            yield var10;
         }
         case "color" -> {
            Setting var9 = new Setting<>(var0, new Color(var2.getAsInt(), true));
            yield var9;
         }
         case "string" -> {
            Setting var8 = new Setting<>(var0, var2.getAsString(), var1.has("list") ? "Comma-separated Minecraft registry identifiers" : "");
            yield var8;
         }
         case "enum" -> {
            Setting var5;
            try {
               Class var6 = Class.forName(MioChoices.class.getName() + "$" + var1.get("class").getAsString());
               var5 = new Setting<>(var0, ((Enum[])var6.getEnumConstants())[var2.getAsInt()]);
            } catch (ClassNotFoundException var7) {
               throw new IllegalStateException(var7);
            }

            yield var5;
         }
         default -> throw new IllegalArgumentException(var1.toString());
      };
   }

   public boolean b(String var1) {
      return this.options.containsKey(var1) && Boolean.TRUE.equals(this.options.get(var1).getValue());
   }

   public double n(String var1) {
      Object var2 = this.options.get(var1).getValue();
      return ((Number)var2).doubleValue();
   }

   public int i(String var1) {
      return (int)this.n(var1);
   }

   public float f(String var1) {
      return (float)this.n(var1);
   }

   public Color c(String var1) {
      return (Color)this.options.get(var1).getValue();
   }

   public String s(String var1) {
      return this.options.get(var1).getValue().toString();
   }

   public boolean choice(String var1, String var2) {
      Object var3 = this.options.get(var1).getValue();
      return var3 instanceof Enum var4 ? var4.name().equalsIgnoreCase(var2) || var4.toString().equalsIgnoreCase(var2) : var3.toString().equalsIgnoreCase(var2);
   }

   public boolean listed(String var1, String var2) {
      return Arrays.stream(this.s(var1).split("[,;\\s]+")).anyMatch(var1x -> var1x.equals(var2) || ("minecraft:" + var1x).equals(var2));
   }

   public static MioConfiguredModule active(String var0) {
      MioConfiguredModule var1 = MODULES.get(var0);
      return var1 != null && var1.isEnabled() ? var1 : null;
   }

   @Override
   public void onEnable() {
      MioVisuals.enable(this);
   }

   @Override
   public void onDisable() {
      MioVisuals.disable(this);
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         MioVisuals.tick(this);
      }
   }

   @Override
   public void onRender3D(Render3DEvent var1) {
      if (!nullCheck()) {
         MioVisuals.world(this, var1);
      }
   }

   @Override
   public void onRender2D(Render2DEvent var1) {
      if (!nullCheck()) {
         MioVisuals.screen(this, var1);
      }
   }
}
