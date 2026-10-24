package dev.creationcore.compat.sophisticated;

import dev.creationcore.CreativeCoreMod;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;

public final class SophisticatedIntegration {
    public static void register(IEventBus bus) {
        var items = DeferredRegister.createItems(CreativeCoreMod.MODID);
        if (ModList.get().isLoaded("sophisticatedbackpacks")) {
            var upgrade = items.register("backpack_creative_crafting_upgrade", () -> new CreativeCraftingUpgradeItem(
                    "sophisticatedbackpacks", net.p3pp3rf1y.sophisticatedbackpacks.Config.SERVER.maxUpgradesPerStorage));
            bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> UpgradeContainerRegistry.register(upgrade.getId(),
                    net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.CRAFTING_TYPE)));
            bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
                if (event.getTab() == net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.CREATIVE_TAB.get()) event.accept(upgrade);
            });
        }
        if (ModList.get().isLoaded("sophisticatedstorage")) {
            var upgrade = items.register("storage_creative_crafting_upgrade", () -> new CreativeCraftingUpgradeItem(
                    "sophisticatedstorage", net.p3pp3rf1y.sophisticatedstorage.Config.SERVER.maxUpgradesPerStorage));
            bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> UpgradeContainerRegistry.register(upgrade.getId(),
                    net.p3pp3rf1y.sophisticatedstorage.init.ModItems.CRAFTING_TYPE)));
            bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
                if (event.getTab() == net.p3pp3rf1y.sophisticatedstorage.init.ModItems.CREATIVE_TAB.get()) event.accept(upgrade);
            });
        }
        items.register(bus);
    }
    private SophisticatedIntegration() {}
}
