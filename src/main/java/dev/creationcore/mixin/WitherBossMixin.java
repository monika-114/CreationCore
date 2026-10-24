package dev.creationcore.mixin;

import dev.creationcore.entity.SpecialWitherAccess;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherBoss.class)
public abstract class WitherBossMixin implements SpecialWitherAccess {
    @Unique private static final EntityDataAccessor<Boolean> creationcore$SPECIAL =
            SynchedEntityData.defineId(WitherBoss.class,EntityDataSerializers.BOOLEAN);
    @Inject(method="defineSynchedData",at=@At("TAIL"))
    private void creationcore$define(SynchedEntityData.Builder builder,CallbackInfo ci) {
        builder.define(creationcore$SPECIAL,false);
    }
    @Inject(method="aiStep",at=@At("TAIL"))
    private void creationcore$sync(CallbackInfo ci) {
        WitherBoss self=(WitherBoss)(Object)this;
        if(!self.level().isClientSide)self.getEntityData().set(creationcore$SPECIAL,
                self.getPersistentData().getBoolean("creationcore_wither_charged"));
    }
    @Override public boolean creationcore$isSpecial() {
        return ((WitherBoss)(Object)this).getEntityData().get(creationcore$SPECIAL);
    }
}
