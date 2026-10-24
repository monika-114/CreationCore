package dev.creationcore.compat.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGridNode;

public final class CreativeNetworkGate {
    public static boolean authorized(IGridNode node) {
        if (node == null || !node.isActive()) return false;
        for (NetworkCreativeTable table : node.getGrid().getMachines(NetworkCreativeTable.class)) {
            if (!table.isRemoved() && table.getMainNode().isActive()
                    && table.getMainNode().getGrid() == node.getGrid()) return true;
        }
        for (var crafter : node.getGrid().getMachines(net.minecraft.world.level.block.entity.CrafterBlockEntity.class)) {
            if (!crafter.isRemoved() && crafter instanceof CrafterNetworkAccess access) {
                var main = access.creationcore$getMainNode();
                if (main != null && main.isActive() && main.getGrid() == node.getGrid()) return true;
            }
        }
        return false;
    }
    public static boolean requiresTable(IPatternDetails pattern) {
        return pattern instanceof CreativePatternAccess access && access.creationcore$isCreativeRecipe();
    }
    private CreativeNetworkGate() {}
}
