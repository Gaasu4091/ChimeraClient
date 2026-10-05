package me.alpha432.chimeraclient.features.modules.client;

import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;

public final class FontModule extends Module {
   public static FontModule INSTANCE;
   public final Setting<FontModule.Face> face = this.mode("Style", FontModule.Face.Smooth);
   public final Setting<Boolean> chat = this.bool("Chat", false);
   private boolean previousChat;
   private FontModule.Face previousFace;

   public FontModule() {
      super("Font", "Smooth font for UI and HUD. Chat is optional.", Module.Category.CLIENT);
      INSTANCE = this;
      this.enabled.setValueNoEvent(true);
      this.drawn.setValueNoEvent(false);
   }

   @Override
   public void onTick() {
      if (this.chat.getValue() != this.previousChat || this.face.getValue() != this.previousFace) {
         this.previousChat = this.chat.getValue();
         this.previousFace = this.face.getValue();
         this.refreshChat();
      }
   }

   @Override
   public void onEnable() {
      this.refreshChat();
   }

   @Override
   public void onDisable() {
      this.refreshChat();
   }

   private void refreshChat() {
      if (mc.inGameHud != null) {
         mc.inGameHud.getChatHud().reset();
      }
   }

   public static enum Face {
      Smooth,
      Bold;
   }
}
