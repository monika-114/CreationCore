package dev.creationcore.client;

import dev.creationcore.particle.InwardParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.WeakHashMap;

/** Vanilla instant_effect sprites; a short client emitter keeps network traffic bounded. */
public final class RitualInwardParticle extends TextureSheetParticle {
    private static final Map<ClientLevel, ActiveGroup> ACTIVE = new WeakHashMap<>();
    // The value must not retain ClientLevel: a map value pointing back at its weak key leaks worlds.
    private static final class ActiveGroup {
        final int id;
        Vec3 center;
        ActiveGroup(int id, Vec3 center) { this.id = id; this.center = center; }
    }
    private final SpriteSet sprites;
    private final int group;

    private RitualInwardParticle(ClientLevel level, Vec3 position, Emitter emitter) {
        super(level, position.x, position.y, position.z);
        this.sprites = emitter.sprites;
        this.group = emitter.options.group();
        lifetime = 12 + random.nextInt(9);
        hasPhysics = false;
        quadSize = 0.06F + random.nextFloat() * 0.055F;
        int color = emitter.options.color();
        setColor(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F);
        setSpriteFromAge(sprites);
    }

    @Override public void tick() {
        ActiveGroup active = ACTIVE.get(level);
        if (active == null || active.id != group) { remove(); return; }
        xo = x; yo = y; zo = z;
        if (age++ >= lifetime) { remove(); return; }
        double remaining = Math.max(1, lifetime - age + 1);
        Vec3 target = active.center;
        setPos(x + (target.x - x) / remaining, y + (target.y - y) / remaining, z + (target.z - z) / remaining);
        alpha = Math.min(1F, (lifetime - age) / 5F);
        setSpriteFromAge(sprites);
    }

    @Override protected int getLightColor(float partialTick) { return 240; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    private static final class Emitter extends NoRenderParticle {
        private final InwardParticleOptions options;
        private final SpriteSet sprites;
        private final ActiveGroup active;

        private Emitter(ClientLevel level, double x, double y, double z,
                        InwardParticleOptions options, SpriteSet sprites) {
            super(level, x, y, z);
            this.options = options;
            this.sprites = sprites;
            active = new ActiveGroup(options.group(), new Vec3(x, y, z));
            lifetime = options.lifetime();
            ACTIVE.put(level, active);
        }

        @Override public void tick() {
            // New packets supersede an older emitter even within the same ritual generation.
            if (ACTIVE.get(level) != active || age++ >= lifetime) { remove(); return; }
            for (int i = 0; i < 4; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double vertical = random.nextDouble() * 2 - 1;
                double radius = 0.3 + random.nextDouble() * 1.7;
                double horizontal = Math.sqrt(1 - vertical * vertical) * radius;
                Vec3 position = active.center.add(Math.cos(angle) * horizontal, vertical * radius,
                        Math.sin(angle) * horizontal);
                net.minecraft.client.Minecraft.getInstance().particleEngine.add(
                        new RitualInwardParticle(level, position, this));
            }
            active.center = active.center.add(options.dx(), options.dy(), options.dz());
        }
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<InwardParticleOptions> {
        @Override public Particle createParticle(InwardParticleOptions data, ClientLevel level,
                double x, double y, double z, double vx, double vy, double vz) {
            return new Emitter(level, x, y, z, data, sprites);
        }
    }
}
