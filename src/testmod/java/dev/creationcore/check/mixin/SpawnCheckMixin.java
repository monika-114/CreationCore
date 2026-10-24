package dev.creationcore.check.mixin;

import dev.creationcore.check.GameplayChecks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public class SpawnCheckMixin {
  @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
  private void reject(Entity entity, CallbackInfoReturnable<Boolean> ci) {
    if (GameplayChecks.reject(entity)) ci.setReturnValue(false);
  }

  @Inject(method = "addFreshEntity", at = @At("RETURN"))
  private void record(Entity entity, CallbackInfoReturnable<Boolean> ci) {
    if (ci.getReturnValue()) GameplayChecks.accepted(entity);
  }
}
