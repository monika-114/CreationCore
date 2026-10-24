package dev.creationcore.mixin;

import dev.creationcore.block.CreativeCrafterBlock;
import dev.creationcore.menu.CreativeCrafterMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** State-based dispatch also works after vanilla block-entity NBT reload. */
@Mixin(CrafterBlockEntity.class)
public abstract class CrafterBlockEntityMixin {
  @Shadow @Final protected ContainerData containerData;

  @Inject(method = "createMenu", at = @At("HEAD"), cancellable = true)
  private void creationcore$menu(
      int id, Inventory inventory, CallbackInfoReturnable<AbstractContainerMenu> cir) {
    var self = (CrafterBlockEntity) (Object) this;
    if (self.getBlockState().getBlock() instanceof CreativeCrafterBlock) {
      cir.setReturnValue(new CreativeCrafterMenu(id, inventory, self, containerData));
    }
  }

  @Inject(method = "getDefaultName", at = @At("HEAD"), cancellable = true)
  private void creationcore$title(CallbackInfoReturnable<Component> cir) {
    if (((CrafterBlockEntity) (Object) this).getBlockState().getBlock()
        instanceof CreativeCrafterBlock) {
      cir.setReturnValue(Component.translatable("container.creationcore.creative_crafter"));
    }
  }
}
