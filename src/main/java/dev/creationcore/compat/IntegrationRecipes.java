package dev.creationcore.compat;

import dev.creationcore.recipe.CreativeCrafterRecipes;
import dev.creationcore.registry.ModRecipes;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Adapters are local to authorized menus; no special recipe becomes an ordinary recipe. */
public final class IntegrationRecipes {
  public static Optional<RecipeHolder<CraftingRecipe>> special(CraftingInput input, Level level) {
    return level
        .getRecipeManager()
        .getRecipeFor(ModRecipes.CREATIVE_CRAFTING_TYPE.get(), input, level)
        .map(h -> new RecipeHolder<>(h.id(), new CreativeCrafterRecipes.Adapter(h.value())));
  }

  public static List<RecipeHolder<CraftingRecipe>> specialMatches(
      CraftingInput input, Level level) {
    return level
        .getRecipeManager()
        .getRecipesFor(ModRecipes.CREATIVE_CRAFTING_TYPE.get(), input, level)
        .stream()
        .map(
            h ->
                new RecipeHolder<CraftingRecipe>(
                    h.id(), new CreativeCrafterRecipes.Adapter(h.value())))
        .toList();
  }

  private IntegrationRecipes() {}
}
