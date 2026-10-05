package me.alpha432.chimeraclient.mio.render;

import me.alpha432.chimeraclient.mio.MioConfiguredModule;

public final class Markers extends MioConfiguredModule {
   public Markers() {
      super("Markers");
      this.options.put("setting", this.num("Radius", 45.0F, 10.0F, 200.0F));
      this.options.put("setting2", this.bool("OnlyOffscreen", false));
   }
}
