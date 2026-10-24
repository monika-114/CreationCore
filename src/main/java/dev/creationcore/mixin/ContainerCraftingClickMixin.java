package dev.creationcore.mixin;

import dev.creationcore.compat.RefreshBeforeCrafting;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class ContainerCraftingClickMixin {
  @Inject(method = "clicked", at = @At("HEAD"))
  private void creationcore$refresh(CallbackInfo ci) {
    if (this instanceof RefreshBeforeCrafting menu) menu.creationcore$refreshBeforeCrafting();
  }
}
