package dev.creationcore.compat.mixin;

import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import com.tom.storagemod.menu.StorageTerminalMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = StorageTerminalMenu.class, remap = false)
public interface TomsStorageMenuAccessor {
  @Accessor("te")
  StorageTerminalBlockEntity creationcore$terminal();
}
