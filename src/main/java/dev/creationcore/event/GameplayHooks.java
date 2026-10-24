package dev.creationcore.event;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class GameplayHooks {
  public record LevelTick(Level getLevel) {}

  public record Death(LivingEntity getEntity) {}

  public record ExplosionDetonate(
      Level getLevel, Explosion getExplosion, List<Entity> getAffectedEntities) {}

  public static class EntityTick {
    private final Entity entity;
    public boolean canceled;

    public EntityTick(Entity e) {
      entity = e;
    }

    public Entity getEntity() {
      return entity;
    }

    public void setCanceled(boolean v) {
      canceled = v;
    }
  }

  public static final class Travel extends EntityTick {
    private final ResourceKey<Level> dimension;

    public Travel(Entity e, ResourceKey<Level> d) {
      super(e);
      dimension = d;
    }

    public ResourceKey<Level> getDimension() {
      return dimension;
    }
  }

  public static final class BlockDrops {
    private final ServerLevel level;
    private final BlockPos pos;
    private final BlockState state;
    private final BlockEntity blockEntity;
    private final Entity breaker;
    private final ItemStack tool;
    private final List<ItemEntity> drops;
    private boolean dropExperience = true;

    public BlockDrops(
        ServerLevel level,
        BlockPos pos,
        BlockState state,
        BlockEntity blockEntity,
        Entity breaker,
        ItemStack tool,
        List<ItemEntity> drops) {
      this.level = level;
      this.pos = pos;
      this.state = state;
      this.blockEntity = blockEntity;
      this.breaker = breaker;
      this.tool = tool;
      this.drops = drops;
    }

    public ServerLevel getLevel() {
      return level;
    }

    public BlockPos getPos() {
      return pos;
    }

    public BlockState getState() {
      return state;
    }

    public BlockEntity getBlockEntity() {
      return blockEntity;
    }

    public Entity getBreaker() {
      return breaker;
    }

    public ItemStack getTool() {
      return tool;
    }

    public List<ItemEntity> getDrops() {
      return drops;
    }

    public void setDroppedExperience(int n) {
      dropExperience = n > 0;
    }

    public boolean shouldDropExperience() {
      return dropExperience;
    }
  }
}
