package dev.creationcore.compat.mixin;

import dev.creationcore.compat.sophisticated.CreativeCraftingUpgradeItem;
import dev.creationcore.compat.sophisticated.SophisticatedRecipes;
import java.util.List;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftingUpgradeContainer.class, remap = false)
public abstract class SophisticatedCraftingMixin {
  @Shadow private RecipeHolder<CraftingRecipe> lastRecipe;
  @Unique private boolean creationcore$wasCreative;

  @Inject(method = "updateCraftingResult", at = @At("HEAD"))
  private void creationcore$permissionChanged(CallbackInfo ci) {
    boolean creative =
        ((CraftingUpgradeContainer) (Object) this).getUpgradeStack().getItem()
            instanceof CreativeCraftingUpgradeItem;
    if (creative != creationcore$wasCreative) lastRecipe = null;
    creationcore$wasCreative = creative;
  }

  @Redirect(
      method = "updateCraftingResult",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/p3pp3rf1y/sophisticatedcore/util/RecipeHelper;safeGetRecipesFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Ljava/util/List;"))
  private List<RecipeHolder<CraftingRecipe>> creationcore$recipes(
      RecipeType<?> type, CraftingInput input, Level level) {
    return SophisticatedRecipes.find(
        (CraftingUpgradeContainer) (Object) this, (CraftingInput) input, level);
  }
}
