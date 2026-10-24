package dev.creationcore.compat.ae2.mixin;

import appeng.api.networking.security.IActionHost;
import appeng.menu.guisync.GuiSync;
import appeng.menu.me.items.PatternEncodingTermMenu;
import dev.creationcore.compat.ae2.CreativeAeRecipe;
import dev.creationcore.compat.ae2.CreativeNetworkGate;
import dev.creationcore.registry.ModRecipes;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;

@Mixin(value = PatternEncodingTermMenu.class, remap = false)
public abstract class PatternEncodingMenuMixin {
    @Shadow private RecipeHolder<CraftingRecipe> currentRecipe;
    @Shadow private ItemStack getAndUpdateOutput() { throw new AssertionError(); }
    // AE2's menu synchronization also keeps the client-side preview honest.
    @Unique @GuiSync(32001) public boolean creationcore$tableOnline;
    @Unique private boolean creationcore$previouslyAuthorized;

    @Unique private boolean creationcore$authorized() {
        var menu = (PatternEncodingTermMenu)(Object)this;
        if (menu.getPlayerInventory().player.level().isClientSide) return creationcore$tableOnline;
        return menu.getTarget() instanceof IActionHost host && CreativeNetworkGate.authorized(host.getActionableNode());
    }

    @Inject(method = "broadcastChanges", at = @At("HEAD"))
    private void creationcore$sync(CallbackInfo ci) {
        var menu = (PatternEncodingTermMenu)(Object)this;
        if (!menu.getPlayerInventory().player.level().isClientSide) creationcore$tableOnline = creationcore$authorized();
    }

    @Inject(method = "onServerDataSync", at = @At("TAIL"))
    private void creationcore$preview(ShortSet changed, CallbackInfo ci) {
        if (changed.contains((short)32001)) getAndUpdateOutput();
    }

    @Inject(method = "getAndUpdateOutput", at = @At("HEAD"))
    private void creationcore$invalidateCachedPermission(CallbackInfoReturnable<ItemStack> cir) {
        boolean allowed = creationcore$authorized();
        // AE2 re-enters this method while checking fluid substitutions. Keep a normal
        // recipe cached while the table is online so that nested lookup terminates.
        // Record the transition before AE2 can re-enter; clear once on either edge
        // so newly available creative recipes take priority and revoked ones stop.
        if (allowed != creationcore$previouslyAuthorized) currentRecipe = null;
        creationcore$previouslyAuthorized = allowed;
    }

    @Redirect(method = "getAndUpdateOutput", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/item/crafting/RecipeManager;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"))
    private Optional<RecipeHolder<CraftingRecipe>> creationcore$find(RecipeManager manager, RecipeType<?> type,
                                                                   RecipeInput input, Level level) {
        CraftingInput grid = (CraftingInput)input;
        if (creationcore$authorized()) {
            var special = manager.getRecipeFor(ModRecipes.CREATIVE_CRAFTING_TYPE.get(), grid, level);
            if (special.isPresent()) {
                var holder = special.get();
                return Optional.of(new RecipeHolder<>(holder.id(), new CreativeAeRecipe(holder.value())));
            }
        }
        return manager.getRecipeFor(RecipeType.CRAFTING, grid, level);
    }
}
