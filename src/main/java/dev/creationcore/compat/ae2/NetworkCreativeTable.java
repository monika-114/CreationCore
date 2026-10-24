package dev.creationcore.compat.ae2;

import appeng.api.networking.GridFlags;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** One powered channel authorizes only the AE2 grid to which this table actually belongs. */
public final class NetworkCreativeTable extends AENetworkedBlockEntity {
    public NetworkCreativeTable(BlockPos pos, BlockState state) {
        super(Ae2Integration.TABLE.get(), pos, state);
        getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL).setIdlePowerUsage(1.0)
                .setVisualRepresentation(dev.creationcore.registry.ModItems.CREATIVE_CRAFTING_TABLE.get());
    }
    @Override public AECableType getCableConnectionType(Direction side) { return AECableType.COVERED; }
}
