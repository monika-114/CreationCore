package dev.creationcore.mixin;

import dev.creationcore.event.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public class ExplosionRitualMixin {
  @Shadow @Final private Level level;
  @Shadow @Final private double x;
  @Shadow @Final private double y;
  @Shadow @Final private double z;
  @Shadow @Final private float radius;

  @Inject(method = "explode", at = @At("HEAD"))
  private void creationcore$birth(CallbackInfo ci) {
    double r = radius * 2;
    CoreGameplayEvents.onWitherBirthExplosion(
        new GameplayHooks.ExplosionDetonate(
            level,
            (Explosion) (Object) this,
            level.getEntities((Entity) null, new AABB(x - r, y - r, z - r, x + r, y + r, z + r))));
  }
}
