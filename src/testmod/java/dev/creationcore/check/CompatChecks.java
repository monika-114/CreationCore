package dev.creationcore.check;

import com.mojang.authlib.GameProfile;
import com.tom.storagemod.block.entity.CraftingTerminalBlockEntity;
import com.tom.storagemod.menu.CraftingTerminalMenu;
import dev.creationcore.compat.RefreshBeforeCrafting;
import dev.creationcore.compat.sophisticated.CreativeCraftingUpgradeItem;
import dev.creationcore.compat.toms.TomsNetworkGate;
import dev.creationcore.registry.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.*;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

public final class CompatChecks {
  private static final List<String> results = new ArrayList<>();
  private static ServerLevel level;
  private static net.minecraft.server.level.ServerPlayer player;
  private static int ticks;
  private static boolean done;
  private static final BlockPos POS = new BlockPos(220, 200, 220);

  private static void check(boolean condition, String label) {
    if (!condition) throw new AssertionError(label);
    results.add("PASS: " + label);
    System.out.println("COMPAT_CHECK " + results.getLast());
  }

  private static Item item(String id) {
    return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
  }

  private static void place(BlockPos pos, String id) {
    var state = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id)).defaultBlockState();
    if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
      state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
    level.setBlock(pos, state, 3);
  }

  private static ItemStack[] stoneGrid() {
    ItemStack[] inputs = new ItemStack[9];
    for (int i = 0; i < 9; i++)
      inputs[i] = new ItemStack(i == 4 ? Items.SILVERFISH_SPAWN_EGG : Items.STONE);
    return inputs;
  }

  private static CraftingInput input(ItemStack... items) {
    return CraftingInput.of(3, 3, Arrays.asList(items));
  }

  private static void upgradeGrid(CraftingUpgradeContainer menu, ItemStack[] inputs) {
    for (int i = 0; i < 9; i++)
      menu.getSlots().get(i).set(i < inputs.length ? inputs[i].copy() : ItemStack.EMPTY);
  }

  private static long count(CraftingUpgradeContainer menu, Item item) {
    return menu.getSlots().subList(0, 9).stream()
        .map(Slot::getItem)
        .filter(s -> s.is(item))
        .mapToLong(ItemStack::getCount)
        .sum();
  }

  private static void testUpgrade(
      IStorageWrapper storage,
      String id,
      String ordinary,
      String label,
      net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase<?> inventoryMenu) {
    var special = (CreativeCraftingUpgradeItem) item(id);
    check(
        special.canAddUpgradeTo(storage, new ItemStack(special), false, false).successful(),
        label + " permits its own upgrade");
    var upgradeSlot = inventoryMenu.upgradeSlots.getFirst();
    var other =
        item(
            "creationcore:"
                + (label.equals("backpack") ? "storage" : "backpack")
                + "_creative_crafting_upgrade");
    check(
        storage.getUpgradeHandler().isItemValid(0, new ItemStack(special))
            && upgradeSlot.mayPlace(new ItemStack(special)),
        label + " native item handler and GUI slot both accept its own upgrade");
    check(
        !upgradeSlot.mayPlace(new ItemStack(other)),
        label + " native GUI slot rejects the other mod's upgrade");
    player.containerMenu = inventoryMenu;
    inventoryMenu.setCarried(new ItemStack(special));
    inventoryMenu.clicked(upgradeSlot.index, 0, ClickType.PICKUP, player);
    check(
        inventoryMenu.getCarried().isEmpty() && upgradeSlot.getItem().is(special),
        label + " real player pickup click installs upgrade into native menu");
    var wrapper = (CraftingUpgradeWrapper) storage.getUpgradeHandler().getSlotWrappers().get(0);
    var menu =
        (CraftingUpgradeContainer)
            (Object)
                UpgradeContainerRegistry.instantiateContainer(player, 0, wrapper).orElseThrow();
    menu.onInit();
    upgradeGrid(menu, stoneGrid());
    var result = menu.getSlots().get(9);
    check(
        result.getItem().is(Items.INFESTED_STONE) && result.getItem().getCount() == 8,
        label + " previews exclusive recipe through native upgrade container");
    var taken = result.remove(8);
    result.onTake(player, taken);
    check(
        taken.getCount() == 8
            && count(menu, Items.STONE) == 0
            && count(menu, Items.SILVERFISH_SPAWN_EGG) == 0,
        label + " takes eight outputs and consumes exactly one batch");
    ItemStack[] bucket = new ItemStack[9];
    Arrays.fill(bucket, ItemStack.EMPTY);
    bucket[8] = new ItemStack(Items.TADPOLE_BUCKET);
    upgradeGrid(menu, bucket);
    taken = result.remove(1);
    result.onTake(player, taken);
    check(
        taken.is(Items.TADPOLE_SPAWN_EGG) && count(menu, Items.BUCKET) == 1,
        label + " returns bucket from offset shapeless recipe");
    upgradeGrid(menu, new ItemStack[] {new ItemStack(Items.OAK_LOG)});
    check(result.getItem().is(Items.OAK_PLANKS), label + " keeps ordinary crafting recipes");
    var persisted =
        ItemStack.CODEC
            .encodeStart(
                level
                    .registryAccess()
                    .createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),
                wrapper.getUpgradeStack())
            .getOrThrow();
    var restored =
        ItemStack.CODEC
            .parse(
                level
                    .registryAccess()
                    .createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),
                persisted)
            .getOrThrow();
    check(restored.is(special), label + " serializes its distinct upgrade identity");
    storage.getUpgradeHandler().setStackInSlot(0, new ItemStack(item(ordinary)));
    var normalWrapper =
        (CraftingUpgradeWrapper) storage.getUpgradeHandler().getSlotWrappers().get(0);
    var normal =
        (CraftingUpgradeContainer)
            (Object)
                UpgradeContainerRegistry.instantiateContainer(player, 0, normalWrapper)
                    .orElseThrow();
    normal.onInit();
    upgradeGrid(normal, stoneGrid());
    check(
        normal.getSlots().get(9).getItem().isEmpty(),
        label + " ordinary upgrade cannot use exclusive recipes");
  }

  private static void recipes() {
    var dragon =
        level
            .getRecipeManager()
            .byKey(ResourceLocation.parse("creationcore:ender_dragon_spawn_egg"))
            .orElseThrow()
            .value();
    check(
        dragon.getIngredients().get(3).test(new ItemStack(Items.ELYTRA))
            && dragon.getIngredients().get(5).test(new ItemStack(Items.DRAGON_HEAD)),
        "dragon egg recipe displays elytra at middle-left and dragon head at middle-right");
    var turtle =
        level
            .getRecipeManager()
            .getRecipeFor(
                ModRecipes.CREATIVE_CRAFTING_TYPE.get(),
                CraftingInput.of(1, 1, List.of(new ItemStack(Items.TURTLE_EGG))),
                level)
            .orElseThrow();
    check(
        turtle.value().getResultItem(level.registryAccess()).is(Items.TURTLE_SPAWN_EGG),
        "turtle egg now crafts one turtle spawn egg exclusively");
    check(
        level.getRecipeManager().getAllRecipesFor(RecipeType.STONECUTTING).stream()
            .noneMatch(
                h -> h.value().getResultItem(level.registryAccess()).is(Items.TURTLE_SPAWN_EGG)),
        "old biome-to-turtle stonecutting mapping is removed");
    for (String name : List.of("barrier", "light_level_1")) {
      var recipe =
          level
              .getRecipeManager()
              .byKey(ResourceLocation.parse("creationcore:" + name))
              .orElseThrow()
              .value();
      check(
          recipe.getResultItem(level.registryAccess()).getCount() == 8
              && recipe.getIngredients().get(4).test(new ItemStack(ModItems.BLANK_MATTER.get())),
          name + " uses blank matter and produces eight items");
    }
    var bottle =
        level
            .getRecipeManager()
            .byKey(ResourceLocation.parse("creationcore:void_bottling"))
            .orElseThrow()
            .value();
    check(
        bottle.getResultItem(level.registryAccess()).getCount() == 4,
        "void bottling outputs four bottles");
    for (String name :
        List.of(
            "compat/backpack_creative_crafting_upgrade",
            "compat/storage_creative_crafting_upgrade")) {
      check(
          level
                  .getRecipeManager()
                  .byKey(ResourceLocation.parse("creationcore:" + name))
                  .orElseThrow()
                  .value()
                  .getType()
              == ModRecipes.CREATIVE_CRAFTING_TYPE.get(),
          "optional exclusive recipe loads: " + name);
    }
  }

  public static void started(net.minecraft.server.MinecraftServer server) {
    level = server.overworld();
    level.getChunk(POS);
    level.removeBlock(POS.south().east(), false);
    level.removeBlock(POS.offset(20, 0, 0), false);
    var profile = new GameProfile(UUID.randomUUID(), "CompatChecks");
    player =
        new net.minecraft.server.level.ServerPlayer(
            server, level, profile, net.minecraft.server.level.ClientInformation.createDefault());
    player.connection =
        new net.minecraft.server.network.ServerGamePacketListenerImpl(
            server,
            new net.minecraft.network.Connection(
                net.minecraft.network.protocol.PacketFlow.SERVERBOUND),
            player,
            net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false)) {
          @Override
          public void send(net.minecraft.network.protocol.Packet<?> packet) {}
        };
    player.setPos(220.5, 200, 218.5);
    place(POS, "toms_storage:crafting_terminal");
    level.setBlock(
        POS,
        level
            .getBlockState(POS)
            .setValue(
                com.tom.storagemod.block.AbstractStorageTerminalBlock.FACING, Direction.SOUTH),
        3);
    place(POS.south(), "toms_storage:inventory_connector");
    level.setBlock(POS.south(2), Blocks.CHEST.defaultBlockState(), 3);
  }

  public static void tick(net.minecraft.server.level.ServerLevel world) {
    if (done || level == null || world != level || ++ticks < 50) return;
    done = true;
    try {
      recipes();
      var backpackStack = new ItemStack(item("sophisticatedbackpacks:backpack"));
      player.getInventory().setItem(0, backpackStack);
      var backpack = BackpackWrapper.fromStack(backpackStack);
      if (backpack instanceof BackpackWrapper real) {
        real.setContentsUuid(UUID.randomUUID());
        real.setSlotNumbers(27, 3);
        real.onInit();
      }
      place(POS.offset(10, 0, 0), "sophisticatedstorage:barrel");
      var chest =
          ((StorageBlockEntity) level.getBlockEntity(POS.offset(10, 0, 0))).getStorageWrapper();
      check(
          !((CreativeCraftingUpgradeItem) item("creationcore:backpack_creative_crafting_upgrade"))
              .canAddUpgradeTo(
                  chest,
                  new ItemStack(item("creationcore:backpack_creative_crafting_upgrade")),
                  false,
                  false)
              .successful(),
          "backpack upgrade rejects Sophisticated Storage");
      check(
          !((CreativeCraftingUpgradeItem) item("creationcore:storage_creative_crafting_upgrade"))
              .canAddUpgradeTo(
                  backpack,
                  new ItemStack(item("creationcore:storage_creative_crafting_upgrade")),
                  false,
                  false)
              .successful(),
          "storage upgrade rejects Sophisticated Backpacks");
      testUpgrade(
          backpack,
          "creationcore:backpack_creative_crafting_upgrade",
          "sophisticatedbackpacks:crafting_upgrade",
          "backpack",
          new net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer(
              9,
              player,
              new net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext.Item("main", 0)));
      testUpgrade(
          chest,
          "creationcore:storage_creative_crafting_upgrade",
          "sophisticatedstorage:crafting_upgrade",
          "storage",
          new net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu(
              10, player, POS.offset(10, 0, 0)));
      CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), true, level.registryAccess());
      for (String target : List.of("backpack", "storage")) {
        var tab =
            target.equals("backpack")
                ? net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.CREATIVE_TAB.get()
                : net.p3pp3rf1y.sophisticatedstorage.init.ModItems.CREATIVE_TAB.get();
        var own = item("creationcore:" + target + "_creative_crafting_upgrade");
        var other =
            item(
                "creationcore:"
                    + (target.equals("backpack") ? "storage" : "backpack")
                    + "_creative_crafting_upgrade");
        check(
            tab.getDisplayItems().stream().anyMatch(s -> s.is(own))
                && tab.getDisplayItems().stream().noneMatch(s -> s.is(other)),
            target + " native creative tab shows only its corresponding creative upgrade");
      }
      player.getInventory().clearContent();
      var terminal = (CraftingTerminalBlockEntity) level.getBlockEntity(POS);
      var menu = new CraftingTerminalMenu(8, player.getInventory(), terminal);
      player.containerMenu = menu;
      var grid = stoneGrid();
      for (int i = 0; i < 9; i++) terminal.getCraftingInv().setItem(i, grid[i]);
      check(
          terminal.getCraftResult().getItem(0).isEmpty(),
          "Tom terminal does not craft exclusive recipes without attached table");
      level.setBlock(
          POS.south().east(),
          (Boolean.getBoolean("creationcore.checkCrafter")
                  ? ModBlocks.CREATIVE_CRAFTER.get()
                  : ModBlocks.CREATIVE_CRAFTING_TABLE.get())
              .defaultBlockState(),
          3);
      check(
          TomsNetworkGate.authorized(level, POS),
          "Tom connector recognizes table attached to its actual network");
      ((RefreshBeforeCrafting) (Object) terminal).creationcore$refreshBeforeCrafting();
      check(
          terminal.getCraftResult().getItem(0).getCount() == 8,
          "same Tom terminal refreshes existing grid when table joins");
      menu.clicked(0, 0, ClickType.PICKUP, player);
      check(
          menu.getCarried().is(Items.INFESTED_STONE)
              && menu.getCarried().getCount() == 8
              && terminal.getCraftingInv().isEmpty(),
          "real Tom terminal click returns eight outputs and consumes exact inputs");
      menu.setCarried(ItemStack.EMPTY);
      for (int i = 0; i < 9; i++) terminal.getCraftingInv().setItem(i, stoneGrid()[i]);
      level.removeBlock(POS.south().east(), false);
      menu.clicked(0, 0, ClickType.PICKUP, player);
      check(
          menu.getCarried().isEmpty() && terminal.getCraftingInv().getItem(0).getCount() == 1,
          "Tom stale output click is rejected immediately after table removal without consuming"
              + " inputs");
      level.setBlock(
          POS.offset(20, 0, 0),
          (Boolean.getBoolean("creationcore.checkCrafter")
                  ? ModBlocks.CREATIVE_CRAFTER.get()
                  : ModBlocks.CREATIVE_CRAFTING_TABLE.get())
              .defaultBlockState(),
          3);
      check(
          !TomsNetworkGate.authorized(level, POS),
          "unconnected creative table cannot authorize Tom network");

      results.add("ALL COMPAT CHECKS PASSED");
    } catch (Throwable failure) {
      failure.printStackTrace();
      results.add("FAIL: " + failure);
    }
    try {
      Files.write(
          Path.of(
              "../build/compat-checks"
                  + (Boolean.getBoolean("creationcore.checkCrafter") ? "-crafter" : "-table")
                  + ".txt"),
          results);
    } catch (Exception failure) {
      throw new RuntimeException(failure);
    }
    level.getServer().halt(false);
  }
}
