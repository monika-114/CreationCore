package dev.creationcore.mixin;

import dev.creationcore.event.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndPortalBlock.class)
public class EndPortalRitualMixin {
  @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
  private void creationcore$portal(
      BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
    if (level.isClientSide || !level.dimension().equals(Level.END)) return;
    var e = new GameplayHooks.Travel(entity, Level.OVERWORLD);
    CoreGameplayEvents.onTravelToDimension(e);
    if (e.canceled) ci.cancel();
  }
}
