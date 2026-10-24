package dev.creationcore.compat.mixin;

import com.tom.storagemod.block.IInventoryNode;
import dev.creationcore.block.CreativeCraftingTableBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(
    value = {CreativeCraftingTableBlock.class, dev.creationcore.block.CreativeCrafterBlock.class},
    remap = false)
public abstract class TomsTableNodeMixin implements IInventoryNode {}
