package dev.creationcore.compat.ae2.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.blockentity.crafting.MolecularAssemblerBlockEntity;
import appeng.util.inv.AppEngInternalInventory;
import dev.creationcore.compat.ae2.CreativeNetworkGate;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MolecularAssemblerBlockEntity.class, remap = false)
public abstract class MolecularAssemblerMixin {
    @Shadow private IMolecularAssemblerSupportedPattern myPlan;
    @Shadow @Final private AppEngInternalInventory gridInv;
    @Shadow private boolean reboot;

    @Inject(method = "pushPattern", at = @At("HEAD"), cancellable = true)
    private void creationcore$accept(IPatternDetails pattern, KeyCounter[] inputs, Direction from,
                                      CallbackInfoReturnable<Boolean> cir) {
        var assembler = (MolecularAssemblerBlockEntity)(Object)this;
        if (CreativeNetworkGate.requiresTable(pattern)
                && !CreativeNetworkGate.authorized(assembler.getMainNode().getNode())) cir.setReturnValue(false);
    }

    @Inject(method = "tickingRequest", at = @At("HEAD"), cancellable = true)
    private void creationcore$run(IGridNode node, int elapsed, CallbackInfoReturnable<TickRateModulation> cir) {
        if (gridInv.getStackInSlot(9).isEmpty() && CreativeNetworkGate.requiresTable(myPlan)
                && !CreativeNetworkGate.authorized(node)) {
            // Preserve ingredients and progress; elapsed offline time must not accelerate the next craft.
            reboot = true;
            cir.setReturnValue(TickRateModulation.IDLE);
        }
    }
}
