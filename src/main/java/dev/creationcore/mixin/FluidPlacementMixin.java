package dev.creationcore.mixin;

import dev.creationcore.fluid.VoidFluidEffects;
import dev.creationcore.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidUtil.class, remap = false)
public abstract class FluidPlacementMixin {
    @Inject(method = "tryPlaceFluid(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/core/BlockPos;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;Lnet/neoforged/neoforge/fluids/FluidStack;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void creationcore$disperse(Player player, Level level, InteractionHand hand, BlockPos pos,
                                             IFluidHandler source, FluidStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.isEmpty() || !ModFluids.isVoid(stack.getFluid())) return;
        if (level == null || pos == null || source == null || level.isClientSide || !level.hasChunkAt(pos)
                || !level.getWorldBorder().isWithinBounds(pos)) {
            cir.setReturnValue(false);
            return;
        }
        FluidStack available = source.drain(stack, IFluidHandler.FluidAction.SIMULATE);
        if (available.isEmpty() || !FluidStack.isSameFluidSameComponents(available, stack)) {
            cir.setReturnValue(false);
            return;
        }
        FluidStack drained = source.drain(available, IFluidHandler.FluidAction.EXECUTE);
        if (!drained.isEmpty()) VoidFluidEffects.disperse(level, pos);
        cir.setReturnValue(!drained.isEmpty());
    }
}
