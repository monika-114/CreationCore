package dev.creationcore.compat.sophisticated;

import dev.creationcore.compat.IntegrationRecipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

public final class SophisticatedRecipes {
  public static List<RecipeHolder<CraftingRecipe>> find(
      CraftingUpgradeContainer menu, CraftingInput input, Level level) {
    var normal = RecipeHelper.safeGetRecipesFor(RecipeType.CRAFTING, input, level);
    if (!(menu.getUpgradeStack().getItem() instanceof CreativeCraftingUpgradeItem)) return normal;
    var recipes = new ArrayList<>(IntegrationRecipes.specialMatches(input, level));
    recipes.addAll(normal);
    return recipes;
  }

  private SophisticatedRecipes() {}
}
