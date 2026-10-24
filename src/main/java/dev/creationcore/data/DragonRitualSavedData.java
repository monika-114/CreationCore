package dev.creationcore.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public final class DragonRitualSavedData extends SavedData {
  private static final String FILE_NAME = "creationcore_dragon_ritual";
  private static final String PENDING = "pending";
  private static final String ORIGIN_X = "origin_x";
  private static final String ORIGIN_Z = "origin_z";

  private boolean pending;
  private int originX;
  private int originZ;
  private double sourceY;
  private double targetY;
  private long movementStart = -1;
  private int particleGroup;
  private long nextPortalSearch = Long.MIN_VALUE;
  private long nextOutputAttempt = Long.MIN_VALUE;

  public static DragonRitualSavedData get(ServerLevel level) {
    return level
        .getDataStorage()
        .computeIfAbsent(
            new SavedData.Factory<>(
                DragonRitualSavedData::new,
                DragonRitualSavedData::load,
                DataFixTypes.SAVED_DATA_COMMAND_STORAGE),
            FILE_NAME);
  }

  private static DragonRitualSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
    DragonRitualSavedData data = new DragonRitualSavedData();
    data.pending = tag.getBoolean(PENDING);
    data.originX = tag.getInt(ORIGIN_X);
    data.originZ = tag.getInt(ORIGIN_Z);
    data.sourceY = tag.contains("source_y") ? tag.getDouble("source_y") : 64.5D;
    data.targetY = tag.getDouble("target_y");
    data.movementStart = tag.contains("movement_start") ? tag.getLong("movement_start") : -1;
    data.particleGroup = tag.getInt("particle_group");
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    tag.putBoolean(PENDING, pending);
    tag.putInt(ORIGIN_X, originX);
    tag.putInt(ORIGIN_Z, originZ);
    tag.putDouble("source_y", sourceY);
    tag.putDouble("target_y", targetY);
    tag.putLong("movement_start", movementStart);
    tag.putInt("particle_group", particleGroup);
    return tag;
  }

  public void begin(BlockPos origin, double sourceY, int particleGroup) {
    pending = true;
    originX = origin.getX();
    originZ = origin.getZ();
    this.sourceY = sourceY;
    this.particleGroup = particleGroup == 0 ? 1 : particleGroup;
    movementStart = -1;
    nextPortalSearch = Long.MIN_VALUE;
    nextOutputAttempt = Long.MIN_VALUE;
    setDirty();
  }

  public int sourceHeight() {
    return (int) Math.floor(sourceY);
  }

  public boolean shouldSearchPortal(long now) {
    if (now < nextPortalSearch) return false;
    nextPortalSearch = now + 10;
    return true;
  }

  public boolean shouldAttemptOutput(long now) {
    if (now < nextOutputAttempt) return false;
    nextOutputAttempt = now + 20;
    return true;
  }

  public boolean isMoving() {
    return movementStart >= 0;
  }

  public double targetY() {
    return targetY;
  }

  public int particleGroup() {
    return particleGroup;
  }

  public void startMovement(long now, double targetY) {
    movementStart = now;
    this.targetY = targetY;
    setDirty();
  }

  public boolean movementComplete(long now) {
    return isMoving() && now - movementStart >= 80;
  }

  public net.minecraft.world.phys.Vec3 particleCenter(long now) {
    double progress = isMoving() ? Math.min(1D, Math.max(0D, (now - movementStart) / 80.0)) : 0;
    return new net.minecraft.world.phys.Vec3(
        originX + 0.5, sourceY + (targetY - sourceY) * progress, originZ + 0.5);
  }

  public boolean isPending() {
    return pending;
  }

  public BlockPos originAt(int y) {
    return new BlockPos(originX, y, originZ);
  }

  public void complete() {
    pending = false;
    setDirty();
  }
}
