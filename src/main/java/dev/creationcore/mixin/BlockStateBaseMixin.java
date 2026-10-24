package dev.creationcore.mixin;

import dev.creationcore.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Normalizes Mine Craft mining progress for hardness values outside the supported [0, 50] band. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    private static final float NORMALIZED_HARDNESS = 50.0F;
    private static final float CORRECT_TOOL_DIVISOR = 30.0F;

    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void creationcore$mineCraftNormalizesExtremeHardness(Player player, BlockGetter level, BlockPos pos,
                                                                  CallbackInfoReturnable<Float> cir) {
        BlockState state = (BlockState) (Object) this;
        if (!player.getMainHandItem().is(ModItems.MINE_CRAFT.get())) return;

        float hardness = state.getDestroySpeed(level, pos);
        if (hardness >= 0.0F && hardness <= NORMALIZED_HARDNESS) return;

        // Mine Craft is always a valid harvesting tool. Vanilla correct-tool mining progress is
        // playerSpeed / hardness / 30, so an out-of-range block behaves exactly as hardness 50.
        float playerSpeed = Math.max(0.0F, player.getDestroySpeed(state));
        cir.setReturnValue(playerSpeed / NORMALIZED_HARDNESS / CORRECT_TOOL_DIVISOR);
    }
}
