package me.alpha432.chimeraclient.mio.support;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Map.Entry;
import me.alpha432.chimeraclient.features.gui.font.FontDraw;
import me.alpha432.chimeraclient.mio.MioConfiguredModule;
import me.alpha432.chimeraclient.util.traits.Util;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BeehiveBlockEntity.BeeData;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BeesComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class TooltipsSupport implements Util {
   public static ItemStack hovered = ItemStack.EMPTY;
   private static final ThreadLocal<Boolean> NESTED = ThreadLocal.withInitial(() -> false);

   public static List<TooltipComponent> components(List<TooltipComponent> var0) {
      MioConfiguredModule var1 = MioConfiguredModule.active("Tooltips");
      if (var1 != null && !hovered.isEmpty() && !NESTED.get()) {
         ArrayList var2 = new ArrayList(var0);
         ContainerComponent var3 = (ContainerComponent)hovered.get(DataComponentTypes.CONTAINER);
         if (var3 != null && var1.b("shulkerContent")) {
             List<ItemStack> var4 = var3.stream().map(ItemStack::copy).toList();
            if (var4.stream().anyMatch(var0x -> !var0x.isEmpty())) {
               if (!var1.b("keepText")) {
                  var2.clear();
                  var2.add(TooltipComponent.of(hovered.getName().asOrderedText()));
               }

               var2.add(new TooltipsSupport.Contents(var4, color(hovered, var1)));
            }
         }

         MapIdComponent var9 = (MapIdComponent)hovered.get(DataComponentTypes.MAP_ID);
         if (var9 != null && mc.world != null) {
            MapState var5 = mc.world.getMapState(var9);
            if (var5 != null) {
               if (!var1.b("keepText")) {
                  var2.clear();
                  var2.add(TooltipComponent.of(hovered.getName().asOrderedText()));
               }

               MapRenderState var6 = new MapRenderState();
               mc.getMapRenderer().update(var9, var5, var6);
               var2.add(new TooltipsSupport.MapView(var6));
            }
         }

         BeesComponent var10 = (BeesComponent)hovered.get(DataComponentTypes.BEES);
         if (var10 != null && var1.b("beeNest")) {
            var2.add(TooltipComponent.of(Text.literal("Bees: " + var10.bees().size()).asOrderedText()));

            for (BeeData var7 : var10.bees()) {
               NbtCompound var8 = var7.entityData().copyNbtWithoutId();
               var8.getIntArray("FlowerPos")
                  .filter(var0x -> var0x.length == 3)
                  .ifPresent(
                     var1x -> var2.add(
                        TooltipComponent.of(Text.literal("Position " + new BlockPos(var1x[0], var1x[1], var1x[2]).toShortString()).asOrderedText())
                     )
                  );
            }
         }

         if (var9 != null && var1.b("mapOverlay")) {
            var2.add(TooltipComponent.of(Text.literal("Hold Left-Alt to hide item amount.").asOrderedText()));
         }

         if (var1.b("size") && mc.world != null) {
             Optional<net.minecraft.nbt.NbtElement> var12 = ItemStack.CODEC.encodeStart(mc.world.getRegistryManager().getOps(NbtOps.INSTANCE), hovered).result();
             var12.ifPresent(
                var1x -> {
                  try {
                     TooltipsSupport.Counter var2x = new TooltipsSupport.Counter();
                     var1x.write(new DataOutputStream(var2x));
                     long var3x = var2x.size;
                     String var5x = var3x >= 1048576L
                        ? String.format(Locale.ROOT, "%,d MB", var3x / 1048576L)
                        : (var3x >= 1024L ? String.format(Locale.ROOT, "%,d KB", var3x / 1024L) : String.format(Locale.ROOT, "%,d B", var3x));
                     var2.add(TooltipComponent.of(Text.literal(var5x).asOrderedText()));
                  } catch (IOException var6x) {
                     throw new UncheckedIOException(var6x);
                  }
               }
            );
         }

         return var2;
      } else {
         return var0;
      }
   }

   private static int color(ItemStack var0, MioConfiguredModule var1) {
      return var1.b("colored") && var0.getItem() instanceof BlockItem var2 && var2.getBlock() instanceof ShulkerBoxBlock var3 && var3.getColor() != null
         ? -1442840576 | var3.getColor().getMapColor().color & 16777215
         : -1441787880;
   }

   public static boolean openPreview() {
      return openPreview(false);
   }

   public static boolean openPreview(boolean var0) {
      MioConfiguredModule var1 = MioConfiguredModule.active("Tooltips");
      if (var1 != null && var1.b("shulkerContent") && !hovered.isEmpty()) {
         ContainerComponent var2 = (ContainerComponent)hovered.get(DataComponentTypes.CONTAINER);
         if (var2 == null) {
            return false;
         } else {
            mc.setScreen(
               new TooltipsSupport.Preview(
                  mc.currentScreen, hovered.getName(), var2.stream().<ItemStack>map(ItemStack::copy).toList(), color(hovered, var1), var0
               )
            );
            return true;
         }
      } else {
         return false;
      }
   }

   public static void itemOverlay(DrawContext var0, ItemStack var1, int var2, int var3) {
      MioConfiguredModule var4 = MioConfiguredModule.active("Tooltips");
      if (var4 != null) {
         ContainerComponent var5 = (ContainerComponent)var1.get(DataComponentTypes.CONTAINER);
         if (var4.b("majorityItem") && var5 != null) {
             LinkedHashMap<Item, Integer> var6 = new LinkedHashMap<>();
            var5.stream().filter(var0x -> !var0x.isEmpty()).forEach(var1x -> var6.merge(var1x.getItem(), 1, Integer::sum));
            Optional var7 = var6.entrySet().stream().max(Entry.comparingByValue());
            if (var7.isPresent()) {
               var0.getMatrices().pushMatrix();
               var0.getMatrices().translate(var2 + 6, var3 - 4);
               var0.getMatrices().scale(0.625F, 0.625F);
               var0.drawItem(((Item)((Entry)var7.get()).getKey()).getDefaultStack(), 0, 0);
               var0.getMatrices().popMatrix();
            }
         }

         MapIdComponent var10 = (MapIdComponent)var1.get(DataComponentTypes.MAP_ID);
         if (var10 != null && var4.b("mapOverlay") && !mc.isAltPressed() && mc.player != null) {
            int var11 = 0;

            for (int var8 = 0; var8 < mc.player.getInventory().size(); var8++) {
               ItemStack var9 = mc.player.getInventory().getStack(var8);
               if (var10.equals(var9.get(DataComponentTypes.MAP_ID))) {
                  var11 += var9.getCount();
               }
            }

            if (var11 > 1) {
               FontDraw.drawTextWithShadow(var0, mc.textRenderer, var11 + "", var2 + 17 - FontDraw.width(mc.textRenderer, var11 + ""), var3 + 9, -1);
            }
         }
      }
   }

   private record Contents(List<ItemStack> stacks, int color) implements TooltipComponent {
      public int getHeight(TextRenderer textRenderer) {
         return 56;
      }

      public int getWidth(TextRenderer textRenderer) {
         return 164;
      }

      public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
         context.fill(x, y, x + 164, y + 56, this.color);

         for (int var7 = 0; var7 < Math.min(this.stacks.size(), 27); var7++) {
            int var8 = x + 1 + var7 % 9 * 18;
            int var9 = y + 1 + var7 / 9 * 18;
            context.drawItem(this.stacks.get(var7), var8, var9);
            context.drawStackOverlay(textRenderer, this.stacks.get(var7), var8, var9);
         }
      }
   }

   private static final class Counter extends OutputStream {
      long size;

      @Override
      public void write(int var1) {
         this.size++;
      }

      @Override
      public void write(byte[] var1, int var2, int var3) {
         this.size += var3;
      }
   }

   private record MapView(MapRenderState state) implements TooltipComponent {
      public int getHeight(TextRenderer textRenderer) {
         return 130;
      }

      public int getWidth(TextRenderer textRenderer) {
         return 128;
      }

      public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
         context.getMatrices().pushMatrix();
         context.getMatrices().translate(x, y);
         context.drawMap(this.state);
         context.getMatrices().popMatrix();
      }
   }

   private static final class Preview extends Screen {
      private final Screen parent;
      private final TooltipsSupport.Contents contents;
      private final boolean holdAlt;

      Preview(Screen var1, Text var2, List<ItemStack> var3, int var4, boolean var5) {
         super(var2);
         this.parent = var1;
         this.contents = new TooltipsSupport.Contents(var3, var4);
         this.holdAlt = var5;
      }

      public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
         if (this.holdAlt && !Util.mc.isAltPressed()) {
            this.close();
         } else {
            int var5 = (this.width - 164) / 2;
            int var6 = (this.height - 56) / 2;
            context.fill(0, 0, this.width, this.height, 1996488704);
            FontDraw.drawTextWithShadow(context, Util.mc.textRenderer, this.title, var5, var6 - 13, -1);
            this.contents.drawItems(Util.mc.textRenderer, var5, var6, 164, 56, context);
            int var7 = (mouseX - var5 - 1) / 18 + (mouseY - var6 - 1) / 18 * 9;
            if (mouseX >= var5 + 1 && mouseX < var5 + 163 && mouseY >= var6 + 1 && mouseY < var6 + 55 && var7 >= 0 && var7 < this.contents.stacks.size()) {
               TooltipsSupport.NESTED.set(true);

               try {
                  context.drawItemTooltip(Util.mc.textRenderer, this.contents.stacks.get(var7), mouseX, mouseY);
               } finally {
                  TooltipsSupport.NESTED.set(false);
               }
            }
         }
      }

      public void close() {
         Util.mc.setScreen(this.parent);
      }

      public boolean shouldPause() {
         return false;
      }
   }
}
