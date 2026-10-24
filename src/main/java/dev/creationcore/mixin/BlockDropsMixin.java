package dev.creationcore.mixin;

import dev.creationcore.event.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class BlockDropsMixin {
  @Inject(
      method =
          "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
      at = @At("HEAD"),
      cancellable = true)
  private static void creationcore$drops(
      BlockState state,
      Level level,
      BlockPos pos,
      BlockEntity be,
      Entity breaker,
      ItemStack tool,
      CallbackInfo ci) {
    if (!(level instanceof ServerLevel server)) return;
    var drops = new java.util.ArrayList<ItemEntity>();
    var event = new GameplayHooks.BlockDrops(server, pos, state, be, breaker, tool, drops);
    if (CoreGameplayEvents.handleFabricDrops(event)) {
      for (var drop : drops) server.addFreshEntity(drop);
      state.spawnAfterBreak(server, pos, tool, event.shouldDropExperience());
      ci.cancel();
    }
  }
}
