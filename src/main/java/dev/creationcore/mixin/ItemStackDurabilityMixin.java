package dev.creationcore.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public class ItemStackDurabilityMixin {
  @Inject(method = "setDamageValue", at = @At("HEAD"), cancellable = true)
  private void creationcore$durability(int damage, CallbackInfo ci) {
    if (((ItemStack) (Object) this).getItem() instanceof dev.creationcore.item.MineCraftItem
        && damage > 0) ci.cancel();
  }
}
