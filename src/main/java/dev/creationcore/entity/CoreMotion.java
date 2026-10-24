package dev.creationcore.entity;

/** Shared deterministic animation curve, sampled every tick and every rendered frame. */
public final class CoreMotion {
    private CoreMotion() {}

    public static double verticalOffset(double seconds) {
        double phase = (Math.max(0.0D, seconds) + 1.5D) % 6.0D;
        return phase <= 3.0D ? phase / 3.0D : (6.0D - phase) / 3.0D;
    }
}
