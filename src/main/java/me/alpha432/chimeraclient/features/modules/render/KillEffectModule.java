package me.alpha432.chimeraclient.features.modules.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.event.impl.render.Render3DEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class KillEffectModule extends Module {
   private static final Identifier TEXTURE = Identifier.of("chimeraclient", "textures/effect/yazirusi.png");
   private static final RenderPipeline TEX_QUAD_PIPELINE = RenderPipeline.builder(
         new Snippet[]{RenderPipelines.TRANSFORMS_PROJECTION_FOG_SNIPPET, RenderPipelines.GLOBALS_SNIPPET}
      )
      .withLocation("pipeline/kill_effect_pipeline")
      .withVertexShader(Identifier.ofVanilla("core/position_tex_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_tex_color"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
      .withSampler("Sampler0")
      .withBlend(BlendFunction.TRANSLUCENT)
      .withCull(false)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .build();
   private static final RenderSetup TEX_QUAD_SETUP = RenderSetup.builder(TEX_QUAD_PIPELINE)
      .texture("Sampler0", TEXTURE, () -> RenderSystem.getSamplerCache().get(FilterMode.NEAREST))
      .build();
   private static final RenderLayer TEX_QUAD_LAYER = RenderLayer.of("kill_effect_tex_quad", TEX_QUAD_SETUP);
   private final Setting<Double> range = this.num("Range", 20.0, 1.0, 100.0);
   private final Setting<Integer> duration = this.num("Duration (MS)", 10000, 500, 10000);
   private final Setting<Float> blinkSpeed = this.num("BlinkSpeed", 0.9F, 0.5F, 20.0F);
   private final Setting<Float> size = this.num("Size", 1.5F, 0.1F, 5.0F);
   private final Map<String, KillEffectModule.DeathEffect> effects = new ConcurrentHashMap<>();
   private final Map<String, Boolean> deadPlayers = new ConcurrentHashMap<>();

   public KillEffectModule() {
      super("KillEffect", "Shows a blinking arrow at the location where a nearby enemy died.", Module.Category.RENDER);
   }

   @Override
   public void onDisable() {
      this.effects.clear();
      this.deadPlayers.clear();
   }

   @Override
   public void onTick() {
      if (!nullCheck() && mc.world != null && mc.player != null) {
         long now = System.currentTimeMillis();
         this.effects.entrySet().removeIf(e -> now - e.getValue().startTime > this.duration.getValue().intValue());

         for (PlayerEntity target : mc.world.getPlayers()) {
            if (target != mc.player && !ChimeraClient.friendManager.isFriend(target) && !(mc.player.distanceTo(target) > this.range.getValue())) {
               String name = target.getGameProfile().name();
               if (!target.isDead() && !(target.getHealth() <= 0.0F)) {
                  this.deadPlayers.remove(name);
               } else if (!this.deadPlayers.containsKey(name)) {
                  this.deadPlayers.put(name, true);
                  Vec3d pos = target.getEntityPos();
                  this.effects.put(name, new KillEffectModule.DeathEffect(pos, now));
               }
            }
         }
      }
   }

   @Subscribe
   @Override
   public void onRender3D(Render3DEvent event) {
      if (!this.effects.isEmpty()) {
         long now = System.currentTimeMillis();
         float half = this.size.getValue() / 2.0F;
         Camera camera = mc.gameRenderer.getCamera();
         Vec3d cam = camera.getCameraPos();
         MatrixStack ps = event.getMatrix();

         for (KillEffectModule.DeathEffect fx : this.effects.values()) {
            long elapsed = now - fx.startTime;
            float progress = (float)elapsed / this.duration.getValue().intValue();
            float blink = (float)Math.abs(Math.sin((float)elapsed * 0.001F * this.blinkSpeed.getValue() * Math.PI));
            float fadeOut = progress > 0.8F ? 1.0F - (progress - 0.8F) / 0.2F : 1.0F;
            float alpha = blink * fadeOut;
            if (!(alpha < 0.01F)) {
               ps.push();
               double dx = fx.pos.x - cam.x;
               double dy = fx.pos.y - cam.y;
               double dz = fx.pos.z - cam.z;
               ps.translate(dx, dy, dz);
               float yaw = (float)Math.atan2(cam.x - fx.pos.x, cam.z - fx.pos.z);
               ps.multiply(new Quaternionf().rotationY(yaw));
               Matrix4f mat = ps.peek().getPositionMatrix();
               BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
               float s = this.size.getValue();
               float yOffset = 0.5F;
               buf.vertex(mat, -half, yOffset, 0.0F).texture(0.0F, 1.0F).color(1.0F, 1.0F, 1.0F, alpha);
               buf.vertex(mat, half, yOffset, 0.0F).texture(1.0F, 1.0F).color(1.0F, 1.0F, 1.0F, alpha);
               buf.vertex(mat, half, yOffset + s, 0.0F).texture(1.0F, 0.0F).color(1.0F, 1.0F, 1.0F, alpha);
               buf.vertex(mat, -half, yOffset + s, 0.0F).texture(0.0F, 0.0F).color(1.0F, 1.0F, 1.0F, alpha);
               TEX_QUAD_LAYER.draw(buf.end());
               ps.pop();
            }
         }
      }
   }

   private static class DeathEffect {
      final Vec3d pos;
      final long startTime;

      DeathEffect(Vec3d pos, long startTime) {
         this.pos = pos;
         this.startTime = startTime;
      }
   }
}
