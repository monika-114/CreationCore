package dev.creationcore.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;

/** Five broad, feathered wave crests per event, visible from above and below. */
public final class VoidRippleParticle extends TextureSheetParticle {
  private static final int RING_COUNT = 5;
  private static final int RING_DELAY = 8;
  private static final int RING_DURATION = 60;
  private static final int SEGMENTS = 64;
  private static final float[] COS = new float[SEGMENTS + 1];
  private static final float[] SIN = new float[SEGMENTS + 1];

  static {
    for (int i = 0; i <= SEGMENTS; i++) {
      double angle = i * Math.PI * 2 / SEGMENTS;
      COS[i] = (float) Math.cos(angle);
      SIN[i] = (float) Math.sin(angle);
    }
  }

  private static final double MAX_RADIUS = 10D;
  private static final double RING_WIDTH = 0.45D;

  private VoidRippleParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
    super(level, x, y, z);
    lifetime = RING_DURATION + (RING_COUNT - 1) * RING_DELAY;
    hasPhysics = false;
    xd = yd = zd = 0;
    pickSprite(sprites);
    // Keep the full twenty-block wave within the particle culling box.
    setSize(20.2F, 0.2F);
  }

  @Override
  public ParticleRenderType getRenderType() {
    return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
  }

  @Override
  public void render(VertexConsumer vertices, Camera camera, float partialTick) {
    Vec3 c = new Vec3(x, y, z).subtract(camera.getPosition());
    // Sample the opaque sprite center; radial alpha supplies a continuous soft edge.
    float u = (getU0() + getU1()) * 0.5F;
    float v = (getV0() + getV1()) * 0.5F;
    for (int ring = 0; ring < RING_COUNT; ring++) {
      float ringAge = age + partialTick - ring * RING_DELAY;
      if (ringAge <= 0 || ringAge >= RING_DURATION) continue;
      float t = ringAge / RING_DURATION;
      double outer = MAX_RADIUS * t;
      double inner = Math.max(0, outer - RING_WIDTH);
      double crest = (inner + outer) * 0.5D;
      float opacity = Math.min(1F, ringAge / 5F) * (1F - t * t) * 0.85F;
      for (int i = 0; i < SEGMENTS; i++) {
        band(vertices, c, i, i + 1, inner, crest, u, v, 0F, opacity);
        band(vertices, c, i, i + 1, crest, outer, u, v, opacity, 0F);
      }
    }
  }

  private static void band(
      VertexConsumer vertices,
      Vec3 c,
      int a,
      int b,
      double inner,
      double outer,
      float u,
      float v,
      float innerAlpha,
      float outerAlpha) {
    vertex(vertices, c, a, inner, u, v, innerAlpha);
    vertex(vertices, c, a, outer, u, v, outerAlpha);
    vertex(vertices, c, b, outer, u, v, outerAlpha);
    vertex(vertices, c, b, inner, u, v, innerAlpha);
    vertex(vertices, c, b, inner, u, v, innerAlpha);
    vertex(vertices, c, b, outer, u, v, outerAlpha);
    vertex(vertices, c, a, outer, u, v, outerAlpha);
    vertex(vertices, c, a, inner, u, v, innerAlpha);
  }

  private static void vertex(
      VertexConsumer v, Vec3 c, int angle, double radius, float u, float w, float alpha) {
    v.addVertex(
            (float) (c.x + COS[angle] * radius), (float) c.y, (float) (c.z + SIN[angle] * radius))
        .setUv(u, w)
        .setColor(1F, 1F, 1F, alpha)
        .setLight(240);
  }

  public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
    @Override
    public Particle createParticle(
        SimpleParticleType data,
        ClientLevel level,
        double x,
        double y,
        double z,
        double vx,
        double vy,
        double vz) {
      return new VoidRippleParticle(level, x, y, z, sprites);
    }
  }
}
