package dev.creationcore;

import dev.creationcore.registry.ModBlocks;
import dev.creationcore.registry.ModItems;
import dev.creationcore.registry.ModEntities;
import dev.creationcore.registry.ModMenus;
import dev.creationcore.registry.ModRecipes;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(CreativeCoreMod.MODID)
public final class CreativeCoreMod {
    public static final String MODID = "creationcore";

    public CreativeCoreMod(IEventBus modBus) {
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        if (net.neoforged.fml.ModList.get().isLoaded("sophisticatedcore")) {
            dev.creationcore.compat.sophisticated.SophisticatedIntegration.register(modBus);
        }
        ModEntities.register(modBus);
        ModMenus.register(modBus);
        ModRecipes.register(modBus);
        dev.creationcore.registry.ModFluids.register(modBus);
        if (net.neoforged.fml.ModList.get().isLoaded("create")) {
            dev.creationcore.compat.create.CreateIntegration.register(modBus);
        }
        if (net.neoforged.fml.ModList.get().isLoaded("ae2")) {
            dev.creationcore.compat.ae2.Ae2Integration.register(modBus);
        }
        dev.creationcore.particle.ModParticles.register(modBus);
        modBus.addListener(this::addCrafterBlock);
        modBus.addListener(this::addCreativeTabContents);
    }

    private void addCrafterBlock(net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent event) {
        event.modify(net.minecraft.world.level.block.entity.BlockEntityType.CRAFTER, ModBlocks.CREATIVE_CRAFTER.get());
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.BLANK_MATTER.get());
            event.accept(ModItems.BASE_MATTER.get());
            event.accept(ModItems.CREATIVE_MATTER.get());
            event.accept(ModItems.CREATIVE_CORE.get());
            event.accept(ModItems.VOID_BUCKET.get());
            event.accept(ModItems.BOTTLED_NOTHING.get());
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.CREATIVE_CRAFTING_TABLE.get());
            event.accept(ModItems.CREATIVE_CRAFTER.get());
            for (String name : new String[]{"backpack_creative_crafting_upgrade", "storage_creative_crafting_upgrade"}) {
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, name)).ifPresent(event::accept);
            }
        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            // Mine Craft is classified as a sword-like weapon while retaining its universal mining behavior.
            event.accept(ModItems.MINE_CRAFT.get());
        }
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.CAVE_BIOME_SPAWN_EGG.get());
            event.accept(ModItems.ARID_BIOME_SPAWN_EGG.get());
            event.accept(ModItems.OCEAN_BIOME_SPAWN_EGG.get());
            event.accept(ModItems.PLAINS_BIOME_SPAWN_EGG.get());
            event.accept(ModItems.FOREST_BIOME_SPAWN_EGG.get());
            event.accept(ModItems.MOUNTAIN_BIOME_SPAWN_EGG.get());
            event.accept(ModItems.WETLAND_BIOME_SPAWN_EGG.get());
            event.accept(ModItems.NETHER_BIOME_SPAWN_EGG.get());
            event.accept(ModItems.END_BIOME_SPAWN_EGG.get());
        }
    }
}
