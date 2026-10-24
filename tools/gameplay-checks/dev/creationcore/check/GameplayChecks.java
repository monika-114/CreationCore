package dev.creationcore.check;

import com.mojang.authlib.GameProfile;
import dev.creationcore.event.CoreGameplayEvents;
import dev.creationcore.entity.VoidBucketEntity;
import dev.creationcore.data.DragonRitualSavedData;
import dev.creationcore.menu.CreativeCraftingMenu;
import dev.creationcore.registry.ModBlocks;
import dev.creationcore.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Opt-in disposable development-world checks. This class is not a production source. */
@EventBusSubscriber(modid = "creationcore")
public final class GameplayChecks {
    private static final List<String> results = new ArrayList<>();
    private static boolean rejectBucket, rejectMatter;
    private static final List<ItemEntity> joined = new ArrayList<>();

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST) public static void spawn(EntityJoinLevelEvent event) {
        if (rejectBucket && event.getEntity() instanceof VoidBucketEntity) event.setCanceled(true);
        if (rejectMatter && event.getEntity() instanceof ItemEntity item
                && item.getItem().is(ModItems.CREATIVE_MATTER.get())) event.setCanceled(true);
        if (!event.isCanceled() && event.getEntity() instanceof ItemEntity item) joined.add(item);
    }

    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        results.add("PASS: " + name);
        System.out.println("CREATIONCORE_CHECK " + results.getLast());
    }

    private static int drops(ServerLevel level, BlockPos pos, Item item) {
        // At ServerStarted the newly loaded chunk's entity disk callback may not have completed.
        // Observe accepted spawn events directly rather than depending on that async index.
        return joined.stream().filter(e -> e.level() == level && e.isAlive() && e.getItem().is(item)
                && new AABB(pos).inflate(6).contains(e.position())).mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static void clear(ServerLevel level, BlockPos pos) {
        joined.stream().filter(e -> e.level() == level && new AABB(pos).inflate(8).contains(e.position())).forEach(ItemEntity::discard);
        level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(8)).forEach(ItemEntity::discard);
    }

    @SubscribeEvent public static void started(ServerStartedEvent event) {
        try {
            ServerLevel level = event.getServer().overworld();
            FakePlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "CoreChecks"));
            player.setGameMode(GameType.SURVIVAL);
            BlockPos pos = new BlockPos(100, 200, 100);
            level.getChunk(pos);
            BlockPos lightPos = pos.offset(-4, 0, 0);
            level.setBlock(lightPos, Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 1), 3);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.LIGHT, 4));
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(lightPos), Direction.UP, lightPos, false);
            player.gameMode.useItemOn(player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            check(level.getBlockState(lightPos).getValue(net.minecraft.world.level.block.LightBlock.LEVEL) == 2
                    && player.getMainHandItem().getCount() == 4, "survival Light item cycles brightness without consuming held item");
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.MINE_CRAFT.get()));
            level.setBlock(lightPos, level.getBlockState(lightPos).setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 15)
                    .setValue(net.minecraft.world.level.block.LightBlock.WATERLOGGED, true), 3);
            player.gameMode.useItemOn(player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            check(level.getBlockState(lightPos).getValue(net.minecraft.world.level.block.LightBlock.LEVEL) == 0
                    && level.getBlockState(lightPos).getValue(net.minecraft.world.level.block.LightBlock.WATERLOGGED),
                    "survival Mine Craft wraps brightness fifteen to zero while preserving waterlogged state");
            player.setGameMode(GameType.ADVENTURE);
            player.gameMode.useItemOn(player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            check(level.getBlockState(lightPos).getValue(net.minecraft.world.level.block.LightBlock.LEVEL) == 0,
                    "adventure permissions are not bypassed by brightness interaction");
            player.setGameMode(GameType.SURVIVAL);
            player.getInventory().clearContent();
            player.setPos(100.5, 200, 102.5);
            level.setBlock(pos, ModBlocks.CREATIVE_CRAFTING_TABLE.get().defaultBlockState(), 3);
            var menu = new CreativeCraftingMenu(1, player.getInventory(), ContainerLevelAccess.create(level, pos));
            for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
            player.getInventory().setItem(0, new ItemStack(Items.INFESTED_STONE, 63));
            for (int i = 0; i < 9; i++) menu.getSlot(i + 1).set(new ItemStack(i == 4 ? Items.SILVERFISH_SPAWN_EGG : Items.STONE));
            check(menu.getSlot(0).getItem().getCount() == 8, "exclusive recipe previews eight outputs");
            clear(level, pos);
            menu.quickMoveStack(player, 0);
            check(player.getInventory().getItem(0).getCount() == 64 && drops(level, pos, Items.INFESTED_STONE) == 7,
                    "partial Shift craft conserves all eight outputs (one stored, seven dropped)");
            check(java.util.stream.IntStream.rangeClosed(1, 9).allMatch(i -> !menu.getSlot(i).hasItem()), "partial Shift craft consumes exactly one input batch");
            check(!menu.canTakeItemForPickAll(ItemStack.EMPTY, menu.getSlot(0)), "double-click collection excludes result slot");

            clear(level, pos);
            player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 63));
            menu.getSlot(1).set(new ItemStack(Items.OAK_LOG));
            menu.quickMoveStack(player, 0);
            check(player.getInventory().getItem(0).getCount() == 64 && drops(level, pos, Items.OAK_PLANKS) == 3,
                    "partial ordinary craft conserves four planks");

            player.getInventory().clearContent();
            player.getInventory().setItem(0, new ItemStack(ModItems.MINE_CRAFT.get()));
            player.getInventory().selected = 0;
            BlockPos foot = pos.offset(4, 0, 0);
            BlockPos head = foot.north();
            level.setBlock(foot.below(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(head.below(), Blocks.STONE.defaultBlockState(), 3);
            BlockState footState = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT);
            BlockState headState = footState.setValue(BedBlock.PART, BedPart.HEAD);
            level.setBlock(foot, footState, 3); level.setBlock(head, headState, 3);
            clear(level, foot);
            check(player.gameMode.destroyBlock(foot), "Mine Craft breaks bed");
            check(drops(level, foot, Items.RED_BED) == 1, "bed produces one item");
            var companion = blockDrop(level, head, headState, null, ItemStack.EMPTY);
            CoreGameplayEvents.onMineCraftBlockDrops(companion);
            check(companion.getDrops().isEmpty(), "confirmed delayed bed companion is suppressed");
            var distant = blockDrop(level, head.offset(1000, 0, 0), headState, null, ItemStack.EMPTY);
            CoreGameplayEvents.onMineCraftBlockDrops(distant);
            check(distant.getDrops().size() == 1, "distant same-item drop survives recent break");
            var adjacent = blockDrop(level, foot.east(), footState, null, ItemStack.EMPTY);
            CoreGameplayEvents.onMineCraftBlockDrops(adjacent);
            check(adjacent.getDrops().size() == 1, "adjacent unrelated bed drop survives");
            FakePlayer other = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "OtherChecks"));
            var otherDrop = blockDrop(level, head, headState, other, ItemStack.EMPTY);
            CoreGameplayEvents.onMineCraftBlockDrops(otherDrop);
            check(otherDrop.getDrops().size() == 1, "another player's drop is not suppressed");

            BlockPos door = pos.offset(8, 0, 0);
            level.setBlock(door.below(), Blocks.STONE.defaultBlockState(), 3);
            BlockState lower = Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
            level.setBlock(door, lower, 3);
            level.setBlock(door.above(), lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), 3);
            clear(level, door);
            check(player.gameMode.destroyBlock(door), "Mine Craft breaks door");
            check(drops(level, door, Items.OAK_DOOR) == 1, "door produces one item");

            ServerLevel end = event.getServer().getLevel(Level.END);
            end.getChunk(0, 0);
            clear(end, new BlockPos(10, 0, 10));
            ItemEntity source = new ItemEntity(end, 10, -20, 10, new ItemStack(Items.BUCKET));
            source.getPersistentData().putInt("creationcore_void_threshold", -10);
            source.getPersistentData().putDouble("creationcore_void_return_y", 60);
            rejectBucket = true;
            var failedTick = new EntityTickEvent.Pre(source);
            CoreGameplayEvents.onItemTick(failedTick);
            check(!source.isRemoved() && failedTick.isCanceled(), "rejected bucket output preserves source and prevents void tick");
            var retryTick = new EntityTickEvent.Pre(source);
            CoreGameplayEvents.onItemTick(retryTick);
            check(!source.isRemoved() && retryTick.isCanceled(), "bucket source survives retry cooldown");
            source.getPersistentData().putLong("creationcore_void_retry_at", 0);
            rejectBucket = false;
            CoreGameplayEvents.onItemTick(new EntityTickEvent.Pre(source));
            check(source.isRemoved() && drops(end, new BlockPos(10, 0, 10), ModItems.VOID_BUCKET.get()) == 1,
                    "bucket succeeds once after spawn rejection ends");

            clear(end, new BlockPos(0, 80, 0));
            DragonRitualSavedData state = DragonRitualSavedData.get(end);
            event.getServer().getWorldData().overworldData().setGameTime(1000);
            long now = end.getGameTime();
            BlockPos movedPortal = new BlockPos(20, 75, 20);
            end.setBlock(movedPortal, Blocks.END_PORTAL.defaultBlockState(), 3);
            state.begin(new BlockPos(20, 0, 20), 70.5, 122);
            CoreGameplayEvents.onLevelTick(new LevelTickEvent.Post(() -> true, end));
            check(state.isMoving() && state.targetY() == 84.5, "portal search supports a moved fountain and correct output height");
            end.removeBlock(movedPortal, false);
            state.begin(BlockPos.ZERO, 70.5, 123);
            state.startMovement(now - 80, 80.5);
            rejectMatter = true;
            CoreGameplayEvents.onLevelTick(new LevelTickEvent.Post(() -> true, end));
            check(state.isPending(), "rejected matter output keeps persistent ritual pending");
            rejectMatter = false;
            event.getServer().getWorldData().overworldData().setGameTime(now + 20);
            CoreGameplayEvents.onLevelTick(new LevelTickEvent.Post(() -> true, end));
            check(!state.isPending() && drops(end, new BlockPos(0, 80, 0), ModItems.CREATIVE_MATTER.get()) == 1,
                    "matter output recovers exactly once");
            CoreGameplayEvents.onLevelTick(new LevelTickEvent.Post(() -> true, end));
            check(drops(end, new BlockPos(0, 80, 0), ModItems.CREATIVE_MATTER.get()) == 1, "completed ritual does not duplicate output");
            results.add("ALL GAMEPLAY CHECKS PASSED");
        } catch (Throwable failure) {
            failure.printStackTrace();
            results.add("FAIL: " + failure);
        } finally {
            rejectBucket = rejectMatter = false;
            try { Files.write(Path.of("../build/gameplay-checks.txt"), results); }
            catch (Exception failure) { throw new RuntimeException(failure); }
            event.getServer().halt(false);
        }
    }

    private static BlockDropsEvent blockDrop(ServerLevel level, BlockPos pos, BlockState state,
            net.minecraft.world.entity.Entity breaker, ItemStack tool) {
        return new BlockDropsEvent(level, pos, state, null,
                new ArrayList<>(List.of(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(state.getBlock().asItem())))),
                breaker, tool);
    }
}
