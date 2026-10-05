package me.alpha432.chimeraclient.mio.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Stream;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.features.commands.Command;
import me.alpha432.chimeraclient.manager.CommandManager;
import me.alpha432.chimeraclient.util.player.ChatUtil;
import me.alpha432.chimeraclient.util.traits.Jsonable;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.command.argument.CoordinateArgument;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.text.HoverEvent.ShowText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

public final class WaypointsSupport implements Util {
   private static final List<WaypointsSupport.Waypoint> WAYPOINTS = new CopyOnWriteArrayList<>();
   private static final AtomicLong MESSAGE_ID = new AtomicLong();
   private static boolean initialized;
   private static final SimpleCommandExceptionType INCOMPLETE = new SimpleCommandExceptionType(Text.literal("Incomplete (expected 3 coordinates)"));
   private static final DynamicCommandExceptionType BAD_DIMENSION = new DynamicCommandExceptionType(var0 -> Text.literal("Unknown dimension " + var0));

   private WaypointsSupport() {
   }

   public static void init() {
      if (!initialized) {
         initialized = true;
         if (ChimeraClient.configManager != null) {
            ChimeraClient.configManager.addConfig(new WaypointsSupport.Store());
         }

         if (ChimeraClient.commandManager != null) {
            ChimeraClient.commandManager.register(new WaypointsSupport.WaypointsCommand());
         }
      }
   }

   public static List<WaypointsSupport.Waypoint> waypoints() {
      return WAYPOINTS;
   }

   public static String dimension() {
      if (mc.world == null) {
         return "overworld";
      } else {
         String var0 = mc.world.getRegistryKey().getValue().getPath();

         return switch (var0) {
            case "the_nether" -> "nether";
            case "the_end" -> "end";
            default -> "overworld";
         };
      }
   }

   public static String server() {
      if (mc.player == null) {
         return "singleplayer";
      } else {
         ServerInfo var0 = mc.player.networkHandler.getServerInfo();
         return var0 == null ? "singleplayer" : normalize(var0.address);
      }
   }

   public static String normalize(String var0) {
      String[] var1 = var0.split(":");
      return var1.length == 2 && var1[1].equalsIgnoreCase("25565") ? var1[0] : var0;
   }

   public static Vec3d converted(WaypointsSupport.Waypoint var0) {
      Vec3d var1 = var0.pos();
      String var2 = dimension();
      if (var0.dimension.equals("overworld") && var2.equals("nether")) {
         return var1.multiply(0.125, 1.0, 0.125);
      } else if (var0.dimension.equals("nether") && var2.equals("overworld")) {
         return var1.multiply(8.0, 1.0, 8.0);
      } else {
         return var0.dimension.equals(var2) ? var1 : null;
      }
   }

   public static boolean register(WaypointsSupport.Waypoint var0) {
      for (WaypointsSupport.Waypoint var2 : WAYPOINTS) {
         if (var2.name.equalsIgnoreCase(var0.name) && var2.server.equalsIgnoreCase(var0.server)) {
            return false;
         }
      }

      return WAYPOINTS.add(var0);
   }

   public static void purgeInvalid() {
      WAYPOINTS.removeIf(var0 -> var0 == null || !var0.valid());
   }

   private static void message(Text var0) {
      ChatUtil.sendMessage(var0, "mio-waypoints-" + MESSAGE_ID.incrementAndGet());
   }

   private static CompletableFuture<Suggestions> suggest(Stream<String> var0, SuggestionsBuilder var1) {
      String var2 = var1.getRemaining().toLowerCase(Locale.ROOT);
      var0.filter(var1x -> var1x.toLowerCase(Locale.ROOT).startsWith(var2)).forEach(var1::suggest);
      return var1.buildFuture();
   }

   private static String quotedServer(WaypointsSupport.Waypoint var0) {
      String var1 = var0.server.toLowerCase(Locale.ROOT);
      return var1.contains(":") ? "\"" + var1 + "\"" : var1;
   }

