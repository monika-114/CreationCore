package dev.creationcore.compat.mixin;

import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu;
import dev.creationcore.compat.IntegrationRecipes;
import java.util.Optional;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DimensionsCraftMenu.class, remap = false)
public abstract class BeyondCraftingMixin {
    @Inject(method = "getRecipe", at = @At("HEAD"), cancellable = true)
    private static void creationcore$recipes(Player player, CraftingInput input, Level level,
                                              CallbackInfoReturnable<Optional<RecipeHolder<CraftingRecipe>>> cir) {
        var special = IntegrationRecipes.special(input, level);
        if (special.isPresent()) cir.setReturnValue(special);
    }
}
