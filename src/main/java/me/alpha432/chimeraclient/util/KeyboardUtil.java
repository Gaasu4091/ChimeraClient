package me.alpha432.chimeraclient.util;

import me.alpha432.chimeraclient.features.settings.Bind;

public class KeyboardUtil {
   public static String getKeyName(int key) {
      String str = new Bind(key).toString().toUpperCase();
      return str.replace("KEY.KEYBOARD", "").replace(".", " ");
   }
}
