package me.alpha432.chimeraclient.manager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.event.impl.render.Render2DEvent;
import me.alpha432.chimeraclient.event.impl.render.Render3DEvent;
import me.alpha432.chimeraclient.features.Feature;
import me.alpha432.chimeraclient.features.commands.ModuleCommand;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.modules.client.ClickGuiModule;
import me.alpha432.chimeraclient.features.modules.client.FontModule;
import me.alpha432.chimeraclient.features.modules.client.HudEditorModule;
import me.alpha432.chimeraclient.features.modules.client.NotificationsModule;
import me.alpha432.chimeraclient.features.modules.client.ShorelineRotationsModule;
import me.alpha432.chimeraclient.features.modules.combat.AntiCEV;
import me.alpha432.chimeraclient.features.modules.combat.AntiCIV;
import me.alpha432.chimeraclient.features.modules.combat.AntiPA;
import me.alpha432.chimeraclient.features.modules.combat.Aura;
import me.alpha432.chimeraclient.features.modules.combat.AutoFire;
import me.alpha432.chimeraclient.features.modules.combat.AutoHitboxDesync;
import me.alpha432.chimeraclient.features.modules.combat.AutoMace;
import me.alpha432.chimeraclient.features.modules.combat.AutoMine;
import me.alpha432.chimeraclient.features.modules.combat.AutoSwordPVP;
import me.alpha432.chimeraclient.features.modules.combat.AutoTrap;
import me.alpha432.chimeraclient.features.modules.combat.AutoXP;
import me.alpha432.chimeraclient.features.modules.combat.Blocker;
import me.alpha432.chimeraclient.features.modules.combat.CriticalsModule;
import me.alpha432.chimeraclient.features.modules.combat.CrystalAura;
import me.alpha432.chimeraclient.features.modules.combat.FaceBlocker;
import me.alpha432.chimeraclient.features.modules.combat.KeyPearlModule;
import me.alpha432.chimeraclient.features.modules.combat.Offhand;
import me.alpha432.chimeraclient.features.modules.combat.Phase;
import me.alpha432.chimeraclient.features.modules.combat.PistonCrystal;
import me.alpha432.chimeraclient.features.modules.combat.SelfFill;
import me.alpha432.chimeraclient.features.modules.combat.SelfTrap;
import me.alpha432.chimeraclient.features.modules.combat.Surround;
import me.alpha432.chimeraclient.features.modules.combat.TwoBPiston;
import me.alpha432.chimeraclient.features.modules.hud.CoordinatesHudModule;
import me.alpha432.chimeraclient.features.modules.hud.WatermarkHudModule;
import me.alpha432.chimeraclient.features.modules.misc.AntiRegear;
import me.alpha432.chimeraclient.features.modules.misc.AutoDeathMessage;
import me.alpha432.chimeraclient.features.modules.misc.AutoGG;
import me.alpha432.chimeraclient.features.modules.misc.AutoRegear;
import me.alpha432.chimeraclient.features.modules.misc.MCFModule;
import me.alpha432.chimeraclient.features.modules.misc.Xcarry;
import me.alpha432.chimeraclient.features.modules.movement.FakeFly;
import me.alpha432.chimeraclient.features.modules.movement.PlayerTp;
import me.alpha432.chimeraclient.features.modules.movement.ReverseStepModule;
import me.alpha432.chimeraclient.features.modules.movement.SpeedModule;
import me.alpha432.chimeraclient.features.modules.movement.Sprint;
import me.alpha432.chimeraclient.features.modules.movement.StepModule;
import me.alpha432.chimeraclient.features.modules.player.FastPlaceModule;
import me.alpha432.chimeraclient.features.modules.player.NoFallModule;
import me.alpha432.chimeraclient.features.modules.player.SpeedMine;
import me.alpha432.chimeraclient.features.modules.player.VelocityModule;
import me.alpha432.chimeraclient.features.modules.render.BlockHighlightModule;
import me.alpha432.chimeraclient.features.modules.render.KillEffectModule;
import me.alpha432.chimeraclient.mio.MioRenderModules;
import me.alpha432.chimeraclient.util.traits.Jsonable;
import me.alpha432.chimeraclient.util.traits.Toggleable;
import me.alpha432.chimeraclient.util.traits.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleManager implements Jsonable, Util {
   private static final Logger LOGGER = LoggerFactory.getLogger("ModuleManager");
   private final Map<Class<? extends Module>, Module> fastRegistry = new HashMap<>();
   private final List<Module> modules = new ArrayList<>();

   public void init() {
      this.register(new WatermarkHudModule());
      this.register(new CoordinatesHudModule());
      this.register(new HudEditorModule());
      this.register(new ClickGuiModule());
      this.register(new FontModule());
      this.register(new NotificationsModule());
      this.register(new CriticalsModule());
      this.register(new AntiCIV());
      this.register(new AntiCEV());
      this.register(new AntiPA());
      this.register(new PistonCrystal());
      this.register(new TwoBPiston());
      this.register(ShorelineRotationsModule.getInstance());
      this.register(new AntiRegear());
      this.register(new Surround());
      this.register(new AutoFire());
      this.register(new SelfTrap());
      this.register(new SelfFill());
      this.register(new CrystalAura());
      this.register(new AutoSwordPVP());
      this.register(new Phase());
      this.register(new Offhand());
      this.register(new FaceBlocker());
      this.register(new AutoHitboxDesync());
      this.register(new AutoMine());
      this.register(new Blocker());
      this.register(new Aura());
      this.register(new AutoTrap());
      this.register(new AutoXP());
      this.register(new AutoMace());
      this.register(new AutoRegear());
      this.register(new MCFModule());
      this.register(new AutoGG());
      this.register(new Xcarry());
      this.register(new AutoDeathMessage());
      this.register(new StepModule());
      this.register(new ReverseStepModule());
      this.register(new PlayerTp());
      this.register(new SpeedModule());
      this.register(new Sprint());
      this.register(new FakeFly());
      this.register(new FastPlaceModule());
      this.register(new VelocityModule());
      this.register(new SpeedMine());
      this.register(new BlockHighlightModule());
      this.register(new KillEffectModule());
      this.register(new NoFallModule());
      this.register(new KeyPearlModule());
      MioRenderModules.register(this);
      LOGGER.info("Registered {} modules", this.modules.size());

      for (Module module : this.modules) {
         ChimeraClient.commandManager.register(new ModuleCommand(module));
      }

      ChimeraClient.configManager.addConfig(this);
   }

   public void register(Module module) {
      this.getModules().add(module);
      this.fastRegistry.put((Class<? extends Module>)module.getClass(), module);
   }

   public List<Module> getModules() {
      return this.modules;
   }

   public Stream<Module> stream() {
      return this.getModules().stream();
   }

   public <T extends Module> T getModuleByClass(Class<T> clazz) {
      return (T)this.fastRegistry.get(clazz);
   }

   public Module getModuleByName(String name) {
      return this.stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
   }

   public Module getModuleByDisplayName(String display) {
      return this.stream().filter(m -> m.getDisplayName().equalsIgnoreCase(display)).findFirst().orElse(null);
   }

   public List<Module> getModulesByCategory(Module.Category category) {
      return this.stream().filter(m -> m.getCategory() == category).toList();
   }

   public List<Module.Category> getCategories() {
      return Arrays.asList(Module.Category.values());
   }

   public void onLoad() {
      this.getModules().forEach(Module::onLoad);
   }

   public void onTick() {
      this.stream().filter(Feature::isEnabled).forEach(Module::onTick);
   }

   public void onRender2D(Render2DEvent event) {
      this.stream().filter(Feature::isEnabled).forEach(module -> module.onRender2D(event));
   }

   public void onRender3D(Render3DEvent event) {
      this.stream().filter(Feature::isEnabled).forEach(module -> module.onRender3D(event));
   }

   public void onUnload() {
      this.getModules().forEach(EVENT_BUS::unregister);
      this.getModules().forEach(Module::onUnload);
   }

   public void onKeyPressed(int key) {
      if (key > 0 && mc.currentScreen == null) {
         this.stream().filter(module -> module.getBind().getKey() == key).forEach(Toggleable::toggle);
      }
   }

   public void onMouseClicked(int button) {
      if (mc.currentScreen == null) {
         int key = -button - 2;
         this.stream().filter(module -> module.getBind().getKey() == key).forEach(Toggleable::toggle);
      }
   }

   @Override
   public JsonElement toJson() {
      JsonObject object = new JsonObject();

      for (Module module : this.getModules()) {
         object.add(module.getName(), module.toJson());
      }

      return object;
   }

   @Override
   public void fromJson(JsonElement element) {
      for (Module module : this.getModules()) {
         module.fromJson(element.getAsJsonObject().get(module.getName()));
      }
   }

   @Override
   public String getFileName() {
      return "modules.json";
   }
}