   private static CompletableFuture<Suggestions> suggestServers(CommandContext<CommandManager> var0, SuggestionsBuilder var1) {
      return suggest(WAYPOINTS.stream().map(WaypointsSupport::quotedServer).distinct(), var1);
   }

   private static CompletableFuture<Suggestions> suggestNames(CommandContext<CommandManager> var0, SuggestionsBuilder var1) {
      String var2 = StringArgumentType.getString(var0, "server");
      return suggest(WAYPOINTS.stream().filter(var1x -> var1x.server.equalsIgnoreCase(var2)).map(var0x -> var0x.name), var1);
   }

   private static Predicate<WaypointsSupport.Waypoint> matcher(CommandContext<CommandManager> var0) {
      String var1 = StringArgumentType.getString(var0, "name");
      String var2 = StringArgumentType.getString(var0, "server");
      return var2x -> var2.equalsIgnoreCase(var2x.server) && var1.equalsIgnoreCase(var2x.name);
   }

   private static int add(String var0, Vec3d var1, String var2) {
      WaypointsSupport.Waypoint var3 = new WaypointsSupport.Waypoint(var0, var1, var2, server());
      register(var3);
      message(Text.literal("Waypoint ").append(var3.text()).append(" has been created"));
      return -1;
   }

   private static final class DimensionArgument implements ArgumentType<String> {
      private static final List<String> NAMES = List.of("overworld", "the_nether", "the_end");

      public String parse(StringReader var1) throws CommandSyntaxException {
         String var2 = var1.readString().toLowerCase(Locale.ROOT);

         return switch (var2) {
            case "overworld" -> "overworld";
            case "the_nether", "nether" -> "nether";
            case "the_end", "end" -> "end";
            default -> throw WaypointsSupport.BAD_DIMENSION.createWithContext(var1, var2);
         };
      }

