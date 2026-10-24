package dev.creationcore.mixin;

import dev.creationcore.event.PersistentData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Entity.class)
public abstract class EntityPersistentDataMixin implements PersistentData {
  @Unique private CompoundTag creationcore$data = new CompoundTag();

  public CompoundTag creationcore$data() {
    return creationcore$data;
  }

  @Inject(method = "saveWithoutId", at = @At("RETURN"))
  private void creationcore$save(CompoundTag tag, CallbackInfoReturnable<CompoundTag> ci) {
    if (!creationcore$data.isEmpty()) tag.put("creationcore:data", creationcore$data.copy());
  }

  @Inject(method = "load", at = @At("HEAD"))
  private void creationcore$load(CompoundTag tag, CallbackInfo ci) {
    creationcore$data = tag.getCompound("creationcore:data").copy();
  }
}
