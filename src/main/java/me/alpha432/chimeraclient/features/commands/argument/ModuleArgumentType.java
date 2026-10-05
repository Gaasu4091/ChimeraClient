package me.alpha432.chimeraclient.features.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.features.commands.CommandExceptions;
import me.alpha432.chimeraclient.features.modules.Module;

public record ModuleArgumentType(boolean fullName) implements ArgumentType<Module> {
   public Module parse(StringReader reader) throws CommandSyntaxException {
      String value = reader.readString().toLowerCase();

      for (Module module : ChimeraClient.moduleManager.getModules()) {
         if (value.equalsIgnoreCase(module.getName()) || module.getName().startsWith(value)) {
            return module;
         }
      }

      throw CommandExceptions.invalidArgument("Invalid module").createWithContext(reader);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      String input = builder.getRemainingLowerCase();

      for (Module module : ChimeraClient.moduleManager.getModules()) {
         String name = module.getName().toLowerCase();
         String displayName = module.getDisplayName().toLowerCase();
         if (name.contains(input) || displayName.contains(input)) {
            builder.suggest(name);
         }
      }

      return builder.buildFuture();
   }

   public static ModuleArgumentType module() {
      return module(false);
   }

   public static ModuleArgumentType module(boolean fullName) {
      return new ModuleArgumentType(fullName);
   }

   public static Module getModule(CommandContext<?> ctx, String name) {
      return (Module)ctx.getArgument(name, Module.class);
   }
}
