package dev.creationcore.event;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Positions supported by the original structure state, rather than global item-name guesses. */
public final class MineCraftDropScope {
  private MineCraftDropScope() {}

  public static Set<BlockPos> companions(ServerLevel level, BlockPos pos, BlockState state) {
    Set<BlockPos> result = new HashSet<>();
    if (state.getBlock().asItem() == Items.AIR) return Set.of();
    if (state.hasProperty(BlockStateProperties.BED_PART)
        && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
      Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
      if (state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD)
        direction = direction.getOpposite();
      addIfSameItem(level, pos.relative(direction), state, result);
    } else if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
      Direction direction =
          state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
              ? Direction.UP
              : Direction.DOWN;
      BlockPos other = pos.relative(direction);
      BlockState neighbor = level.getBlockState(other);
      if (neighbor.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
          && neighbor.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)
              != state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
        addIfSameItem(level, other, state, result);
      }
    }
    // Support vertical modded structures which explicitly declare lower/middle/upper parts.
    // Ordinary adjacent blocks without a structural property are never delayed companions.
    for (var property : state.getProperties()) {
      if (!property.getName().equals("part") && !property.getName().equals("half")) continue;
      String part = String.valueOf(state.getValue(property));
      if (!Set.of("lower", "upper", "bottom", "middle", "top").contains(part)) continue;
      for (Direction direction : new Direction[] {Direction.UP, Direction.DOWN}) {
        for (int distance = 1; distance <= 8; distance++) {
          BlockPos other = pos.relative(direction, distance);
          BlockState neighbor = level.getBlockState(other);
          if (neighbor.getBlock().asItem() != state.getBlock().asItem()
              || !neighbor.hasProperty(property)) break;
          String neighborPart = String.valueOf(neighbor.getValue(property));
          if (!Set.of("lower", "upper", "bottom", "middle", "top").contains(neighborPart)
              || neighborPart.equals(part)) break;
          result.add(other.immutable());
        }
      }
    }
    return Set.copyOf(result);
  }

  private static void addIfSameItem(
      ServerLevel level, BlockPos pos, BlockState state, Set<BlockPos> result) {
    if (level.getBlockState(pos).getBlock().asItem() == state.getBlock().asItem())
      result.add(pos.immutable());
  }
}
