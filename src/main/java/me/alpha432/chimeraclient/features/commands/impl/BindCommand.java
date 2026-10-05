package me.alpha432.chimeraclient.features.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.alpha432.chimeraclient.event.impl.input.KeyInputEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.commands.Command;
import me.alpha432.chimeraclient.features.commands.argument.ModuleArgumentType;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Bind;
import me.alpha432.chimeraclient.manager.CommandManager;
import me.alpha432.chimeraclient.util.KeyboardUtil;

public class BindCommand extends Command {
   private Module module;

   public BindCommand() {
      super("bind", "setbind");
      this.setDescription("Sets a key bind for a module");
      EVENT_BUS.register(this);
   }

   @Override
   public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
      builder.then(argument("module", ModuleArgumentType.module(true)).executes(ctx -> {
         this.module = ModuleArgumentType.getModule(ctx, "module");
         return this.success("Press any key...", new Object[0]);
      }));
   }

   @Subscribe
   public void onKey(KeyInputEvent event) {
      if (!nullCheck() && this.module != null && event.getKey() != -1) {
         if (event.getKey() == 256) {
            this.module = null;
            sendMessage("Operation canceled.", "fail");
         } else {
            sendMessage("Bind for {green} %s {} set to {green} %s", this.module.getName(), new Object[]{KeyboardUtil.getKeyName(event.getKey())});
            this.module.bind.setValue(new Bind(event.getKey()));
            this.module = null;
         }
      }
   }
}
