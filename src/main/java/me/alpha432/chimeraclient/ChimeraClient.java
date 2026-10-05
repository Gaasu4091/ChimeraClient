package me.alpha432.chimeraclient;

import me.alpha432.chimeraclient.manager.ColorManager;
import me.alpha432.chimeraclient.manager.CommandManager;
import me.alpha432.chimeraclient.manager.ConfigManager;
import me.alpha432.chimeraclient.manager.EventManager;
import me.alpha432.chimeraclient.manager.FriendManager;
import me.alpha432.chimeraclient.manager.HoleManager;
import me.alpha432.chimeraclient.manager.ModuleManager;
import me.alpha432.chimeraclient.manager.PositionManager;
import me.alpha432.chimeraclient.manager.RotationManager;
import me.alpha432.chimeraclient.manager.ServerManager;
import me.alpha432.chimeraclient.manager.SpeedManager;
import me.alpha432.chimeraclient.util.TextUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChimeraClient implements ModInitializer, ClientModInitializer {
   public static float TIMER = 1.0F;
   public static final Logger LOGGER = LogManager.getLogger("ChimeraClient");
   public static ServerManager serverManager;
   public static ColorManager colorManager;
   public static RotationManager rotationManager;
   public static PositionManager positionManager;
   public static HoleManager holeManager;
   public static EventManager eventManager;
   public static SpeedManager speedManager;
   public static CommandManager commandManager;
   public static FriendManager friendManager;
   public static ModuleManager moduleManager;
   public static ConfigManager configManager;

   public void onInitialize() {
      LOGGER.info("Pre-initializing {} v{}", "ChimeraClient", "1.0.0");
      configManager = new ConfigManager();
      eventManager = new EventManager();
      serverManager = new ServerManager();
      rotationManager = new RotationManager();
      positionManager = new PositionManager();
      friendManager = new FriendManager();
      colorManager = new ColorManager();
      commandManager = new CommandManager();
      moduleManager = new ModuleManager();
      speedManager = new SpeedManager();
      holeManager = new HoleManager();
      TextUtil.init();
   }

   public void onInitializeClient() {
      LOGGER.info("Initializing {}", "ChimeraClient");
      long startTime = System.nanoTime();
      eventManager.init();
      commandManager.init();
      moduleManager.init();
      friendManager.init();
      configManager.load();
      colorManager.init();
      Runtime.getRuntime().addShutdownHook(new Thread(() -> configManager.save()));
      long endTime = System.nanoTime();
      LOGGER.info("Initialized {} in {}ms", "ChimeraClient", (endTime - startTime) / 1000000.0);
   }
}
