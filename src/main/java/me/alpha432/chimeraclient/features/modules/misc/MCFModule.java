package me.alpha432.chimeraclient.features.modules.misc;

import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.features.commands.Command;
import me.alpha432.chimeraclient.features.modules.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;

public class MCFModule extends Module {
   private boolean pressed;

   public MCFModule() {
      super("MCF", "Middle click friend", Module.Category.MISC);
   }

   @Override
   public void onTick() {
      if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 2) == 1) {
         if (!this.pressed) {
            this.click();
         }

         this.pressed = true;
      } else {
         this.pressed = false;
      }
   }

   private void click() {
      Entity targetedEntity = mc.targetedEntity;
      if (targetedEntity instanceof PlayerEntity) {
         String name = ((PlayerEntity)targetedEntity).getGameProfile().name();
         if (ChimeraClient.friendManager.isFriend(name)) {
            ChimeraClient.friendManager.removeFriend(name);
            Command.sendMessage("{red} %s has been unfriended.", name);
         } else {
            ChimeraClient.friendManager.addFriend(name);
            Command.sendMessage("{aqua} %s has been friended.", name);
         }
      }
   }
}
