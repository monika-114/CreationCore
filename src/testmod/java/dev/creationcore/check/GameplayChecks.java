package dev.creationcore.check;

import com.mojang.authlib.GameProfile;
import dev.creationcore.data.DragonRitualSavedData;
import dev.creationcore.entity.VoidBucketEntity;
import dev.creationcore.event.CoreGameplayEvents;
import dev.creationcore.menu.CreativeCraftingMenu;
import dev.creationcore.registry.ModBlocks;
import dev.creationcore.registry.ModItems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

/** Opt-in disposable development-world checks. This class is not a production source. */
public final class GameplayChecks {
  private static final List<String> results = new ArrayList<>();
  private static boolean rejectBucket, rejectMatter;
  private static final List<ItemEntity> joined = new ArrayList<>();

  public static boolean reject(net.minecraft.world.entity.Entity entity) {
    return (rejectBucket && entity instanceof VoidBucketEntity)
        || (rejectMatter
            && entity instanceof ItemEntity item
            && item.getItem().is(ModItems.CREATIVE_MATTER.get()));
  }

  public static void accepted(net.minecraft.world.entity.Entity entity) {
    if (entity instanceof ItemEntity item) joined.add(item);
  }

  private static void check(boolean ok, String name) {
    if (!ok) throw new AssertionError(name);
    results.add("PASS: " + name);
    System.out.println("CREATIONCORE_CHECK " + results.getLast());
  }

  private static int drops(ServerLevel level, BlockPos pos, Item item) {
    // At ServerStarted the newly loaded chunk's entity disk callback may not have completed.
    // Observe accepted spawn events directly rather than depending on that async index.
    return joined.stream()
        .filter(
            e ->
                e.level() == level
                    && e.isAlive()
                    && e.getItem().is(item)
                    && new AABB(pos).inflate(6).contains(e.position()))
        .mapToInt(e -> e.getItem().getCount())
        .sum();
  }

