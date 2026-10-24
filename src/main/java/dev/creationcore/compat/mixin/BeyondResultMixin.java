package dev.creationcore.compat.mixin;

import com.wintercogs.beyonddimensions.common.menu.widget.slot.AutoRefillResultSlot;
import dev.creationcore.compat.IntegrationRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AutoRefillResultSlot.class, remap = false)
public abstract class BeyondResultMixin {
    @Redirect(method = "onTake", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/RecipeManager;getRemainingItemsFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Lnet/minecraft/core/NonNullList;"))
    private NonNullList<ItemStack> creationcore$remainders(RecipeManager manager, RecipeType<?> type, RecipeInput input, Level level) {
        return IntegrationRecipes.special((CraftingInput)input, level)
                .map(h -> h.value().getRemainingItems((CraftingInput)input))
                .orElseGet(() -> manager.getRemainingItemsFor(RecipeType.CRAFTING, (CraftingInput)input, level));
    }
}
