package dev.creationcore.compat.jei;

import dev.creationcore.recipe.CreativeCraftingRecipe;
import dev.creationcore.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.Arrays;

public final class CreativeCraftingJeiCategory implements IRecipeCategory<CreativeCraftingRecipe> {
    private final IDrawable icon;

    public CreativeCraftingJeiCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemLike(ModItems.CREATIVE_CRAFTING_TABLE.get());
    }

    @Override
    public RecipeType<CreativeCraftingRecipe> getRecipeType() {
        return CreationCoreJeiPlugin.CRAFTING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.creationcore.creative_crafting_table");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 116;
    }

    @Override
    public int getHeight() {
        return 58;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CreativeCraftingRecipe recipe, IFocusGroup focuses) {
        int width = 3;
        int height = 3;
        int offsetX = 0;
        int offsetY = 0;
        if (recipe.delegate() instanceof ShapedRecipe shaped) {
            width = shaped.getWidth();
            height = shaped.getHeight();
            offsetX = (3 - width) / 2;
            offsetY = (3 - height) / 2;
        }
        var ingredients = recipe.getIngredients();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                var slot = builder.addSlot(RecipeIngredientRole.INPUT, 1 + col * 18, 1 + row * 18)
                        .setStandardSlotBackground();
                int x = col - offsetX;
                int y = row - offsetY;
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    int index = y * width + x;
                    if (index < ingredients.size() && !ingredients.get(index).isEmpty()) {
                        slot.addItemStacks(Arrays.asList(ingredients.get(index).getItems()));
                    }
                }
            }
        }
        var level = Minecraft.getInstance().level;
        if (level != null) {
            ItemStack result = recipe.getResultItem(level.registryAccess());
            var output = builder.addSlot(RecipeIngredientRole.OUTPUT, 95, 19)
                    .setOutputSlotBackground()
                    .addItemStack(result);
            if (recipe.isDiscCopy()) {
                output.addTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.creationcore.disc_copy_remainder")));
            }
        }
    }
}
