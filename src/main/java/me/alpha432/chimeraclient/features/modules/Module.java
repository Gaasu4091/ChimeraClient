package me.alpha432.chimeraclient.features.modules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.Color;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.event.impl.ClientEvent;
import me.alpha432.chimeraclient.event.impl.render.Render2DEvent;
import me.alpha432.chimeraclient.event.impl.render.Render3DEvent;
import me.alpha432.chimeraclient.features.Feature;
import me.alpha432.chimeraclient.features.commands.Command;
import me.alpha432.chimeraclient.features.settings.Bind;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.manager.ConfigManager;
import me.alpha432.chimeraclient.util.TextUtil;
import me.alpha432.chimeraclient.util.player.ChatUtil;
import me.alpha432.chimeraclient.util.traits.Jsonable;
import me.alpha432.chimeraclient.util.traits.Toggleable;
import net.minecraft.util.Formatting;
import org.joml.Vector2f;

public class Module extends Feature implements Jsonable, Toggleable {
   private static final String MODULE_FORMAT = "Toggled %s %s %s";
   private final String description;
   private final Module.Category category;
   public final Setting<Boolean> enabled = this.bool("Enabled", false);
   public final Setting<Boolean> drawn = this.bool("Drawn", true);
   public final Setting<Bind> bind = this.key("Keybind", new Bind(-1));
   public final Setting<String> displayName;
   public boolean hidden;

   public Module(String name, String description, Module.Category category) {
      super(name);
      this.displayName = this.str("DisplayName", name);
      this.description = description;
      this.category = category;
   }

   public void onEnable() {
   }

   public void onDisable() {
   }

   public void onToggle() {
   }

   public void onLoad() {
   }

   public void onTick() {
   }

   public void onRender2D(Render2DEvent event) {
   }

   public void onRender3D(Render3DEvent event) {
   }

   public void onUnload() {
   }

   public String getDisplayInfo() {
      return null;
   }

   @Override
   public void enable() {
      this.enabled.setValue(true);
      EVENT_BUS.register(this);
      this.sendToggleNotification(true);
      EVENT_BUS.post(new ClientEvent(ClientEvent.Type.TOGGLE_MODULE, this));
      this.onToggle();
      this.onEnable();
   }

   @Override
   public void disable() {
      this.enabled.setValue(false);
      EVENT_BUS.unregister(this);
      this.sendToggleNotification(false);
      EVENT_BUS.post(new ClientEvent(ClientEvent.Type.TOGGLE_MODULE, this));
      this.onToggle();
      this.onDisable();
   }

   private void sendToggleNotification(boolean state) {
      if (!this.getName().equals("ClickGui")) {
         ChatUtil.sendMessage(TextUtil.text("Toggled %s %s %s", this.getName(), state ? "{green}" : "{red}", state ? "on" : "off"), this.getName());
      }
   }

   public String getDisplayName() {
      return this.displayName.getValue();
   }

   public void setDisplayName(String name) {
      Module module = ChimeraClient.moduleManager.getModuleByDisplayName(name);
      Module originalModule = ChimeraClient.moduleManager.getModuleByName(name);
      if (module == null && originalModule == null) {
         Command.sendMessage(this.getDisplayName() + ", name: " + this.getName() + ", has been renamed to: " + name, "general");
         this.displayName.setValue(name);
      } else {
         Command.sendMessage("{red} A module of this name already exists.", "general");
      }
   }

   @Override
   public boolean isEnabled() {
      return this.enabled.getValue();
   }

   @Override
   public boolean isToggled() {
      return this.isEnabled();
   }

   public String getDescription() {
      return this.description;
   }

   public boolean isDrawn() {
      return this.drawn.getValue();
   }

   public void setDrawn(boolean drawn) {
      this.drawn.setValue(drawn);
   }

   public Module.Category getCategory() {
      return this.category;
   }

   public String getInfo() {
      return null;
   }

   public Bind getBind() {
      return this.bind.getValue();
   }

   public void setBind(int key) {
      this.bind.setValue(new Bind(key));
   }

   public String getFullArrayString() {
      return this.getDisplayName()
         + Formatting.GRAY
         + (this.getDisplayInfo() != null ? " [" + Formatting.WHITE + this.getDisplayInfo() + Formatting.GRAY + "]" : "");
   }

   @Override
   public JsonElement toJson() {
      JsonObject object = new JsonObject();

      for (Setting<?> setting : this.getSettings()) {
         try {
            if (setting.getValue() instanceof Bind keyBind) {
               object.addProperty(setting.getName(), keyBind.getKey());
            } else if (setting.getValue() instanceof Color color) {
               object.addProperty(setting.getName(), color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "," + color.getAlpha());
            } else if (setting.getValue() instanceof Vector2f pos) {
               object.addProperty(setting.getName(), pos.x() + "," + pos.y());
            } else {
               object.addProperty(setting.getName(), setting.getValueAsString());
            }
         } catch (Throwable var8) {
            ChimeraClient.LOGGER.error("Failed to create JSON field", var8);
         }
      }

      return object;
   }

   @Override
   public void fromJson(JsonElement element) {
      if (element != null && !element.isJsonNull()) {
         JsonObject object = element.getAsJsonObject();
         if (object.has("Enabled")) {
            String enabled = object.get("Enabled").getAsString();
            if (Boolean.parseBoolean(enabled)) {
               this.toggle();
            }
         }

         for (Setting<?> setting : this.getSettings()) {
            try {
               JsonElement settingElement = object.get(setting.getName());
               if (settingElement != null && !settingElement.isJsonNull()) {
                  ConfigManager.setValueFromJson(this, setting, settingElement);
               }
            } catch (Throwable var6) {
               ChimeraClient.LOGGER.error("Failed to load from JSON", var6);
            }
         }
      }
   }

   public static enum Category {
      COMBAT("Combat"),
      MISC("Misc"),
      RENDER("Render"),
      MOVEMENT("Movement"),
      PLAYER("Player"),
      CLIENT("Client"),
      HUD("Hud");

      private final String name;

      private Category(String name) {
         this.name = name;
      }

      public String getName() {
         return this.name;
      }
   }
}
