package dev.creationcore.compat.sophisticated;

import dev.creationcore.registry.Registrations;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;

public final class SophisticatedIntegration {
  public static void register() {
    if (FabricLoader.getInstance().isModLoaded("sophisticatedbackpacks")) registerBackpack();
    if (FabricLoader.getInstance().isModLoaded("sophisticatedstorage")) registerStorage();
  }

  private static void registerBackpack() {
    var items = new Registrations.Items();
    var upgrade =
        items.register(
            "backpack_creative_crafting_upgrade",
            () ->
                new CreativeCraftingUpgradeItem(
                    "sophisticatedbackpacks",
                    net.p3pp3rf1y.sophisticatedbackpacks.Config.SERVER.maxUpgradesPerStorage));
    items.register();
    UpgradeContainerRegistry.register(
        upgrade.getId(), net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.CRAFTING_TYPE);
    addTab("sophisticatedbackpacks", upgrade.get());
  }

  private static void registerStorage() {
    var items = new Registrations.Items();
    var upgrade =
        items.register(
            "storage_creative_crafting_upgrade",
            () ->
                new CreativeCraftingUpgradeItem(
                    "sophisticatedstorage",
                    net.p3pp3rf1y.sophisticatedstorage.Config.SERVER.maxUpgradesPerStorage));
    items.register();
    UpgradeContainerRegistry.register(
        upgrade.getId(), net.p3pp3rf1y.sophisticatedstorage.init.ModItems.CRAFTING_TYPE);
    addTab("sophisticatedstorage", upgrade.get());
  }

  private static void addTab(String owner, net.minecraft.world.item.Item item) {
    ItemGroupEvents.MODIFY_ENTRIES_ALL.register(
        (tab, entries) -> {
          if (owner.equals(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).getNamespace()))
            entries.accept(item);
        });
  }
}
