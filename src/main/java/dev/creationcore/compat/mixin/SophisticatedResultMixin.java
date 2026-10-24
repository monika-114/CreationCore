package dev.creationcore.compat.mixin;

import dev.creationcore.compat.sophisticated.SophisticatedRecipes;
import java.util.List;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer$2", remap = false)
public abstract class SophisticatedResultMixin {
    @Shadow @Final private CraftingUpgradeContainer this$0;
    @Redirect(method = "setChanged", at = @At(value = "INVOKE", target = "Lnet/p3pp3rf1y/sophisticatedcore/util/RecipeHelper;safeGetRecipesFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Ljava/util/List;"))
    private List<RecipeHolder<CraftingRecipe>> creationcore$recipes(RecipeType<?> type, CraftingInput input, Level level) {
        return SophisticatedRecipes.find(this$0, (CraftingInput)input, level);
    }
}
