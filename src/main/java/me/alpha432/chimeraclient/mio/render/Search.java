package me.alpha432.chimeraclient.mio.render;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.event.impl.ClientEvent;
import me.alpha432.chimeraclient.event.impl.entity.player.TickEvent;
import me.alpha432.chimeraclient.event.impl.network.PacketEvent;
import me.alpha432.chimeraclient.event.impl.render.Render3DEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.commands.Command;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.manager.CommandManager;
import me.alpha432.chimeraclient.mio.MioRender;
import me.alpha432.chimeraclient.mio.support.SearchChunkScanner;
import me.alpha432.chimeraclient.mio.support.SearchSupport;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor.Brightness;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundInstance.AttenuationType;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.AbstractPiglinEntity;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PiglinActivity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.FishEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.chunk.WorldChunk;

public class Search extends Module {
   private static final Color FRIEND_COLOR = new Color(-16711681, true);
   public final Setting<String> whitelist = this.str("Whitelist", "");
   public final Setting<String> entityTypes = this.str("EntityTypes", "");
   public final Setting<Boolean> entities = this.bool("Entities", false);
   public final Setting<Boolean> ignoreNeg = this.bool("IgnoreNeg", false);
   public final Setting<Boolean> fill = this.bool("Fill", true);
   public final Setting<Float> fillOpacity = this.num("FillOpacity", 0.1F, 0.0F, 1.0F);
   public final Setting<Boolean> outline = this.bool("Outline", true);
   public final Setting<Boolean> tracers = this.bool("Tracers", true);
   public final Setting<Float> tracerOpacity = this.num("TracerOpacity", 0.5F, 0.0F, 1.0F);
   public final Setting<Boolean> sound = this.bool("Sound", false);
   public final Setting<Search.SoundType> type = this.mode("Type", Search.SoundType.WARNING);
   public final Setting<Float> volume = this.num("Volume", 1.0F, 0.0F, 1.0F);
   public final Setting<Boolean> ignoreNatural = this.bool("IgnoreNatural", false);
   public final Setting<Boolean> dungeon = this.bool("Dungeon", true);
   public final Setting<Boolean> fortress = this.bool("Fortress", true);
   public final Setting<Boolean> bastion = this.bool("Bastion", true);
   public final Setting<Boolean> ancientCity = this.bool("AncientCity", false);
   public final Setting<Boolean> trialChambers = this.bool("TrialChambers", true);
   private final Set<BlockPos> set = Collections.synchronizedSet(new HashSet<>());
   private final SearchChunkScanner scanner;
   private final Mutable mutable = new Mutable();
   private final Map<Entity, Long> lastAttack = new WeakHashMap<>();
   private volatile boolean found;
   private long lastSound;
   private volatile String blockSource;
   private volatile Set<Block> blocks = Set.of();
   private String entitySource;
   private Set<EntityType<?>> types = Set.of();

   public Search() {
      super("Search", "Helps finding whitelisted stuff.", Module.Category.RENDER);
      this.fillOpacity.setVisibility(var1 -> this.fill.getValue());
      this.tracerOpacity.setVisibility(var1 -> this.tracers.getValue());
      this.type.setVisibility(var1 -> this.sound.getValue());
      this.volume.setVisibility(var1 -> this.sound.getValue());
      this.dungeon.setVisibility(var1 -> this.ignoreNatural.getValue());
      this.fortress.setVisibility(var1 -> this.ignoreNatural.getValue());
      this.bastion.setVisibility(var1 -> this.ignoreNatural.getValue());
      this.ancientCity.setVisibility(var1 -> this.ignoreNatural.getValue());
      this.trialChambers.setVisibility(var1 -> this.ignoreNatural.getValue());
      this.scanner = new SearchChunkScanner(
         "mio-search",
         1,
         (var1, var2) -> this.scan(var2),
         var1 -> this.set
            .removeIf(
               var1x -> var1x.getX() >= var1.getStartX()
                  && var1x.getX() <= var1.getEndX()
                  && var1x.getZ() >= var1.getStartZ()
                  && var1x.getZ() <= var1.getEndZ()
            ),
         (var1, var2) -> {
            if (!this.blocks().contains(var2.getBlock())) {
               this.set.remove(var1);
            } else if (!var2.isAir() && this.set.add(var1)) {
               this.found = true;
            }
         }
      );
      if (ChimeraClient.commandManager != null) {
         ChimeraClient.commandManager.register(new Search.SearchCommand());
      }
   }

