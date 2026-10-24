package dev.creationcore.registry;

import dev.creationcore.recipe.CreativeCraftingRecipe;
import dev.creationcore.recipe.RecipeSerializerWrapper;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public final class ModRecipes {
  public static final Registrations<RecipeType<?>> TYPES =
      new Registrations<>(net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE);
  public static final Registrations<RecipeSerializer<?>> SERIALIZERS =
      new Registrations<>(net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER);

  public static final Registrations.Entry<RecipeType<CreativeCraftingRecipe>>
      CREATIVE_CRAFTING_TYPE =
          TYPES.register(
              "creative_crafting",
              () ->
                  new RecipeType<CreativeCraftingRecipe>() {
                    public String toString() {
                      return "creationcore:creative_crafting";
                    }
                  });

  public static final Registrations.Entry<RecipeSerializer<CreativeCraftingRecipe>>
      CREATIVE_CRAFTING_SERIALIZER =
          SERIALIZERS.register(
              "creative_crafting",
              () ->
                  new RecipeSerializerWrapper<>(
                      RecipeSerializer.SHAPED_RECIPE
                          .codec()
                          .xmap(CreativeCraftingRecipe::new, r -> (ShapedRecipe) r.delegate()),
                      RecipeSerializer.SHAPED_RECIPE
                          .streamCodec()
                          .map(CreativeCraftingRecipe::new, r -> (ShapedRecipe) r.delegate())));

  public static final Registrations.Entry<RecipeSerializer<CreativeCraftingRecipe>>
      CREATIVE_CRAFTING_SHAPELESS_SERIALIZER =
          SERIALIZERS.register(
              "creative_crafting_shapeless",
              () ->
                  new RecipeSerializerWrapper<>(
                      RecipeSerializer.SHAPELESS_RECIPE
                          .codec()
                          .xmap(CreativeCraftingRecipe::new, r -> (ShapelessRecipe) r.delegate()),
                      RecipeSerializer.SHAPELESS_RECIPE
                          .streamCodec()
                          .map(CreativeCraftingRecipe::new, r -> (ShapelessRecipe) r.delegate())));

  private ModRecipes() {}

  public static void register() {
    TYPES.register();
    SERIALIZERS.register();
  }
}
