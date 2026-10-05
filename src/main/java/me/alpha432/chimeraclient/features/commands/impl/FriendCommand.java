package me.alpha432.chimeraclient.features.commands.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import java.util.StringJoiner;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.features.commands.Command;
import me.alpha432.chimeraclient.manager.CommandManager;

public class FriendCommand extends Command {
   public FriendCommand() {
      super("friend", "friends", "f");
      this.setDescription("Manages your friends list");
   }

   @Override
   public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
      ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)builder.then(literal("list").executes(ctx -> {
         List<String> friends = ChimeraClient.friendManager.getFriends();
         if (friends.isEmpty()) {
            return this.success("You have no friends :(", new Object[0]);
         } else {
            StringJoiner joiner = new StringJoiner(",");
            friends.forEach(joiner::add);
            return this.success("Friends (%s): %s", new Object[]{friends.size(), joiner});
         }
      }))).then(literal("clear").executes(ctx -> {
         ChimeraClient.friendManager.getFriends().clear();
         return this.success("Cleared friends list", new Object[0]);
      }))).then(literal("add").then(argument("username", StringArgumentType.word()).executes(ctx -> {
         String username = StringArgumentType.getString(ctx, "username");
         if (ChimeraClient.friendManager.isFriend(username)) {
            return this.success("{green} %s {reset} is already on your friends list.", new Object[]{username});
         } else {
            ChimeraClient.friendManager.addFriend(username);
            return this.success("Added {green} %s {reset} to your friends list", new Object[]{username});
         }
      })))).then(literal("remove").then(argument("username", StringArgumentType.word()).executes(ctx -> {
         String username = StringArgumentType.getString(ctx, "username");
         if (!ChimeraClient.friendManager.isFriend(username)) {
            return this.success("{green} %s {reset} is not on your friends list.", new Object[]{username});
         } else {
            ChimeraClient.friendManager.removeFriend(username);
            return this.success("Removed {green} %s {reset} from your friends list", new Object[]{username});
         }
      })));
   }
}
