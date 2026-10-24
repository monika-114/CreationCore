package dev.creationcore.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import dev.creationcore.recipe.RestrictedCraftingRecipe;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
    @Shadow private Map<ResourceLocation, RecipeHolder<?>> byName;
    @Shadow private Multimap<RecipeType<?>, RecipeHolder<?>> byType;
    @Shadow @Final private HolderLookup.Provider registries;

    @Inject(method = {"apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", "replaceRecipes"}, at = @At("TAIL"))
    private void creationcore$restrictGenerator(CallbackInfo ci) {
        var names = ImmutableMap.<ResourceLocation, RecipeHolder<?>>builder();
        var types = ImmutableMultimap.<RecipeType<?>, RecipeHolder<?>>builder();
        boolean changed = false;
        for (var holder : byName.values()) {
            RecipeHolder<?> updated = holder;
            if (holder.value() instanceof CraftingRecipe crafting
                    && BuiltInRegistries.ITEM.getKey(crafting.getResultItem(registries).getItem()).toString().equals("beyonddimensions:net_creater")) {
                updated = new RecipeHolder<>(holder.id(), new RestrictedCraftingRecipe(crafting));
                changed = true;
            }
            names.put(updated.id(), updated);
            types.put(updated.value().getType(), updated);
        }
        if (changed) { byName = names.build(); byType = types.build(); }
    }
}
