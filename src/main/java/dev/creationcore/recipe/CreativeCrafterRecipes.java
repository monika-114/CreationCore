package dev.creationcore.recipe;

import dev.creationcore.registry.ModRecipes;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CrafterBlock;

public final class CreativeCrafterRecipes {
  private CreativeCrafterRecipes() {}

  public static Optional<RecipeHolder<CraftingRecipe>> find(Level level, CraftingInput input) {
    var special =
        level
            .getRecipeManager()
            .getRecipeFor(ModRecipes.CREATIVE_CRAFTING_TYPE.get(), input, level);
    if (special.isPresent()) {
      var holder = special.get();
      return Optional.of(new RecipeHolder<>(holder.id(), new Adapter(holder.value())));
    }
    return CrafterBlock.getPotentialResults(level, input);
  }

  /** Feed the vanilla dispenser the actual custom assemble/remainder behavior. */
  public record Adapter(CreativeCraftingRecipe recipe) implements CraftingRecipe {
    @Override
    public boolean matches(CraftingInput input, Level level) {
      return recipe.matches(input, level);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
      return recipe.assemble(input, registries);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
      return recipe.getRemainingItems(input);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
      return recipe.canCraftInDimensions(width, height);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
      return recipe.getResultItem(registries);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
      return recipe.getSerializer();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
      return recipe.getIngredients();
    }

    @Override
    public String getGroup() {
      return recipe.getGroup();
    }

    @Override
    public CraftingBookCategory category() {
      return recipe.delegate().category();
    }
  }
}
