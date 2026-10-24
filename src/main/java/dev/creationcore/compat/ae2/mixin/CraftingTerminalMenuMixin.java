package dev.creationcore.compat.ae2.mixin;

import appeng.api.storage.ITerminalHost;
import appeng.menu.guisync.GuiSync;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.CraftingTermMenu;
import dev.creationcore.compat.ae2.CreativeAeRecipe;
import dev.creationcore.compat.ae2.CreativeNetworkGate;
import dev.creationcore.compat.ae2.CreativeTerminalAccess;
import dev.creationcore.registry.ModRecipes;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.Optional;

@Mixin(value = CraftingTermMenu.class, remap = false)
public abstract class CraftingTerminalMenuMixin extends MEStorageMenu implements CreativeTerminalAccess {
    @Shadow private void updateCurrentRecipeAndOutput(boolean force) { throw new AssertionError(); }
    @Unique @GuiSync(32002) public boolean creationcore$craftingTableOnline;

    protected CraftingTerminalMenuMixin(MenuType<?> type, int id, Inventory inventory, ITerminalHost host) {
        super(type, id, inventory, host);
    }

    // These override methods inherited by CraftingTermMenu from MEStorageMenu.
    @Override public void broadcastChanges() {
        if (!isClientSide()) {
            boolean online = CreativeNetworkGate.authorized(getGridNode());
            if (online != creationcore$craftingTableOnline) {
                creationcore$craftingTableOnline = online;
                updateCurrentRecipeAndOutput(true);
            }
        }
        super.broadcastChanges();
    }

    @Override public void onServerDataSync(ShortSet changed) {
        super.onServerDataSync(changed);
        if (changed.contains((short)32002)) updateCurrentRecipeAndOutput(true);
    }

    @Override public void creationcore$refreshOutput() {
        updateCurrentRecipeAndOutput(true);
    }

    @Redirect(method = "updateCurrentRecipeAndOutput", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/item/crafting/RecipeManager;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"))
    private Optional<RecipeHolder<CraftingRecipe>> creationcore$find(RecipeManager manager, RecipeType<?> type,
                                                                   RecipeInput input, Level level) {
        boolean allowed = isClientSide() ? creationcore$craftingTableOnline : CreativeNetworkGate.authorized(getGridNode());
        if (allowed) {
            var special = manager.getRecipeFor(ModRecipes.CREATIVE_CRAFTING_TYPE.get(), (CraftingInput)input, level);
            if (special.isPresent()) {
                var holder = special.get();
                return Optional.of(new RecipeHolder<>(holder.id(), new CreativeAeRecipe(holder.value())));
            }
        }
        return manager.getRecipeFor(RecipeType.CRAFTING, (CraftingInput)input, level);
    }
}
