package me.alpha432.chimeraclient.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.util.traits.Jsonable;
import net.minecraft.entity.player.PlayerEntity;

public class FriendManager implements Jsonable {
   private final List<String> friends = new ArrayList<>();

   public void init() {
      ChimeraClient.configManager.addConfig(this);
   }

   public boolean isFriend(String name) {
      return this.friends.stream().anyMatch(friend -> friend.equalsIgnoreCase(name));
   }

   public boolean isFriend(PlayerEntity player) {
      return this.isFriend(player.getGameProfile().name());
   }

   public void addFriend(String name) {
      this.friends.add(name);
   }

   public void removeFriend(String name) {
      this.friends.removeIf(s -> s.equalsIgnoreCase(name));
   }

   public List<String> getFriends() {
      return this.friends;
   }

   @Override
   public JsonElement toJson() {
      JsonObject object = new JsonObject();
      JsonArray array = new JsonArray();

      for (String friend : this.friends) {
         array.add(friend);
      }

      object.add("friends", array);
      return object;
   }

   @Override
   public void fromJson(JsonElement element) {
      for (JsonElement e : element.getAsJsonObject().get("friends").getAsJsonArray()) {
         this.friends.add(e.getAsString());
      }
   }

   @Override
   public String getFileName() {
      return "friends.json";
   }
}
