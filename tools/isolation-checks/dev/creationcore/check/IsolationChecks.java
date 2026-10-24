package dev.creationcore.check;

import com.mojang.authlib.GameProfile;
import dev.creationcore.menu.CreativeCraftingMenu;
import dev.creationcore.registry.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = "creationcore")
public final class IsolationChecks {
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        var result = new ArrayList<String>();
        try {
            for (String target : List.of("backpack", "storage")) {
                String mod = target.equals("backpack") ? "sophisticatedbackpacks" : "sophisticatedstorage";
                boolean expected = ModList.get().isLoaded(mod);
                boolean present = BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("creationcore:" + target + "_creative_crafting_upgrade"));
                if (present != expected) throw new AssertionError("wrong registration for " + target);
                result.add("PASS: " + target + " upgrade registered exactly when its own mod is installed");
            }
            var level = event.getServer().overworld();
            var pos = new BlockPos(210, 200, 210);
            level.getChunk(pos);
            level.setBlock(pos, ModBlocks.CREATIVE_CRAFTING_TABLE.get().defaultBlockState(), 3);
            var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "IsolationCheck"));
            var menu = new CreativeCraftingMenu(1, player.getInventory(), ContainerLevelAccess.create(level, pos));
            menu.getSlot(1).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TURTLE_EGG));
            if (!menu.getSlot(0).getItem().is(net.minecraft.world.item.Items.TURTLE_SPAWN_EGG)) throw new AssertionError("base recipe unavailable");
            result.add("PASS: base creative worktable and updated turtle recipe work in this mod combination");
            if (ModList.get().isLoaded("sophisticatedcore")) {
                Class.forName("net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer");
                Class.forName("net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer$2");
            }
            if (ModList.get().isLoaded("toms_storage")) {
                Class.forName("com.tom.storagemod.block.entity.CraftingTerminalBlockEntity");
                Class.forName("com.tom.storagemod.menu.CraftingTerminalMenu");
            }
            if (ModList.get().isLoaded("beyonddimensions")) {
                Class.forName("com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu");
                Class.forName("com.wintercogs.beyonddimensions.common.menu.widget.slot.AutoRefillResultSlot");
            }
            result.add("PASS: installed integrations link successfully without any missing optional mod classes");
            result.add("ALL ISOLATION CHECKS PASSED");
        } catch (Throwable failure) { failure.printStackTrace(); result.add("FAIL: " + failure); }
        try { Files.write(Path.of("../build/isolation-checks.txt"), result); }
        catch (Exception failure) { throw new RuntimeException(failure); }
        event.getServer().halt(false);
    }
}
