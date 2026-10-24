package dev.creationcore.block;

import dev.creationcore.menu.CreativeCraftingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class CreativeCraftingTableBlock extends CraftingTableBlock implements net.minecraft.world.level.block.EntityBlock {
    private static final Component TITLE = Component.translatable("container.creationcore.creative_crafting_table");

    public CreativeCraftingTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return net.neoforged.fml.ModList.get().isLoaded("ae2")
                ? dev.creationcore.compat.ae2.Ae2Integration.createTable(pos, state) : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (net.neoforged.fml.ModList.get().isLoaded("ae2")) {
            dev.creationcore.compat.ae2.Ae2Integration.placed(level, pos, placer);
        }
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
                (containerId, inventory, player) -> new CreativeCraftingMenu(
                        containerId,
                        inventory,
                        ContainerLevelAccess.create(level, pos)
                ),
                TITLE
        );
    }
}