   @Override
   public void onEnable() {
      this.found = false;
      this.scanner.start();
      if (!nullCheck()) {
         this.scanner.scanLoaded();
      }
   }

   @Override
   public void onDisable() {
      this.set.clear();
      this.scanner.stop();
   }

   @Subscribe
   public void onSettingChange(ClientEvent var1) {
      if (var1.getType() == ClientEvent.Type.SETTING_UPDATE && var1.getSetting() == this.whitelist) {
         String var2 = (String)var1.getSetting().getPlannedValue();
         this.blocks = parse(var2, Registries.BLOCK);
         this.blockSource = var2;
         if (mc.world != null && this.scanner.running()) {
            this.set.clear();
            this.scanner.scanLoaded();
         }
      }
   }

   @Subscribe
   public void onPacketReceive(PacketEvent.Receive var1) {
      if (!(var1.getPacket() instanceof GameJoinS2CPacket) && !(var1.getPacket() instanceof PlayerRespawnS2CPacket)) {
         this.scanner.onPacket(var1.getPacket());
      } else {
         this.scanner.reset();
         this.set.clear();
      }
   }

   @Subscribe
   public void onPlayerTick(TickEvent var1) {
      if (this.sound.getValue() && this.found) {
         this.found = false;
         if (System.currentTimeMillis() - this.lastSound >= 100L) {
            this.playSound();
            this.lastSound = System.currentTimeMillis();
         }
      }
   }

   @Subscribe
   public void onRenderWorld(Render3DEvent var1) {
      if (!nullCheck()) {
         MatrixStack var2 = var1.getMatrix();
         Frustum var3 = SearchSupport.frustum();
         if (this.entities.getValue()) {
            Set var4 = this.entityTypes();

            for (Entity var6 : mc.world.getEntities()) {
               if (var4.contains(var6.getType())) {
                  Box var7 = MioRender.lerpBox(var6, var1.getDelta());
                  this.draw(var2, var3, var7, var7.getCenter(), this.entityColor(var6));
               }
            }
         }

         synchronized (this.set) {
            for (BlockPos var13 : this.set) {
               BlockState var14 = mc.world.getBlockState(var13);
               if (!var14.isAir() && !var14.isOf(Blocks.AIR)) {
                  VoxelShape var8 = var14.getOutlineShape(mc.world, var13);
                  if (var8.isEmpty()) {
                     var8 = VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
                  }

                  this.draw(var2, var3, var8.getBoundingBox().offset(var13), Vec3d.ofCenter(var13), this.blockColor(var14, var13));
               }
            }
         }
      }
   }

   private void draw(MatrixStack var1, Frustum var2, Box var3, Vec3d var4, Color var5) {
      if (var2.isVisible(var3)) {
         if (this.fill.getValue()) {
            MioRender.fill(var1, var3, MioRender.alpha(var5, (int)(255.0F * this.fillOpacity.getValue())));
         }

         if (this.outline.getValue()) {
            MioRender.outline(var1, var3, var5, 1.0F);
         }
      }

      if (this.tracers.getValue()) {
         Camera var6 = mc.gameRenderer.getCamera();
         Vec3d var7 = new Vec3d(0.0, 0.0, 1.0)
            .rotateX(-((float)Math.toRadians(var6.getPitch())))
            .rotateY(-((float)Math.toRadians(var6.getYaw())))
            .add(MioRender.camera());
         MioRender.line(var1, var7, var4, MioRender.alpha(var5, (int)(255.0F * this.tracerOpacity.getValue())), 1.0F);
      }
   }