  private static void clear(ServerLevel level, BlockPos pos) {
    joined.stream()
        .filter(e -> e.level() == level && new AABB(pos).inflate(8).contains(e.position()))
        .forEach(ItemEntity::discard);
    level
        .getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(8))
        .forEach(ItemEntity::discard);
  }

  public static void started(net.minecraft.server.MinecraftServer server) {
    try {
      ServerLevel level = server.overworld();
      TestPlayer player = new TestPlayer(level, new GameProfile(UUID.randomUUID(), "CoreChecks"));
      player.setGameMode(GameType.SURVIVAL);
      BlockPos pos = new BlockPos(100, 200, 100);
      level.getChunk(pos);
      BlockPos lightPos = pos.offset(-4, 0, 0);
      level.setBlock(
          lightPos,
          Blocks.LIGHT
              .defaultBlockState()
              .setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 1),
          3);
      player.setItemInHand(
          net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.LIGHT, 4));
      var hit =
          new net.minecraft.world.phys.BlockHitResult(
              net.minecraft.world.phys.Vec3.atCenterOf(lightPos), Direction.UP, lightPos, false);
      player.gameMode.useItemOn(
          player,
          level,
          player.getMainHandItem(),
          net.minecraft.world.InteractionHand.MAIN_HAND,
          hit);
      check(
          level.getBlockState(lightPos).getValue(net.minecraft.world.level.block.LightBlock.LEVEL)
                  == 2
              && player.getMainHandItem().getCount() == 4,
          "survival Light item cycles brightness without consuming held item");
      player.setItemInHand(
          net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.MINE_CRAFT.get()));
      level.setBlock(
          lightPos,
          level
              .getBlockState(lightPos)
              .setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 15)
              .setValue(net.minecraft.world.level.block.LightBlock.WATERLOGGED, true),
          3);
      player.gameMode.useItemOn(
          player,
          level,
          player.getMainHandItem(),
          net.minecraft.world.InteractionHand.MAIN_HAND,
          hit);
      check(
          level.getBlockState(lightPos).getValue(net.minecraft.world.level.block.LightBlock.LEVEL)
                  == 0
              && level
                  .getBlockState(lightPos)
                  .getValue(net.minecraft.world.level.block.LightBlock.WATERLOGGED),
          "survival Mine Craft wraps brightness fifteen to zero while preserving waterlogged"
              + " state");
      player.setGameMode(GameType.ADVENTURE);
      player.gameMode.useItemOn(
          player,
          level,
          player.getMainHandItem(),
          net.minecraft.world.InteractionHand.MAIN_HAND,
          hit);
      check(
          level.getBlockState(lightPos).getValue(net.minecraft.world.level.block.LightBlock.LEVEL)
              == 0,
          "adventure permissions are not bypassed by brightness interaction");
      player.setGameMode(GameType.SURVIVAL);
      player.getInventory().clearContent();
      player.setPos(100.5, 200, 102.5);
      level.setBlock(pos, ModBlocks.CREATIVE_CRAFTING_TABLE.get().defaultBlockState(), 3);
      var menu =
          new CreativeCraftingMenu(
              1, player.getInventory(), ContainerLevelAccess.create(level, pos));
      for (int i = 0; i < 36; i++)
        player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
      player.getInventory().setItem(0, new ItemStack(Items.INFESTED_STONE, 63));
      for (int i = 0; i < 9; i++)
        menu.getSlot(i + 1).set(new ItemStack(i == 4 ? Items.SILVERFISH_SPAWN_EGG : Items.STONE));
      check(menu.getSlot(0).getItem().getCount() == 8, "exclusive recipe previews eight outputs");
      clear(level, pos);
      menu.quickMoveStack(player, 0);
      check(
          player.getInventory().getItem(0).getCount() == 64
              && drops(level, pos, Items.INFESTED_STONE) == 7,
          "partial Shift craft conserves all eight outputs (one stored, seven dropped)");
      check(
          java.util.stream.IntStream.rangeClosed(1, 9).allMatch(i -> !menu.getSlot(i).hasItem()),
          "partial Shift craft consumes exactly one input batch");
      check(
          !menu.canTakeItemForPickAll(ItemStack.EMPTY, menu.getSlot(0)),
          "double-click collection excludes result slot");

      clear(level, pos);
      player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 63));
      menu.getSlot(1).set(new ItemStack(Items.OAK_LOG));
      menu.quickMoveStack(player, 0);
      check(
          player.getInventory().getItem(0).getCount() == 64
              && drops(level, pos, Items.OAK_PLANKS) == 3,
          "partial ordinary craft conserves four planks");

      player.getInventory().clearContent();
      player.getInventory().setItem(0, new ItemStack(ModItems.MINE_CRAFT.get()));
      player.getInventory().selected = 0;
      BlockPos foot = pos.offset(4, 0, 0);
      BlockPos head = foot.north();
      level.setBlock(foot.below(), Blocks.STONE.defaultBlockState(), 3);
      level.setBlock(head.below(), Blocks.STONE.defaultBlockState(), 3);
      BlockState footState =
          Blocks.RED_BED
              .defaultBlockState()
              .setValue(BedBlock.FACING, Direction.NORTH)
              .setValue(BedBlock.PART, BedPart.FOOT);
      BlockState headState = footState.setValue(BedBlock.PART, BedPart.HEAD);
      level.setBlock(foot, footState, 3);
      level.setBlock(head, headState, 3);
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
      TestPlayer other = new TestPlayer(level, new GameProfile(UUID.randomUUID(), "OtherChecks"));
      var otherDrop = blockDrop(level, head, headState, other, ItemStack.EMPTY);
      CoreGameplayEvents.onMineCraftBlockDrops(otherDrop);
      check(otherDrop.getDrops().size() == 1, "another player's drop is not suppressed");

      BlockPos door = pos.offset(8, 0, 0);
      level.setBlock(door.below(), Blocks.STONE.defaultBlockState(), 3);
      BlockState lower =
          Blocks.OAK_DOOR
              .defaultBlockState()
              .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
      level.setBlock(door, lower, 3);
      level.setBlock(
          door.above(),
          lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER),
          3);
      clear(level, door);
      check(player.gameMode.destroyBlock(door), "Mine Craft breaks door");
      check(drops(level, door, Items.OAK_DOOR) == 1, "door produces one item");

      ServerLevel end = server.getLevel(Level.END);
      end.getChunk(0, 0);
      clear(end, new BlockPos(10, 0, 10));
      ItemEntity source = new ItemEntity(end, 10, -20, 10, new ItemStack(Items.BUCKET));
      dev.creationcore.event.PersistentData.get(source).putInt("creationcore_void_threshold", -10);
      dev.creationcore.event.PersistentData.get(source).putDouble("creationcore_void_return_y", 60);
      rejectBucket = true;
      var failedTick = new dev.creationcore.event.GameplayHooks.EntityTick(source);
      CoreGameplayEvents.onItemTick(failedTick);
      check(
          !source.isRemoved() && failedTick.canceled,
          "rejected bucket output preserves source and prevents void tick");
      var retryTick = new dev.creationcore.event.GameplayHooks.EntityTick(source);
      CoreGameplayEvents.onItemTick(retryTick);
      check(!source.isRemoved() && retryTick.canceled, "bucket source survives retry cooldown");
      dev.creationcore.event.PersistentData.get(source).putLong("creationcore_void_retry_at", 0);
      rejectBucket = false;
      CoreGameplayEvents.onItemTick(new dev.creationcore.event.GameplayHooks.EntityTick(source));
      check(
          source.isRemoved()
              && drops(end, new BlockPos(10, 0, 10), ModItems.VOID_BUCKET.get()) == 1,
          "bucket succeeds once after spawn rejection ends");

      clear(end, new BlockPos(0, 80, 0));
      DragonRitualSavedData state = DragonRitualSavedData.get(end);
      server.getWorldData().overworldData().setGameTime(1000);
      long now = end.getGameTime();
      BlockPos movedPortal = new BlockPos(20, 75, 20);
      end.setBlock(movedPortal, Blocks.END_PORTAL.defaultBlockState(), 3);
      state.begin(new BlockPos(20, 0, 20), 70.5, 122);
      CoreGameplayEvents.onLevelTick(new dev.creationcore.event.GameplayHooks.LevelTick(end));
      check(
          state.isMoving() && state.targetY() == 84.5,
          "portal search supports a moved fountain and correct output height");
      end.removeBlock(movedPortal, false);
      state.begin(BlockPos.ZERO, 70.5, 123);
      state.startMovement(now - 80, 80.5);
      rejectMatter = true;
      CoreGameplayEvents.onLevelTick(new dev.creationcore.event.GameplayHooks.LevelTick(end));
      check(state.isPending(), "rejected matter output keeps persistent ritual pending");
      rejectMatter = false;
      server.getWorldData().overworldData().setGameTime(now + 20);
      CoreGameplayEvents.onLevelTick(new dev.creationcore.event.GameplayHooks.LevelTick(end));
      check(
          !state.isPending()
              && drops(end, new BlockPos(0, 80, 0), ModItems.CREATIVE_MATTER.get()) == 1,
          "matter output recovers exactly once");
      CoreGameplayEvents.onLevelTick(new dev.creationcore.event.GameplayHooks.LevelTick(end));
      check(
          drops(end, new BlockPos(0, 80, 0), ModItems.CREATIVE_MATTER.get()) == 1,
          "completed ritual does not duplicate output");
      // Exercise real Fabric mixin hooks, persistence and tool rules.
      BlockPos ritualPos = level.getSharedSpawnPos().above(80);
      level.getChunk(ritualPos);
      clear(level, ritualPos);
      var wither = net.minecraft.world.entity.EntityType.WITHER.create(level);
      wither.setPos(ritualPos.getX() + 0.5, ritualPos.getY(), ritualPos.getZ() + 0.5);
      var blank =
          new ItemEntity(
              level,
              wither.getX(),
              wither.getY(),
              wither.getZ() + 0.5,
              new ItemStack(ModItems.BLANK_MATTER.get(), 3));
      level.addFreshEntity(blank);
      level.explode(
          wither, wither.getX(), wither.getY(), wither.getZ(), 7, Level.ExplosionInteraction.MOB);
      check(
          blank.isRemoved()
              && dev.creationcore.event.PersistentData.get(wither)
                  .getBoolean("creationcore_wither_charged"),
          "real explosion consumes blank matter and marks wither");
      net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
      wither.saveWithoutId(saved);
      var restored = net.minecraft.world.entity.EntityType.WITHER.create(level);
      restored.load(saved);
      check(
          dev.creationcore.event.PersistentData.get(restored)
              .getBoolean("creationcore_wither_charged"),
          "Fabric entity data survives save and reload");
      wither.die(level.damageSources().generic());
      check(
          drops(level, ritualPos, ModItems.BASE_MATTER.get()) == 1
              && drops(level, ritualPos, Items.NETHER_STAR) == 0,
          "charged wither drops exactly one base matter and no nether star");
      clear(level, ritualPos);
      var tool = new ItemStack(ModItems.MINE_CRAFT.get());
      tool.setDamageValue(100);
      check(tool.getDamageValue() == 0, "Mine Craft durability damage is suppressed");
      check(
          !tool.getItem().isValidRepairItem(tool, new ItemStack(Items.NETHERITE_INGOT)),
          "Mine Craft rejects repair materials");
      for (var key :
          List.of(
              net.minecraft.world.item.enchantment.Enchantments.UNBREAKING,
              net.minecraft.world.item.enchantment.Enchantments.MENDING,
              net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,
              net.minecraft.world.item.enchantment.Enchantments.FORTUNE)) {
        var holder =
            level
                .registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(key);
        check(
            !holder.value().canEnchant(tool) && !holder.value().isPrimaryItem(tool),
            "blocked enchantment rejected: " + key.location());
        tool.enchant(holder, 3);
        check(
            net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                    holder, tool)
                == 0,
            "forced blocked enchantment has zero effective level: " + key.location());
      }
      var dragon = net.minecraft.world.entity.EntityType.ENDER_DRAGON.create(end);
      var origin = dragon.getFightOrigin();
      var material = new BlockPos(origin.getX(), 230, origin.getZ());
      end.getChunk(material);
      end.setBlock(material, ModBlocks.BASE_MATTER.get().defaultBlockState(), 3);
      var death =
          net.minecraft.world.entity.boss.enderdragon.EnderDragon.class.getDeclaredMethod(
              "tickDeath");
      death.setAccessible(true);
      death.invoke(dragon);
      check(
          end.getBlockState(material).isAir() && DragonRitualSavedData.get(end).isPending(),
          "real dragon death tick consumes base matter and schedules ritual");
      end.setBlock(material, ModBlocks.BASE_MATTER.get().defaultBlockState(), 3);
      death.invoke(dragon);
      check(
          end.getBlockState(material).is(ModBlocks.BASE_MATTER.get()),
          "dragon death animation cannot trigger ritual twice");
      var arrivals = dev.creationcore.data.CoreArrivalSavedData.get(level);
      int before =
          arrivals
              .save(new net.minecraft.nbt.CompoundTag(), level.registryAccess())
              .getList("arrivals", 10)
              .size();
      var box = new ItemStack(Items.SHULKER_BOX);
      box.set(
          net.minecraft.core.component.DataComponents.CONTAINER,
          net.minecraft.world.item.component.ItemContainerContents.fromItems(
              List.of(new ItemStack(ModItems.CREATIVE_MATTER.get()))));
      var portalPos = new BlockPos(40, 75, 40);
      end.getChunk(portalPos);
      end.setBlock(portalPos, Blocks.END_PORTAL.defaultBlockState(), 3);
      var sacrifice = new ItemEntity(end, 40.5, 75.1, 40.5, box);
      var portal =
          net.minecraft.world.level.block.EndPortalBlock.class.getDeclaredMethod(
              "entityInside",
              BlockState.class,
              Level.class,
              BlockPos.class,
              net.minecraft.world.entity.Entity.class);
      portal.setAccessible(true);
      portal.invoke(Blocks.END_PORTAL, end.getBlockState(portalPos), end, portalPos, sacrifice);
      check(
          sacrifice.isRemoved()
              && arrivals
                      .save(new net.minecraft.nbt.CompoundTag(), level.registryAccess())
                      .getList("arrivals", 10)
                      .size()
                  == before + 1,
          "real End portal hook consumes shulker and schedules core arrival");
      check(
          level
              .getRecipeManager()
              .byKey(
                  net.minecraft.resources.ResourceLocation.parse(
                      "creationcore:wandering_trader_spawn_egg"))
              .isPresent(),
          "component-sensitive potion recipe loads on Fabric");
      var trader =
          level
              .getRecipeManager()
              .byKey(
                  net.minecraft.resources.ResourceLocation.parse(
                      "creationcore:wandering_trader_spawn_egg"))
              .orElseThrow()
              .value();
      var potion = new ItemStack(Items.POTION);
      potion.set(
          net.minecraft.core.component.DataComponents.POTION_CONTENTS,
          new net.minecraft.world.item.alchemy.PotionContents(
              net.minecraft.world.item.alchemy.Potions.INVISIBILITY));
      check(
          trader.getIngredients().get(1).test(potion)
              && !trader.getIngredients().get(1).test(new ItemStack(Items.POTION)),
          "Fabric component ingredient requires invisibility potion");
      var buffer =
          new net.minecraft.network.RegistryFriendlyByteBuf(
              io.netty.buffer.Unpooled.buffer(), level.registryAccess());
      net.minecraft.world.item.crafting.Recipe.STREAM_CODEC.encode(buffer, trader);
      var synced = net.minecraft.world.item.crafting.Recipe.STREAM_CODEC.decode(buffer);
      buffer.release();
      check(
          synced.getIngredients().get(1).test(potion),
          "component-sensitive creative recipe survives network synchronization");
      results.add("ALL GAMEPLAY CHECKS PASSED");
    } catch (Throwable failure) {
      failure.printStackTrace();
      results.add("FAIL: " + failure);
    } finally {
      rejectBucket = rejectMatter = false;
      try {
        Files.write(Path.of("../build/gameplay-checks.txt"), results);
      } catch (Exception failure) {
        throw new RuntimeException(failure);
      }
      server.halt(false);
    }
  }

  private static dev.creationcore.event.GameplayHooks.BlockDrops blockDrop(
      ServerLevel level,
      BlockPos pos,
      BlockState state,
      net.minecraft.world.entity.Entity breaker,
      ItemStack tool) {
    return new dev.creationcore.event.GameplayHooks.BlockDrops(
        level,
        pos,
        state,
        null,
        breaker,
        tool,
        new ArrayList<>(
            List.of(
                new ItemEntity(
                    level,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    new ItemStack(state.getBlock().asItem())))));
  }
}
