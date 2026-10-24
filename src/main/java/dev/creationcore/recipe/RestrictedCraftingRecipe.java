package dev.creationcore.recipe;

import dev.creationcore.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

/** Retypes the resolved pack recipe without altering its ingredients, result or remainder semantics. */
public final class RestrictedCraftingRecipe extends CreativeCraftingRecipe {
    public RestrictedCraftingRecipe(CraftingRecipe delegate) { super(delegate); }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.RESTRICTED_CRAFTING_SERIALIZER.get(); }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) { return delegate().assemble(input, registries); }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) { return delegate().getRemainingItems(input); }
}
