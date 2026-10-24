package dev.creationcore.entity;

import dev.creationcore.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CreativeCoreEntity extends ItemEntity {
  private static final EntityDataAccessor<Long> MOTION_START =
      SynchedEntityData.defineId(CreativeCoreEntity.class, EntityDataSerializers.LONG);
  private static final EntityDataAccessor<Float> BASE_Y =
      SynchedEntityData.defineId(CreativeCoreEntity.class, EntityDataSerializers.FLOAT);
  private double anchorX, anchorY, anchorZ;
  private boolean anchored;

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    super.defineSynchedData(builder);
    builder.define(MOTION_START, 0L);
    builder.define(BASE_Y, 0.0F);
  }

  public double motionSeconds(float partialTick) {
    return Math.max(
        0.0, (level().getGameTime() - entityData.get(MOTION_START) + partialTick) / 20.0);
  }

  /** A six-second triangular wave: continuous position, constant ascent/descent speed. */
  public double animatedY(float partialTick) {
    return entityData.get(BASE_Y) + CoreMotion.verticalOffset(motionSeconds(partialTick));
  }

  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    tag.putBoolean("CoreAnchored", anchored);
    tag.putDouble("CoreAnchorX", anchorX);
    tag.putDouble("CoreAnchorY", anchorY);
    tag.putDouble("CoreAnchorZ", anchorZ);
    tag.putLong("CoreMotionStart", entityData.get(MOTION_START));
  }

  @Override
  public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);
    anchored = tag.getBoolean("CoreAnchored");
    anchorX = tag.getDouble("CoreAnchorX");
    anchorY = tag.getDouble("CoreAnchorY");
    anchorZ = tag.getDouble("CoreAnchorZ");
    entityData.set(MOTION_START, tag.getLong("CoreMotionStart"));
    entityData.set(BASE_Y, (float) anchorY);
    initialize();
  }

  private static final double CONVERSION_RADIUS = 5.0D;
  private static final double CONVERSION_RADIUS_SQR = CONVERSION_RADIUS * CONVERSION_RADIUS;

  public CreativeCoreEntity(EntityType<? extends ItemEntity> type, Level level) {
    super(type, level);
    initialize();
  }

  public CreativeCoreEntity(Level level, double x, double y, double z) {
    this(dev.creationcore.registry.ModEntities.CREATIVE_CORE.get(), level);
    setPos(x, y, z);
    anchorX = x;
    anchorY = y - 0.5;
    anchorZ = z;
    anchored = true;
    entityData.set(MOTION_START, level.getGameTime());
    entityData.set(BASE_Y, (float) anchorY);
    setItem(new ItemStack(ModItems.CREATIVE_CORE.get()));
    initialize();
  }

  private void initialize() {
    noPhysics = true;
    setNoGravity(true);
    setInvulnerable(true);
    setGlowingTag(true);
    setNeverPickUp();
    setUnlimitedLifetime();
    setDeltaMovement(0, 0, 0);
  }

  @Override
  public void tick() {
    setDeltaMovement(0, 0, 0);
    super.tick();

    // The entity form is only a persistent, highlighted world marker. It must not
    // behave like a normal dropped item until a player approaches it.
    setNoGravity(true);
    setInvulnerable(true);
    setGlowingTag(true);
    setNeverPickUp();
    setUnlimitedLifetime();
    setDeltaMovement(0, 0, 0);

    if (!level().isClientSide) {
      if (!anchored) {
        anchorX = getX();
        anchorY = getY() - 0.5;
        anchorZ = getZ();
        anchored = true;
        entityData.set(MOTION_START, level().getGameTime());
        entityData.set(BASE_Y, (float) anchorY);
      }
      setPos(anchorX, animatedY(0), anchorZ);
      if (tickCount % 3 == 0)
        dev.creationcore.particle.RitualEffects.core(
            (net.minecraft.server.level.ServerLevel) level(), position(), tickCount);
      boolean playerNearby =
          !level()
              .getEntitiesOfClass(
                  Player.class,
                  getBoundingBox().inflate(CONVERSION_RADIUS),
                  player ->
                      !player.isSpectator() && player.distanceToSqr(this) <= CONVERSION_RADIUS_SQR)
              .isEmpty();

      if (playerNearby) {
        convertToDroppedItem();
      }
    }
  }

  private void convertToDroppedItem() {
    ItemEntity dropped =
        new ItemEntity(
            level(), getX(), getY(), getZ(), new ItemStack(ModItems.CREATIVE_CORE.get()));
    dropped.setDeltaMovement(0, 0, 0);
    dropped.setNoPickUpDelay();

    // Only remove the persistent core after the replacement item was accepted by
    // the level, preventing the core from disappearing if entity spawning fails.
    if (level().addFreshEntity(dropped)) {
      discard();
    }
  }

  /**
   * Eye-of-Ender-like selection behaviour: the entity form cannot be targeted by the player's
   * crosshair or normal melee interaction.
   */
  @Override
  public boolean isPickable() {
    return false;
  }

  /** Prevent arrows, tridents and other projectile hit tests from selecting it. */
  @Override
  public boolean canBeHitByProjectile() {
    return false;
  }

  /** Stop the player attack pipeline before damage/interaction code is entered. */
  @Override
  public boolean isAttackable() {
    return false;
  }

  /** Additional guard for attack interactions from players or modded callers. */
  @Override
  public boolean skipAttackInteraction(Entity entity) {
    return true;
  }

  /** Final damage guard for non-standard/modded damage paths. */
  @Override
  public boolean hurt(DamageSource source, float amount) {
    return false;
  }
}
