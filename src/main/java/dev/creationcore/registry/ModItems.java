package dev.creationcore.registry;

import dev.creationcore.item.MineCraftItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

public final class ModItems {
  public static final Registrations.Items ITEMS = new Registrations.Items();

  public static final Registrations.Entry<Item> BLANK_MATTER =
      ITEMS.registerSimpleItem("blank_matter", new Item.Properties());

  public static final Registrations.Entry<BlockItem> BASE_MATTER =
      ITEMS.register(
          "base_matter", () -> new BlockItem(ModBlocks.BASE_MATTER.get(), new Item.Properties()));

  public static final Registrations.Entry<Item> CREATIVE_MATTER =
      ITEMS.registerSimpleItem("creative_matter", new Item.Properties());
  public static final Registrations.Entry<Item> CREATIVE_CORE =
      ITEMS.registerSimpleItem("creative_core", new Item.Properties().stacksTo(16));

  // Intentionally a plain Item rather than BucketItem: it has no liquid/cauldron behavior.
  public static final Registrations.Entry<Item> VOID_BUCKET =
      ITEMS.registerSimpleItem(
          "void_bucket", new Item.Properties().stacksTo(16).craftRemainder(Items.BUCKET));
  public static final Registrations.Entry<Item> BOTTLED_NOTHING =
      ITEMS.registerSimpleItem(
          "bottled_nothing", new Item.Properties().craftRemainder(Items.GLASS_BOTTLE));

  /** Universal end-game sword-class tool; gameplay durability loss is fully suppressed. */
  public static final Registrations.Entry<MineCraftItem> MINE_CRAFT =
      ITEMS.register(
          "mine_craft",
          () ->
              new MineCraftItem(
                  new Item.Properties()
                      .stacksTo(1)
                      .fireResistant()
                      // Tooltip target: 10 attack damage, 1.6 attack speed.
                      .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 5, -2.4F))));

  // Biome-themed spawn-egg items. They are plain items for now; behavior can be added later.
  public static final Registrations.Entry<Item> CAVE_BIOME_SPAWN_EGG =
      ITEMS.registerSimpleItem("cave_biome_spawn_egg", new Item.Properties());
  public static final Registrations.Entry<Item> ARID_BIOME_SPAWN_EGG =
      ITEMS.registerSimpleItem("arid_biome_spawn_egg", new Item.Properties());
  public static final Registrations.Entry<Item> OCEAN_BIOME_SPAWN_EGG =
      ITEMS.registerSimpleItem("ocean_biome_spawn_egg", new Item.Properties());
  public static final Registrations.Entry<Item> PLAINS_BIOME_SPAWN_EGG =
      ITEMS.registerSimpleItem("plains_biome_spawn_egg", new Item.Properties());
  public static final Registrations.Entry<Item> FOREST_BIOME_SPAWN_EGG =
      ITEMS.registerSimpleItem("forest_biome_spawn_egg", new Item.Properties());
  public static final Registrations.Entry<Item> MOUNTAIN_BIOME_SPAWN_EGG =
      ITEMS.registerSimpleItem("mountain_biome_spawn_egg", new Item.Properties());
  public static final Registrations.Entry<Item> WETLAND_BIOME_SPAWN_EGG =
      ITEMS.registerSimpleItem("wetland_biome_spawn_egg", new Item.Properties());
  public static final Registrations.Entry<Item> NETHER_BIOME_SPAWN_EGG =
      ITEMS.registerSimpleItem("nether_biome_spawn_egg", new Item.Properties());
  public static final Registrations.Entry<Item> END_BIOME_SPAWN_EGG =
      ITEMS.registerSimpleItem("end_biome_spawn_egg", new Item.Properties());

  public static final Registrations.Entry<BlockItem> CREATIVE_CRAFTING_TABLE =
      ITEMS.register(
          "creative_crafting_table",
          () -> new BlockItem(ModBlocks.CREATIVE_CRAFTING_TABLE.get(), new Item.Properties()));

  public static final Registrations.Entry<BlockItem> CREATIVE_CRAFTER =
      ITEMS.register(
          "creative_crafter",
          () -> new BlockItem(ModBlocks.CREATIVE_CRAFTER.get(), new Item.Properties()));

  private ModItems() {}

  public static void register() {
    ITEMS.register();
  }
}
