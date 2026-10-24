package dev.creationcore;

import dev.creationcore.registry.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTabs;

public final class CreativeCoreMod implements ModInitializer {
  public static final String MODID = "creationcore";

  @Override
  public void onInitialize() {
    ModBlocks.register();
    ModItems.register();
    ModEntities.register();
    ModMenus.register();
    ModRecipes.register();
    dev.creationcore.particle.ModParticles.register();
    var type = net.minecraft.world.level.block.entity.BlockEntityType.CRAFTER;
    var blocks = new java.util.HashSet<>(type.validBlocks);
    blocks.add(ModBlocks.CREATIVE_CRAFTER.get());
    type.validBlocks = blocks;
    if (FabricLoader.getInstance().isModLoaded("sophisticatedcore"))
      dev.creationcore.compat.sophisticated.SophisticatedIntegration.register();
    ItemGroupEvents.MODIFY_ENTRIES_ALL.register(
        (group, entries) ->
            addCreativeTabContents(
                net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB
                    .getResourceKey(group)
                    .orElse(null),
                entries));
    net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_WORLD_TICK.register(
        level -> {
          dev.creationcore.event.CoreGameplayEvents.onLevelTick(
              new dev.creationcore.event.GameplayHooks.LevelTick(level));
          dev.creationcore.particle.RitualEffects.tick(
              new dev.creationcore.event.GameplayHooks.LevelTick(level));
        });
  }

  private void addCreativeTabContents(
      net.minecraft.resources.ResourceKey<net.minecraft.world.item.CreativeModeTab> tab,
      net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries event) {
    if (tab == CreativeModeTabs.INGREDIENTS) {
      event.accept(ModItems.BLANK_MATTER.get());
      event.accept(ModItems.BASE_MATTER.get());
      event.accept(ModItems.CREATIVE_MATTER.get());
      event.accept(ModItems.CREATIVE_CORE.get());
      event.accept(ModItems.VOID_BUCKET.get());
      event.accept(ModItems.BOTTLED_NOTHING.get());
    }
    if (tab == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
      event.accept(ModItems.CREATIVE_CRAFTING_TABLE.get());
      event.accept(ModItems.CREATIVE_CRAFTER.get());
      for (String name :
          new String[] {
            "backpack_creative_crafting_upgrade", "storage_creative_crafting_upgrade"
          }) {
        net.minecraft.core.registries.BuiltInRegistries.ITEM
            .getOptional(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, name))
            .ifPresent(event::accept);
      }
    }
    if (tab == CreativeModeTabs.COMBAT) {
      // Mine Craft is classified as a sword-like weapon while retaining its universal mining
      // behavior.
      event.accept(ModItems.MINE_CRAFT.get());
    }
    if (tab == CreativeModeTabs.SPAWN_EGGS) {
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
