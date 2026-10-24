package dev.creationcore.compat.mixin;

import com.tom.storagemod.menu.CraftingTerminalMenu;
import dev.creationcore.compat.RefreshBeforeCrafting;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = CraftingTerminalMenu.class, remap = false)
public abstract class TomsCraftingMenuMixin implements RefreshBeforeCrafting {
    @Override public void creationcore$refreshBeforeCrafting() {
        var terminal = ((TomsStorageMenuAccessor)this).creationcore$terminal();
        if (terminal instanceof RefreshBeforeCrafting refresh) refresh.creationcore$refreshBeforeCrafting();
    }
}
