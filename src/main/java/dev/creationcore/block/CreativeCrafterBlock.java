package dev.creationcore.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.CrafterBlock;

/** All inventory, redstone, output and orientation behavior remains vanilla. */
public final class CreativeCrafterBlock extends CrafterBlock {
    public static final MapCodec<CrafterBlock> CODEC = simpleCodec(CreativeCrafterBlock::new);

    public CreativeCrafterBlock(Properties properties) { super(properties); }

    @Override protected MapCodec<CrafterBlock> codec() { return CODEC; }
    @Override public void setPlacedBy(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.entity.LivingEntity placer,
            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (net.neoforged.fml.ModList.get().isLoaded("ae2"))
            dev.creationcore.compat.ae2.Ae2Integration.placed(level, pos, placer);
    }
}
