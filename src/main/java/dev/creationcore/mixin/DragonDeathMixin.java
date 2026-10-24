package dev.creationcore.mixin;

import dev.creationcore.event.*;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragon.class)
public class DragonDeathMixin {
  @Inject(method = "tickDeath", at = @At("HEAD"))
  private void creationcore$death(CallbackInfo ci) {
    var self = (EnderDragon) (Object) this;
    if (self.level().isClientSide) return;
    var data = PersistentData.get(self);
    if (!data.getBoolean("creationcore:ritual_checked")) {
      data.putBoolean("creationcore:ritual_checked", true);
      CoreGameplayEvents.onDragonDeath(new GameplayHooks.Death(self));
    }
  }
}
