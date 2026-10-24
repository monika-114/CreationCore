package dev.creationcore.compat.ae2;

import dev.creationcore.recipe.CreativeCraftingRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** An AE2-local view, never registered in the ordinary crafting recipe type. */
public record CreativeAeRecipe(CreativeCraftingRecipe recipe) implements CraftingRecipe {
    @Override public boolean matches(CraftingInput input, Level level) { return recipe.matches(input, level); }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) { return recipe.assemble(input, registries); }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) { return recipe.getRemainingItems(input); }
    @Override public boolean canCraftInDimensions(int width, int height) { return recipe.canCraftInDimensions(width, height); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return recipe.getResultItem(registries); }
    @Override public RecipeSerializer<?> getSerializer() { return recipe.getSerializer(); }
    @Override public NonNullList<Ingredient> getIngredients() { return recipe.getIngredients(); }
    @Override public String getGroup() { return recipe.getGroup(); }
    @Override public CraftingBookCategory category() { return recipe.delegate().category(); }

    public Ingredient ingredientAt(int slot, CraftingInput.Positioned frame) {
        if (!(recipe.delegate() instanceof ShapedRecipe shaped)) return Ingredient.EMPTY;
        CraftingInput input = frame.input();
        int x = slot % 3 - frame.left();
        int y = slot / 3 - frame.top();
        if (x < 0 || y < 0 || x >= shaped.getWidth() || y >= shaped.getHeight()) return Ingredient.EMPTY;
        boolean direct = true;
        for (int row = 0; row < shaped.getHeight(); row++) for (int col = 0; col < shaped.getWidth(); col++) {
            if (!shaped.getIngredients().get(row * shaped.getWidth() + col).test(input.getItem(col, row))) direct = false;
        }
        return shaped.getIngredients().get(y * shaped.getWidth() + (direct ? x : shaped.getWidth() - 1 - x));
    }
}