      public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> var1, SuggestionsBuilder var2) {
         return WaypointsSupport.suggest(NAMES.stream(), var2);
      }
   }

   private static final class PositionArgument implements ArgumentType<Vec3d> {
      public Vec3d parse(StringReader var1) throws CommandSyntaxException {
         int var2 = var1.getCursor();
         CoordinateArgument var3 = CoordinateArgument.parse(var1);
         if (var1.canRead() && var1.peek() == ' ') {
            var1.skip();
            CoordinateArgument var4 = CoordinateArgument.parse(var1, false);
            if (var1.canRead() && var1.peek() == ' ') {
               var1.skip();
               CoordinateArgument var5 = CoordinateArgument.parse(var1);
               Vec3d var6 = Util.mc.player == null ? Vec3d.ZERO : Util.mc.player.getEntityPos();
               return new Vec3d(var3.toAbsoluteCoordinate(var6.x), var4.toAbsoluteCoordinate(var6.y), var5.toAbsoluteCoordinate(var6.z));
            } else {
               var1.setCursor(var2);
               throw WaypointsSupport.INCOMPLETE.createWithContext(var1);
            }
         } else {
            var1.setCursor(var2);
            throw WaypointsSupport.INCOMPLETE.createWithContext(var1);
         }
      }

      public List<String> getExamples() {
         return List.of("0 0 0", "~ ~ ~", "0.1 -0.5 .9", "~0.5 ~1 ~-5");
      }
   }

   private static final class Store implements Jsonable {
      @Override
      public JsonElement toJson() {
         JsonObject var1 = new JsonObject();
         JsonArray var2 = new JsonArray();

         for (WaypointsSupport.Waypoint var4 : WaypointsSupport.WAYPOINTS) {
            JsonObject var5 = new JsonObject();
            var5.addProperty("x", var4.x);
            var5.addProperty("y", var4.y);
            var5.addProperty("z", var4.z);
            var5.addProperty("name", var4.name);
            var5.addProperty("dimension", var4.dimension);
            var5.addProperty("server", var4.server);
            var5.addProperty("toggled", var4.toggled);
            var2.add(var5);
         }

         var1.add("waypoints", var2);
         return var1;
      }

      @Override
      public void fromJson(JsonElement var1) {
         if (var1 != null && var1.isJsonObject() && var1.getAsJsonObject().has("waypoints")) {
            for (JsonElement var3 : var1.getAsJsonObject().getAsJsonArray("waypoints")) {
               try {
                  JsonObject var4 = var3.getAsJsonObject();
                  if (var4.has("name") && var4.has("dimension") && var4.has("server")) {
                     WaypointsSupport.Waypoint var5 = new WaypointsSupport.Waypoint(
                        var4.get("name").getAsString(),
                        var4.get("x").getAsDouble(),
                        var4.get("y").getAsDouble(),
                        var4.get("z").getAsDouble(),
                        var4.get("dimension").getAsString(),
                        var4.get("server").getAsString()
                     );
                     var5.toggled = !var4.has("toggled") || var4.get("toggled").getAsBoolean();
                     WaypointsSupport.WAYPOINTS.add(var5);
                  }
               } catch (Exception var6) {
                  ChimeraClient.LOGGER.error("Failed to load waypoint", var6);
               }
            }
         }
      }

      @Override
      public String getFileName() {
         return "waypoints.json";
      }
   }

   public static final class Waypoint {
      public final double x;
      public final double y;
      public final double z;
      public final String name;
      public final String dimension;
      public final String server;
      public boolean toggled;
      private Vec3d pos;

      public Waypoint(String var1, double var2, double var4, double var6, String var8, String var9) {
         this.x = floor1(var2);
         this.y = floor1(var4);
         this.z = floor1(var6);
         this.name = var1;
         this.dimension = var8;
         this.server = var9;
         this.toggled = true;
         this.pos = new Vec3d(var2, var4, var6);
      }

      public Waypoint(String var1, Vec3d var2, String var3, String var4) {
         this(var1, var2.x, var2.y, var2.z, var3, var4);
      }

      public Vec3d pos() {
         if (this.pos == null) {
            this.pos = new Vec3d(this.x, this.y, this.z);
         }

         return this.pos;
      }

      public boolean valid() {
         return this.name != null && this.dimension != null && this.server != null;
      }

      public Text text() {
         MutableText var1 = Text.literal(
               String.format("%s's info:\nx: %.1f\ny: %.1f\nz: %.1f\ndimension: %s\nserver: %s", this.name, this.x, this.y, this.z, this.dimension, this.server)
            )
            .formatted(Formatting.GRAY);
         return Text.literal(this.name).styled(var1x -> var1x.withHoverEvent(new ShowText(var1)));
      }

      private static double floor1(double var0) {
         return BigDecimal.valueOf(var0).setScale(1, RoundingMode.FLOOR).doubleValue();
      }
   }

   public static final class WaypointsCommand extends Command {
      public WaypointsCommand() {
         super("waypoints", "wp");
         this.setDescription("Manages Waypoints module markers: add, remove, rename, list, toggle");
      }

      @Override
      public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> var1) {
         var1.then(
            literal("add")
               .then(
                  ((RequiredArgumentBuilder)argument("name", StringArgumentType.string())
                        .executes(
                           var0 -> WaypointsSupport.add(StringArgumentType.getString(var0, "name"), mc.player.getEntityPos(), WaypointsSupport.dimension())
                        ))
                     .then(
                        ((RequiredArgumentBuilder)argument("pos", new WaypointsSupport.PositionArgument())
                              .executes(
                                 var0 -> WaypointsSupport.add(
                                    StringArgumentType.getString(var0, "name"), (Vec3d)var0.getArgument("pos", Vec3d.class), WaypointsSupport.dimension()
                                 )
                              ))
                           .then(
                              argument("dimension", new WaypointsSupport.DimensionArgument())
                                 .executes(
                                    var0 -> WaypointsSupport.add(
                                       StringArgumentType.getString(var0, "name"),
                                       (Vec3d)var0.getArgument("pos", Vec3d.class),
                                       (String)var0.getArgument("dimension", String.class)
                                    )
                                 )
                           )
                     )
               )
         );

         for (String var5 : new String[]{"remove", "delete", "del"}) {
            var1.then(
               literal(var5)
                  .then(
                     argument("server", StringArgumentType.string())
                        .suggests(WaypointsSupport::suggestServers)
                        .then(argument("name", StringArgumentType.string()).suggests(WaypointsSupport::suggestNames).executes(var0 -> {
                           String var1x = StringArgumentType.getString(var0, "name");
                           WaypointsSupport.WAYPOINTS.removeIf(WaypointsSupport.matcher(var0));
                           WaypointsSupport.message(Text.literal("Waypoint ").append(var1x).append(" has been removed"));
                           return -1;
                        }))
                  )
            );
         }

         var1.then(
            literal("rename")
               .then(
                  argument("server", StringArgumentType.string())
                     .suggests(WaypointsSupport::suggestServers)
                     .then(
                        argument("name", StringArgumentType.string())
                           .suggests(WaypointsSupport::suggestNames)
                           .then(argument("target", StringArgumentType.string()).executes(var0 -> {
                              String var1x = StringArgumentType.getString(var0, "name");
                              String var2 = StringArgumentType.getString(var0, "target");
                              Predicate<WaypointsSupport.Waypoint> var3 = WaypointsSupport.matcher(var0);
                              WaypointsSupport.Waypoint var4 = WaypointsSupport.WAYPOINTS.stream().filter(var3).findFirst().orElse(null);
                              if (var4 == null) {
                                 WaypointsSupport.message(Text.of("Waypoint not found"));
                                 return -1;
                              } else {
                                 WaypointsSupport.WAYPOINTS.removeIf(var3);
                                 WaypointsSupport.Waypoint var5x = new WaypointsSupport.Waypoint(var2, var4.pos(), var4.dimension, var4.server);
                                 var5x.toggled = var4.toggled;
                                 WaypointsSupport.WAYPOINTS.add(var5x);
                                 WaypointsSupport.message(Text.literal("Waypoint ").append(var1x).append(" has been renamed to ").append(var2));
                                 return -1;
                              }
                           }))
                     )
               )
         );
         var1.then(literal("list").executes(var0 -> {
            MutableText var1x = Text.empty().append("Waypoints list: ");
            ArrayList var2 = new ArrayList();

            for (WaypointsSupport.Waypoint var4 : WaypointsSupport.WAYPOINTS) {
               var2.add(var4.text());
            }

            var1x.append(Texts.join(var2, Text.literal(", ")));
            WaypointsSupport.message(var1x);
            return -1;
         }));
         var1.then(
            literal("toggle")
               .then(
                  argument("server", StringArgumentType.string())
                     .suggests(WaypointsSupport::suggestServers)
                     .then(argument("name", StringArgumentType.string()).suggests(WaypointsSupport::suggestNames).executes(var0 -> {
                        String var1x = StringArgumentType.getString(var0, "name");
                        WaypointsSupport.Waypoint var2 = WaypointsSupport.WAYPOINTS.stream().filter(WaypointsSupport.matcher(var0)).findFirst().orElse(null);
                        if (var2 == null) {
                           WaypointsSupport.message(Text.of("Waypoint not found"));
                           return -1;
                        } else {
                           var2.toggled = !var2.toggled;
                           WaypointsSupport.message(Text.literal("Made the waypoint %s %s.".formatted(var1x, var2.toggled ? "visible" : "invisible")));
                           return -1;
                        }
                     }))
               )
         );
      }
   }
}
