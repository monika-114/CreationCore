package dev.creationcore.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.creationcore.event.CoreGameplayEvents;
import dev.creationcore.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.core.BlockPos;

/**
 * Vanilla normally refuses to remove GameMasterBlock implementations for survival players even
 * after normal break progress completes. Mine Craft is an explicit exception for the destroy
 * path only; it does not grant permission to edit/open command or structure block interfaces.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow @Final protected ServerPlayer player;
    @Shadow protected ServerLevel level;

    @Redirect(
            method = "destroyBlock",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;canUseGameMasterBlocks()Z"),
            require = 0
    )
    private boolean creationcore$mineCraftCanDestroyGameMasterBlock(Player player) {
        return player.getMainHandItem().is(ModItems.MINE_CRAFT.get()) || player.canUseGameMasterBlocks();
    }

    /**
     * Wrap the whole destroy call so cleanup is guaranteed even when another mod cancels/short-circuits
     * the method or an exception escapes. This prevents a stale transaction from contaminating the
     * player's next Mine Craft break.
     */
    @WrapMethod(method = "destroyBlock")
    private boolean creationcore$mineCraftBreakTransaction(BlockPos pos, Operation<Boolean> original) {
        CoreGameplayEvents.enterMineCraftDestroyBlockCall(player, level, pos);
        boolean succeeded = false;
        try {
            succeeded = original.call(pos);
            return succeeded;
        } finally {
            CoreGameplayEvents.finishMineCraftDestroyBlockCall(succeeded);
        }
    }
}
