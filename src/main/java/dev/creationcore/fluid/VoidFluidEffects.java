package dev.creationcore.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class VoidFluidEffects {
    public static void disperse(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + .5, pos.getY() + .5,
                    pos.getZ() + .5, 8, .25, .25, .25, .025);
        }
    }
    private VoidFluidEffects() {}
}
