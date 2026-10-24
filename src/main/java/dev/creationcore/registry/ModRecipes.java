package dev.creationcore.registry;

import dev.creationcore.CreativeCoreMod;
import dev.creationcore.recipe.CreativeCraftingRecipe;
import dev.creationcore.recipe.RecipeSerializerWrapper;
import dev.creationcore.recipe.RestrictedCraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, CreativeCoreMod.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, CreativeCoreMod.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CreativeCraftingRecipe>> CREATIVE_CRAFTING_TYPE =
            TYPES.register("creative_crafting", RecipeType::simple);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CreativeCraftingRecipe>> CREATIVE_CRAFTING_SERIALIZER =
            SERIALIZERS.register("creative_crafting", () -> new RecipeSerializerWrapper<>(
                    RecipeSerializer.SHAPED_RECIPE.codec().xmap(CreativeCraftingRecipe::new, r -> (ShapedRecipe) r.delegate()),
                    RecipeSerializer.SHAPED_RECIPE.streamCodec().map(CreativeCraftingRecipe::new, r -> (ShapedRecipe) r.delegate())
            ));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CreativeCraftingRecipe>> CREATIVE_CRAFTING_SHAPELESS_SERIALIZER =
            SERIALIZERS.register("creative_crafting_shapeless", () -> new RecipeSerializerWrapper<>(
                    RecipeSerializer.SHAPELESS_RECIPE.codec().xmap(CreativeCraftingRecipe::new, r -> (ShapelessRecipe) r.delegate()),
                    RecipeSerializer.SHAPELESS_RECIPE.streamCodec().map(CreativeCraftingRecipe::new, r -> (ShapelessRecipe) r.delegate())
            ));

    private ModRecipes() {}

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RestrictedCraftingRecipe>> RESTRICTED_CRAFTING_SERIALIZER =
            SERIALIZERS.register("restricted_crafting", () -> new RecipeSerializerWrapper<>(
                    Recipe.CODEC.fieldOf("recipe").xmap(r -> new RestrictedCraftingRecipe((CraftingRecipe)r), RestrictedCraftingRecipe::delegate),
                    Recipe.STREAM_CODEC.map(r -> new RestrictedCraftingRecipe((CraftingRecipe)r), RestrictedCraftingRecipe::delegate)));

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
    }
}
