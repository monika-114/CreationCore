package dev.creationcore.compat.create.mixin;

import com.simibubi.create.content.fluids.hosePulley.HosePulleyFluidHandler;
import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import dev.creationcore.fluid.VoidFluidEffects;
import dev.creationcore.registry.ModFluids;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = HosePulleyFluidHandler.class, remap = false)
public abstract class HosePulleyFluidMixin {
    @Shadow private SmartFluidTank internalTank;
    @Shadow private FluidDrainingBehaviour drainer;
    @Shadow private Supplier<BlockPos> rootPosGetter;
    @Shadow private Supplier<Boolean> predicate;

    @Unique private boolean creationcore$inVoid() {
        Level level = drainer.getWorld();
        return level != null && !level.isClientSide && predicate.get()
                && rootPosGetter.get().getY() < level.getMinBuildHeight();
    }
    @Inject(method = "getFluidInTank", at = @At("HEAD"), cancellable = true)
    private void creationcore$visibleSource(int tank, CallbackInfoReturnable<FluidStack> cir) {
        if (tank == 0 && internalTank.isEmpty() && creationcore$inVoid())
            cir.setReturnValue(new FluidStack(ModFluids.VOID.get(), 1000));
    }
    @Inject(method = "drainInternal", at = @At("HEAD"), cancellable = true)
    private void creationcore$draw(int maxDrain, FluidStack requested, FluidAction action, CallbackInfoReturnable<FluidStack> cir) {
        if (!creationcore$inVoid()) return;
        if (maxDrain <= 0 || (requested != null && !ModFluids.isVoid(requested.getFluid()))) {
            if (internalTank.isEmpty()) cir.setReturnValue(FluidStack.EMPTY);
            return;
        }
        // Drain any existing contents normally first: never mix with or erase a prior fluid.
        if (!internalTank.isEmpty()) return;
        FluidStack available = new FluidStack(ModFluids.VOID.get(), Math.min(maxDrain, 1000));
        if (requested != null && !FluidStack.isSameFluidSameComponents(available, requested))
            available = FluidStack.EMPTY;
        cir.setReturnValue(available);
    }
    @Inject(method = "fill", at = @At("HEAD"), cancellable = true)
    private void creationcore$discard(FluidStack stack, FluidAction action, CallbackInfoReturnable<Integer> cir) {
        if (stack.isEmpty() || !ModFluids.isVoid(stack.getFluid())) return;
        Level level = drainer.getWorld();
        if (level == null || !predicate.get() || (!internalTank.isEmpty()
                && !FluidStack.isSameFluidSameComponents(internalTank.getFluid(), stack))) {
            cir.setReturnValue(0);
            return;
        }
        int accepted = Math.min(stack.getAmount(), 1000);
        if (action.execute()) VoidFluidEffects.disperse(level, rootPosGetter.get());
        cir.setReturnValue(accepted);
    }
}
