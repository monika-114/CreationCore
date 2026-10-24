package dev.creationcore.mixin.client;

import dev.creationcore.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Extends vanilla Barrier/Light marker particles into Survival while the corresponding item or
 * Mine Craft is held. When Mine Craft is held both marker types are alternated quickly so both
 * invisible block types remain discoverable.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "getMarkerParticleTarget", at = @At("HEAD"), cancellable = true)
    private void creationcore$survivalBarrierAndLightMarkers(CallbackInfoReturnable<Block> cir) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean mineCraft = main.is(ModItems.MINE_CRAFT.get()) || off.is(ModItems.MINE_CRAFT.get());
        boolean showBarrier = mineCraft || main.is(Items.BARRIER) || off.is(Items.BARRIER);
        boolean showLight = mineCraft || main.is(Items.LIGHT) || off.is(Items.LIGHT);

        if (!showBarrier && !showLight) return;
        if (showBarrier && showLight) {
            ClientLevel level = (ClientLevel) (Object) this;
            cir.setReturnValue(((level.getGameTime() / 4L) & 1L) == 0L ? Blocks.BARRIER : Blocks.LIGHT);
        } else {
            cir.setReturnValue(showBarrier ? Blocks.BARRIER : Blocks.LIGHT);
        }
    }
}
