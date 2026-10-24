package dev.creationcore.check;

import dev.creationcore.registry.ModItems;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class RedstoneChecks {
  private static MinecraftServer server;
  private static int ticks, phase;
  private static boolean done;
  private static final List<String> result = new ArrayList<>();

  private static void command(String line) {
    server
        .getCommands()
        .performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), line);
  }

  private static long count(int x, int y, int z, Item item) {
    var c = (Container) server.overworld().getBlockEntity(new BlockPos(x, y, z));
    long total = 0;
    for (int i = 0; i < c.getContainerSize(); i++)
      if (c.getItem(i).is(item)) total += c.getItem(i).getCount();
    return total;
  }

  private static void check(boolean ok, String message) {
    if (!ok) throw new AssertionError(message);
    result.add("PASS: " + message);
  }

  public static void start(MinecraftServer started) throws Exception {
    server = started;
    for (String line : Files.readAllLines(Path.of("../tools/crafter-smoke-setup.mcfunction")))
      if (!line.isBlank()) command(line);
  }

  public static void tick(MinecraftServer ignored) throws Exception {
    if (done || server == null || ++ticks < 30) return;
    try {
      if (phase == 0) {
        check(
            count(100, 200, 99, ModItems.CAVE_BIOME_SPAWN_EGG.get()) == 1,
            "redstone pulse crafts exclusive biome egg into front chest");
        check(
            ((Container) server.overworld().getBlockEntity(new BlockPos(100, 200, 100))).isEmpty(),
            "exclusive redstone craft consumes exact input batch");
        check(
            ((Container) server.overworld().getBlockEntity(new BlockPos(105, 200, 99))).isEmpty()
                && count(105, 200, 100, Items.EGG) == 1,
            "ordinary redstone crafter cannot execute exclusive recipe");
        check(
            count(110, 200, 99, ModItems.BOTTLED_NOTHING.get()) == 4
                && count(110, 200, 99, Items.BUCKET) == 1,
            "ordinary bottling outputs four bottles and returns empty bucket");
        check(
            count(115, 200, 99, Items.MUSIC_DISC_13) == 2,
            "redstone disc copying returns original and one copy");
        check(
            count(120, 200, 99, Items.BARRIER) == 8,
            "current blank-matter barrier recipe outputs eight barriers");
        check(
            count(125, 200, 99, ModItems.BLANK_MATTER.get()) == 1
                && count(125, 200, 99, Items.GLASS_BOTTLE) == 1,
            "ordinary crafter makes blank matter and returns glass bottle");
        for (String line :
            List.of(
                "setblock 136 200 100 air",
                "setblock 135 200 99 minecraft:chest",
                "setblock 135 200 100 creationcore:creative_crafter[orientation=north_up]",
                "data merge block 135 200 100 {disabled_slots:[I;0,1,2,3,5,6,7,8]}",
                "setblock 135 201 100 minecraft:hopper",
                "data merge block 135 201 100"
                    + " {Items:[{Slot:0b,id:\"minecraft:oak_log\",count:1}]}"))
          command(line);
        phase = 1;
        ticks = 0;
        return;
      }
      if (phase == 1) {
        check(
            count(135, 200, 100, Items.OAK_LOG) == 1, "hopper feeds the only enabled crafter slot");
        command("setblock 136 200 100 minecraft:redstone_block");
        phase = 2;
        ticks = 0;
        return;
      }
      check(
          count(135, 200, 99, Items.OAK_PLANKS) == 4,
          "hopper-fed crafter still outputs four planks after redstone pulse");
      result.add("ALL REDSTONE CHECKS PASSED");
      done = true;
    } catch (Throwable failure) {
      failure.printStackTrace();
      result.add("FAIL: " + failure);
      done = true;
    }
    Files.write(Path.of("../build/redstone-checks.txt"), result);
    server.halt(false);
  }
}
