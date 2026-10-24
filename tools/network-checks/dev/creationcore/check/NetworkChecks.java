package dev.creationcore.check;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.*;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.util.AEColor;
import appeng.blockentity.crafting.MolecularAssemblerBlockEntity;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import appeng.helpers.InventoryAction;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.menu.SlotSemantics;
import appeng.parts.reporting.CraftingTerminalPart;
import appeng.parts.encoding.EncodingMode;
import appeng.parts.encoding.PatternEncodingTerminalPart;
import com.mojang.authlib.GameProfile;
import dev.creationcore.compat.ae2.CreativeNetworkGate;
import dev.creationcore.compat.ae2.NetworkCreativeTable;
import dev.creationcore.registry.ModBlocks;
import dev.creationcore.registry.ModItems;
import dev.creationcore.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

/** Runs in a disposable world, with real terminal, channels, CPU, provider and assembler. */
@EventBusSubscriber(modid = "creationcore")
public final class NetworkChecks {
    private static final List<String> results = new ArrayList<>();
    private static ServerLevel level;
    private static final BlockPos POS = new BlockPos(110, 200, 110);
    private static final BlockPos TABLE = POS.below();
    private static final BlockPos OTHER = POS.offset(10, 0, 0);
    private static PatternProviderBlockEntity provider;
    private static MolecularAssemblerBlockEntity assembler;
    private static DriveBlockEntity drive;
    private static PatternEncodingTerminalPart terminal;
    private static PatternEncodingTermMenu menu;
    private static FakePlayer player;
    private static CraftingTerminalPart craftingTerminal;
    private static CraftingTermMenu craftingMenu;
    private static ItemStack creativePattern;
    private static IPatternDetails decoded;
    private static Future<ICraftingPlan> calculation;
    private static int ticks, phase, since, initialChannels;
    private static boolean done;

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        results.add("PASS: " + label);
        System.out.println("NETWORK_CHECK " + results.getLast());
    }
    private static void place(String name, BlockPos pos) {
        level.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("ae2", name)).defaultBlockState(), 3);
    }
    private static final boolean CRAFTER = Boolean.getBoolean("creationcore.checkCrafter");
    private static net.minecraft.world.level.block.Block device() { return CRAFTER ? ModBlocks.CREATIVE_CRAFTER.get() : ModBlocks.CREATIVE_CRAFTING_TABLE.get(); }
    private static BlockEntity table() { return level.getBlockEntity(TABLE); }
    private static appeng.api.networking.IManagedGridNode main(BlockEntity be) {
        return be instanceof NetworkCreativeTable t ? t.getMainNode()
            : ((dev.creationcore.compat.ae2.CrafterNetworkAccess)be).creationcore$getMainNode();
    }
    private static boolean online() { return CreativeNetworkGate.authorized(provider.getMainNode().getNode()); }
    private static long stock(AEItemKey key) {
        return provider.getMainNode().getGrid().getStorageService().getInventory().extract(key, Long.MAX_VALUE,
                Actionable.SIMULATE, IActionSource.empty());
    }
    private static void next(int target) { phase = target; since = ticks; }
    private static void grid(ItemStack[] stacks) {
        for (int i = 0; i < 9; i++) terminal.getLogic().getEncodedInputInv().setStack(i,
                i < stacks.length && !stacks[i].isEmpty() ? GenericStack.fromItemStack(stacks[i]) : null);
    }
    private static ItemStack[] stoneGrid() {
        ItemStack[] grid = new ItemStack[9];
        for (int i = 0; i < 9; i++) grid[i] = new ItemStack(i == 4 ? Items.SILVERFISH_SPAWN_EGG : Items.STONE);
        return grid;
    }
    private static ItemStack encode() {
        terminal.getLogic().getEncodedPatternInv().setItemDirect(0, ItemStack.EMPTY);
        terminal.getLogic().getBlankPatternInv().setItemDirect(0, AEItems.BLANK_PATTERN.stack(8));
        menu.encode();
        return terminal.getLogic().getEncodedPatternInv().getStackInSlot(0).copy();
    }
    private static KeyCounter[] materials(IPatternDetails pattern) {
        var inputs = pattern.getInputs();
        KeyCounter[] counters = new KeyCounter[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            counters[i] = new KeyCounter();
            var input = inputs[i].getPossibleInputs()[0];
            counters[i].add(input.what(), input.amount() * inputs[i].getMultiplier());
        }
        return counters;
    }
    private static int assemblerInputs() {
        int amount = 0;
        for (int i = 0; i < 9; i++) amount += assembler.getInternalInventory().getStackInSlot(i).getCount();
        return amount;
    }

    private static void craftingGrid(ItemStack[] stacks) {
        for (int i = 0; i < 9; i++) craftingMenu.getCraftingMatrix().setItemDirect(i,
                i < stacks.length ? stacks[i].copy() : ItemStack.EMPTY);
        craftingMenu.slotsChanged(craftingMenu.getCraftingMatrix().toContainer());
    }
    private static ItemStack craftingOutput() {
        return craftingMenu.getSlots(SlotSemantics.CRAFTING_RESULT).getFirst().getItem();
    }
    private static void craftClick(InventoryAction action) {
        player.containerMenu = craftingMenu;
        craftingMenu.doAction(player, action, craftingMenu.getSlots(SlotSemantics.CRAFTING_RESULT).getFirst().index, 0);
    }
    private static long manualTotal(AEItemKey key) {
        long count = stock(key);
        if (key.matches(craftingMenu.getCarried())) count += craftingMenu.getCarried().getCount();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (key.matches(stack)) count += stack.getCount();
        }
        for (int i = 0; i < 9; i++) {
            var stack = craftingMenu.getCraftingMatrix().getStackInSlot(i);
            if (key.matches(stack)) count += stack.getCount();
        }
        return count;
    }

    private static void checkManualCrafting() {
        player.containerMenu = craftingMenu;
        craftingMenu.broadcastChanges();
        check(craftingOutput().is(Items.INFESTED_STONE), "unchanged manual crafting grid gains preview when table comes online");
        craftClick(InventoryAction.CRAFT_ITEM);
        check(craftingMenu.getCarried().is(Items.INFESTED_STONE) && craftingMenu.getCarried().getCount() == 8,
                "manual crafting terminal gives all eight products to cursor");
        check(manualTotal(AEItemKey.of(Items.STONE)) == 0 && manualTotal(AEItemKey.of(Items.SILVERFISH_SPAWN_EGG)) == 0,
                "manual craft consumes exactly one complete exclusive input grid");
        craftingMenu.setCarried(ItemStack.EMPTY);
        var twice = stoneGrid();
        for (var stack : twice) stack.setCount(2);
        craftingGrid(twice);
        craftClick(InventoryAction.CRAFT_SHIFT);
        check(manualTotal(AEItemKey.of(Items.INFESTED_STONE)) == 16
                        && manualTotal(AEItemKey.of(Items.STONE)) == 0 && manualTotal(AEItemKey.of(Items.SILVERFISH_SPAWN_EGG)) == 0,
                "manual Shift crafting returns sixteen products and consumes two complete batches");
        player.getInventory().clearContent();
        craftingGrid(stoneGrid());
        var refillStorage = provider.getMainNode().getGrid().getStorageService().getInventory();
        if (refillStorage.insert(AEItemKey.of(Items.STONE), 16, Actionable.MODULATE, IActionSource.empty()) != 16
                || refillStorage.insert(AEItemKey.of(Items.SILVERFISH_SPAWN_EGG), 2, Actionable.MODULATE, IActionSource.empty()) != 2)
            throw new AssertionError("manual refill material setup failed");
        craftClick(InventoryAction.CRAFT_SHIFT);
        check(manualTotal(AEItemKey.of(Items.INFESTED_STONE)) == 24
                        && manualTotal(AEItemKey.of(Items.STONE)) == 0 && manualTotal(AEItemKey.of(Items.SILVERFISH_SPAWN_EGG)) == 0,
                "manual Shift crafting refills from ME storage and consumes exactly three batches for twenty-four products");
        player.getInventory().clearContent();
        var single = new ItemStack[9];
        java.util.Arrays.fill(single, ItemStack.EMPTY);
        single[8] = new ItemStack(Items.TADPOLE_BUCKET);
        craftingGrid(single);
        craftClick(InventoryAction.CRAFT_ITEM);
        check(craftingMenu.getCarried().is(Items.TADPOLE_SPAWN_EGG) && manualTotal(AEItemKey.of(Items.BUCKET)) == 1,
                "offset manual tadpole recipe returns exactly one empty bucket");
        craftingMenu.setCarried(ItemStack.EMPTY);
        craftingGrid(new ItemStack[0]);
        var disc = new ItemStack(Items.MUSIC_DISC_13);
        disc.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Manual Original"));
        var discs = new ItemStack[9];
        for (int i = 0; i < 9; i++) discs[i] = i == 4 ? disc.copy()
                : new ItemStack(i == 1 ? ModItems.BLANK_MATTER.get() : Items.IRON_INGOT);
        craftingGrid(discs);
        craftClick(InventoryAction.CRAFT_ITEM);
        check(AEItemKey.of(disc).matches(craftingMenu.getCarried()) && manualTotal(AEItemKey.of(disc)) == 2,
                "manual disc copy preserves original and copied disc components without duplication beyond recipe output");
        craftingMenu.setCarried(ItemStack.EMPTY);
        craftingGrid(new ItemStack[0]);
        player.getInventory().clearContent();
        craftingGrid(stoneGrid());
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        craftClick(InventoryAction.CRAFT_SHIFT);
        check(manualTotal(AEItemKey.of(Items.INFESTED_STONE)) == 0
                        && manualTotal(AEItemKey.of(Items.STONE)) == 8 && manualTotal(AEItemKey.of(Items.SILVERFISH_SPAWN_EGG)) == 1,
                "full player inventory rejects manual Shift craft without consuming ingredients");
        player.getInventory().clearContent();
        craftingGrid(new ItemStack[]{new ItemStack(Items.OAK_LOG)});
        craftClick(InventoryAction.CRAFT_ITEM);
        check(craftingMenu.getCarried().is(Items.OAK_PLANKS) && craftingMenu.getCarried().getCount() == 4,
                "ordinary manual recipes still craft with an online creative table");
        craftingMenu.setCarried(ItemStack.EMPTY);
        craftingGrid(stoneGrid());
        player.containerMenu = menu;
    }

    @SubscribeEvent public static void started(ServerStartedEvent event) {
        try {
            level = event.getServer().overworld();
            level.setChunkForced(POS.getX() >> 4, POS.getZ() >> 4, true);
            level.setChunkForced(OTHER.getX() >> 4, OTHER.getZ() >> 4, true);
            level.getChunk(POS); level.getChunk(OTHER);
            for (BlockPos pos : List.of(POS, POS.north(), POS.south(), POS.east(), POS.west(), POS.above(), TABLE,
                    OTHER, OTHER.above())) level.removeBlock(pos, false);
            for (int i = 1; i <= 10; i++) level.removeBlock(POS.east(i).above(), false);
            place("pattern_provider", POS);
            place("molecular_assembler", POS.north());
            place("drive", POS.east());
            place("1k_crafting_storage", POS.west());
            place("creative_energy_cell", POS.above());
            player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "NetworkChecks"));
            player.setPos(110.5, 200, 112.5);
            PartHelper.setPart(level, POS.south(), null, player, AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT));
            terminal = PartHelper.setPart(level, POS.south(), Direction.SOUTH, player, AEParts.PATTERN_ENCODING_TERMINAL.get());
            craftingTerminal = PartHelper.setPart(level, POS.south(), Direction.EAST, player, AEParts.CRAFTING_TERMINAL.get());
            provider = (PatternProviderBlockEntity)level.getBlockEntity(POS);
            assembler = (MolecularAssemblerBlockEntity)level.getBlockEntity(POS.north());
            drive = (DriveBlockEntity)level.getBlockEntity(POS.east());
            drive.getInternalInventory().setItemDirect(0, new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath("ae2", "item_storage_cell_1k"))));
            assembler.getUpgrades().addItems(AEItems.SPEED_CARD.stack(5));
            check(terminal != null, "real AE2 pattern terminal installed on glass cable");
        } catch (Throwable failure) { finish(failure); }
    }

    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if (done || level == null || event.getLevel() != level) return;
        try {
            ticks++;
            if (ticks > 400) throw new AssertionError("timeout in phase " + phase + "; table=" + online()
                    + "; ready=" + (table() == null ? "none" : main(table()).isReady())
                    + "; powered=" + (table() == null ? "none" : main(table()).isPowered())
                    + "; invoked=" + (table() == null ? "none" : main(table()).isReady())
                    + "; sameGrid=" + (table() != null && main(table()).getGrid() == provider.getMainNode().getGrid())
                    + "; tableActive=" + (table() != null && main(table()).isActive())
                    + "; channels=" + (table() == null || main(table()).getNode() == null ? -1 : main(table()).getNode().getUsedChannels())
                    + "; providerActive=" + provider.getMainNode().isActive()
                    + "; indexed=" + provider.getMainNode().getGrid().getMachines(NetworkCreativeTable.class).size());
            if (phase == 0) {
                if (ticks < 40 || !provider.getMainNode().isActive() || !terminal.getMainNode().isActive()) return;
                drive.getInternalInventory().setItemDirect(1, new ItemStack(BuiltInRegistries.ITEM.get(
                        ResourceLocation.fromNamespaceAndPath("ae2", "fluid_storage_cell_1k"))));
                var fluids = provider.getMainNode().getGrid().getStorageService().getInventory();
                var voidKey = AEFluidKey.of(ModFluids.VOID.get());
                check(fluids.insert(voidKey, 1250, Actionable.MODULATE, IActionSource.empty()) == 1250,
                        "real AE2 fluid storage cell accepts core void fluid alongside item storage");
                check(fluids.extract(voidKey, 250, Actionable.MODULATE, IActionSource.empty()) == 250
                                && fluids.extract(voidKey, 1000, Actionable.MODULATE, IActionSource.empty()) == 1000,
                        "AE2 fluid cell returns exact void amounts through its native fluid key");
                menu = new PatternEncodingTermMenu(1, player.getInventory(), terminal);
                player.containerMenu = menu;
                craftingMenu = new CraftingTermMenu(2, player.getInventory(), craftingTerminal);
                craftingGrid(stoneGrid());
                check(craftingOutput().isEmpty(), "manual exclusive recipe has no preview without an online table");
                craftClick(InventoryAction.CRAFT_ITEM);
                check(craftingMenu.getCarried().isEmpty() && manualTotal(AEItemKey.of(Items.STONE)) == 8,
                        "manual crafting without a table produces nothing and preserves ingredients");
                player.containerMenu = menu;
                terminal.getLogic().setMode(EncodingMode.CRAFTING);
                grid(stoneGrid());
                check(!online() && encode().isEmpty(), "exclusive crafting pattern cannot be encoded without an online table");
                check(terminal.getLogic().getBlankPatternInv().getStackInSlot(0).getCount() == 8,
                        "rejected encoding does not consume blank patterns");
                check(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, List.of(stoneGrid())), level).isEmpty(),
                        "exclusive recipes never leak into ordinary workbench recipe type");
                grid(new ItemStack[]{new ItemStack(Items.OAK_LOG)});
                ItemStack ordinary = encode();
                var ordinaryDetails = PatternDetailsHelper.decodePattern(ordinary, level);
                check(!ordinary.isEmpty() && !CreativeNetworkGate.requiresTable(ordinaryDetails), "ordinary patterns still encode without a table");
                check(assembler.pushPattern(ordinaryDetails, materials(ordinaryDetails), Direction.SOUTH),
                        "ordinary pattern is accepted by a real molecular assembler without a table");
                next(1);
            } else if (phase == 1) {
                if (stock(AEItemKey.of(Items.OAK_PLANKS)) < 4) return;
                check(stock(AEItemKey.of(Items.OAK_PLANKS)) == 4, "ordinary molecular assembly completes and returns four planks without a table");
                initialChannels = provider.getMainNode().getGrid().getPathingService().getUsedChannels();
                level.setBlock(TABLE, device().defaultBlockState(), 3);
                next(2);
            } else if (phase == 2) {
                if (ticks - since == 60 && !online()) throw new AssertionError("table diagnosis: active=" + main(table()).isActive()
                        + ", channel=" + main(table()).getNode().getUsedChannels()
                        + ", meets=" + main(table()).getNode().meetsChannelRequirements()
                        + ", provider=" + provider.getMainNode().isActive()
                        + ", indexed=" + provider.getMainNode().getGrid().getMachines(NetworkCreativeTable.class).size()
                        + ", removed=" + table().isRemoved());
                if (!online() || ticks - since < 10) return;
                checkManualCrafting();
                check(main(table()).isActive() && main(table()).getNode().hasFlag(appeng.api.networking.GridFlags.REQUIRE_CHANNEL),
                        "creative table is powered and requires a channel");
                check(provider.getMainNode().getGrid().getPathingService().getUsedChannels() == initialChannels + 1,
                        "adding the table increases network channel use by exactly one");
                if (CRAFTER) {
                    var crafter = (net.minecraft.world.level.block.entity.CrafterBlockEntity)table();
                    crafter.setItem(0, new ItemStack(Items.DIAMOND, 3));
                    crafter.setSlotState(1, false);
                    var copy = (net.minecraft.world.level.block.entity.CrafterBlockEntity)BlockEntity.loadStatic(TABLE,
                            crafter.getBlockState(), crafter.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
                    check(copy.getItem(0).is(Items.DIAMOND) && copy.getItem(0).getCount() == 3 && copy.isSlotDisabled(1),
                            "network crafter retains native inventory and disabled slots through full NBT reload");
                    check(copy.getType() == net.minecraft.world.level.block.entity.BlockEntityType.CRAFTER,
                            "network crafter retains vanilla entity type for existing save compatibility");
                    crafter.clearContent(); crafter.setSlotState(1, true);
                }
                var saved = table().saveWithFullMetadata(level.registryAccess());
                check((CRAFTER ? BlockEntity.loadStatic(TABLE, table().getBlockState(), saved, level.registryAccess()) instanceof net.minecraft.world.level.block.entity.CrafterBlockEntity : BlockEntity.loadStatic(TABLE, table().getBlockState(), saved, level.registryAccess()) instanceof NetworkCreativeTable),
                        "network table saves and reloads its own block entity type");
                check(!encode().isEmpty(), "ordinary cached recipe survives table becoming online in the same open terminal");
                grid(new ItemStack[0]);
                var inventoryForClick = provider.getMainNode().getGrid().getStorageService().getInventory();
                check(inventoryForClick.insert(AEItemKey.of(Items.OAK_LOG), 16, Actionable.MODULATE, IActionSource.empty()) == 16,
                        "right-click regression seeds real ME storage with sixteen logs");
                long extracted = inventoryForClick.extract(AEItemKey.of(Items.OAK_LOG), 16, Actionable.MODULATE, IActionSource.empty());
                menu.setCarried(new ItemStack(Items.OAK_LOG, (int)extracted));
                var ghost = menu.getCraftingGridSlots()[0];
                menu.doAction(player, InventoryAction.SPLIT_OR_PLACE_SINGLE, ghost.index, 0);
                menu.onSlotChange(ghost);
                // setItem is also used by AE2 when applying ghost-grid synchronization.
                menu.setItem(ghost.index, 0, ghost.getItem().copy());
                var ordinaryOnline = PatternDetailsHelper.decodePattern(encode(), level);
                check(extracted == 16 && ghost.getItem().is(Items.OAK_LOG)
                                && menu.getCarried().getCount() == 16 && ordinaryOnline != null
                                && !CreativeNetworkGate.requiresTable(ordinaryOnline)
                                && ordinaryOnline.getPrimaryOutput().what().equals(AEItemKey.of(Items.OAK_PLANKS)),
                        "ME-extracted log right-click into ghost grid encodes ordinary planks with online table without consuming cursor items");
                for (int i = 0; i < 32; i++) {
                    menu.setItem(ghost.index, i + 1, ItemStack.EMPTY);
                    menu.setItem(ghost.index, i + 2, new ItemStack(Items.OAK_LOG));
                    menu.onSlotChange(ghost);
                    menu.broadcastChanges();
                    if (encode().isEmpty()) throw new AssertionError("ordinary preview lost during repeated ghost edits " + i);
                }
                check(true, "32 repeated empty-to-ordinary ghost edits and fluid-substitution checks terminate with online table");
                menu.setCarried(ItemStack.EMPTY);
                ItemStack[] bucketGrid = new ItemStack[9];
                java.util.Arrays.fill(bucketGrid, ItemStack.EMPTY);
                bucketGrid[8] = new ItemStack(Items.TADPOLE_BUCKET);
                grid(bucketGrid);
                var bucketPlan = PatternDetailsHelper.decodePattern(encode(), level);
                check(CreativeNetworkGate.requiresTable(bucketPlan)
                        && bucketPlan.getInputs()[0].getRemainingKey(AEItemKey.of(Items.TADPOLE_BUCKET)).equals(AEItemKey.of(Items.BUCKET)),
                        "offset shapeless pattern retains empty-bucket remainder for AE2 scheduling");
                ItemStack namedDisc = new ItemStack(Items.MUSIC_DISC_13);
                namedDisc.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Original Disc"));
                ItemStack[] discs = new ItemStack[9];
                for (int i = 0; i < 9; i++) discs[i] = i == 4 ? namedDisc.copy()
                        : new ItemStack(i == 1 ? ModItems.BLANK_MATTER.get() : Items.IRON_INGOT);
                grid(discs);
                var discPlan = PatternDetailsHelper.decodePattern(encode(), level);
                check(discPlan != null && discPlan.getPrimaryOutput().what().equals(AEItemKey.of(namedDisc)),
                        "disc copy encoded output retains original components");
                boolean discReturned = false;
                for (var in : discPlan.getInputs()) for (var possible : in.getPossibleInputs()) {
                    if (possible.what().equals(AEItemKey.of(namedDisc))) {
                        discReturned = AEItemKey.of(namedDisc).equals(in.getRemainingKey(possible.what()));
                    }
                }
                check(discReturned, "disc pattern returns the original component-bearing disc");
                grid(stoneGrid());
                creativePattern = encode();
                decoded = PatternDetailsHelper.decodePattern(creativePattern, level);
                check(!creativePattern.isEmpty() && CreativeNetworkGate.requiresTable(decoded), "real terminal encodes exclusive recipe as a native AE2 crafting pattern");
                check(decoded.getPrimaryOutput().what().equals(AEItemKey.of(Items.INFESTED_STONE))
                        && decoded.getPrimaryOutput().amount() == 8, "encoded pattern preserves eight-item recipe output");
                check(assembler.pushPattern(decoded, materials(decoded), Direction.SOUTH), "online table authorizes exclusive molecular assembly");
                check(assemblerInputs() == 9, "assembler receives exactly one complete input grid");
                level.removeBlock(TABLE, false);
                craftClick(InventoryAction.CRAFT_ITEM);
                check(craftingMenu.getCarried().isEmpty() && manualTotal(AEItemKey.of(Items.STONE)) == 8,
                        "stale manual output click immediately after table removal is rejected without consuming materials");
                craftingMenu.broadcastChanges();
                check(craftingOutput().isEmpty(), "same open crafting terminal clears preview after table removal");
                player.containerMenu = menu;
                int previousProgress = assembler.getCraftingProgress();
                assembler.tickingRequest(assembler.getMainNode().getNode(), 100);
                check(assemblerInputs() == 9 && assembler.getCraftingProgress() == previousProgress,
                        "removing table mid-craft immediately preserves ingredients and progress");
                next(3);
            } else if (phase == 3) {
                if (ticks - since < 20) return;
                check(assemblerInputs() == 9 && stock(AEItemKey.of(Items.INFESTED_STONE)) == 0,
                        "paused exclusive craft produces nothing while table is absent");
                grid(stoneGrid());
                check(encode().isEmpty(), "same open terminal invalidates its cached creative recipe when table is removed");
                place("creative_energy_cell", OTHER.above());
                level.setBlock(OTHER, device().defaultBlockState(), 3);
                next(4);
            } else if (phase == 4) {
                var remote = level.getBlockEntity(OTHER);
                if (!main(remote).isActive() || ticks - since < 20) return;
                check(main(remote).getNode().getUsedChannels() == 1, "isolated powered table consumes exactly one channel");
                check(!online() && encode().isEmpty() && assemblerInputs() == 9,
                        "online table in a separate ME network cannot authorize this terminal or assembler");
                craftingMenu.broadcastChanges();
                check(craftingOutput().isEmpty(), "manual terminal cannot use an online creative table in another network");
                level.setBlock(TABLE, device().defaultBlockState(), 3);
                next(5);
            } else if (phase == 5) {
                if (!online() || stock(AEItemKey.of(Items.INFESTED_STONE)) < 8) return;
                check(stock(AEItemKey.of(Items.INFESTED_STONE)) == 8 && assemblerInputs() == 0,
                        "reconnecting table resumes the held craft exactly once and returns its output");
                check(!encode().isEmpty(), "open terminal can encode again after table reconnects");
                craftingMenu.broadcastChanges();
                check(craftingOutput().is(Items.INFESTED_STONE), "same open manual terminal restores unchanged preview when table reconnects");
                craftingGrid(new ItemStack[0]);
                provider.getMainNode().getGrid().getStorageService().getInventory().extract(AEItemKey.of(Items.INFESTED_STONE), 8,
                        Actionable.MODULATE, IActionSource.empty());
                provider.getLogic().getPatternInv().setItemDirect(0, creativePattern.copy());
                provider.getLogic().updatePatterns();
                var inventory = provider.getMainNode().getGrid().getStorageService().getInventory();
                check(inventory.insert(AEItemKey.of(Items.STONE), 128, Actionable.MODULATE, IActionSource.empty()) == 128
                        && inventory.insert(AEItemKey.of(Items.SILVERFISH_SPAWN_EGG), 16, Actionable.MODULATE, IActionSource.empty()) == 16,
                        "ME storage receives exact materials for a 128-item native crafting job");
                provider.getMainNode().getGrid().getStorageService().invalidateCache();
                next(6);
            } else if (phase == 6) {
                if (ticks - since < 5) return;
                var service = provider.getMainNode().getGrid().getCraftingService();
                if (service.getCpus().isEmpty()) return;
                calculation = service.beginCraftingCalculation(level, new ICraftingSimulationRequester() {
                    @Override public IActionSource getActionSource() { return IActionSource.empty(); }
                    @Override public IGridNode getGridNode() { return provider.getMainNode().getNode(); }
                }, AEItemKey.of(Items.INFESTED_STONE), 128, CalculationStrategy.REPORT_MISSING_ITEMS);
                next(7);
            } else if (phase == 7) {
                if (!calculation.isDone()) return;
                ICraftingPlan plan = calculation.get();
                check(!plan.simulation() && plan.missingItems().isEmpty(), "AE2 calculates native crafting job for exclusive recipe");
                check(provider.getMainNode().getGrid().getCraftingService().submitJob(plan, null, null, false, IActionSource.empty()).successful(),
                        "real ME crafting CPU accepts exclusive crafting job");
                next(8);
            } else if (phase == 8) {
                if (stock(AEItemKey.of(Items.INFESTED_STONE)) < 128) return;
                check(stock(AEItemKey.of(Items.INFESTED_STONE)) == 128 && stock(AEItemKey.of(Items.STONE)) == 0
                        && stock(AEItemKey.of(Items.SILVERFISH_SPAWN_EGG)) == 0, "all 128 exclusive products return through native assemblers with exact material consumption");
                check(assemblerInputs() == 0 && assembler.getUpgrades().getInstalledUpgrades(AEItems.SPEED_CARD) == 5,
                        "assembler finishes with no leftover inputs and keeps native acceleration cards");
                var restoredData = table().saveWithFullMetadata(level.registryAccess());
                table().onChunkUnloaded();
                check(!online(), "unloading the table chunk revokes authorization");
                // Minecraft recreates a fresh block entity when a chunk is read again.
                level.removeBlock(TABLE, false);
                level.setBlock(TABLE, device().defaultBlockState(), 3);
                table().loadWithComponents(restoredData, level.registryAccess());
                next(9);
            } else if (phase == 9) {
                if (!online() || ticks - since < 15) return;
                check(online(), "table reloaded from saved node data restores authorization");
                level.removeBlock(POS.above(), false);
                provider.getMainNode().getGrid().getEnergyService().extractAEPower(1_000_000,
                        Actionable.MODULATE, appeng.api.config.PowerMultiplier.ONE);
                next(11);
            } else if (phase == 11) {
                if (ticks - since < 20) return;
                check(!main(table()).isPowered() && !online(), "loss of ME power disables table authorization");
                check(!assembler.pushPattern(decoded, materials(decoded), Direction.SOUTH), "unpowered network cannot start exclusive molecular assembly");
                place("creative_energy_cell", POS.above());
                next(12);
            } else if (phase == 12) {
                if (!online() || ticks - since < 15) return;
                // Overflow the default eight-channel ad-hoc network with ordinary AE2 devices.
                for (int i = 1; i <= 10; i++) place("pattern_provider", POS.east(i).above());
                next(10);
            } else if (phase == 10) {
                if (ticks - since < 40) return;
                check(!main(table()).isActive() && !online(), "missing channels disable the table authorization");
                check(!assembler.pushPattern(decoded, materials(decoded), Direction.SOUTH),
                        "offline assembler rejects exclusive pattern without consuming a supplied batch");
                results.add("ALL NETWORK CHECKS PASSED in " + ticks + " ticks");
                finish(null);
            }
        } catch (Throwable failure) { finish(failure); }
    }

    private static void finish(Throwable failure) {
        done = true;
        if (failure != null) { failure.printStackTrace(); results.add("FAIL: " + failure); }
        try { Files.write(Path.of("../build/network-checks.txt"), results); }
        catch (Exception error) { throw new RuntimeException(error); }
        if (level != null) level.getServer().halt(false);
    }
}
