package me.alpha432.chimeraclient.util.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;

public class Layers {
   private static final RenderLayer GLOBAL_QUADS = RenderLayer.of("global_fill", RenderSetup.builder(Pipelines.GLOBAL_QUADS_PIPELINE).build());
   private static final RenderLayer GLOBAL_LINES = RenderLayer.of("global_lines", RenderSetup.builder(Pipelines.GLOBAL_LINES_PIPELINE).build());

   public static RenderLayer quads() {
      return GLOBAL_QUADS;
   }

   public static RenderLayer lines() {
      return GLOBAL_LINES;
   }
}
