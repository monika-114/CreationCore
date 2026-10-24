package dev.creationcore.mixin;

import dev.creationcore.event.*;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemEntityTickMixin {
  @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
  private void creationcore$tick(CallbackInfo ci) {
    var e = new GameplayHooks.EntityTick((ItemEntity) (Object) this);
    CoreGameplayEvents.onItemTick(e);
    if (e.canceled) ci.cancel();
  }
}
