package dev.creationcore.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.CrafterBlock;

/** All inventory, redstone, output and orientation behavior remains vanilla. */
public final class CreativeCrafterBlock extends CrafterBlock {
  public static final MapCodec<CrafterBlock> CODEC = simpleCodec(CreativeCrafterBlock::new);

  public CreativeCrafterBlock(Properties properties) {
    super(properties);
  }

  @Override
  protected MapCodec<CrafterBlock> codec() {
    return CODEC;
  }
}
