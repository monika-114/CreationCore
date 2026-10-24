package dev.creationcore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.creationcore.entity.SpecialWitherAccess;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WitherBossRenderer;
import net.minecraft.world.entity.boss.wither.WitherBoss;

/** Palette-only appearance: keep vanilla UVs, facial features and all model layers. */
public final class SpecialWitherRenderer extends WitherBossRenderer {
  public SpecialWitherRenderer(EntityRendererProvider.Context context) {
    super(context);
  }

  @Override
  public void render(
      WitherBoss wither,
      float yaw,
      float partialTick,
      PoseStack pose,
      MultiBufferSource buffers,
      int light) {
    if (((SpecialWitherAccess) wither).creationcore$isSpecial()) {
      super.render(
          wither,
          yaw,
          partialTick,
          pose,
          type -> new PurpleConsumer(buffers.getBuffer(type)),
          light);
    } else super.render(wither, yaw, partialTick, pose, buffers, light);
  }

  private record PurpleConsumer(VertexConsumer delegate) implements VertexConsumer {
    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
      delegate.addVertex(x, y, z);
      return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
      delegate.setColor(r * 185 / 255, g * 137 / 255, b * 220 / 255, a);
      return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
      delegate.setUv(u, v);
      return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
      delegate.setUv1(u, v);
      return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
      delegate.setUv2(u, v);
      return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
      delegate.setNormal(x, y, z);
      return this;
    }
  }
}
