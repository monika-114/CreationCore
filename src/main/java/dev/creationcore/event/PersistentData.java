package dev.creationcore.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public interface PersistentData {
  CompoundTag creationcore$data();

  static CompoundTag get(Entity e) {
    return ((PersistentData) e).creationcore$data();
  }
}
