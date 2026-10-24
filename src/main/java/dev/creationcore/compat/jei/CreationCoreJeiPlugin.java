package dev.creationcore.compat.jei;

import dev.creationcore.CreativeCoreMod;
import dev.creationcore.recipe.CreativeCraftingRecipe;
import dev.creationcore.registry.ModItems;
import dev.creationcore.registry.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

@JeiPlugin
public final class CreationCoreJeiPlugin implements IModPlugin {
  public static final RecipeType<CreativeCraftingRecipe> CRAFTING =
      RecipeType.create(CreativeCoreMod.MODID, "creative_crafting", CreativeCraftingRecipe.class);
  public static final RecipeType<CoreInfoPage> INFO =
      RecipeType.create(CreativeCoreMod.MODID, "item_information", CoreInfoPage.class);

  @Override
  public ResourceLocation getPluginUid() {
    return ResourceLocation.fromNamespaceAndPath(CreativeCoreMod.MODID, "jei");
  }

  @Override
  public void registerCategories(IRecipeCategoryRegistration registration) {
    registration.addRecipeCategories(
        new CreativeCraftingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    registration.addRecipeCategories(
        new CoreInfoJeiCategory(registration.getJeiHelpers().getGuiHelper()));
  }

  @Override
  public void registerRecipes(IRecipeRegistration registration) {
    registration.addRecipes(INFO, CoreInfoPage.pages());
    var level = Minecraft.getInstance().level;
    if (level == null) return;
    registration.addRecipes(
        CRAFTING,
        level.getRecipeManager().getAllRecipesFor(ModRecipes.CREATIVE_CRAFTING_TYPE.get()).stream()
            .map(RecipeHolder::value)
            .toList());
  }

  @Override
  public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
    registration.addRecipeCatalyst(new ItemStack(ModItems.CREATIVE_CRAFTING_TABLE.get()), CRAFTING);
    registration.addRecipeCatalyst(new ItemStack(ModItems.CREATIVE_CRAFTER.get()), CRAFTING);
    registration.addRecipeCatalyst(
        new ItemStack(ModItems.CREATIVE_CRAFTING_TABLE.get()),
        mezz.jei.api.constants.RecipeTypes.CRAFTING);
    registration.addRecipeCatalyst(
        new ItemStack(ModItems.CREATIVE_CRAFTER.get()),
        mezz.jei.api.constants.RecipeTypes.CRAFTING);
  }
}