   private void scan(WorldChunk var1) {
      if (this.set.size() <= 100000 && mc.world != null) {
         Set var2 = this.blocks();
         if (!var2.isEmpty()) {
            int var3 = var1.getPos().getStartX();
            int var4 = var1.getPos().getStartZ();
            int var5 = this.ignoreNeg.getValue() ? 0 : mc.world.getBottomY();
            int var6 = mc.world.getTopYInclusive();

            for (int var7 = 0; var7 < 16; var7++) {
               for (int var8 = var5; var8 <= var6; var8++) {
                  for (int var9 = 0; var9 < 16; var9++) {
                     this.mutable.set(var3 + var7, var8, var4 + var9);
                     BlockState var10 = var1.getBlockState(this.mutable);
                     if (!var10.isAir()
                        && (
                           !this.ignoreNatural.getValue()
                              || !SearchSupport.naturalChest(
                                 var1,
                                 this.mutable,
                                 this.fortress.getValue(),
                                 this.bastion.getValue(),
                                 this.ancientCity.getValue(),
                                 this.dungeon.getValue(),
                                 this.trialChambers.getValue()
                              )
                        )
                        && var2.contains(var10.getBlock())) {
                        this.found = true;
                        this.set.add(this.mutable.toImmutable());
                     }
                  }
               }
            }
         }
      }
   }

   private void playSound() {
      Identifier var1 = Identifier.of("chimeraclient", "mio." + this.type.getValue().name().toLowerCase(Locale.ROOT));
      mc.execute(
         () -> mc.getSoundManager()
            .play(
               new PositionedSoundInstance(
                  var1, SoundCategory.MASTER, this.volume.getValue(), 1.0F, SoundInstance.createRandom(), false, 0, AttenuationType.NONE, 0.0, 0.0, 0.0, true
               )
            )
      );
   }

   private Color blockColor(BlockState var1, BlockPos var2) {
      if (var1.getBlock() == Blocks.NETHER_PORTAL) {
         return new Color(144, 28, 255);
      } else if (var1.getBlock() == Blocks.ENDER_CHEST) {
         return new Color(125, 40, 180);
      } else {
         int var3 = var1.getMapColor(mc.world, var2).getRenderColor(Brightness.HIGH);
         if (var1.getBlock() instanceof BedBlock var4) {
            var3 = var4.getColor().getMapColor().getRenderColor(Brightness.HIGH);
         }

         return new Color(var3 >> 16 & 0xFF, var3 >> 8 & 0xFF, var3 & 0xFF);
      }
   }

   private Color entityColor(Entity var1) {
      Color var2 = Color.GRAY;
      if (var1 instanceof AbstractMinecartEntity) {
         var2 = Color.BLUE;
      }

      if (this.hostile(var1)) {
         var2 = Color.RED;
      }

      if (var1 instanceof PlayerEntity var3 && ChimeraClient.friendManager.isFriend(var3)) {
         var2 = FRIEND_COLOR;
      }

      if (var1 instanceof AnimalEntity || var1 instanceof FishEntity) {
         var2 = Color.GREEN;
      }

      return var2;
   }

   private boolean hostile(Entity var1) {
      if (var1 instanceof EndermanEntity var8) {
         return var8.isAngry();
      } else if (var1 instanceof ZombifiedPiglinEntity var7) {
         long var9 = System.currentTimeMillis();
         if (var7.isAttacking()) {
            this.lastAttack.put(var7, var9);
         }

         Long var5 = this.lastAttack.get(var7);
         return var5 != null && var9 - var5 < 1000L;
      } else if (var1 instanceof Angerable var6) {
         return var6.hasAngerTime();
      } else if (!(var1 instanceof AbstractPiglinEntity var2)) {
         return var1 instanceof PolarBearEntity ? true : var1 instanceof Monster;
      } else {
         PiglinActivity var3 = var2.getActivity();
         return var3 == PiglinActivity.CROSSBOW_CHARGE || var3 == PiglinActivity.CROSSBOW_HOLD || var3 == PiglinActivity.ATTACKING_WITH_MELEE_WEAPON;
      }
   }

