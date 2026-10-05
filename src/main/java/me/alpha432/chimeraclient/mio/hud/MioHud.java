package me.alpha432.chimeraclient.mio.hud;

import com.google.gson.JsonParser;
import java.awt.Color;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.event.impl.input.MouseInputEvent;
import me.alpha432.chimeraclient.event.impl.render.Render2DEvent;
import me.alpha432.chimeraclient.event.system.Subscribe;
import me.alpha432.chimeraclient.features.gui.HudEditorScreen;
import me.alpha432.chimeraclient.features.gui.font.FontDraw;
import me.alpha432.chimeraclient.features.modules.Module;
import me.alpha432.chimeraclient.features.modules.client.HudModule;
import me.alpha432.chimeraclient.features.settings.Setting;
import me.alpha432.chimeraclient.mio.MioState;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector2f;

public final class MioHud extends HudModule {
   public final MioHud.Type type;
   public final Setting<MioHud.Anchor> anchor;
   public final Map<String, Setting<?>> options = new LinkedHashMap<>();
   public final Setting<Float> size;
   private final Deque<Double> samples = new ArrayDeque<>();
   private long lastCrypto;
   private volatile String quote = "0.00";
   private volatile int quoteColor = -1;
   private CompletableFuture<?> quoteTask;
   private int fakeX;
   private int fakeZ;
   private boolean wasFake;
   private float smoothWidth;
   private long lastLayout;
   private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();

   public MioHud(MioHud.Type var1) {
      super(name(var1), "Mio " + name(var1) + " HUD", 80.0F, 12.0F);
      this.type = var1;
      this.setDrawn(false);

      MioHud.Anchor var2 = switch (var1) {
         case DIRECTION, POSITION -> MioHud.Anchor.BOTTOM_LEFT;
         case EFFECTS, METRICS -> MioHud.Anchor.BOTTOM_RIGHT;
         default -> MioHud.Anchor.NONE;
         case MODULE_LIST -> MioHud.Anchor.TOP_RIGHT;
      };
      this.anchor = this.mode("Anchor", var2);
      this.size = this.num("Size", 1.0F, 0.5F, 2.0F);
      this.pos.setValue(new Vector2f(0.5F, 0.5F));
      switch (var1) {
         case ARMOR:
            this.bool("Durability", true);
            this.bool("Percentage", true);
            this.bool("BarColor", true);
            this.pos.setValue(new Vector2f(0.5F, 0.9F));
         case CHAT:
         case DIRECTION:
         case MUSIC:
         case PLAYER_MODEL:
         default:
            break;
         case CRYPTO:
            this.str("Coin", "BTC");
            this.mode("Fiat", MioHud.Fiat.USD);
            break;
         case EFFECTS:
            this.bool("Vanilla", false);
            this.mode("Selection", MioHud.Selection.ANY);
            this.str("WhiteList", "");
            break;
         case ENTITY_LIST:
            this.str("WhiteList", "minecraft:player");
            this.str("Items", "minecraft:obsidian");
            this.mode("Selection", MioHud.Selection.BLACKLIST);
            this.mode("ItemSelection", MioHud.Selection.ANY);
            this.mode("Sorting", MioHud.Sort.ALPHABET);
            this.bool("CustomNames", true);
            this.bool("ColoredCount", true);
            break;
         case GRAPH:
            this.num("Ceil", 26.0F, 10.0F, 100.0F);
            this.num("Height", 30, 10, 40);
            this.num("Width", 100, 50, 150);
            break;
         case INVENTORY:
            this.color("Background", new Color(10, 10, 10, 50));
            this.color("Outline", new Color(10, 10, 10, 100));
            this.bool("HideEmpty", true);
            break;
         case LAG:
            this.color("Color", Color.GRAY);
            break;
         case MAP:
            this.mode("Pointer", MioHud.Pointer.ARROW);
            this.num("Width", 76.0F, 50.0F, 500.0F);
            this.num("Height", 76.0F, 50.0F, 250.0F);
            this.color("PointerColor", Color.WHITE);
            this.color("Background", new Color(10, 10, 10, 50));
            this.color("Outline", new Color(10, 10, 10, 100));
            break;
         case METRICS:
            this.bool("Speed", true);
            this.bool("BPS", false);
            this.bool("TPS", true);
            this.bool("Ping", true);
            this.bool("FPS", true);
            this.bool("ServerBrand", true);
            this.bool("Durability", false);
            this.bool("Chest", false);
            this.bool("Double", false);
            break;
         case MODULE_LIST:
            this.bool("OnlyBound", false);
            this.mode("Sort", MioHud.Sort.LENGTH);
            break;
         case POSITION:
            this.num("SafeRange", 0.0F, 0.0F, 100.0F);
            this.bool("Nether", true);
            this.bool("Fake", false);
            break;
         case TEXT_RADAR:
            this.mode("Sort", MioHud.Sort.DISTANCE);
            this.bool("Health", true);
            this.bool("Distance", false);
            this.bool("TotemPops", false);
            this.bool("FriendColor", true);
            this.bool("EnemyColor", true);
            this.bool("Armor", false);
            this.bool("Limit", false);
            this.num("Max", 8, 1, 32);
            this.bool("Ignore", false);
            this.bool("Friends", false);
            this.bool("Nakeds", false);
            break;
         case TOTEMS:
            this.bool("White", false);
            break;
         case WELCOMER:
            this.mode("Mode", MioHud.Welcome.NAME);
            this.str("Text", "Welcome to Mio.");
      }

      for (Setting var4 : new ArrayList<>(this.getSettings())) {
         if (!Set.of("Enabled", "Drawn", "Keybind", "DisplayName", "Position", "Anchor", "Size").contains(var4.getName())) {
            this.options.put(var4.getName(), var4);
         }
      }

      if (var1 == MioHud.Type.WELCOMER) {
         this.options.get("Text").setVisibility(var1x -> this.value("Mode") == MioHud.Welcome.CUSTOM);
      }

      if (var1 == MioHud.Type.TEXT_RADAR) {
         this.options.get("Max").setVisibility(var1x -> this.b("Limit"));
         this.options.get("Friends").setVisibility(var1x -> this.b("Ignore"));
         this.options.get("Nakeds").setVisibility(var1x -> this.b("Ignore"));
      }

      if (var1 == MioHud.Type.METRICS) {
         this.options.get("BPS").setVisibility(var1x -> this.b("Speed"));
         this.options.get("Double").setVisibility(var1x -> this.b("Chest"));
      }
   }

