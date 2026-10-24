package dev.creationcore.mixin;

import dev.creationcore.item.MineCraftItem;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public class EnchantmentLevelMixin {
  @Inject(method = "getItemEnchantmentLevel", at = @At("HEAD"), cancellable = true)
  private static void creationcore$level(
      Holder<Enchantment> enchantment, ItemStack stack, CallbackInfoReturnable<Integer> ci) {
    if (stack.getItem() instanceof MineCraftItem && MineCraftItem.isBlockedEnchantment(enchantment))
      ci.setReturnValue(0);
  }
}