   private Set<Block> blocks() {
      String var1 = this.whitelist.getValue();
      if (!var1.equals(this.blockSource)) {
         this.blocks = parse(var1, Registries.BLOCK);
         this.blockSource = var1;
      }

      return this.blocks;
   }

   private Set<EntityType<?>> entityTypes() {
      String var1 = this.entityTypes.getValue();
      if (!var1.equals(this.entitySource)) {
         this.types = parse(var1, Registries.ENTITY_TYPE);
         this.entitySource = var1;
      }

      return this.types;
   }

   private static <T> Set<T> parse(String var0, Registry<T> var1) {
      LinkedHashSet var2 = new LinkedHashSet();

      for (String var6 : var0.split("[,\\s]+")) {
         Identifier var7 = Identifier.tryParse(var6.trim().toLowerCase(Locale.ROOT));
         if (var7 != null && var1.containsId(var7)) {
            var1.getOptionalValue(var7).ifPresent(var2::add);
         }
      }

      return Set.copyOf(var2);
   }

   private static <T> String join(Set<T> var0, Registry<T> var1) {
      StringBuilder var2 = new StringBuilder();

       for (T var4 : var0) {
         if (!var2.isEmpty()) {
            var2.append(',');
         }

         var2.append(var1.getId(var4));
      }

      return var2.toString();
   }

   private final class SearchCommand extends Command {
      SearchCommand() {
         super("search");
         this.setDescription("Edits Search's block whitelist (or entity types with 'entity')");
      }

      @Override
      public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> var1) {
         var1.then(this.branch(literal("entity"), Search.this.entityTypes, Registries.ENTITY_TYPE, Search.this::entityTypes));
         this.branch(var1, Search.this.whitelist, Registries.BLOCK, Search.this::blocks);
      }

      private <T> LiteralArgumentBuilder<CommandManager> branch(
         LiteralArgumentBuilder<CommandManager> var1, Setting<String> var2, Registry<T> var3, Supplier<Set<T>> var4
      ) {
         var1.then(literal("add").then(argument("ids", StringArgumentType.greedyString()).executes(var4x -> this.edit(var4x, var2, var3, var4, true))));
         var1.then(literal("remove").then(argument("ids", StringArgumentType.greedyString()).executes(var4x -> this.edit(var4x, var2, var3, var4, false))));
         var1.then(literal("del").then(argument("ids", StringArgumentType.greedyString()).executes(var4x -> this.edit(var4x, var2, var3, var4, false))));
         var1.then(literal("list").executes(var4x -> {
            Set var5 = (Set)var4.get();
            return this.success("%s (%s): %s", new Object[]{var2.getName(), var5.size(), var5.isEmpty() ? "empty" : Search.join(var5, var3)});
         }));
         var1.then(literal("clear").executes(var2x -> {
            var2.setValue("");
            return this.success("Cleared %s", new Object[]{var2.getName()});
         }));
         return var1;
      }

      private <T> int edit(CommandContext<CommandManager> var1, Setting<String> var2, Registry<T> var3, Supplier<Set<T>> var4, boolean var5) {
         String var6 = StringArgumentType.getString(var1, "ids");
         Set var7 = Search.parse(var6, var3);
         if (var7.isEmpty()) {
            return this.fail("Unknown id(s): %s", new Object[]{var6});
         } else {
            LinkedHashSet var8 = new LinkedHashSet((Collection)var4.get());
            if (var5) {
               var8.addAll(var7);
            } else {
               var8.removeAll(var7);
            }

            var2.setValue(Search.join(var8, var3));
            return this.success("%s %s: %s", new Object[]{var5 ? "Added to" : "Removed from", var2.getName(), Search.join(var7, var3)});
         }
      }
   }

   public static enum SoundType {
      KABAN,
      NEVERLOSE,
      CSS,
      COD,
      QUAKE,
      TOOLBOX,
      WARNING,
      STEAM,
      WHATSAPP,
      VK,
      ICQ,
      STALKER,
      HOVER,
      CLICK,
      RCLICK,
      BODYSPLAT;
   }
}
