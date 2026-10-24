package dev.creationcore.mixin;

import dev.creationcore.item.MineCraftItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public class EnchantmentRulesMixin {
  @Inject(
      method = {"canEnchant", "isPrimaryItem"},
      at = @At("HEAD"),
      cancellable = true)
  private void creationcore$enchant(ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
    if (!(stack.getItem() instanceof MineCraftItem)) return;
    var contents = ((Enchantment) (Object) this).description().getContents();
    if (contents instanceof net.minecraft.network.chat.contents.TranslatableContents text) {
      String key = text.getKey();
      if (java.util.Set.of(
              "enchantment.minecraft.unbreaking",
              "enchantment.minecraft.mending",
              "enchantment.minecraft.silk_touch",
              "enchantment.minecraft.fortune")
          .contains(key)) ci.setReturnValue(false);
    }
  }
}
