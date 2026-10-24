package dev.creationcore.compat.toms;

import com.tom.storagemod.block.entity.IInventoryConnector;
import com.tom.storagemod.block.AbstractStorageTerminalBlock;
import com.tom.storagemod.inventory.InventoryCableNetwork;
import dev.creationcore.compat.mixin.TomsConnectorAccessor;
import dev.creationcore.registry.ModBlocks;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class TomsNetworkGate {
    public static boolean authorized(Level level, BlockPos terminal) {
        if (level == null || level.isClientSide) return false;
        Set<IInventoryConnector> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        var queue = new ArrayDeque<IInventoryConnector>();
        var state = level.getBlockState(terminal);
        if (!state.hasProperty(AbstractStorageTerminalBlock.FACING)
                || !state.hasProperty(AbstractStorageTerminalBlock.TERMINAL_POS)) return false;
        Direction attached = state.getValue(AbstractStorageTerminalBlock.FACING);
        var position = state.getValue(AbstractStorageTerminalBlock.TERMINAL_POS);
        if (position == AbstractStorageTerminalBlock.TerminalPos.UP) attached = Direction.UP;
        if (position == AbstractStorageTerminalBlock.TerminalPos.DOWN) attached = Direction.DOWN;
        for (BlockPos node : InventoryCableNetwork.getNetwork(level).getNetworkNodes(terminal.relative(attached))) {
            if (!level.hasChunkAt(node)) continue;
            if (level.getBlockEntity(node) instanceof IInventoryConnector connector) queue.add(connector);
            // Cable endpoints can directly attach the worktable as a network device.
            for (Direction side : Direction.values()) {
                BlockPos adjacent = node.relative(side);
                if (level.hasChunkAt(adjacent) && isCreativeDevice(level.getBlockState(adjacent))) return true;
            }
        }
        while (!queue.isEmpty()) {
            var connector = queue.remove();
            if (!seen.add(connector)) continue;
            if (connector instanceof BlockEntity be && !be.isRemoved() && be.getLevel() != null) {
                Level world = be.getLevel();
                if (connector instanceof TomsConnectorAccessor access) for (var face : access.creationcore$interfaces()) {
                    if (world.hasChunkAt(face.pos()) && isCreativeDevice(world.getBlockState(face.pos()))) return true;
                }
                queue.addAll(connector.getConnectedConnectors());
            }
        }
        return false;
    }
    private static boolean isCreativeDevice(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(ModBlocks.CREATIVE_CRAFTING_TABLE.get()) || state.is(ModBlocks.CREATIVE_CRAFTER.get());
    }
    private TomsNetworkGate() {}
}
