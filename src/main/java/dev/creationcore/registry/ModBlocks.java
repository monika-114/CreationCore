package dev.creationcore.registry;

import dev.creationcore.CreativeCoreMod;
import dev.creationcore.block.BaseMatterBlock;
import dev.creationcore.block.CreativeCraftingTableBlock;
import dev.creationcore.block.CreativeCrafterBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreativeCoreMod.MODID);

    public static final DeferredBlock<BaseMatterBlock> BASE_MATTER = BLOCKS.registerBlock(
            "base_matter",
            BaseMatterBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(4.5F, 3_600_000F)
                    .sound(SoundType.AMETHYST)
                    .noOcclusion()
                    .noCollission()
                    .pushReaction(PushReaction.BLOCK)
                    .lightLevel(state -> 6)
    );

    public static final DeferredBlock<CreativeCraftingTableBlock> CREATIVE_CRAFTING_TABLE = BLOCKS.registerBlock(
            "creative_crafting_table",
            CreativeCraftingTableBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(2.5F, 12.0F)
                    .sound(SoundType.WOOD)
    );

    public static final DeferredBlock<CreativeCrafterBlock> CREATIVE_CRAFTER = BLOCKS.registerBlock(
            "creative_crafter", CreativeCrafterBlock::new,
            BlockBehaviour.Properties.of().strength(2.5F, 12.0F).sound(SoundType.METAL));

    private ModBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
