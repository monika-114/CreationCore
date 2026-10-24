package dev.creationcore.compat.mixin;

import com.tom.storagemod.block.entity.InventoryConnectorBlockEntity;
import com.tom.storagemod.util.BlockFace;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = InventoryConnectorBlockEntity.class, remap = false)
public interface TomsConnectorAccessor {
    @Accessor("interfaces") Set<BlockFace> creationcore$interfaces();
}
