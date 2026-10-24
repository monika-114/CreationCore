package dev.creationcore.compat.mixin;

import com.tom.storagemod.block.entity.CraftingTerminalBlockEntity;
import dev.creationcore.compat.IntegrationRecipes;
import dev.creationcore.compat.RefreshBeforeCrafting;
import dev.creationcore.compat.toms.TomsNetworkGate;
import java.util.Optional;
import net.minecraft.world.item.crafting.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = CraftingTerminalBlockEntity.class, remap = false)
public abstract class TomsCraftingMixin implements RefreshBeforeCrafting {
  @Shadow private Optional<RecipeHolder<CraftingRecipe>> currentRecipe;

  @Shadow
  protected abstract void onCraftingMatrixChanged();

  @Unique private boolean creationcore$wasAuthorized;

  @Unique
  private boolean creationcore$allowed() {
    var terminal = (CraftingTerminalBlockEntity) (Object) this;
    return TomsNetworkGate.authorized(terminal.getLevel(), terminal.getBlockPos());
  }

  @Override
  public void creationcore$refreshBeforeCrafting() {
    boolean allowed = creationcore$allowed();
    if (allowed != creationcore$wasAuthorized) {
      creationcore$wasAuthorized = allowed;
      currentRecipe = Optional.empty();
      onCraftingMatrixChanged();
    }
  }

  @Inject(method = "onCraftingMatrixChanged", at = @At("HEAD"))
  private void creationcore$invalidate(CallbackInfo ci) {
    boolean allowed = creationcore$allowed();
    if (allowed != creationcore$wasAuthorized) currentRecipe = Optional.empty();
    creationcore$wasAuthorized = allowed;
  }

  @Inject(method = "updateServer", at = @At("TAIL"))
  private void creationcore$networkChanged(CallbackInfo ci) {
    creationcore$refreshBeforeCrafting();
  }

  @Inject(method = "getRecipe", at = @At("HEAD"), cancellable = true)
  private void creationcore$find(
      CraftingInput input, CallbackInfoReturnable<Optional<RecipeHolder<CraftingRecipe>>> cir) {
    var terminal = (CraftingTerminalBlockEntity) (Object) this;
    if (creationcore$allowed()) {
      var recipe = IntegrationRecipes.special(input, terminal.getLevel());
      if (recipe.isPresent()) cir.setReturnValue(recipe);
    }
  }
}
