package dev.creationcore.mixin;

import dev.creationcore.menu.CreativeCrafterMenu;
import dev.creationcore.recipe.CreativeCrafterRecipes;
import java.util.Optional;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CrafterBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CrafterMenu.class)
public abstract class CrafterMenuMixin {
  @Redirect(
      method = "refreshRecipeResult",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/level/block/CrafterBlock;getPotentialResults(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/CraftingInput;)Ljava/util/Optional;"))
  private Optional<RecipeHolder<CraftingRecipe>> creationcore$preview(
      Level level, CraftingInput input) {
    return (Object) this instanceof CreativeCrafterMenu
        ? CreativeCrafterRecipes.find(level, input)
        : CrafterBlock.getPotentialResults(level, input);
  }
}
