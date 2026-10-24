package dev.creationcore.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbilities;

import java.util.function.BiConsumer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BaseMatterBlock extends Block {
    private static final VoxelShape OUTLINE = Block.box(4, 4, 4, 12, 12, 12);
    private static final float FIXED_BREAK_PROGRESS = 1.0F / 90.0F; // empty-hand baseline: 4.5 seconds
    private static final float VANILLA_EQUIVALENT_HARDNESS = 4.5F;

    public BaseMatterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext context) {
        return false;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        ItemStack held = player.getMainHandItem();
        if (held.canPerformAction(ItemAbilities.PICKAXE_DIG)) {
            // Any vanilla/modded pickaxe-like item can accelerate Base Matter. Use the normal
            // correct-tool divisor (30) while preserving the original 4.5-second hand baseline.
            float accelerated = Math.max(0.0F, player.getDestroySpeed(state))
                    / (VANILLA_EQUIVALENT_HARDNESS * 30.0F);
            return Math.max(FIXED_BREAK_PROGRESS, accelerated);
        }
        return FIXED_BREAK_PROGRESS;
    }


    @Override
    protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion,
                                  BiConsumer<ItemStack, BlockPos> dropConsumer) {
        // Ritual material is absolutely explosion-proof; do not remove it or emit explosion drops.
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        if (entity instanceof EnderDragon) {
            return false;
        }
        return super.canEntityDestroy(state, level, pos, entity);
    }
}
