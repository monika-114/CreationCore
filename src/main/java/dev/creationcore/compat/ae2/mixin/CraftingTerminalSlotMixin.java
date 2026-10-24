package dev.creationcore.compat.ae2.mixin;

import appeng.helpers.InventoryAction;
import appeng.helpers.ICraftingGridMenu;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.menu.slot.CraftingTermSlot;
import dev.creationcore.compat.ae2.CreativeTerminalAccess;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftingTermSlot.class, remap = false)
public abstract class CraftingTerminalSlotMixin {
    @Shadow @Final private ICraftingGridMenu menu;
    @Inject(method = "doClick", at = @At("HEAD"))
    private void creationcore$checkBeforeTake(InventoryAction action, Player player, CallbackInfo ci) {
        if (menu instanceof CraftingTermMenu terminal) {
            // Refresh before AE2 reads the displayed result, including stale click packets
            // between table removal and the next menu broadcast. AE2 handles extraction,
            // refilling, Shift crafting, inventory capacity and recipe remainders itself.
            ((CreativeTerminalAccess)terminal).creationcore$refreshOutput();
        }
    }
}
