package me.alpha432.chimeraclient.manager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.features.Feature;
import me.alpha432.chimeraclient.features.commands.Command;
import me.alpha432.chimeraclient.features.commands.impl.BindCommand;
import me.alpha432.chimeraclient.features.commands.impl.DrawnCommand;
import me.alpha432.chimeraclient.features.commands.impl.FriendCommand;
import me.alpha432.chimeraclient.features.commands.impl.HelpCommand;
import me.alpha432.chimeraclient.features.commands.impl.PrefixCommand;
import me.alpha432.chimeraclient.features.commands.impl.ToggleCommand;
import me.alpha432.chimeraclient.util.traits.Jsonable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommandManager extends Feature implements Jsonable {
   private static final Logger LOGGER = LogManager.getLogger("Commands");
   private final CommandDispatcher<CommandManager> dispatcher = new CommandDispatcher();
   private final Map<String, Command> commandAliasMap = new LinkedHashMap<>();
   private final List<Command> commandList = new LinkedList<>();
   private String commandPrefix = ".";

   public CommandManager() {
      super("Commands");
   }

   public void init() {
      this.register(new BindCommand());
      this.register(new DrawnCommand());
      this.register(new FriendCommand());
      this.register(new HelpCommand());
      this.register(new PrefixCommand());
      this.register(new ToggleCommand());
      LOGGER.info("Registered {} commands", this.commandList.size());
      ChimeraClient.configManager.addConfig(this);
   }

   public void onChatSent(String message) {
      try {
         int result = this.dispatcher.execute(message.substring(this.commandPrefix.length()).trim(), this);
         if (result == 1) {
            Command.sendMessage("{green} Command executed successfully", "general");
         } else if (result == 0) {
            Command.sendMessage("{red} Failed to execute command", "general");
         }
      } catch (CommandSyntaxException var3) {
         LOGGER.error("Failed to execute command", var3);
         Command.sendMessage("{red} %s", "general", var3.getMessage());
      }
   }

   public void register(Command command) {
      this.commandList.add(command);

      for (String alias : command.getAliases()) {
         this.commandAliasMap.put(alias, command);
         LiteralArgumentBuilder<CommandManager> builder = Command.literal(alias);
         command.createArgumentBuilder(builder);
         this.dispatcher.register(builder);
      }
   }

   public void setCommandPrefix(String commandPrefix) {
      this.commandPrefix = commandPrefix;
   }

   public String getCommandPrefix() {
      return this.commandPrefix;
   }

   public Command getCommand(String alias) {
      return this.commandAliasMap.get(alias);
   }

   public Set<String> getCommandAliases() {
      return this.commandAliasMap.keySet();
   }

   public List<Command> getCommands() {
      return this.commandList;
   }

   public CommandDispatcher<CommandManager> getDispatcher() {
      return this.dispatcher;
   }

   @Override
   public JsonElement toJson() {
      JsonObject object = new JsonObject();
      object.addProperty("prefix", this.commandPrefix);
      return object;
   }

   @Override
   public void fromJson(JsonElement element) {
      if (element != null && element.isJsonObject()) {
         JsonObject object = element.getAsJsonObject();
         if (object.has("prefix")) {
            this.setCommandPrefix(object.get("prefix").getAsString());
         }
      }
   }

   @Override
   public String getFileName() {
      return "commands.json";
   }
}
