package dev.creationcore.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

/**
 * Temporary carrier used only while an “empty” bucket returns from the void.
 *
 * The motion intentionally follows ExecutiveOrders' void-transmutation style: the entity gets a
 * single upward launch with gravity disabled, then vanilla ItemEntity drag handles the visible
 * deceleration. The gameplay event stops it at Creation Core's remembered return height.
 */
public final class VoidBucketEntity extends ItemEntity {
    /** ExecutiveOrders uses an initial +1.5 Y velocity for its returned transmutation item. */
    public static final double EXECUTIVE_LAUNCH_SPEED = 1.5D;

    /**
     * Vanilla ItemEntity drag is approximately 0.98 per tick. A launch travels about v/(1-0.98),
     * so 0.024 per block gives a little headroom when the target is farther away than the
     * Executive baseline launch can naturally reach (mainly Overworld/Nether).
     */
    public static final double DRAG_COMPENSATION = 0.024D;

    public VoidBucketEntity(EntityType<? extends ItemEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void onBelowWorld() {
        // The return may begin below the build floor in the Overworld/Nether. It must survive
        // there long enough for the one-shot upward launch to bring it back into the world.
    }
}
