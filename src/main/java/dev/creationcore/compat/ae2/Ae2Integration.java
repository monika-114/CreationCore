package dev.creationcore.compat.ae2;

import appeng.api.AECapabilities;
import dev.creationcore.CreativeCoreMod;
import dev.creationcore.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** This class is resolved only after the optional AE2 dependency has been detected. */
public final class Ae2Integration {
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(
            Registries.BLOCK_ENTITY_TYPE, CreativeCoreMod.MODID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkCreativeTable>> TABLE =
            TYPES.register("network_creative_table", () -> BlockEntityType.Builder.of(
                    NetworkCreativeTable::new, ModBlocks.CREATIVE_CRAFTING_TABLE.get()).build(null));

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        bus.addListener(Ae2Integration::capabilities);
    }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, TABLE.get(), (table, unused) -> table);
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, BlockEntityType.CRAFTER, (crafter, unused) ->
                crafter.getBlockState().is(ModBlocks.CREATIVE_CRAFTER.get())
                        ? (appeng.api.networking.IInWorldGridNodeHost)crafter : null);
    }
    public static BlockEntity createTable(BlockPos pos, BlockState state) { return new NetworkCreativeTable(pos, state); }
    public static void placed(Level level, BlockPos pos, LivingEntity placer) {
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof NetworkCreativeTable table) {
            table.setOwner(player);
        }
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof CrafterNetworkAccess crafter) {
            var node = crafter.creationcore$getMainNode();
            if (node != null) node.setOwningPlayer(player);
        }
    }
    private Ae2Integration() {}
}
