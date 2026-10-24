import dev.creationcore.entity.CoreMotion;

/** Run without Minecraft to verify the production curve, including frame-by-frame continuity. */
public final class CoreMotionCheck {
    private static void close(double actual, double expected) {
        if (Math.abs(actual - expected) > 1e-9) throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) {
        close(CoreMotion.verticalOffset(0), 0.5);
        close(CoreMotion.verticalOffset(1.5), 1);
        close(CoreMotion.verticalOffset(3), 0.5);
        close(CoreMotion.verticalOffset(4.5), 0);
        close(CoreMotion.verticalOffset(6), 0.5);
        for (int i = 0; i < 3600; i++) {
            double t = i / 60.0;
            double y = CoreMotion.verticalOffset(t);
            if (y < 0 || y > 1) throw new AssertionError("Out of range");
            close(y, CoreMotion.verticalOffset(t + 6));
            if (Math.abs(CoreMotion.verticalOffset(t + 1.0/60) - y) > 1.0/180 + 1e-9)
                throw new AssertionError("Discontinuous animation");
        }
        System.out.println("PASS: center start, endpoints, 6-second period, continuous 60 FPS movement at 1/3 block/s.");
    }
}