   public static String name(MioHud.Type var0) {
      return switch (var0) {
         case ENTITY_LIST -> "EntityList";
         default -> {
            String var1 = var0.name().toLowerCase(Locale.ROOT);
            yield Character.toUpperCase(var1.charAt(0)) + var1.substring(1);
         }
         case LAG -> "Lag'O'Meter";
         case MODULE_LIST -> "ModuleList";
         case PLAYER_MODEL -> "PlayerModel";
         case TEXT_RADAR -> "TextRadar";
      };
   }

   private Object value(String var1) {
      return this.options.get(var1).getValue();
   }

   private boolean b(String var1) {
      return Boolean.TRUE.equals(this.value(var1));
   }

   private float n(String var1) {
      return ((Number)this.value(var1)).floatValue();
   }

   private String s(String var1) {
      return this.value(var1).toString();
   }

   private Color c(String var1) {
      return (Color)this.value(var1);
   }

   @Subscribe
   public void dragAnchor(MouseInputEvent var1) {
      if (mc.currentScreen instanceof HudEditorScreen && var1.getAction() == 1 && this.isHovering()) {
         this.anchor.setValue(MioHud.Anchor.NONE);
      }
   }

   @Override
   public void onEnable() {
      this.samples.clear();
      if (this.type == MioHud.Type.MUSIC && !System.getProperty("os.name", "").startsWith("Windows")) {
         this.enabled.setValueNoEvent(false);
         EVENT_BUS.unregister(this);
      }
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         if (this.type == MioHud.Type.GRAPH) {
            this.samples.addLast(ChimeraClient.speedManager.getSpeedKmH(mc.player));

            while (this.samples.size() > this.n("Width")) {
               this.samples.removeFirst();
            }
         }

         if (this.type == MioHud.Type.POSITION && this.b("Fake") && !this.wasFake) {
            this.fakeX = (int)(Math.random() * 10000.0) - 5000;
            this.fakeZ = (int)(Math.random() * 10000.0) - 5000;
         }

         if (this.type == MioHud.Type.POSITION) {
            this.wasFake = this.b("Fake");
         }

         if (this.type == MioHud.Type.CRYPTO && System.currentTimeMillis() - this.lastCrypto > 5000L && (this.quoteTask == null || this.quoteTask.isDone())) {
            this.lastCrypto = System.currentTimeMillis();
            String var1 = this.s("Coin").toUpperCase(Locale.ROOT);
            String var2 = this.s("Fiat");
            if (!var1.matches("[A-Z]+")) {
               this.quote = "UNKNOWN";
               return;
            }

            HttpRequest var3 = HttpRequest.newBuilder(URI.create("https://api.coinconvert.net/convert/" + var1 + "/" + var2 + "?amount=1"))
               .timeout(Duration.ofSeconds(10L))
               .header("Accept", "application/json")
               .header("User-Agent", "MioClient/2.0")
               .GET()
               .build();
            this.quoteTask = HTTP.sendAsync(var3, BodyHandlers.ofString()).thenAccept(var2x -> {
               try {
                  if (var2x.statusCode() != 200) {
                     this.quote = "UNKNOWN";
                     return;
                  }

                  double var3x = JsonParser.parseString(var2x.body()).getAsJsonObject().get(var2).getAsDouble();
                  double var5 = Double.parseDouble(this.quote);
                  this.quoteColor = Math.abs(var3x - var5) < 0.01 ? -1 : (var3x > var5 ? -11141291 : -43691);
                  this.quote = String.format(Locale.ROOT, "%.2f", var3x);
               } catch (Exception var7) {
                  this.quote = "UNKNOWN";
                  this.quoteColor = -1;
               }
            }).exceptionally(var1x -> {
               this.quote = "UNKNOWN";
               return null;
            });
         }
      }
   }

   @Override
   protected void render(Render2DEvent var1) {
      if (!nullCheck()) {
         boolean var2 = mc.currentScreen instanceof HudEditorScreen;
         if (MioHudConfig.INSTANCE == null || MioHudConfig.INSTANCE.isEnabled() || var2) {
            label311: {
               DrawContext var3 = var1.getContext();
               List<Text> var4 = this.lines();
               float var5 = 0.0F;
               float var6 = 0.0F;

               var6 = switch (this.type) {
                  case ARMOR -> {
                     var5 = 72.0F;
                     yield 28.0F;
                  }
                  case CHAT -> {
                     var5 = ChatHud.getWidth((Double)mc.options.getChatWidth().getValue());
                     yield ChatHud.getHeight((Double)mc.options.getChatHeightUnfocused().getValue());
                  }
                  default -> {
                     for (Text var8 : var4) {
                        var5 = Math.max(var5, (float)FontDraw.width(mc.textRenderer, var8));
                     }

                     yield var4.size() * 10;
                  }
                  case GRAPH -> {
                     var5 = this.n("Width");
                     yield this.n("Height");
                  }
                  case INVENTORY -> {
                     var5 = 162.0F;
                     yield 54.0F;
                  }
                  case MAP -> {
                     var5 = this.n("Width");
                     yield this.n("Height");
                  }
                  case PLAYER_MODEL -> {
                     var5 = 50.0F;
                     yield 80.0F;
                  }
                  case TOTEMS -> {
                     var5 = 16.0F;
                     yield 16.0F;
                  }
               };
               if (var2 && var5 == 0.0F) {
                  var5 = 80.0F;
                  var6 = 12.0F;
                  var4 = List.of(Text.literal(this.getName()));
               }

               long var23 = System.currentTimeMillis();
               if (MioHudConfig.INSTANCE != null && MioHudConfig.INSTANCE.smoothWidth.getValue() && this.lastLayout != 0L) {
                  this.smoothWidth = this.smoothWidth
                     + (var5 - this.smoothWidth) * (float)(1.0 - Math.exp(-Math.max(0L, var23 - this.lastLayout) / 100.0));
               } else {
                  this.smoothWidth = var5;
               }

               this.lastLayout = var23;
               var5 = Math.max(0.0F, this.smoothWidth);
               float var9 = this.size.getValue();
               this.setWidth(Math.max(4.0F, var5 * var9));
               this.setHeight(Math.max(4.0F, var6 * var9));
               float var10 = this.getX();
               float var11 = this.getY();
               if (this.anchor.getValue() != MioHud.Anchor.NONE) {
                  int var12 = mc.getWindow().getScaledWidth();
                  int var13 = mc.getWindow().getScaledHeight();
                  MioHudConfig var14 = MioHudConfig.INSTANCE;
                  float var15 = var14 == null ? 1.0F : var14.safeX.getValue().intValue();
                  float var16 = var14 == null ? 1.0F : var14.safeY.getValue().intValue();
                  boolean var17 = this.anchor.getValue() == MioHud.Anchor.TOP_RIGHT || this.anchor.getValue() == MioHud.Anchor.BOTTOM_RIGHT;
                  boolean var18 = this.anchor.getValue() == MioHud.Anchor.BOTTOM_LEFT || this.anchor.getValue() == MioHud.Anchor.BOTTOM_RIGHT;
                  var10 = var17
                     ? var12 - this.getWidth() - var15
                     : (this.anchor.getValue() == MioHud.Anchor.TOP_CENTER ? (var12 - this.getWidth()) / 2.0F : var15);
                  var11 = var18 ? var13 - this.getHeight() - var16 : var16;

                  for (MioHud var20 : MioHudRegistry.ELEMENTS) {
                     if (var20 == this) {
                        break;
                     }

                     if (var20.isEnabled() && var20.anchor.getValue() == this.anchor.getValue()) {
                        var11 += (var18 ? -1 : 1) * (var20.getHeight() + 1.0F);
                     }
                  }

                  if (var14 != null
                     && this.anchor.getValue() == MioHud.Anchor.TOP_RIGHT
                     && var14.icons.getValue() == MioHudConfig.Icons.MOVE
                     && !mc.player.getStatusEffects().isEmpty()) {
                     var11 = Math.max(
                        var11,
                        mc.player.getStatusEffects().stream().anyMatch(var0 -> !((StatusEffect)var0.getEffectType().value()).isBeneficial())
                           ? 52.0F
                           : 26.0F
                     );
                  }

                  this.pos.getValue().set(var10 / var12, var11 / var13);
               }

               var3.getMatrices().pushMatrix();
               var3.getMatrices().translate(var10, var11);
               var3.getMatrices().scale(var9, var9);
               switch (this.type) {
                  case ARMOR:
                     EquipmentSlot[] var28 = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
                     byte var34 = 0;

                     for (EquipmentSlot var46 : var28) {
                        ItemStack var47 = mc.player.getEquippedStack(var46);
                        if (!var47.isEmpty()) {
                           item(var3, var47, var34, 10);
                           if (this.b("Durability") && var47.isDamageable()) {
                              int var48 = (int)((var47.getMaxDamage() - var47.getDamage()) * 100.0F / var47.getMaxDamage());
                              String var49 = var48 + (this.b("Percentage") ? "%" : "");
                              FontDraw.drawTextWithShadow(
                                 var3, mc.textRenderer, var49, var34, 0, this.b("BarColor") ? 0xFF000000 | var47.getItemBarColor() : color(var11)
                              );
                           }

                           var34 += 18;
                        }
                     }
                  case CHAT:
                     break;
                  case CRYPTO:
                  case DIRECTION:
                  case EFFECTS:
                  case ENTITY_LIST:
                  case LAG:
                  case METRICS:
                  case MODULE_LIST:
                  case MUSIC:
                  case POSITION:
                  case TEXT_RADAR:
                  default:
                     byte var27 = 0;

                     for (Text var37 : var4) {
                        int var40 = this.type == MioHud.Type.LAG
                           ? this.c("Color").getRGB()
                           : (this.type == MioHud.Type.CRYPTO ? this.quoteColor : color(var11 + var27));
                        if (this.type == MioHud.Type.EFFECTS && this.b("Vanilla")) {
                           List var42 = mc.player
                              .getStatusEffects()
                              .stream()
                              .filter(
                                 var1x -> this.select(
                                    "Selection", "WhiteList", Registries.STATUS_EFFECT.getId((StatusEffect)var1x.getEffectType().value()).toString()
                                 )
                              )
                              .sorted(Comparator.comparing(var0 -> ((StatusEffect)var0.getEffectType().value()).getName().getString()))
                              .toList();
                           if (var27 / 10 < var42.size()) {
                              var40 = 0xFF000000 | ((StatusEffect)((StatusEffectInstance)var42.get(var27 / 10)).getEffectType().value()).getColor();
                           }
                        }

                        int var43 = this.anchor.getValue() != MioHud.Anchor.TOP_RIGHT && this.anchor.getValue() != MioHud.Anchor.BOTTOM_RIGHT
                           ? 0
                           : Math.round(var5) - FontDraw.width(mc.textRenderer, var37);
                        FontDraw.drawTextWithShadow(var3, mc.textRenderer, var37, var43, var27, var40);
                        var27 += 10;
                     }
                     break;
                  case GRAPH:
                     int var26 = 0;
                     int var32 = 0;

                     for (double var39 : this.samples) {
                        int var45 = (int)(1.0 + (this.n("Height") - 2.0F) * (1.0 - Math.min(var39, (double)this.n("Ceil")) / this.n("Ceil")));
                        if (var26 > 0) {
                           var3.fill(var26 - 1, Math.min(var32, var45), var26 + 1, Math.max(var32, var45) + 1, color(var11 + var45));
                        }

                        var32 = var45;
                        var26++;
                     }
                     break;
                  case INVENTORY:
                     boolean var25 = true;

                     for (int var30 = 9; var30 < 36; var30++) {
                        if (!mc.player.getInventory().getStack(var30).isEmpty()) {
                           var25 = false;
                        }
                     }

                     if (!this.b("HideEmpty") || !var25 || var2) {
                        panel(var3, 162, 54, this.c("Background").getRGB(), this.c("Outline").getRGB());

                        for (int var31 = 9; var31 < 36; var31++) {
                           item(var3, mc.player.getInventory().getStack(var31), (var31 - 9) % 9 * 18, (var31 - 9) / 9 * 18);
                        }
                     }
                     break;
                  case MAP:
                     this.drawMap(var3, var5, var6);
                     break;
                  case PLAYER_MODEL:
                     InventoryScreen.drawEntity(var3, 0, 0, 50, 80, 45, 0.0625F, 25.0F, 40.0F, mc.player);
                     break;
                  case TOTEMS:
                     int var24 = 0;

                     for (int var29 = 0; var29 < mc.player.getInventory().size(); var29++) {
                        ItemStack var35 = mc.player.getInventory().getStack(var29);
                        if (var35.isOf(Items.TOTEM_OF_UNDYING)) {
                           var24 += var35.getCount();
                        }
                     }

                     if (var24 > 0) {
                        item(var3, new ItemStack(Items.TOTEM_OF_UNDYING), 0, 0);
                        if (var24 > 1) {
                           FontDraw.drawTextWithShadow(
                              var3, mc.textRenderer, var24 + "", 17 - FontDraw.width(mc.textRenderer, var24 + ""), 9, this.b("White") ? -1 : color(var11)
                           );
                        }
                     }
               }

               var3.getMatrices().popMatrix();
               super.render(var1);
            }
         }
      }
   }

   private static void item(DrawContext var0, ItemStack var1, int var2, int var3) {
      var0.drawItem(var1, var2, var3);
      var0.drawStackOverlay(mc.textRenderer, var1, var2, var3);
   }

   private static int color(float var0) {
      return MioHudConfig.INSTANCE == null ? -1 : MioHudConfig.INSTANCE.color(var0);
   }

   private static void panel(DrawContext var0, int var1, int var2, int var3, int var4) {
      var0.fill(0, 0, var1, var2, var3);
      var0.drawStrokedRectangle(-1, -1, var1 + 2, var2 + 2, var4);
   }

   private boolean select(String var1, String var2, String var3) {
      boolean var4 = Arrays.stream(this.s(var2).split("[,;\\s]+")).anyMatch(var1x -> var1x.equals(var3) || ("minecraft:" + var1x).equals(var3));
      return this.value(var1) == MioHud.Selection.ANY || (this.value(var1) == MioHud.Selection.WHITELIST ? var4 : !var4);
   }

   private List<Text> lines() {
       ArrayList<Text> var1 = new ArrayList<>();
      switch (this.type) {
         case CRYPTO:
            var1.add(Text.literal(this.s("Coin").toUpperCase(Locale.ROOT) + " " + this.quote));
            break;
         case DIRECTION:
            String[] var13 = new String[]{
               "South (+Z)", "SouthWest (-X +Z)", "West (-X)", "NorthWest (-X -Z)", "North (-Z)", "NorthEast (+X -Z)", "East (+X)", "SouthEast (+X +Z)"
            };
            float var19 = mc.gameRenderer.getCamera().getYaw();
            var1.add(Text.literal(var13[Math.floorMod(Math.round(var19 / 45.0F), 8)] + " (" + (int)MathHelper.wrapDegrees(var19) + ")"));
            break;
         case EFFECTS:
            for (StatusEffectInstance var18 : mc.player.getStatusEffects()) {
               String var23 = Registries.STATUS_EFFECT.getId((StatusEffect)var18.getEffectType().value()).toString();
               if (this.select("Selection", "WhiteList", var23)) {
                  int var28 = var18.getDuration() / 20;
                  var1.add(
                     Text.literal(
                        ((StatusEffect)var18.getEffectType().value()).getName().getString()
                           + " "
                           + (var18.getAmplifier() + 1)
                           + " §f"
                           + (var18.isInfinite() ? "∞" : String.format(Locale.ROOT, "%d:%02d", var28 / 60, var28 % 60))
                     )
                  );
               }
            }

            var1.sort(Comparator.comparing(Text::getString));
            break;
         case ENTITY_LIST:
             LinkedHashMap<String, Integer> var11 = new LinkedHashMap<>();

            for (Entity var21 : mc.world.getEntities()) {
               if (this.select("Selection", "WhiteList", Registries.ENTITY_TYPE.getId(var21.getType()).toString())
                  && !(
                     var21 instanceof ItemEntity var25 && !this.select("ItemSelection", "Items", Registries.ITEM.getId(var25.getStack().getItem()).toString())
                  )) {
                  String var26 = var21 instanceof ItemEntity var6
                     ? var6.getStack().getName().getString()
                     : (this.b("CustomNames") ? var21.getName().getString() : var21.getType().getName().getString());
                  var11.merge(var26, var21 instanceof ItemEntity var29 ? var29.getStack().getCount() : 1, Integer::sum);
               }
            }

             ArrayList<Entry<String, Integer>> var17 = new ArrayList<>(var11.entrySet());
            var17.sort(
               this.value("Sorting") == MioHud.Sort.COUNT
                  ? Comparator.comparingInt(var0 -> -(Integer)var0.getValue())
                  : (
                     this.value("Sorting") == MioHud.Sort.LENGTH
                        ? Comparator.comparingInt(var0 -> FontDraw.width(mc.textRenderer, (String)var0.getKey()))
                        : Entry.comparingByKey()
                  )
            );

             for (Entry<String, Integer> var27 : var17) {
               var1.add(Text.literal(var27.getValue() + " x" + (this.b("ColoredCount") ? "§f" : "") + (String)var27.getKey()));
            }
         case GRAPH:
         case INVENTORY:
         case MAP:
         case PLAYER_MODEL:
         case TOTEMS:
         default:
            break;
         case LAG:
            long var10 = ChimeraClient.serverManager.serverRespondingTime();
            if (var10 > 1000L && !mc.isInSingleplayer() || mc.currentScreen instanceof HudEditorScreen) {
               var1.add(Text.literal(String.format(Locale.ROOT, "The server is not responding for %.1fs", (float)var10 / 1000.0F)));
            }
            break;
         case METRICS:
            if (this.b("Speed")) {
               var1.add(
                  Text.literal(
                     String.format(
                        Locale.ROOT,
                        "Speed §f%.2f%s",
                        this.b("BPS") ? ChimeraClient.speedManager.getSpeedBpS(mc.player) : ChimeraClient.speedManager.getSpeedKmH(mc.player),
                        this.b("BPS") ? "b/s" : "km/h"
                     )
                  )
               );
            }

            if (this.b("TPS")) {
               var1.add(Text.literal(String.format(Locale.ROOT, "TPS §f%.2f", ChimeraClient.serverManager.getTps())));
            }

            if (this.b("Ping")) {
               var1.add(Text.literal("Ping §f" + ChimeraClient.serverManager.getPing() + "ms"));
            }

            if (this.b("FPS")) {
               var1.add(Text.literal("FPS §f" + mc.getCurrentFps()));
            }

            if (this.b("ServerBrand")) {
               var1.add(Text.literal("ServerBrand §f" + ChimeraClient.serverManager.getServerBrand()));
            }

            if (this.b("Durability") && mc.player.getMainHandStack().isDamageable()) {
               var1.add(Text.literal("Durability §f" + (mc.player.getMainHandStack().getMaxDamage() - mc.player.getMainHandStack().getDamage())));
            }

            if (this.b("Chest")) {
               long var9 = mc.world.getBlockEntities().stream().filter(var0 -> var0 instanceof ChestBlockEntity || var0 instanceof BarrelBlockEntity).count();
               var1.add(Text.literal("Chests §f" + (this.b("Double") ? var9 / 2L : var9)));
            }

            var1.sort(Comparator.comparingInt(var0 -> -FontDraw.width(mc.textRenderer, var0)));
            break;
         case MODULE_LIST:
            for (Module var15 : ChimeraClient.moduleManager.stream().toList()) {
               if (var15.isEnabled() && var15.isDrawn() && (!this.b("OnlyBound") || var15.getBind().getKey() != -1)) {
                  var1.add(Text.literal(var15.getFullArrayString()));
               }
            }

            var1.sort(
               this.value("Sort") == MioHud.Sort.ALPHABET
                  ? Comparator.comparing(Text::getString)
                  : Comparator.comparingInt(var0 -> -FontDraw.width(mc.textRenderer, var0))
            );
            break;
         case MUSIC:
            if (mc.currentScreen instanceof HudEditorScreen) {
               var1.add(Text.literal("HydrachFM - f3dot"));
            } else if (!MioState.music.isBlank()) {
               var1.add(Text.literal(MioState.music));
            }
            break;
         case POSITION:
            Vec3d var7 = mc.player.getEntityPos();
            if (this.n("SafeRange") > 0.0F && var7.length() > this.n("SafeRange") * 1000.0F) {
               var1.add(Text.literal("XYZ: REDACTED"));
            } else {
               if (this.b("Fake")) {
                  var7 = var7.add(this.fakeX, 0.0, this.fakeZ);
               }

               BlockPos var14 = BlockPos.ofFloored(var7);
               String var20 = "XYZ: §f" + var14.getX() + ", " + var14.getY() + ", " + var14.getZ() + ".";
               if (this.b("Nether") && mc.world.getRegistryKey() != World.END) {
                  double var24 = mc.world.getRegistryKey() == World.NETHER ? 8.0 : 0.125;
                  var20 = var20 + " (" + (int)Math.floor(var7.x * var24) + ", " + var14.getY() + ", " + (int)Math.floor(var7.z * var24) + ".)";
               }

               var1.add(Text.literal(var20));
            }
            break;
         case TEXT_RADAR:
             ArrayList<PlayerEntity> var2 = new ArrayList<>(mc.world.getPlayers());
            var2.removeIf(
               var1x -> var1x == mc.player
                  || !var1x.isAlive()
                  || this.b("Ignore") && (this.b("Friends") && ChimeraClient.friendManager.isFriend(var1x) || this.b("Nakeds") && naked(var1x))
            );
            var2.sort(
               this.value("Sort") == MioHud.Sort.HEALTH
                  ? Comparator.comparingDouble(var0 -> -(var0.getHealth() + var0.getAbsorptionAmount()))
                  : Comparator.comparingDouble(mc.player::squaredDistanceTo)
            );

            for (PlayerEntity var4 : var2) {
               String var5 = (this.b("FriendColor") && ChimeraClient.friendManager.isFriend(var4) ? "§b" : "") + var4.getName().getString();
               if (this.b("Health")) {
                  var5 = var5 + " §f" + (int)(var4.getHealth() + var4.getAbsorptionAmount());
               }

               if (this.b("Distance")) {
                  var5 = var5 + " " + (int)mc.player.distanceTo(var4) + "m";
               }

               if (this.b("TotemPops")) {
                  var5 = var5 + " -" + MioState.pops.getOrDefault(var4.getUuid(), 0);
               }

               if (this.b("Armor")) {
                  var5 = var5 + " " + armorText(var4);
               }

               var1.add(Text.literal(var5));
               if (this.b("Limit") && var1.size() >= this.n("Max")) {
                  return var1;
               }
            }
            break;
         case WELCOMER:
            var1.add(
               Text.literal(
                  this.value("Mode") == MioHud.Welcome.CUSTOM
                     ? this.s("Text")
                     : (this.value("Mode") == MioHud.Welcome.UID ? "Hello uid-1 :')" : "Hello " + mc.player.getName().getString() + " :')")
               )
            );
      }

      return var1;
   }

   private static boolean naked(PlayerEntity var0) {
      for (EquipmentSlot var2 : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
         if (!var0.getEquippedStack(var2).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private static String armorText(PlayerEntity var0) {
      StringJoiner var1 = new StringJoiner("/");

      for (EquipmentSlot var3 : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
         ItemStack var4 = var0.getEquippedStack(var3);
         var1.add(var4.isDamageable() ? (int)((var4.getMaxDamage() - var4.getDamage()) * 100.0F / var4.getMaxDamage()) + "" : "-");
      }

      return var1.toString();
   }

   private void drawMap(DrawContext var1, float var2, float var3) {
      panel(var1, (int)var2, (int)var3, this.c("Background").getRGB(), this.c("Outline").getRGB());
      var1.enableScissor(0, 0, (int)var2, (int)var3);

       for (Entry<Long, Boolean> var5 : MioState.chunks.entrySet()) {
         ChunkPos var6 = new ChunkPos((Long)var5.getKey());
         float var7 = (float)((mc.player.getX() - var6.getStartX()) * 0.5 + var2 * 0.5 - 8.0);
         float var8 = (float)((mc.player.getZ() - var6.getStartZ()) * 0.5 + var3 * 0.5 - 8.0);
         int var9 = var5.getValue() ? 1714657791 : 1144206131;
         var1.fill((int)var7, (int)var8, (int)var7 + 8, (int)var8 + 8, var9);
      }

      var1.disableScissor();
      if (this.value("Pointer") == MioHud.Pointer.DOT) {
         var1.fill((int)var2 / 2 - 1, (int)var3 / 2 - 1, (int)var2 / 2 + 1, (int)var3 / 2 + 1, this.c("PointerColor").getRGB());
      } else if (this.value("Pointer") == MioHud.Pointer.ARROW) {
         var1.getMatrices().pushMatrix();
         var1.getMatrices().translate(var2 / 2.0F, var3 / 2.0F);
         var1.getMatrices().rotate((float)Math.toRadians(mc.gameRenderer.getCamera().getYaw() + 45.0F));
         var1.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            Identifier.of("chimeraclient", "mio/textures/nav.png"),
            -6,
            -6,
            0.0F,
            0.0F,
            12,
            12,
            256,
            256,
            256,
            256,
            this.c("PointerColor").getRGB()
         );
         var1.getMatrices().popMatrix();
      }
   }

   public static enum Anchor {
      TOP_LEFT,
      TOP_RIGHT,
      BOTTOM_LEFT,
      BOTTOM_RIGHT,
      TOP_CENTER,
      NONE;
   }

   public static enum Fiat {
      USD,
      EUR,
      RUB,
      CNY,
      TRY,
      JPY,
      PLN,
      BRL;
   }

   public static enum Pointer {
      NONE,
      ARROW,
      DOT;
   }

   public static enum Selection {
      BLACKLIST,
      WHITELIST,
      ANY;
   }

   public static enum Sort {
      ALPHABET,
      LENGTH,
      COUNT,
      DISTANCE,
      HEALTH;
   }

   public static enum Type {
      ARMOR,
      CHAT,
      CRYPTO,
      DIRECTION,
      EFFECTS,
      ENTITY_LIST,
      GRAPH,
      INVENTORY,
      LAG,
      MAP,
      METRICS,
      MODULE_LIST,
      MUSIC,
      PLAYER_MODEL,
      POSITION,
      TEXT_RADAR,
      TOTEMS,
      WELCOMER;
   }

   public static enum Welcome {
      NAME,
      UID,
      CUSTOM;
   }
}
