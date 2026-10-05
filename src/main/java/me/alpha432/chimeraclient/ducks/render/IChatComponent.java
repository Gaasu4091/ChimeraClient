package me.alpha432.chimeraclient.ducks.render;

import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.network.message.MessageSignatureData;

public interface IChatComponent {
   void chimeraclient$addMessage(ChatHudLine var1);

   void chimeraclient$removeMessage(MessageSignatureData var1);
}
