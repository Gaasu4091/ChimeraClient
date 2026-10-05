package me.alpha432.chimeraclient.util.player;

import java.nio.charset.StandardCharsets;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.ducks.render.IChatComponent;
import me.alpha432.chimeraclient.features.commands.Command;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ChatUtil {
   public static void sendMessage(Text message, String identifier) {
      sendClientSideMessage(
         Text.empty().setStyle(Style.EMPTY.withColor(Formatting.GRAY)).append("<").append(getClientNameComponent()).append(">").append(" ").append(message),
         identifier
      );
   }

   public static void sendClientSideMessage(Text message, String identifier) {
      if (!Command.nullCheck()) {
         IChatComponent chat = (IChatComponent)Util.mc.inGameHud.getChatHud();
         MessageSignatureData signature = new MessageSignatureData(get256Bytes(identifier));
         chat.chimeraclient$removeMessage(signature);
         chat.chimeraclient$addMessage(new ChatHudLine(Util.mc.inGameHud.getTicks(), message, signature, getMessageTag()));
      }
   }

   public static Text getClientNameComponent() {
      return Text.empty().withColor(ChimeraClient.colorManager.getColorAsInt()).append("ChimeraClient");
   }

   private static MessageIndicator getMessageTag() {
      return new MessageIndicator(ChimeraClient.colorManager.getColorAsInt(), null, null, null);
   }

   private static byte[] get256Bytes(String identifier) {
      byte[] bytes = new byte[256];
      byte[] identifierBytes = identifier.getBytes(StandardCharsets.UTF_8);
      System.arraycopy(identifierBytes, 0, bytes, 0, Math.min(bytes.length, identifierBytes.length));
      return bytes;
   }
}
