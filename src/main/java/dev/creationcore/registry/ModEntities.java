package dev.creationcore.registry;

import dev.creationcore.CreativeCoreMod;
import dev.creationcore.entity.CreativeCoreEntity;
import dev.creationcore.entity.VoidBucketEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
  public static final Registrations<EntityType<?>> ENTITIES =
      new Registrations<>(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE);

  public static final Registrations.Entry<EntityType<CreativeCoreEntity>> CREATIVE_CORE =
      ENTITIES.register(
          "creative_core",
          () ->
              EntityType.Builder.<CreativeCoreEntity>of(CreativeCoreEntity::new, MobCategory.MISC)
                  .sized(0.5F, 0.5F)
                  .clientTrackingRange(10)
                  .updateInterval(20)
                  .build(CreativeCoreMod.MODID + ":creative_core"));

  public static final Registrations.Entry<EntityType<VoidBucketEntity>> VOID_BUCKET_RETURN =
      ENTITIES.register(
          "void_bucket_return",
          () ->
              EntityType.Builder.<VoidBucketEntity>of(VoidBucketEntity::new, MobCategory.MISC)
                  .sized(0.25F, 0.25F)
                  .clientTrackingRange(10)
                  .updateInterval(1)
                  .build(CreativeCoreMod.MODID + ":void_bucket_return"));

  private ModEntities() {}

  public static void register() {
    ENTITIES.register();
  }
}
