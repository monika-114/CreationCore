package dev.creationcore.particle;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Client emitter: per-tick center velocity, tint, duration and ritual generation. */
public record InwardParticleOptions(float dx, float dy, float dz, int color, int lifetime, int group) implements ParticleOptions {
    public static final MapCodec<InwardParticleOptions> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("dx").forGetter(InwardParticleOptions::dx),
            Codec.FLOAT.fieldOf("dy").forGetter(InwardParticleOptions::dy),
            Codec.FLOAT.fieldOf("dz").forGetter(InwardParticleOptions::dz),
            Codec.INT.fieldOf("color").forGetter(InwardParticleOptions::color),
            Codec.intRange(1, 80).fieldOf("lifetime").forGetter(InwardParticleOptions::lifetime),
            Codec.INT.fieldOf("group").forGetter(InwardParticleOptions::group)).apply(i, InwardParticleOptions::new));
    public static final StreamCodec<ByteBuf, InwardParticleOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, InwardParticleOptions::dx, ByteBufCodecs.FLOAT, InwardParticleOptions::dy,
            ByteBufCodecs.FLOAT, InwardParticleOptions::dz, ByteBufCodecs.INT, InwardParticleOptions::color,
            ByteBufCodecs.VAR_INT, InwardParticleOptions::lifetime, ByteBufCodecs.INT, InwardParticleOptions::group,
            InwardParticleOptions::new);
    @Override public ParticleType<?> getType() { return ModParticles.INWARD.get(); }
}
