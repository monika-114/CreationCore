package dev.creationcore.registry;

import dev.creationcore.block.BaseMatterBlock;
import dev.creationcore.block.CreativeCrafterBlock;
import dev.creationcore.block.CreativeCraftingTableBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
  public static final Registrations.Blocks BLOCKS = new Registrations.Blocks();

  public static final Registrations.Entry<BaseMatterBlock> BASE_MATTER =
      BLOCKS.registerBlock(
          "base_matter",
          BaseMatterBlock::new,
          BlockBehaviour.Properties.of()
              .strength(4.5F, 3_600_000F)
              .sound(SoundType.AMETHYST)
              .noOcclusion()
              .noCollission()
              .pushReaction(PushReaction.BLOCK)
              .lightLevel(state -> 6));

  public static final Registrations.Entry<CreativeCraftingTableBlock> CREATIVE_CRAFTING_TABLE =
      BLOCKS.registerBlock(
          "creative_crafting_table",
          CreativeCraftingTableBlock::new,
          BlockBehaviour.Properties.of().strength(2.5F, 12.0F).sound(SoundType.WOOD));

  public static final Registrations.Entry<CreativeCrafterBlock> CREATIVE_CRAFTER =
      BLOCKS.registerBlock(
          "creative_crafter",
          CreativeCrafterBlock::new,
          BlockBehaviour.Properties.of().strength(2.5F, 12.0F).sound(SoundType.METAL));

  private ModBlocks() {}

  public static void register() {
    BLOCKS.register();
  }
}
