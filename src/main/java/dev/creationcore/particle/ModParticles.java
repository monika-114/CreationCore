package dev.creationcore.particle;

import com.mojang.serialization.MapCodec;
import dev.creationcore.registry.Registrations;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public final class ModParticles {
  private static final Registrations<ParticleType<?>> TYPES =
      new Registrations<>(net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE);
  public static final Supplier<SimpleParticleType> VOID_RIPPLE =
      TYPES.register("void_ripple", () -> new SimpleParticleType(true));
  public static final Supplier<ParticleType<InwardParticleOptions>> INWARD =
      TYPES.register(
          "ritual_inward",
          () ->
              new ParticleType<InwardParticleOptions>(false) {
                @Override
                public MapCodec<InwardParticleOptions> codec() {
                  return InwardParticleOptions.CODEC;
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, InwardParticleOptions>
                    streamCodec() {
                  return InwardParticleOptions.STREAM_CODEC;
                }
              });

  public static void register() {
    TYPES.register();
  }

  private ModParticles() {}
}
