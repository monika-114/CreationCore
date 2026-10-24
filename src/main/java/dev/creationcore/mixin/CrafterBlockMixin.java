package dev.creationcore.mixin;

import dev.creationcore.block.CreativeCrafterBlock;
import dev.creationcore.recipe.CreativeCrafterRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.Optional;

@Mixin(CrafterBlock.class)
public abstract class CrafterBlockMixin {
    @Redirect(method = "dispenseFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/CrafterBlock;getPotentialResults(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/CraftingInput;)Ljava/util/Optional;"))
    private Optional<RecipeHolder<CraftingRecipe>> creationcore$recipe(Level level, CraftingInput input,
                                                                     BlockState state, ServerLevel server, BlockPos pos) {
        return state.getBlock() instanceof CreativeCrafterBlock
                ? CreativeCrafterRecipes.find(level,input) : CrafterBlock.getPotentialResults(level,input);
    }
}
