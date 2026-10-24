package dev.creationcore.compat.ae2.mixin;

import appeng.crafting.pattern.AECraftingPattern;
import dev.creationcore.compat.ae2.CreativeAeRecipe;
import dev.creationcore.compat.ae2.CreativePatternAccess;
import dev.creationcore.recipe.CreativeCraftingRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;

@Mixin(value = AECraftingPattern.class, remap = false)
public abstract class CraftingPatternMixin implements CreativePatternAccess {
    @Shadow @Final private CraftingRecipe recipe;
    @Shadow @Final private CraftingInput.Positioned positionedPattern;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/item/crafting/RecipeManager;byKey(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;"))
    private Optional<RecipeHolder<?>> creationcore$decode(RecipeManager manager, ResourceLocation id) {
        return manager.byKey(id).map(holder -> holder.value() instanceof CreativeCraftingRecipe creative
                ? new RecipeHolder<>(holder.id(), new CreativeAeRecipe(creative)) : holder);
    }

    @Override public boolean creationcore$isCreativeRecipe() { return recipe instanceof CreativeAeRecipe; }

    @Inject(method = "getRecipeIngredient", at = @At("HEAD"), cancellable = true)
    private void creationcore$shapedIngredient(int slot, CallbackInfoReturnable<Ingredient> cir) {
        if (recipe instanceof CreativeAeRecipe adapter && adapter.recipe().delegate() instanceof ShapedRecipe) {
            cir.setReturnValue(adapter.ingredientAt(slot, positionedPattern));
        }
    }
}
