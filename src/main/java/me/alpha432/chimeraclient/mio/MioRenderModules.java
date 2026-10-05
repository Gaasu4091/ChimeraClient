package me.alpha432.chimeraclient.mio;

import me.alpha432.chimeraclient.manager.ModuleManager;
import me.alpha432.chimeraclient.mio.hud.MioHudRegistry;
import me.alpha432.chimeraclient.mio.render.Ambience;
import me.alpha432.chimeraclient.mio.render.Animations;
import me.alpha432.chimeraclient.mio.render.Blur;
import me.alpha432.chimeraclient.mio.render.Borders;
import me.alpha432.chimeraclient.mio.render.BreakHighlight;
import me.alpha432.chimeraclient.mio.render.Chams;
import me.alpha432.chimeraclient.mio.render.Crosshair;
import me.alpha432.chimeraclient.mio.render.ESP;
import me.alpha432.chimeraclient.mio.render.FreeLook;
import me.alpha432.chimeraclient.mio.render.Glint;
import me.alpha432.chimeraclient.mio.render.Highlight;
import me.alpha432.chimeraclient.mio.render.Hitmarker;
import me.alpha432.chimeraclient.mio.render.HoleESP;
import me.alpha432.chimeraclient.mio.render.LogoutSpots;
import me.alpha432.chimeraclient.mio.render.Markers;
import me.alpha432.chimeraclient.mio.render.NameTags;
import me.alpha432.chimeraclient.mio.render.NoBob;
import me.alpha432.chimeraclient.mio.render.NoRender;
import me.alpha432.chimeraclient.mio.render.Particles;
import me.alpha432.chimeraclient.mio.render.PhaseESP;
import me.alpha432.chimeraclient.mio.render.Search;
import me.alpha432.chimeraclient.mio.render.Shader;
import me.alpha432.chimeraclient.mio.render.Skeleton;
import me.alpha432.chimeraclient.mio.render.SkyColor;
import me.alpha432.chimeraclient.mio.render.Tooltips;
import me.alpha432.chimeraclient.mio.render.Tracers;
import me.alpha432.chimeraclient.mio.render.Trails;
import me.alpha432.chimeraclient.mio.render.Trajectories;
import me.alpha432.chimeraclient.mio.render.Tunnels;
import me.alpha432.chimeraclient.mio.render.ViewClip;
import me.alpha432.chimeraclient.mio.render.ViewModel;
import me.alpha432.chimeraclient.mio.render.VoidESP;
import me.alpha432.chimeraclient.mio.render.Waypoints;
import me.alpha432.chimeraclient.mio.render.Xray;
import me.alpha432.chimeraclient.mio.render.Zoom;
import me.alpha432.chimeraclient.mio.support.WaypointsSupport;

public final class MioRenderModules {
   private MioRenderModules() {
   }

   public static void register(ModuleManager var0) {
      var0.register(new Ambience());
      var0.register(new Animations());
      var0.register(new Blur());
      var0.register(new Borders());
      var0.register(new BreakHighlight());
      var0.register(new Chams());
      var0.register(new Crosshair());
      var0.register(new ESP());
      var0.register(new FreeLook());
      var0.register(new Glint());
      var0.register(new Highlight());
      var0.register(new Hitmarker());
      var0.register(new HoleESP());
      var0.register(new LogoutSpots());
      var0.register(new Markers());
      var0.register(new NameTags());
      var0.register(new NoBob());
      var0.register(new NoRender());
      var0.register(new Particles());
      var0.register(new PhaseESP());
      var0.register(new Search());
      var0.register(new Shader());
      var0.register(new Skeleton());
      var0.register(new SkyColor());
      var0.register(new Tooltips());
      var0.register(new Tracers());
      var0.register(new Trails());
      var0.register(new Trajectories());
      var0.register(new Tunnels());
      var0.register(new ViewClip());
      var0.register(new ViewModel());
      var0.register(new VoidESP());
      var0.register(new Waypoints());
      var0.register(new Xray());
      var0.register(new Zoom());
      MioHudRegistry.register(var0);
      MioState.init();
      MioRenderModules.WaypointsSupportInit.init();
   }

   private static final class WaypointsSupportInit {
      static void init() {
         WaypointsSupport.init();
      }
   }
}
