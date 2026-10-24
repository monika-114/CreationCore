package dev.creationcore.compat.ae2.mixin;

import appeng.api.networking.*;
import appeng.api.util.AECableType;
import dev.creationcore.compat.ae2.CrafterNetworkAccess;
import dev.creationcore.registry.ModBlocks;
import java.util.EnumSet;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a node to the existing vanilla crafter entity, retaining its inventory and saved type. */
@Mixin(CrafterBlockEntity.class)
public abstract class CrafterNetworkMixin extends RandomizableContainerBlockEntity
        implements IInWorldGridNodeHost, CrafterNetworkAccess {
    @Unique private IManagedGridNode creationcore$node;
    protected CrafterNetworkMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    @Override public IManagedGridNode creationcore$getMainNode() {
        if (!getBlockState().is(ModBlocks.CREATIVE_CRAFTER.get())) return null;
        if (creationcore$node == null) creationcore$node = GridHelper.createManagedNode((CrafterBlockEntity)(Object)this,
                (owner, node) -> owner.setChanged()).setTagName("creationcore_crafter_node").setInWorldNode(true)
                .setFlags(GridFlags.REQUIRE_CHANNEL).setIdlePowerUsage(1.0)
                .setExposedOnSides(EnumSet.allOf(Direction.class)).setVisualRepresentation(ModBlocks.CREATIVE_CRAFTER.get());
        return creationcore$node;
    }
    @Override public IGridNode getGridNode(Direction side) {
        var node = creationcore$getMainNode(); return node == null ? null : node.getNode();
    }
    @Override public AECableType getCableConnectionType(Direction side) { return AECableType.COVERED; }
    @Override public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && creationcore$getMainNode() != null)
            GridHelper.onFirstTick((CrafterBlockEntity)(Object)this, be -> {
                if (!be.isRemoved() && be.getLevel() != null)
                    ((CrafterNetworkAccess)be).creationcore$getMainNode().create(be.getLevel(), be.getBlockPos());
            });
    }
    @Override public void setRemoved() {
        if (creationcore$node != null) creationcore$node.destroy();
        super.setRemoved();
    }
    @Override public void onChunkUnloaded() {
        if (creationcore$node != null) creationcore$node.destroy();
        super.onChunkUnloaded();
    }
    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void creationcore$load(CompoundTag data, HolderLookup.Provider registries, CallbackInfo ci) {
        var node = creationcore$getMainNode(); if (node != null) node.loadFromNBT(data);
    }
    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void creationcore$save(CompoundTag data, HolderLookup.Provider registries, CallbackInfo ci) {
        var node = creationcore$getMainNode(); if (node != null) node.saveToNBT(data);
    }
}
