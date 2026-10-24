package dev.creationcore.check;

import com.mojang.authlib.GameProfile;
import com.tom.storagemod.block.entity.CraftingTerminalBlockEntity;
import com.tom.storagemod.menu.CraftingTerminalMenu;
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.UnorderedStackHandlerRemoveZero;
import dev.creationcore.compat.RefreshBeforeCrafting;
import dev.creationcore.compat.sophisticated.CreativeCraftingUpgradeItem;
import dev.creationcore.compat.toms.TomsNetworkGate;
import dev.creationcore.menu.CreativeCraftingMenu;
import dev.creationcore.recipe.RestrictedCraftingRecipe;
import dev.creationcore.registry.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.*;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

@EventBusSubscriber(modid = "creationcore")
public final class CompatChecks {
    private static final List<String> results = new ArrayList<>();
    private static ServerLevel level;
    private static FakePlayer player;
    private static int ticks;
    private static boolean done;
    private static final BlockPos POS = new BlockPos(220, 200, 220);
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        results.add("PASS: " + label);
        System.out.println("COMPAT_CHECK " + results.getLast());
    }
    private static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }
    private static void place(BlockPos pos, String id) {
        var state = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id)).defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        level.setBlock(pos, state, 3);
    }
    private static ItemStack[] stoneGrid() {
        ItemStack[] inputs = new ItemStack[9];
        for (int i = 0; i < 9; i++) inputs[i] = new ItemStack(i == 4 ? Items.SILVERFISH_SPAWN_EGG : Items.STONE);
        return inputs;
    }
    private static CraftingInput input(ItemStack... items) { return CraftingInput.of(3, 3, Arrays.asList(items)); }
    private static void upgradeGrid(CraftingUpgradeContainer menu, ItemStack[] inputs) {
        for (int i = 0; i < 9; i++) menu.getSlots().get(i).set(i < inputs.length ? inputs[i].copy() : ItemStack.EMPTY);
    }
    private static long count(CraftingUpgradeContainer menu, Item item) {
        return menu.getSlots().subList(0, 9).stream().map(Slot::getItem).filter(s -> s.is(item)).mapToLong(ItemStack::getCount).sum();
    }
    private static void testUpgrade(IStorageWrapper storage, String id, String ordinary, String label,
            net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase<?> inventoryMenu) {
        var special = (CreativeCraftingUpgradeItem)item(id);
        check(special.canAddUpgradeTo(storage, new ItemStack(special), false, false).successful(), label + " permits its own upgrade");
        var upgradeSlot = inventoryMenu.upgradeSlots.getFirst();
        var other = item("creationcore:" + (label.equals("backpack") ? "storage" : "backpack") + "_creative_crafting_upgrade");
        check(storage.getUpgradeHandler().isItemValid(0, new ItemStack(special)) && upgradeSlot.mayPlace(new ItemStack(special)),
                label + " native item handler and GUI slot both accept its own upgrade");
        check(!upgradeSlot.mayPlace(new ItemStack(other)), label + " native GUI slot rejects the other mod's upgrade");
        player.containerMenu = inventoryMenu;
        inventoryMenu.setCarried(new ItemStack(special));
        inventoryMenu.clicked(upgradeSlot.index, 0, ClickType.PICKUP, player);
        check(inventoryMenu.getCarried().isEmpty() && upgradeSlot.getItem().is(special),
                label + " real player pickup click installs upgrade into native menu");
        var wrapper = (CraftingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(0);
        var menu = (CraftingUpgradeContainer)(Object)UpgradeContainerRegistry.instantiateContainer(player, 0, wrapper).orElseThrow();
        menu.onInit();
        upgradeGrid(menu, stoneGrid());
        var result = menu.getSlots().get(9);
        check(result.getItem().is(Items.INFESTED_STONE) && result.getItem().getCount() == 8, label + " previews exclusive recipe through native upgrade container");
        var taken = result.remove(8);
        result.onTake(player, taken);
        check(taken.getCount() == 8 && count(menu, Items.STONE) == 0 && count(menu, Items.SILVERFISH_SPAWN_EGG) == 0,
                label + " takes eight outputs and consumes exactly one batch");
        ItemStack[] bucket = new ItemStack[9]; Arrays.fill(bucket, ItemStack.EMPTY); bucket[8] = new ItemStack(Items.TADPOLE_BUCKET);
        upgradeGrid(menu, bucket);
        taken = result.remove(1); result.onTake(player, taken);
        check(taken.is(Items.TADPOLE_SPAWN_EGG) && count(menu, Items.BUCKET) == 1, label + " returns bucket from offset shapeless recipe");
        upgradeGrid(menu, new ItemStack[]{new ItemStack(Items.OAK_LOG)});
        check(result.getItem().is(Items.OAK_PLANKS), label + " keeps ordinary crafting recipes");
        var persisted = ItemStack.CODEC.encodeStart(level.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), wrapper.getUpgradeStack()).getOrThrow();
        var restored = ItemStack.CODEC.parse(level.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), persisted).getOrThrow();
        check(restored.is(special), label + " serializes its distinct upgrade identity");
        storage.getUpgradeHandler().setStackInSlot(0, new ItemStack(item(ordinary)));
        var normalWrapper = (CraftingUpgradeWrapper)storage.getUpgradeHandler().getSlotWrappers().get(0);
        var normal = (CraftingUpgradeContainer)(Object)UpgradeContainerRegistry.instantiateContainer(player, 0, normalWrapper).orElseThrow();
        normal.onInit(); upgradeGrid(normal, stoneGrid());
        check(normal.getSlots().get(9).getItem().isEmpty(), label + " ordinary upgrade cannot use exclusive recipes");
    }
    private static void recipes() {
        var dragon = level.getRecipeManager().byKey(ResourceLocation.parse("creationcore:ender_dragon_spawn_egg")).orElseThrow().value();
        check(dragon.getIngredients().get(3).test(new ItemStack(Items.ELYTRA))
                        && dragon.getIngredients().get(5).test(new ItemStack(Items.DRAGON_HEAD)),
                "dragon egg recipe displays elytra at middle-left and dragon head at middle-right");
        var turtle = level.getRecipeManager().getRecipeFor(ModRecipes.CREATIVE_CRAFTING_TYPE.get(),
                CraftingInput.of(1, 1, List.of(new ItemStack(Items.TURTLE_EGG))), level).orElseThrow();
        check(turtle.value().getResultItem(level.registryAccess()).is(Items.TURTLE_SPAWN_EGG), "turtle egg now crafts one turtle spawn egg exclusively");
        check(level.getRecipeManager().getAllRecipesFor(RecipeType.STONECUTTING).stream()
                        .noneMatch(h -> h.value().getResultItem(level.registryAccess()).is(Items.TURTLE_SPAWN_EGG)),
                "old biome-to-turtle stonecutting mapping is removed");
        for (String name : List.of("barrier", "light_level_1")) {
            var recipe = level.getRecipeManager().byKey(ResourceLocation.parse("creationcore:" + name)).orElseThrow().value();
            check(recipe.getResultItem(level.registryAccess()).getCount() == 8
                            && recipe.getIngredients().get(4).test(new ItemStack(ModItems.BLANK_MATTER.get())),
                    name + " uses blank matter and produces eight items");
        }
        var bottle = level.getRecipeManager().byKey(ResourceLocation.parse("creationcore:void_bottling")).orElseThrow().value();
        check(bottle.getResultItem(level.registryAccess()).getCount() == 4, "void bottling outputs four bottles");
        for (String name : List.of("compat/backpack_creative_crafting_upgrade", "compat/storage_creative_crafting_upgrade", "compat/ae2/creative_energy_cell", "compat/ae2/creative_item_cell")) {
            check(level.getRecipeManager().byKey(ResourceLocation.parse("creationcore:" + name)).orElseThrow().value().getType()
                            == ModRecipes.CREATIVE_CRAFTING_TYPE.get(), "optional exclusive recipe loads: " + name);
        }
        for (String name : List.of("handheld_worldshaper", "creative_motor", "creative_fluid_tank", "creative_blaze_cake")) {
            check(level.getRecipeManager().byKey(ResourceLocation.parse("creationcore:compat/create/" + name)).orElseThrow().value()
                            .getType().toString().equals("create:mechanical_crafting"), "mechanical crafting recipe loads: " + name);
        }
    }
    private static void beyond() throws Exception {
        var manager = level.getRecipeManager();
        var id = ResourceLocation.parse("beyonddimensions:net_creater");
        var original = manager.byKey(id).orElseThrow();
        check(original.value() instanceof RestrictedCraftingRecipe, "resolved Beyond network generator recipe is restricted to creative crafting");
        var snapshot = new ArrayList<RecipeHolder<?>>(manager.getRecipes());
        var replaced = new ArrayList<>(snapshot); replaced.removeIf(h -> h.id().equals(id));
        var packOutput = new ItemStack(item("beyonddimensions:net_creater"), 2);
        packOutput.set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 64);
        var packRecipe = new ShapelessRecipe("pack_override", CraftingBookCategory.MISC, packOutput,
                NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.DIAMOND)));
        replaced.add(new RecipeHolder<>(id, packRecipe)); manager.replaceRecipes(replaced);
        var rewritten = manager.byKey(id).orElseThrow();
        var restricted = (RestrictedCraftingRecipe)rewritten.value();
        var input = CraftingInput.of(1, 1, List.of(new ItemStack(Items.DIAMOND)));
        check(restricted.matches(input, level) && restricted.assemble(input, level.registryAccess()).getCount() == 2
                        && restricted.delegate() == packRecipe, "data-pack replacement ingredients, output count and delegate are preserved");
        check(manager.getRecipeFor(RecipeType.CRAFTING, input, level).isEmpty()
                        && manager.getRecipeFor(ModRecipes.CREATIVE_CRAFTING_TYPE.get(), input, level).isPresent(),
                "pack-modified generator cannot be made in an ordinary workbench");
        var encoded = Recipe.CODEC.encodeStart(level.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), restricted).getOrThrow();
        check(Recipe.CODEC.parse(level.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), encoded).getOrThrow()
                        instanceof RestrictedCraftingRecipe, "restricted pack recipe codec round-trip preserves exclusive type");
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), level.registryAccess());
        Recipe.STREAM_CODEC.encode(buffer, restricted);
        check(Recipe.STREAM_CODEC.decode(buffer) instanceof RestrictedCraftingRecipe, "restricted recipe synchronizes to client with nested recipe serializer");
        buffer.release(); manager.replaceRecipes(snapshot);
        var generator = manager.byKey(id).orElseThrow().value();
        var dispense = net.minecraft.world.level.block.CrafterBlock.class.getDeclaredMethod("dispenseFrom",
                net.minecraft.world.level.block.state.BlockState.class, ServerLevel.class, BlockPos.class);
        dispense.setAccessible(true);
        for (boolean creative : List.of(true, false)) {
            var pos = POS.offset(30, 0, creative ? 0 : 4);
            level.getChunk(pos);
            level.setBlock(pos, (creative ? ModBlocks.CREATIVE_CRAFTER.get() : Blocks.CRAFTER).defaultBlockState(), 3);
            level.setBlock(pos.north(), Blocks.CHEST.defaultBlockState(), 3);
            var crafter = (net.minecraft.world.level.block.entity.CrafterBlockEntity)level.getBlockEntity(pos);
            for (int i = 0; i < 9; i++) crafter.setItem(i, generator.getIngredients().get(i).getItems()[0].copyWithCount(1));
            dispense.invoke(level.getBlockState(pos).getBlock(), level.getBlockState(pos), level, pos);
            var output = (net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(pos.north());
            check(creative ? output.getItem(0).is(item("beyonddimensions:net_creater")) && crafter.isEmpty()
                            : output.isEmpty() && !crafter.isEmpty(),
                    creative ? "native creative crafter produces restricted Beyond generator and consumes exact inputs"
                            : "ordinary crafter cannot bypass Beyond generator restriction");
        }
        var special = DimensionsCraftMenu.getRecipe(player, CompatChecks.input(stoneGrid()), level).orElseThrow();
        check(special.value().assemble(CompatChecks.input(stoneGrid()), level.registryAccess()).getCount() == 8,
                "Beyond network crafting lookup accepts exclusive recipes");
        var storage = new UnorderedStackHandlerRemoveZero(
                com.wintercogs.beyonddimensions.api.storage.handler.impl.AbstractUnorderedStackHandler.UiTimestampPolicy.values()[0]);
        var menu = new DimensionsCraftMenu(DimensionsCraftMenu.Dimensions_Craft_Menu.get(), 7, player.getInventory(), storage,
                NonNullList.withSize(9, ItemStack.EMPTY), null);
        player.containerMenu = menu;
        var inputs = stoneGrid();
        for (int i = 0; i < 9; i++) menu.getSlot(menu.craftSlotStartIndex + i).set(inputs[i]);
        var slot = menu.getSlot(menu.resultSlotIndex);
        var taken = slot.remove(8); slot.onTake(player, taken);
        check(taken.is(Items.INFESTED_STONE) && taken.getCount() == 8
                        && java.util.stream.IntStream.range(0, 9).allMatch(i -> menu.getSlot(menu.craftSlotStartIndex + i).getItem().isEmpty()),
                "real Beyond result slot consumes inputs once without returning them as recipe remainders");
    }
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        level = event.getServer().overworld();
        level.getChunk(POS);
        level.removeBlock(POS.south().east(), false);
        level.removeBlock(POS.offset(20, 0, 0), false);
        player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "CompatChecks"));
        player.setPos(220.5, 200, 218.5);
        place(POS, "toms_storage:crafting_terminal");
        level.setBlock(POS, level.getBlockState(POS).setValue(com.tom.storagemod.block.AbstractStorageTerminalBlock.FACING, Direction.SOUTH), 3);
        place(POS.south(), "toms_storage:inventory_connector");
        level.setBlock(POS.south(2), Blocks.CHEST.defaultBlockState(), 3);
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if (done || level == null || event.getLevel() != level || ++ticks < 50) return;
        done = true;
        try {
            recipes();
            var backpackStack = new ItemStack(item("sophisticatedbackpacks:backpack"));
            player.getInventory().setItem(0, backpackStack);
            var backpack = BackpackWrapper.fromStack(backpackStack);
            if (backpack instanceof BackpackWrapper real) {
                real.setContentsUuid(UUID.randomUUID());
                real.setSlotNumbers(27, 3);
                real.onInit(level);
            }
            place(POS.offset(10, 0, 0), "sophisticatedstorage:barrel");
            var chest = ((StorageBlockEntity)level.getBlockEntity(POS.offset(10, 0, 0))).getStorageWrapper();
            check(!((CreativeCraftingUpgradeItem)item("creationcore:backpack_creative_crafting_upgrade"))
                            .canAddUpgradeTo(chest, new ItemStack(item("creationcore:backpack_creative_crafting_upgrade")), false, false).successful(),
                    "backpack upgrade rejects Sophisticated Storage");
            check(!((CreativeCraftingUpgradeItem)item("creationcore:storage_creative_crafting_upgrade"))
                            .canAddUpgradeTo(backpack, new ItemStack(item("creationcore:storage_creative_crafting_upgrade")), false, false).successful(),
                    "storage upgrade rejects Sophisticated Backpacks");
            testUpgrade(backpack, "creationcore:backpack_creative_crafting_upgrade", "sophisticatedbackpacks:crafting_upgrade", "backpack",
                    new net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer(9, player,
                            new net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext.Item("main", 0)));
            testUpgrade(chest, "creationcore:storage_creative_crafting_upgrade", "sophisticatedstorage:crafting_upgrade", "storage",
                    new net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu(10, player, POS.offset(10, 0, 0)));
            CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), true, level.registryAccess());
            for (String target : List.of("backpack", "storage")) {
                var tab = target.equals("backpack") ? net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.CREATIVE_TAB.get()
                        : net.p3pp3rf1y.sophisticatedstorage.init.ModItems.CREATIVE_TAB.get();
                var own = item("creationcore:" + target + "_creative_crafting_upgrade");
                var other = item("creationcore:" + (target.equals("backpack") ? "storage" : "backpack") + "_creative_crafting_upgrade");
                check(tab.getDisplayItems().stream().anyMatch(s -> s.is(own))
                                && tab.getDisplayItems().stream().noneMatch(s -> s.is(other)),
                        target + " native creative tab shows only its corresponding creative upgrade");
            }
            player.getInventory().clearContent();
            var terminal = (CraftingTerminalBlockEntity)level.getBlockEntity(POS);
            var menu = new CraftingTerminalMenu(8, player.getInventory(), terminal);
            player.containerMenu = menu;
            var grid = stoneGrid(); for (int i = 0; i < 9; i++) terminal.getCraftingInv().setItem(i, grid[i]);
            check(terminal.getCraftResult().getItem(0).isEmpty(), "Tom terminal does not craft exclusive recipes without attached table");
            level.setBlock(POS.south().east(), (Boolean.getBoolean("creationcore.checkCrafter") ? ModBlocks.CREATIVE_CRAFTER.get() : ModBlocks.CREATIVE_CRAFTING_TABLE.get()).defaultBlockState(), 3);
            check(TomsNetworkGate.authorized(level, POS), "Tom connector recognizes table attached to its actual network");
            ((RefreshBeforeCrafting)(Object)terminal).creationcore$refreshBeforeCrafting();
            check(terminal.getCraftResult().getItem(0).getCount() == 8, "same Tom terminal refreshes existing grid when table joins");
            menu.clicked(0, 0, ClickType.PICKUP, player);
            check(menu.getCarried().is(Items.INFESTED_STONE) && menu.getCarried().getCount() == 8
                            && terminal.getCraftingInv().isEmpty(), "real Tom terminal click returns eight outputs and consumes exact inputs");
            menu.setCarried(ItemStack.EMPTY);
            for (int i = 0; i < 9; i++) terminal.getCraftingInv().setItem(i, stoneGrid()[i]);
            level.removeBlock(POS.south().east(), false);
            menu.clicked(0, 0, ClickType.PICKUP, player);
            check(menu.getCarried().isEmpty() && terminal.getCraftingInv().getItem(0).getCount() == 1,
                    "Tom stale output click is rejected immediately after table removal without consuming inputs");
            level.setBlock(POS.offset(20, 0, 0), (Boolean.getBoolean("creationcore.checkCrafter") ? ModBlocks.CREATIVE_CRAFTER.get() : ModBlocks.CREATIVE_CRAFTING_TABLE.get()).defaultBlockState(), 3);
            check(!TomsNetworkGate.authorized(level, POS), "unconnected creative table cannot authorize Tom network");
            beyond();
            results.add("ALL COMPAT CHECKS PASSED");
        } catch (Throwable failure) { failure.printStackTrace(); results.add("FAIL: " + failure); }
        try { Files.write(Path.of("../build/compat-checks.txt"), results); }
        catch (Exception failure) { throw new RuntimeException(failure); }
        level.getServer().halt(false);
    }
}
