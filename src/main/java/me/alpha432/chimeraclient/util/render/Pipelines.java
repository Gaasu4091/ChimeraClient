package me.alpha432.chimeraclient.util.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

class Pipelines {
   static final Snippet GLOBAL_LINES_SNIPPET = RenderPipeline.builder(
         new Snippet[]{RenderPipelines.TRANSFORMS_PROJECTION_FOG_SNIPPET, RenderPipelines.GLOBALS_SNIPPET}
      )
      .withVertexShader(Identifier.of("chimeraclient", "core/rendertype_lines_smooth"))
      .withFragmentShader(Identifier.of("chimeraclient", "core/rendertype_lines_smooth"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH, DrawMode.LINES)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withCull(false)
      .buildSnippet();
   static final RenderPipeline GLOBAL_QUADS_PIPELINE = RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET})
      .withLocation("pipeline/global_fill_pipeline")
      .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthWrite(false)
      .withCull(false)
      .build();
   static final RenderPipeline GLOBAL_LINES_PIPELINE = RenderPipeline.builder(new Snippet[]{GLOBAL_LINES_SNIPPET})
      .withLocation("pipeline/global_lines_pipeline")
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .build();
}
