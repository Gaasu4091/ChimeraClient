package me.alpha432.chimeraclient.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import me.alpha432.chimeraclient.features.Feature;
import me.alpha432.chimeraclient.features.settings.Bind;
import me.alpha432.chimeraclient.features.settings.EnumConverter;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.util.traits.Jsonable;
import net.fabricmc.loader.api.FabricLoader;
import org.joml.Vector2f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigManager {
   private static final Logger LOGGER = LoggerFactory.getLogger("ConfigManager");
   private static final Path CHIMERACLIENT_PATH = FabricLoader.getInstance().getGameDir().resolve("chimeraclient");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private final List<Jsonable> jsonables = new LinkedList<>();

   public void addConfig(Jsonable jsonable) {
      this.jsonables.add(jsonable);
   }

   public void load() {
      this.mkdirs();

      for (Jsonable jsonable : this.jsonables) {
         try {
            String read = Files.readString(CHIMERACLIENT_PATH.resolve(jsonable.getFileName()));
            jsonable.fromJson(JsonParser.parseString(read));
         } catch (Throwable var4) {
            LOGGER.error("Failed to load", var4);
         }
      }
   }

   public void save() {
      this.mkdirs();

      for (Jsonable jsonable : this.jsonables) {
         try {
            JsonElement json = jsonable.toJson();
            Files.writeString(CHIMERACLIENT_PATH.resolve(jsonable.getFileName()), GSON.toJson(json));
         } catch (Throwable var4) {
            LOGGER.error("Failed to write to file", var4);
         }
      }
   }

   private void mkdirs() {
      if (!CHIMERACLIENT_PATH.toFile().exists()) {
         boolean success = CHIMERACLIENT_PATH.toFile().mkdirs();
         if (!success) {
            throw new RuntimeException("Failed to create needed directories!");
         }
      }
   }

   public static void setValueFromJson(Feature feature, Setting setting, JsonElement element) {
      if (element != null && !element.isJsonNull()) {
         String var3 = setting.getType();
         switch (var3) {
            case "Boolean":
               setting.setValue(element.getAsBoolean());
               break;
            case "Double":
               setting.setValue(element.getAsDouble());
               break;
            case "Float":
               setting.setValue(element.getAsFloat());
               break;
            case "Integer":
               setting.setValue(element.getAsInt());
               break;
            case "String":
               setting.setValue(element.getAsString().replace("_", " "));
               break;
            case "Bind":
               setting.setValue(new Bind(element.getAsInt()));
               break;
            case "Color":
               try {
                  String colorStr = element.getAsString();
                  String[] parts = colorStr.split(",");
                  if (parts.length == 4) {
                     int r = Integer.parseInt(parts[0]);
                     int g = Integer.parseInt(parts[1]);
                     int b = Integer.parseInt(parts[2]);
                     int a = Integer.parseInt(parts[3]);
                     setting.setValue(new Color(r, g, b, a));
                  }
               } catch (Exception var13) {
                  LOGGER.error("Error parsing color for: {} : {}", feature.getName(), setting.getName());
               }
               break;
            case "Pos":
               try {
                  String posStr = element.getAsString();
                  String[] parts = posStr.split(",");
                  if (parts.length == 2) {
                     float x = Float.parseFloat(parts[0]);
                     float y = Float.parseFloat(parts[1]);
                     setting.setValue(new Vector2f(x, y));
                  }
               } catch (Exception var12) {
                  LOGGER.error("Error parsing position for: {} : {}", feature.getName(), setting.getName());
               }
               break;
            case "Enum":
               try {
                  EnumConverter converter = new EnumConverter(setting.getValue().getClass());
                  Enum value = converter.doBackward(element);
                  setting.setValue(value);
               } catch (Exception var11) {
                  LOGGER.error("Error parsing enum for {}.{}: {}", new Object[]{feature.getName(), setting.getName(), var11});
               }
               break;
            default:
               LOGGER.error("Unknown Setting type for: {} : {}", feature.getName(), setting.getName());
         }
      }
   }
}
